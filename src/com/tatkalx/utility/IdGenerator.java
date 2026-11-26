package com.tatkalx.utility;

/**
 * Thread-safe synchronized generator for Booking IDs, 10-digit PNRs,
 * Ticket IDs, Payment Gateway references, and Simulation Session IDs.
 */
public final class IdGenerator {

    private static long bookingCounter = 1000L;
    private static long pnrCounter = 4521000000L;
    private static long ticketCounter = 1L;
    private static long paymentCounter = 1L;
    private static long cancellationCounter = 1L;
    private static long refundCounter = 1L;
    private static long txnCounter = 1L;
    private static long sessionCounter = 10L;
    private static long notificationCounter = 1L;

    private IdGenerator() {
    }

    public static synchronized String nextBookingId() {
        bookingCounter++;
        return "TX" + bookingCounter;
    }

    public static synchronized String nextPnrNumber() {
        pnrCounter++;
        return String.valueOf(pnrCounter);
    }

    public static synchronized long nextTicketId() {
        return ticketCounter++;
    }

    public static synchronized long nextPaymentId() {
        return paymentCounter++;
    }

    public static synchronized String nextGatewayRef(String method) {
        return "GW-" + (method == null ? "PAY" : method) + "-" + System.currentTimeMillis() + "-" + paymentCounter;
    }

    public static synchronized long nextCancellationId() {
        return cancellationCounter++;
    }

    public static synchronized long nextRefundId() {
        return refundCounter++;
    }

    public static synchronized long nextTxnId() {
        return txnCounter++;
    }

    public static synchronized long nextSessionId() {
        return sessionCounter++;
    }

    public static synchronized long nextNotificationId() {
        return notificationCounter++;
    }

    /**
     * Synchronizes in-memory ID counters above existing persisted database records
     * so restarting the application never collides on UNIQUE PNR or Booking IDs.
     */
    public static synchronized void syncFromDbMaximums(long maxBookingNum, long maxPnrNum,
                                                         long maxTicketId, long maxSessionId) {
        if (maxBookingNum > bookingCounter) {
            bookingCounter = maxBookingNum;
        }
        if (maxPnrNum > pnrCounter) {
            pnrCounter = maxPnrNum;
        }
        if (maxTicketId >= ticketCounter) {
            ticketCounter = maxTicketId + 1;
        }
        if (maxSessionId >= sessionCounter) {
            sessionCounter = maxSessionId + 1;
        }
    }
}
