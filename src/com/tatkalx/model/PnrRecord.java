package com.tatkalx.model;

/**
 * Represents a 10-digit PNR record aligned with PNR_Records.
 * Implements Comparable so TreeSet stores PNRs in sorted ascending order.
 */
public class PnrRecord implements Comparable<PnrRecord> {

    private long pnrId;
    private String pnrNumber;
    private long ticketId;
    private long sessionId;
    private String currentStatus;
    private boolean chartPrepared;
    private String generatedAt;

    public PnrRecord() {
    }

    public PnrRecord(long pnrId, String pnrNumber, long ticketId, long sessionId,
                     String currentStatus, boolean chartPrepared, String generatedAt) {
        this.pnrId = pnrId;
        this.pnrNumber = pnrNumber;
        this.ticketId = ticketId;
        this.sessionId = sessionId;
        this.currentStatus = currentStatus;
        this.chartPrepared = chartPrepared;
        this.generatedAt = generatedAt;
    }

    @Override
    public int compareTo(PnrRecord other) {
        return this.pnrNumber.compareTo(other.pnrNumber);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (!(obj instanceof PnrRecord)) {
            return false;
        }
        PnrRecord other = (PnrRecord) obj;
        return this.pnrNumber != null && this.pnrNumber.equals(other.pnrNumber);
    }

    @Override
    public int hashCode() {
        return pnrNumber == null ? 0 : pnrNumber.hashCode();
    }

    public long getPnrId() {
        return pnrId;
    }

    public void setPnrId(long pnrId) {
        this.pnrId = pnrId;
    }

    public String getPnrNumber() {
        return pnrNumber;
    }

    public void setPnrNumber(String pnrNumber) {
        this.pnrNumber = pnrNumber;
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

    public String getCurrentStatus() {
        return currentStatus;
    }

    public void setCurrentStatus(String currentStatus) {
        this.currentStatus = currentStatus;
    }

    public boolean isChartPrepared() {
        return chartPrepared;
    }

    public void setChartPrepared(boolean chartPrepared) {
        this.chartPrepared = chartPrepared;
    }

    public String getGeneratedAt() {
        return generatedAt;
    }

    public void setGeneratedAt(String generatedAt) {
        this.generatedAt = generatedAt;
    }

    @Override
    public String toString() {
        return String.format("PNR[%s | ticketId=%d | status=%s | charted=%s]",
                pnrNumber, ticketId, currentStatus, chartPrepared);
    }
}
