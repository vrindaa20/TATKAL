package com.tatkalx.service;

import com.tatkalx.exception.BookingTimeoutException;
import com.tatkalx.exception.BookingValidationException;
import com.tatkalx.exception.DuplicateBookingException;
import com.tatkalx.exception.PaymentFailedException;
import com.tatkalx.exception.SeatUnavailableException;
import com.tatkalx.exception.TatkalWindowClosedException;
import com.tatkalx.model.Berth;
import com.tatkalx.model.BookingEvent;
import com.tatkalx.model.BookingRequest;
import com.tatkalx.model.Passenger;
import com.tatkalx.model.Payment;
import com.tatkalx.model.PnrRecord;
import com.tatkalx.model.SeatInventory;
import com.tatkalx.model.Ticket;
import com.tatkalx.model.TransactionRecord;
import com.tatkalx.simulation.SimulationSessionContext;
import com.tatkalx.utility.FareCalculator;
import com.tatkalx.utility.IdGenerator;
import com.tatkalx.utility.InputValidator;
import com.tatkalx.utility.TimeUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.ListIterator;

/**
 * Atomic Seat Allocation Controller.
 * Protects check-then-act seat decrement, physical berth assignment, RAC/WL overflow,
 * payment verification, and rollback-on-failure using synchronized critical sections.
 */
public class BerthAllocationService {

    private final PaymentService paymentService;
    private final NotificationService notificationService;

    public BerthAllocationService(PaymentService paymentService, NotificationService notificationService) {
        this.paymentService = paymentService;
        this.notificationService = notificationService;
    }

