package com.tatkalx.model;

/**
 * Represents a generated Ticket record (CONFIRMED, RAC, WL, or CANCELLED).
 */
public class Ticket {

    private long ticketId;
    private String bookingId;
    private long sessionId;
    private int passengerId;
    private long scheduleId;
    private int coachId;
    private String coachCode;
    private String ticketStatus;
    private int queuePosition;
    private String seatLabel;
    private double totalFare;
    private String bookedAt;
    private String updatedAt;

    public Ticket() {
    }

    public Ticket(long ticketId, String bookingId, long sessionId, int passengerId,
                  long scheduleId, int coachId, String coachCode, String ticketStatus,
                  int queuePosition, String seatLabel, double totalFare, String bookedAt) {
        this.ticketId = ticketId;
        this.bookingId = bookingId;
        this.sessionId = sessionId;
        this.passengerId = passengerId;
        this.scheduleId = scheduleId;
        this.coachId = coachId;
        this.coachCode = coachCode;
        this.ticketStatus = ticketStatus;
        this.queuePosition = queuePosition;
        this.seatLabel = seatLabel;
        this.totalFare = totalFare;
        this.bookedAt = bookedAt;
    }

    public long getTicketId() {
        return ticketId;
    }

    public void setTicketId(long ticketId) {
        this.ticketId = ticketId;
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

    public long getScheduleId() {
        return scheduleId;
    }

    public void setScheduleId(long scheduleId) {
        this.scheduleId = scheduleId;
    }

    public int getCoachId() {
        return coachId;
    }

    public void setCoachId(int coachId) {
        this.coachId = coachId;
    }

    public String getCoachCode() {
        return coachCode;
    }

    public void setCoachCode(String coachCode) {
        this.coachCode = coachCode;
    }

    public String getTicketStatus() {
        return ticketStatus;
    }

    public void setTicketStatus(String ticketStatus) {
        this.ticketStatus = ticketStatus;
    }

    public int getQueuePosition() {
        return queuePosition;
    }

    public void setQueuePosition(int queuePosition) {
        this.queuePosition = queuePosition;
    }

    public String getSeatLabel() {
        return seatLabel;
    }

    public void setSeatLabel(String seatLabel) {
        this.seatLabel = seatLabel;
    }

    public double getTotalFare() {
        return totalFare;
    }

    public void setTotalFare(double totalFare) {
        this.totalFare = totalFare;
    }

    public String getBookedAt() {
        return bookedAt;
    }

    public void setBookedAt(String bookedAt) {
        this.bookedAt = bookedAt;
    }

    public String getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(String updatedAt) {
        this.updatedAt = updatedAt;
    }

    @Override
    public String toString() {
        return String.format("Ticket[id=%d, booking=%s, status=%s, seat=%s, fare=%.2f]",
                ticketId, bookingId, ticketStatus, seatLabel, totalFare);
    }
}
