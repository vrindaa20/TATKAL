package com.tatkalx.service;

import com.tatkalx.exception.BookingValidationException;
import com.tatkalx.model.Berth;
import com.tatkalx.model.Cancellation;
import com.tatkalx.model.Passenger;
import com.tatkalx.model.Payment;
import com.tatkalx.model.PnrRecord;
import com.tatkalx.model.Refund;
import com.tatkalx.model.SeatInventory;
import com.tatkalx.model.Ticket;
import com.tatkalx.model.TransactionRecord;
import com.tatkalx.simulation.SimulationSessionContext;
import com.tatkalx.utility.IdGenerator;
import com.tatkalx.utility.TimeUtils;

import java.util.List;

/**
 * Handles atomic Ticket Cancellation, Tatkal Refund Calculation, Wallet Credit,
 * and automatic RAC/WL Queue Promotion.
 */
public class CancellationRefundService {

    private static final double CLERICAL_DEDUCTION_PER_TICKET = 60.0;

    private final QueuePromotionService queuePromotionService;
    private final NotificationService notificationService;

    public CancellationRefundService(QueuePromotionService queuePromotionService,
                                     NotificationService notificationService) {
        this.queuePromotionService = queuePromotionService;
        this.notificationService = notificationService;
    }

    public Refund cancelTicketByPnr(String pnrNumber, String reason,
                                    boolean trainCancelled,
                                    SimulationSessionContext context) throws BookingValidationException {
        PnrRecord pnr = context.getPnrDirectory().lookupPnr(pnrNumber);
        if (pnr == null) {
            throw new BookingValidationException("PNR not found: " + pnrNumber);
        }
        Ticket ticket = context.findTicketById(pnr.getTicketId());
        if (ticket == null) {
            throw new BookingValidationException("Ticket not found for PNR: " + pnrNumber);
        }
        if ("CANCELLED".equalsIgnoreCase(ticket.getTicketStatus())) {
            throw new BookingValidationException("Ticket is already CANCELLED for PNR: " + pnrNumber);
        }

        Passenger passenger = context.getPassengerRegistry().findById(ticket.getPassengerId());
        Payment payment = context.findPaymentByBookingId(ticket.getBookingId());
        SeatInventory inventory = context.getSeatInventory();
        String previousStatus = ticket.getTicketStatus();

        synchronized (inventory.getMonitorLock()) {
            ticket.setTicketStatus("CANCELLED");
            ticket.setUpdatedAt(TimeUtils.nowTimestamp());
            context.getPnrDirectory().updateStatus(pnrNumber, "CANCELLED");
            context.getDuplicateGuard().releaseKey(
                    context.getSessionId(), ticket.getScheduleId(), ticket.getPassengerId());

            if ("CONFIRMED".equals(previousStatus)) {
                List<Berth> freedBerths = inventory.getAllocatedBerthsByTicket().remove(ticket.getTicketId());
                if (freedBerths != null) {
                    for (Berth b : freedBerths) {
                        inventory.getOccupiedBerthIds().remove(b.getBerthId());
                        inventory.getAvailableBerthPool().addLast(b);
                        inventory.setAvailableTatkalSeats(inventory.getAvailableTatkalSeats() + 1);
                        inventory.setConfirmedCount(inventory.getConfirmedCount() - 1);
                        // Trigger automatic FIFO promotion for each freed berth!
                        queuePromotionService.promoteNextAfterBerthFreed(context, b);
                    }
                }
            } else {
                context.getWaitingListManager().cancelFromQueues(ticket.getTicketId());
                inventory.setRacAllocated(context.getWaitingListManager().getRacSnapshot().size());
                inventory.setWlAllocated(context.getWaitingListManager().getWlSnapshot().size());
            }
            inventory.incrementVersion();
        }

        // Calculate Tatkal Refund Policy
        double originalAmount = payment == null ? ticket.getTotalFare() : payment.getAmount();
        double deduction;
        double refundAmount;
        String refundStatus;

        if ("CONFIRMED".equals(previousStatus) && !trainCancelled) {
            // IRCTC rule: No refund on confirmed Tatkal ticket cancellation
            deduction = originalAmount;
            refundAmount = 0.0;
            refundStatus = "NO_REFUND";
        } else if (trainCancelled) {
            deduction = 0.0;
            refundAmount = originalAmount;
            refundStatus = "PROCESSED";
        } else {
            // RAC / WL cancellation: full refund minus Rs 60 clerical fee
            deduction = Math.min(originalAmount, CLERICAL_DEDUCTION_PER_TICKET);
            refundAmount = Math.round((originalAmount - deduction) * 100.0) / 100.0;
            refundStatus = "PROCESSED";
        }

        if (refundAmount > 0 && passenger != null) {
            synchronized (passenger) {
                passenger.setWalletBalance(passenger.getWalletBalance() + refundAmount);
            }
        }

        Cancellation cancellation = new Cancellation(
                IdGenerator.nextCancellationId(),
                ticket.getTicketId(),
                context.getSessionId(),
                passenger == null ? 0 : passenger.getUserId(),
                previousStatus,
                reason == null ? "User Requested Cancellation" : reason,
                TimeUtils.nowTimestamp()
        );

        Refund refund = new Refund(
                IdGenerator.nextRefundId(),
                cancellation.getCancellationId(),
                payment == null ? 0L : payment.getPaymentId(),
                originalAmount,
                deduction,
                refundAmount,
                refundStatus,
                TimeUtils.nowTimestamp()
        );

        TransactionRecord refundTxn = null;
        if (refundAmount > 0 && passenger != null) {
            refundTxn = new TransactionRecord(
                    IdGenerator.nextTxnId(),
                    context.getSessionId(),
                    passenger.getUserId(),
                    payment == null ? null : payment.getPaymentId(),
                    refund.getRefundId(),
                    "CREDIT_REFUND",
                    refundAmount,
                    passenger.getWalletBalance(),
                    "COMMITTED",
                    TimeUtils.nowTimestamp()
            );
        }

        context.recordCancellation(cancellation, refund, refundTxn);

        notificationService.sendNotification(
                context.getSessionId(),
                ticket.getPassengerId(),
                ticket.getBookingId(),
                "Ticket Cancelled (PNR " + pnrNumber + ")",
                String.format("Previous: %s | Refund: Rs %.2f (%s)", previousStatus, refundAmount, refundStatus)
        );

        return refund;
    }
}
