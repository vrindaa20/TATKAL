package com.tatkalx.simulation;

import com.tatkalx.collections.BookingHistoryCache;
import com.tatkalx.collections.DuplicateBookingGuard;
import com.tatkalx.collections.PassengerRegistry;
import com.tatkalx.collections.PnrDirectory;
import com.tatkalx.collections.WaitingListManager;
import com.tatkalx.model.BookingEvent;
import com.tatkalx.model.BookingRequest;
import com.tatkalx.model.Cancellation;
import com.tatkalx.model.Coach;
import com.tatkalx.model.Payment;
import com.tatkalx.model.Refund;
import com.tatkalx.model.SeatInventory;
import com.tatkalx.model.Ticket;
import com.tatkalx.model.TrainSchedule;
import com.tatkalx.model.TransactionRecord;
import com.tatkalx.service.TatkalVirtualClock;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Encapsulates completely isolated state for a single booking session or simulation universe
 * (Live, Universe A, Universe B, Universe C, or Failure Lab).
 * Guarantees that one simulation experiment can never corrupt another's inventory or queues.
 */
public class SimulationSessionContext {

    private final long sessionId;
    private final String universeName;
    private final TrainSchedule schedule;
    private final Coach coach;
    private final SeatInventory seatInventory;
    private final TatkalVirtualClock virtualClock;

    private final PassengerRegistry passengerRegistry = new PassengerRegistry();
    private final DuplicateBookingGuard duplicateGuard = new DuplicateBookingGuard();
    private final WaitingListManager waitingListManager = new WaitingListManager();
    private final PnrDirectory pnrDirectory = new PnrDirectory();
    private final BookingHistoryCache historyCache = new BookingHistoryCache();

    private final Map<Long, Ticket> ticketsById = new HashMap<>();
    private final Map<String, Payment> paymentsByBookingId = new HashMap<>();
    private final List<Cancellation> cancellations = new ArrayList<>();
    private final List<Refund> refunds = new ArrayList<>();
    private final List<TransactionRecord> transactions = new ArrayList<>();
    private final Map<String, List<BookingEvent>> eventsByBookingId = new HashMap<>();
    private final List<String> executionOrderLog = new ArrayList<>();

    private int duplicateAttempts = 0;

    public SimulationSessionContext(long sessionId, String universeName, TrainSchedule schedule,
                                    Coach coach, SeatInventory seatInventory, TatkalVirtualClock virtualClock) {
        this.sessionId = sessionId;
        this.universeName = universeName;
        this.schedule = schedule;
        this.coach = coach;
        this.seatInventory = seatInventory;
        this.virtualClock = virtualClock;
    }

    public synchronized void recordTicket(Ticket ticket) {
        ticketsById.put(ticket.getTicketId(), ticket);
    }

    public synchronized Ticket findTicketById(long ticketId) {
        return ticketsById.get(ticketId);
    }

    public synchronized void recordPayment(Payment payment) {
        paymentsByBookingId.put(payment.getBookingId(), payment);
    }

    public synchronized Payment findPaymentByBookingId(String bookingId) {
        return paymentsByBookingId.get(bookingId);
    }

    public synchronized void recordCancellation(Cancellation cancellation, Refund refund, TransactionRecord txn) {
        cancellations.add(cancellation);
        refunds.add(refund);
        if (txn != null) {
            transactions.add(txn);
        }
    }

    public synchronized void recordTransaction(TransactionRecord txn) {
        transactions.add(txn);
    }

    public synchronized void appendBookingEvents(String bookingId, List<BookingEvent> events) {
        List<BookingEvent> list = eventsByBookingId.get(bookingId);
        if (list == null) {
            list = new ArrayList<>();
            eventsByBookingId.put(bookingId, list);
        }
        list.addAll(events);
    }

    public synchronized void recordExecutionOrder(String entry) {
        executionOrderLog.add(entry);
    }

    public synchronized void incrementDuplicateAttempts() {
        duplicateAttempts++;
    }

    public synchronized int getDuplicateAttempts() {
        return duplicateAttempts;
    }

    public long getSessionId() {
        return sessionId;
    }

    public String getUniverseName() {
        return universeName;
    }

    public TrainSchedule getSchedule() {
        return schedule;
    }

    public Coach getCoach() {
        return coach;
    }

    public SeatInventory getSeatInventory() {
        return seatInventory;
    }

    public TatkalVirtualClock getVirtualClock() {
        return virtualClock;
    }

    public PassengerRegistry getPassengerRegistry() {
        return passengerRegistry;
    }

    public DuplicateBookingGuard getDuplicateGuard() {
        return duplicateGuard;
    }

    public WaitingListManager getWaitingListManager() {
        return waitingListManager;
    }

    public PnrDirectory getPnrDirectory() {
        return pnrDirectory;
    }

    public BookingHistoryCache getHistoryCache() {
        return historyCache;
    }

    public synchronized List<Ticket> getAllTickets() {
        return Collections.unmodifiableList(new ArrayList<>(ticketsById.values()));
    }

    public synchronized List<Payment> getAllPayments() {
        return Collections.unmodifiableList(new ArrayList<>(paymentsByBookingId.values()));
    }

    public synchronized List<Refund> getAllRefunds() {
        return Collections.unmodifiableList(new ArrayList<>(refunds));
    }

    public synchronized List<BookingEvent> getEventsForBooking(String bookingId) {
        List<BookingEvent> list = eventsByBookingId.get(bookingId);
        return list == null ? Collections.emptyList() : Collections.unmodifiableList(new ArrayList<>(list));
    }

    public synchronized List<BookingRequest> getAllRequestsSnapshot() {
        return historyCache.getInsertionOrderedHistory();
    }

    public synchronized List<String> getExecutionOrderLog() {
        return Collections.unmodifiableList(new ArrayList<>(executionOrderLog));
    }
}
