package com.tatkalx.model;

/**
 * Immutable financial ledger entry aligned with the Transactions table.
 */
public class TransactionRecord {

    private long txnId;
    private long sessionId;
    private int userId;
    private Long paymentId;
    private Long refundId;
    private String txnType;
    private double amount;
    private double balanceAfter;
    private String txnStatus;
    private String createdAt;

    public TransactionRecord() {
    }

    public TransactionRecord(long txnId, long sessionId, int userId, Long paymentId, Long refundId,
                             String txnType, double amount, double balanceAfter,
                             String txnStatus, String createdAt) {
        this.txnId = txnId;
        this.sessionId = sessionId;
        this.userId = userId;
        this.paymentId = paymentId;
        this.refundId = refundId;
        this.txnType = txnType;
        this.amount = amount;
        this.balanceAfter = balanceAfter;
        this.txnStatus = txnStatus;
        this.createdAt = createdAt;
    }

    public long getTxnId() {
        return txnId;
    }

    public void setTxnId(long txnId) {
        this.txnId = txnId;
    }

    public long getSessionId() {
        return sessionId;
    }

    public void setSessionId(long sessionId) {
        this.sessionId = sessionId;
    }

    public int getUserId() {
        return userId;
    }

    public void setUserId(int userId) {
        this.userId = userId;
    }

    public Long getPaymentId() {
        return paymentId;
    }

    public void setPaymentId(Long paymentId) {
        this.paymentId = paymentId;
    }

    public Long getRefundId() {
        return refundId;
    }

    public void setRefundId(Long refundId) {
        this.refundId = refundId;
    }

    public String getTxnType() {
        return txnType;
    }

    public void setTxnType(String txnType) {
        this.txnType = txnType;
    }

    public double getAmount() {
        return amount;
    }

    public void setAmount(double amount) {
        this.amount = amount;
    }

    public double getBalanceAfter() {
        return balanceAfter;
    }

    public void setBalanceAfter(double balanceAfter) {
        this.balanceAfter = balanceAfter;
    }

    public String getTxnStatus() {
        return txnStatus;
    }

    public void setTxnStatus(String txnStatus) {
        this.txnStatus = txnStatus;
    }

    public String getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(String createdAt) {
        this.createdAt = createdAt;
    }
}
