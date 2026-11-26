package com.tatkalx.model;

/**
 * Represents a single Tatkal booking request aligned with Booking_Requests.
 * Natural ordering compares by requestTimestamp, then bookingId.
 */
public class BookingRequest implements Comparable<BookingRequest> {

    private String bookingId;
    private long sessionId;
    private int passengerId;
    private String passengerName;
    private int passengerAge;
    private String priorityCategory;
    private long scheduleId;
    private String coachClass;
    private int requestedSeats;
    private int priorityScore;
    private String requestTimestamp;
    private String queueEnteredAt;
    private int queuePosition;
    private String threadName;
    private long processingTimeMs;
    private String status;
    private String failureReason;

    public BookingRequest() {
        this.status = "CREATED";
        this.requestedSeats = 1;
        this.priorityCategory = "NORMAL";
    }

    public BookingRequest(String bookingId, long sessionId, int passengerId, String passengerName,
                          int passengerAge, String priorityCategory, long scheduleId,
                          String coachClass, int requestedSeats, String requestTimestamp) {
        this.bookingId = bookingId;
        this.sessionId = sessionId;
        this.passengerId = passengerId;
        this.passengerName = passengerName;
        this.passengerAge = passengerAge;
        this.priorityCategory = priorityCategory;
        this.scheduleId = scheduleId;
        this.coachClass = coachClass;
        this.requestedSeats = requestedSeats;
        this.requestTimestamp = requestTimestamp;
        this.status = "CREATED";
    }

    /**
     * Creates an isolated copy of this request for an independent simulation universe.
     */
    public BookingRequest copyForSession(long targetSessionId) {
        BookingRequest clone = new BookingRequest(
                bookingId + "-S" + targetSessionId,
                targetSessionId,
                passengerId,
                passengerName,
                passengerAge,
                priorityCategory,
                scheduleId,
                coachClass,
                requestedSeats,
                requestTimestamp
        );
        clone.setPriorityScore(this.priorityScore);
        return clone;
    }

    @Override
    public int compareTo(BookingRequest other) {
        int timeCmp = this.requestTimestamp.compareTo(other.requestTimestamp);
        if (timeCmp != 0) {
            return timeCmp;
        }
        return this.bookingId.compareTo(other.bookingId);
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

    public String getPassengerName() {
        return passengerName;
    }

    public void setPassengerName(String passengerName) {
        this.passengerName = passengerName;
    }

    public int getPassengerAge() {
        return passengerAge;
    }

    public void setPassengerAge(int passengerAge) {
        this.passengerAge = passengerAge;
    }

    public String getPriorityCategory() {
        return priorityCategory;
    }

    public void setPriorityCategory(String priorityCategory) {
        this.priorityCategory = priorityCategory;
    }

    public long getScheduleId() {
        return scheduleId;
    }

    public void setScheduleId(long scheduleId) {
        this.scheduleId = scheduleId;
    }

    public String getCoachClass() {
        return coachClass;
    }

    public void setCoachClass(String coachClass) {
        this.coachClass = coachClass;
    }

    public int getRequestedSeats() {
        return requestedSeats;
    }

    public void setRequestedSeats(int requestedSeats) {
        this.requestedSeats = requestedSeats;
    }

    public int getPriorityScore() {
        return priorityScore;
    }

    public void setPriorityScore(int priorityScore) {
        this.priorityScore = priorityScore;
    }

    public String getRequestTimestamp() {
        return requestTimestamp;
    }

    public void setRequestTimestamp(String requestTimestamp) {
        this.requestTimestamp = requestTimestamp;
    }

    public String getQueueEnteredAt() {
        return queueEnteredAt;
    }

    public void setQueueEnteredAt(String queueEnteredAt) {
        this.queueEnteredAt = queueEnteredAt;
    }

    public int getQueuePosition() {
        return queuePosition;
    }

    public void setQueuePosition(int queuePosition) {
        this.queuePosition = queuePosition;
    }

    public String getThreadName() {
        return threadName;
    }

    public void setThreadName(String threadName) {
        this.threadName = threadName;
    }

    public long getProcessingTimeMs() {
        return processingTimeMs;
    }

    public void setProcessingTimeMs(long processingTimeMs) {
        this.processingTimeMs = processingTimeMs;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getFailureReason() {
        return failureReason;
    }

    public void setFailureReason(String failureReason) {
        this.failureReason = failureReason;
    }

    @Override
    public String toString() {
        return String.format("BookingRequest[id=%s, pax=%s(%d,%s), seats=%d, ts=%s, score=%d, status=%s]",
                bookingId, passengerName, passengerAge, priorityCategory,
                requestedSeats, requestTimestamp, priorityScore, status);
    }
}
