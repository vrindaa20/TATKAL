package com.tatkalx.model;

/**
 * Represents a payment attempt aligned with the Payments table.
 */
public class Payment {

    private long paymentId;
    private String bookingId;
    private long sessionId;
    private int passengerId;
    private double amount;
    private String paymentMethod;
    private String gatewayRef;
    private String paymentStatus;
    private long latencyMs;
    private String initiatedAt;
    private String completedAt;

    public Payment() {
    }

    public Payment(long paymentId, String bookingId, long sessionId, int passengerId,
                   double amount, String paymentMethod, String gatewayRef,
                   String paymentStatus, long latencyMs, String initiatedAt, String completedAt) {
        this.paymentId = paymentId;
        this.bookingId = bookingId;
        this.sessionId = sessionId;
        this.passengerId = passengerId;
        this.amount = amount;
        this.paymentMethod = paymentMethod;
        this.gatewayRef = gatewayRef;
        this.paymentStatus = paymentStatus;
        this.latencyMs = latencyMs;
        this.initiatedAt = initiatedAt;
        this.completedAt = completedAt;
    }

    public long getPaymentId() {
        return paymentId;
    }

    public void setPaymentId(long paymentId) {
        this.paymentId = paymentId;
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

    public double getAmount() {
        return amount;
    }

    public void setAmount(double amount) {
        this.amount = amount;
    }

    public String getPaymentMethod() {
        return paymentMethod;
    }

    public void setPaymentMethod(String paymentMethod) {
        this.paymentMethod = paymentMethod;
    }

    public String getGatewayRef() {
        return gatewayRef;
    }

    public void setGatewayRef(String gatewayRef) {
        this.gatewayRef = gatewayRef;
    }

    public String getPaymentStatus() {
        return paymentStatus;
    }

    public void setPaymentStatus(String paymentStatus) {
        this.paymentStatus = paymentStatus;
    }

    public long getLatencyMs() {
        return latencyMs;
    }

    public void setLatencyMs(long latencyMs) {
        this.latencyMs = latencyMs;
    }

    public String getInitiatedAt() {
        return initiatedAt;
    }

    public void setInitiatedAt(String initiatedAt) {
        this.initiatedAt = initiatedAt;
    }

    public String getCompletedAt() {
        return completedAt;
    }

    public void setCompletedAt(String completedAt) {
        this.completedAt = completedAt;
    }

    @Override
    public String toString() {
        return String.format("Payment[id=%d, booking=%s, ref=%s, amt=%.2f, status=%s]",
                paymentId, bookingId, gatewayRef, amount, paymentStatus);
    }
}
