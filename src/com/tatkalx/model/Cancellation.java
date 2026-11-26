package com.tatkalx.model;

/**
 * Represents a ticket cancellation event aligned with the Cancellations table.
 */
public class Cancellation {

    private long cancellationId;
    private long ticketId;
    private long sessionId;
    private int cancelledByUserId;
    private String previousStatus;
    private String cancellationReason;
    private String cancelledAt;

    public Cancellation() {
    }

    public Cancellation(long cancellationId, long ticketId, long sessionId,
                        int cancelledByUserId, String previousStatus,
                        String cancellationReason, String cancelledAt) {
        this.cancellationId = cancellationId;
        this.ticketId = ticketId;
        this.sessionId = sessionId;
        this.cancelledByUserId = cancelledByUserId;
        this.previousStatus = previousStatus;
        this.cancellationReason = cancellationReason;
        this.cancelledAt = cancelledAt;
    }

    public long getCancellationId() {
        return cancellationId;
    }

    public void setCancellationId(long cancellationId) {
        this.cancellationId = cancellationId;
    }

    public long getTicketId() {
        return ticketId;
    }

    public void setTicketId(long ticketId) {
        this.ticketId = ticketId;
    }

    public long getSessionId() {
        return sessionId;
    }

    public void setSessionId(long sessionId) {
        this.sessionId = sessionId;
    }

    public int getCancelledByUserId() {
        return cancelledByUserId;
    }

    public void setCancelledByUserId(int cancelledByUserId) {
        this.cancelledByUserId = cancelledByUserId;
    }

    public String getPreviousStatus() {
        return previousStatus;
    }

    public void setPreviousStatus(String previousStatus) {
        this.previousStatus = previousStatus;
    }

    public String getCancellationReason() {
        return cancellationReason;
    }

    public void setCancellationReason(String cancellationReason) {
        this.cancellationReason = cancellationReason;
    }

    public String getCancelledAt() {
        return cancelledAt;
    }

    public void setCancelledAt(String cancelledAt) {
        this.cancelledAt = cancelledAt;
    }
}