    public BookingOutcome allocateAndConfirm(BookingRequest request,
                                             SimulationSessionContext context,
                                             String paymentMethod) {
        long startMs = System.currentTimeMillis();
        List<BookingEvent> events = new ArrayList<>();
        int seq = 1;
        String threadName = Thread.currentThread().getName();
        request.setThreadName(threadName);

        events.add(new BookingEvent(request.getBookingId(), context.getSessionId(), request.getPassengerId(),
                threadName, seq++, "REQUEST_RECEIVED", "Booking request received by processor",
                request.getStatus(), context.getSeatInventory().getAvailableTatkalSeats(),
                request.getQueuePosition(), null, null, TimeUtils.nowTimestamp()));

        boolean duplicateKeyAcquired = false;
        SeatInventory inventory = context.getSeatInventory();

        try {
            // 1. Validate request inputs & passenger existence
            InputValidator.validateBookingRequestFields(
                    request.getPassengerId(), request.getScheduleId(),
                    request.getCoachClass(), request.getRequestedSeats());

            Passenger passenger = context.getPassengerRegistry().findById(request.getPassengerId());
            if (passenger == null) {
                throw new BookingValidationException("Passenger not found: id=" + request.getPassengerId());
            }
            if (!passenger.isVerified()) {
                throw new BookingValidationException("Passenger ID proof is not verified for Tatkal booking.");
            }

            // 2. Enforce Tatkal virtual clock window (10:00 AM - 11:00 AM)
            context.getVirtualClock().assertWindowOpen();
            events.add(new BookingEvent(request.getBookingId(), context.getSessionId(), request.getPassengerId(),
                    threadName, seq++, "CLOCK_VERIFIED", "Tatkal virtual clock verified OPEN",
                    "PROCESSING", inventory.getAvailableTatkalSeats(), request.getQueuePosition(),
                    null, null, TimeUtils.nowTimestamp()));

            // 3. Duplicate booking prevention (HashSet guard)
            context.getDuplicateGuard().acquireOrThrow(
                    context.getSessionId(), request.getScheduleId(), request.getPassengerId());
            duplicateKeyAcquired = true;

            request.setStatus("PROCESSING");
            events.add(new BookingEvent(request.getBookingId(), context.getSessionId(), request.getPassengerId(),
                    threadName, seq++, "DUPLICATE_CHECK_PASSED", "No duplicate booking found for passenger",
                    "PROCESSING", inventory.getAvailableTatkalSeats(), request.getQueuePosition(),
                    null, null, TimeUtils.nowTimestamp()));

            // 4. Critical Section: atomic seat check & temporary berth hold
            String tentativeStatus;
            List<Berth> heldBerths = new ArrayList<>();
            int preLockAvail;

            synchronized (inventory.getMonitorLock()) {
                preLockAvail = inventory.getAvailableTatkalSeats();
                events.add(new BookingEvent(request.getBookingId(), context.getSessionId(), request.getPassengerId(),
                        threadName, seq++, "CRITICAL_SECTION_ENTERED",
                        "Entered synchronized(inventoryLock); available=" + preLockAvail,
                        "PROCESSING", preLockAvail, request.getQueuePosition(),
                        null, null, TimeUtils.nowTimestamp()));

                int needed = request.getRequestedSeats();
                if (inventory.getAvailableTatkalSeats() >= needed) {
                    tentativeStatus = "CONFIRMED";
                    for (int i = 0; i < needed; i++) {
                        Berth b = pickPreferredBerth(inventory, passenger.getBerthPreference());
                        heldBerths.add(b);
                        inventory.getOccupiedBerthIds().add(b.getBerthId());
                    }
                    inventory.setAvailableTatkalSeats(inventory.getAvailableTatkalSeats() - needed);
                    inventory.setConfirmedCount(inventory.getConfirmedCount() + needed);
                    inventory.incrementVersion();
                } else if (needed == 1 && inventory.getRacAllocated() < inventory.getRacCapacity()) {
                    tentativeStatus = "RAC";
                    inventory.setRacAllocated(inventory.getRacAllocated() + 1);
                    inventory.incrementVersion();
                } else if (needed == 1 && inventory.getWlAllocated() < inventory.getWlCapacity()) {
                    tentativeStatus = "WL";
                    inventory.setWlAllocated(inventory.getWlAllocated() + 1);
                    inventory.incrementVersion();
                } else {
                    throw new SeatUnavailableException(
                            "No Confirmed/RAC/WL quota available (avail=" + inventory.getAvailableTatkalSeats()
                                    + ", rac=" + inventory.getRacAllocated() + "/" + inventory.getRacCapacity()
                                    + ", wl=" + inventory.getWlAllocated() + "/" + inventory.getWlCapacity() + ")");
                }

                events.add(new BookingEvent(request.getBookingId(), context.getSessionId(), request.getPassengerId(),
                        threadName, seq++, "SEAT_HELD",
                        "Provisionally reserved status=" + tentativeStatus + " (" + heldBerths.size() + " berths)",
                        "PROCESSING", inventory.getAvailableTatkalSeats(), request.getQueuePosition(),
                        tentativeStatus, null, TimeUtils.nowTimestamp()));
            }

            // 5. Payment Simulation (outside inventory lock so other threads don't stall on payment latency,
            //    with strict rollback inside lock if payment fails or times out!)
            double totalFare = FareCalculator.calculateTotalFare(
                    context.getSchedule().getTotalDistanceKm(), context.getCoach(), request.getRequestedSeats());

            events.add(new BookingEvent(request.getBookingId(), context.getSessionId(), request.getPassengerId(),
                    threadName, seq++, "PAYMENT_INITIATED",
                    String.format("Initiating payment of Rs %.2f via %s", totalFare, paymentMethod),
                    "PROCESSING", inventory.getAvailableTatkalSeats(), request.getQueuePosition(),
                    tentativeStatus, "INITIATED", TimeUtils.nowTimestamp()));

            Payment payment;
            try {
                payment = paymentService.processPayment(
                        request.getBookingId(), context.getSessionId(), passenger, totalFare, paymentMethod);
            } catch (PaymentFailedException | BookingTimeoutException payEx) {
                // Rollback held seat / RAC / WL slot atomically so zero seats are ever leaked!
                rollbackProvisionalHold(inventory, tentativeStatus, heldBerths);
                context.getDuplicateGuard().releaseKey(
                        context.getSessionId(), request.getScheduleId(), request.getPassengerId());
                duplicateKeyAcquired = false;

                String failStatus = (payEx instanceof BookingTimeoutException) ? "TIMEOUT" : "FAILED";
                request.setStatus(failStatus);
                request.setFailureReason(payEx.getMessage());
                request.setProcessingTimeMs(System.currentTimeMillis() - startMs);

                events.add(new BookingEvent(request.getBookingId(), context.getSessionId(), request.getPassengerId(),
                        threadName, seq++, "PAYMENT_FAILED_ROLLBACK",
                        "Payment failed (" + payEx.getMessage() + "); released held resources",
                        failStatus, inventory.getAvailableTatkalSeats(), request.getQueuePosition(),
                        "ROLLED_BACK", failStatus, TimeUtils.nowTimestamp()));

                context.getHistoryCache().recordBooking(request);
                context.appendBookingEvents(request.getBookingId(), events);
                context.recordExecutionOrder(threadName + " -> " + request.getBookingId() + " [" + failStatus + "]");
                return new BookingOutcome(false, failStatus, payEx.getMessage(), request, null, null, null, events);
            }

            events.add(new BookingEvent(request.getBookingId(), context.getSessionId(), request.getPassengerId(),
                    threadName, seq++, "PAYMENT_SUCCESS",
                    "Payment approved ref=" + payment.getGatewayRef(),
                    "PROCESSING", inventory.getAvailableTatkalSeats(), request.getQueuePosition(),
                    tentativeStatus, "SUCCESS", TimeUtils.nowTimestamp()));

            // 6. Finalize Ticket, Queue entry (if RAC/WL), PNR, Transaction, Notification
            long ticketId = IdGenerator.nextTicketId();
            String seatLabel = buildSeatLabel(heldBerths);
            Ticket ticket = new Ticket(
                    ticketId, request.getBookingId(), context.getSessionId(),
                    request.getPassengerId(), request.getScheduleId(),
                    context.getCoach().getCoachId(), context.getCoach().getCoachCode(),
                    tentativeStatus, 0, seatLabel, totalFare, TimeUtils.nowTimestamp()
            );

            synchronized (inventory.getMonitorLock()) {
                if ("CONFIRMED".equals(tentativeStatus)) {
                    inventory.getAllocatedBerthsByTicket().put(ticketId, heldBerths);
                } else if ("RAC".equals(tentativeStatus)) {
                    context.getWaitingListManager().enqueueRac(ticket);
                } else if ("WL".equals(tentativeStatus)) {
                    context.getWaitingListManager().enqueueWaitingList(ticket);
                }
            }

            String pnrNumber = IdGenerator.nextPnrNumber();
            PnrRecord pnrRecord = new PnrRecord(
                    ticketId, pnrNumber, ticketId, context.getSessionId(),
                    tentativeStatus, false, TimeUtils.nowTimestamp()
            );
            context.getPnrDirectory().registerPnr(pnrRecord);
            context.recordTicket(ticket);
            context.recordPayment(payment);
            context.recordTransaction(new TransactionRecord(
                    IdGenerator.nextTxnId(), context.getSessionId(), passenger.getUserId(),
                    payment.getPaymentId(), null, "DEBIT_BOOKING", totalFare,
                    passenger.getWalletBalance(), "COMMITTED", TimeUtils.nowTimestamp()
            ));

            request.setStatus(tentativeStatus);
            request.setProcessingTimeMs(System.currentTimeMillis() - startMs);
            context.getHistoryCache().recordBooking(request);

            events.add(new BookingEvent(request.getBookingId(), context.getSessionId(), request.getPassengerId(),
                    threadName, seq++, "PNR_GENERATED",
                    "Issued PNR " + pnrNumber + " | Seat: " + ticket.getSeatLabel(),
                    tentativeStatus, inventory.getAvailableTatkalSeats(), ticket.getQueuePosition(),
                    tentativeStatus, "SUCCESS", TimeUtils.nowTimestamp()));

            events.add(new BookingEvent(request.getBookingId(), context.getSessionId(), request.getPassengerId(),
                    threadName, seq, "BOOKING_FINALIZED",
                    "Booking finalized with status " + tentativeStatus,
                    tentativeStatus, inventory.getAvailableTatkalSeats(), ticket.getQueuePosition(),
                    tentativeStatus, "SUCCESS", TimeUtils.nowTimestamp()));

            context.appendBookingEvents(request.getBookingId(), events);
            context.recordExecutionOrder(threadName + " -> " + request.getBookingId()
                    + " [" + tentativeStatus + " | " + ticket.getSeatLabel() + "]");

            notificationService.sendNotification(
                    context.getSessionId(), passenger.getPassengerId(), request.getBookingId(),
                    "Booking " + tentativeStatus + " (PNR " + pnrNumber + ")",
                    "Train " + context.getSchedule().getTrainNumber() + " | Seat: " + ticket.getSeatLabel()
                            + " | Fare: Rs " + String.format("%.2f", totalFare)
            );

            return new BookingOutcome(true, tentativeStatus, "Booking finalized: " + tentativeStatus,
                    request, ticket, pnrRecord, payment, events);

        } catch (DuplicateBookingException dupEx) {
            context.incrementDuplicateAttempts();
            return recordFailureOutcome(request, context, events, seq, threadName, startMs,
                    "FAILED", dupEx.getMessage(), false, duplicateKeyAcquired);
        } catch (TatkalWindowClosedException | BookingValidationException | SeatUnavailableException ex) {
            return recordFailureOutcome(request, context, events, seq, threadName, startMs,
                    "FAILED", ex.getMessage(), true, duplicateKeyAcquired);
        }
    }

