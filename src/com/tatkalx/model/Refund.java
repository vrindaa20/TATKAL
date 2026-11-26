package com.tatkalx.model;

/**
 * Represents a calculated refund aligned with the Refunds table.
 */
public class Refund {

    private long refundId;
    private long cancellationId;
    private long paymentId;
    private double originalAmount;
    private double deductionAmount;
    private double refundAmount;
    private String refundStatus;
    private String processedAt;

    public Refund() {
    }

    public Refund(long refundId, long cancellationId, long paymentId, double originalAmount,
                  double deductionAmount, double refundAmount, String refundStatus, String processedAt) {
        this.refundId = refundId;
        this.cancellationId = cancellationId;
        this.paymentId = paymentId;
        this.originalAmount = originalAmount;
        this.deductionAmount = deductionAmount;
        this.refundAmount = refundAmount;
        this.refundStatus = refundStatus;
        this.processedAt = processedAt;
    }

    public long getRefundId() {
        return refundId;
    }

    public void setRefundId(long refundId) {
        this.refundId = refundId;
    }

    public long getCancellationId() {
        return cancellationId;
    }

    public void setCancellationId(long cancellationId) {
        this.cancellationId = cancellationId;
    }

    public long getPaymentId() {
        return paymentId;
    }

    public void setPaymentId(long paymentId) {
        this.paymentId = paymentId;
    }

    public double getOriginalAmount() {
        return originalAmount;
    }

    public void setOriginalAmount(double originalAmount) {
        this.originalAmount = originalAmount;
    }

    public double getDeductionAmount() {
        return deductionAmount;
    }

    public void setDeductionAmount(double deductionAmount) {
        this.deductionAmount = deductionAmount;
    }

    public double getRefundAmount() {
        return refundAmount;
    }

    public void setRefundAmount(double refundAmount) {
        this.refundAmount = refundAmount;
    }

    public String getRefundStatus() {
        return refundStatus;
    }

    public void setRefundStatus(String refundStatus) {
        this.refundStatus = refundStatus;
    }

    public String getProcessedAt() {
        return processedAt;
    }

    public void setProcessedAt(String processedAt) {
        this.processedAt = processedAt;
    }

    @Override
    public String toString() {
        return String.format("Refund[id=%d, orig=%.2f, deduct=%.2f, refund=%.2f, status=%s]",
                refundId, originalAmount, deductionAmount, refundAmount, refundStatus);
    }
}
