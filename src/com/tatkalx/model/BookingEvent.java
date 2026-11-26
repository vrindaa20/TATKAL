package com.tatkalx.model;

/**
 * Forensic lifecycle event for a single Booking ID (Crime Scene Reconstruction).
 */
public class BookingEvent {

    private long eventId;
    private String bookingId;
    private long sessionId;
    private int passengerId;
    private String threadName;
    private int eventSeq;
    private String eventType;
    private String eventDescription;
    private String requestStatus;
    private Integer availableSeatsSnapshot;
    private Integer queuePositionSnapshot;
    private String allocationResult;
    private String paymentResult;
    private String eventTimestamp;

    public BookingEvent() {
    }

    public BookingEvent(String bookingId, long sessionId, int passengerId, String threadName,
                        int eventSeq, String eventType, String eventDescription,
                        String requestStatus, Integer availableSeatsSnapshot,
                        Integer queuePositionSnapshot, String allocationResult,
                        String paymentResult, String eventTimestamp) {
        this.bookingId = bookingId;
        this.sessionId = sessionId;
        this.passengerId = passengerId;
        this.threadName = threadName;
        this.eventSeq = eventSeq;
        this.eventType = eventType;
        this.eventDescription = eventDescription;
        this.requestStatus = requestStatus;
        this.availableSeatsSnapshot = availableSeatsSnapshot;
        this.queuePositionSnapshot = queuePositionSnapshot;
        this.allocationResult = allocationResult;
        this.paymentResult = paymentResult;
        this.eventTimestamp = eventTimestamp;
    }

    public long getEventId() {
        return eventId;
    }

    public void setEventId(long eventId) {
        this.eventId = eventId;
    }

    public String getBookingId() {
        return bookingId;
    }

    public void setBookingId(String bookingId) {
        this.bookingId = bookingId;
    }

    public long getSessionId() {
        return sessionId;
    }

    public void setSessionId(long sessionId) {
        this.sessionId = sessionId;
    }

    public int getPassengerId() {
        return passengerId;
    }

    public void setPassengerId(int passengerId) {
        this.passengerId = passengerId;
    }

    public String getThreadName() {
        return threadName;
    }

    public void setThreadName(String threadName) {
        this.threadName = threadName;
    }

    public int getEventSeq() {
        return eventSeq;
    }

    public void setEventSeq(int eventSeq) {
        this.eventSeq = eventSeq;
    }

    public String getEventType() {
        return eventType;
    }

    public void setEventType(String eventType) {
        this.eventType = eventType;
    }

    public String getEventDescription() {
        return eventDescription;
    }

    public void setEventDescription(String eventDescription) {
        this.eventDescription = eventDescription;
    }

    public String getRequestStatus() {
        return requestStatus;
    }

    public void setRequestStatus(String requestStatus) {
        this.requestStatus = requestStatus;
    }

    public Integer getAvailableSeatsSnapshot() {
        return availableSeatsSnapshot;
    }

    public void setAvailableSeatsSnapshot(Integer availableSeatsSnapshot) {
        this.availableSeatsSnapshot = availableSeatsSnapshot;
    }

    public Integer getQueuePositionSnapshot() {
        return queuePositionSnapshot;
    }

    public void setQueuePositionSnapshot(Integer queuePositionSnapshot) {
        this.queuePositionSnapshot = queuePositionSnapshot;
    }

    public String getAllocationResult() {
        return allocationResult;
    }

    public void setAllocationResult(String allocationResult) {
        this.allocationResult = allocationResult;
    }

    public String getPaymentResult() {
        return paymentResult;
    }

    public void setPaymentResult(String paymentResult) {
        this.paymentResult = paymentResult;
    }

    public String getEventTimestamp() {
        return eventTimestamp;
    }

    public void setEventTimestamp(String eventTimestamp) {
        this.eventTimestamp = eventTimestamp;
    }

    @Override
    public String toString() {
        return String.format("%s [%s] (#%d %s) - %s [Status=%s, Avail=%s]",
                eventTimestamp, threadName, eventSeq, eventType, eventDescription,
                requestStatus, availableSeatsSnapshot);
    }
}