    private BookingOutcome recordFailureOutcome(BookingRequest request, SimulationSessionContext context,
                                                List<BookingEvent> events, int seq, String threadName,
                                                long startMs, String status, String reason,
                                                boolean releaseDupKey, boolean duplicateKeyAcquired) {
        if (releaseDupKey && duplicateKeyAcquired) {
            context.getDuplicateGuard().releaseKey(
                    context.getSessionId(), request.getScheduleId(), request.getPassengerId());
        }
        request.setStatus(status);
        request.setFailureReason(reason);
        request.setProcessingTimeMs(System.currentTimeMillis() - startMs);

        events.add(new BookingEvent(request.getBookingId(), context.getSessionId(), request.getPassengerId(),
                threadName, seq, "BOOKING_REJECTED", reason, status,
                context.getSeatInventory().getAvailableTatkalSeats(), request.getQueuePosition(),
                "NONE", "NOT_ATTEMPTED", TimeUtils.nowTimestamp()));

        context.getHistoryCache().recordBooking(request);
        context.appendBookingEvents(request.getBookingId(), events);
        context.recordExecutionOrder(threadName + " -> " + request.getBookingId() + " [REJECTED: " + reason + "]");
        return new BookingOutcome(false, status, reason, request, null, null, null, events);
    }

    private void rollbackProvisionalHold(SeatInventory inventory, String tentativeStatus, List<Berth> heldBerths) {
        synchronized (inventory.getMonitorLock()) {
            if ("CONFIRMED".equals(tentativeStatus)) {
                for (Berth b : heldBerths) {
                    inventory.getOccupiedBerthIds().remove(b.getBerthId());
                    inventory.getAvailableBerthPool().addFirst(b);
                }
                inventory.setAvailableTatkalSeats(inventory.getAvailableTatkalSeats() + heldBerths.size());
                inventory.setConfirmedCount(inventory.getConfirmedCount() - heldBerths.size());
            } else if ("RAC".equals(tentativeStatus)) {
                inventory.setRacAllocated(Math.max(0, inventory.getRacAllocated() - 1));
            } else if ("WL".equals(tentativeStatus)) {
                inventory.setWlAllocated(Math.max(0, inventory.getWlAllocated() - 1));
            }
            inventory.incrementVersion();
        }
    }

    private Berth pickPreferredBerth(SeatInventory inventory, String preference) {
        if (preference != null && !"ANY".equalsIgnoreCase(preference)) {
            ListIterator<Berth> it = inventory.getAvailableBerthPool().listIterator();
            while (it.hasNext()) {
                Berth candidate = it.next();
                if (preference.equalsIgnoreCase(candidate.getBerthType())) {
                    it.remove();
                    return candidate;
                }
            }
        }
        return inventory.getAvailableBerthPool().removeFirst();
    }

    private String buildSeatLabel(List<Berth> berths) {
        if (berths == null || berths.isEmpty()) {
            return "PENDING";
        }
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < berths.size(); i++) {
            if (i > 0) {
                sb.append(", ");
            }
            sb.append(berths.get(i).getFormattedLabel());
        }
        return sb.toString();
    }
}
