package com.tatkalx.model;

/**
 * Aggregated metrics produced by a simulation universe (A, B, or C).
 */
public class SimulationResult {

    private long resultId;
    private long sessionId;
    private String universeName;
    private int totalRequests;
    private int confirmedCount;
    private int racCount;
    private int wlCount;
    private int failedCount;
    private int duplicateAttempts;
    private long totalTimeMs;
    private double avgTimeMs;
    private long maxTimeMs;
    private double successRatePct;
    private double seatUtilizationPct;
    private boolean inventoryConsistent;
    private String threadSummary;
    private String recordedAt;

    public long getResultId() {
        return resultId;
    }

    public void setResultId(long resultId) {
        this.resultId = resultId;
    }

    public long getSessionId() {
        return sessionId;
    }

    public void setSessionId(long sessionId) {
        this.sessionId = sessionId;
    }

    public String getUniverseName() {
        return universeName;
    }

    public void setUniverseName(String universeName) {
        this.universeName = universeName;
    }

    public int getTotalRequests() {
        return totalRequests;
    }

    public void setTotalRequests(int totalRequests) {
        this.totalRequests = totalRequests;
    }

    public int getConfirmedCount() {
        return confirmedCount;
    }

    public void setConfirmedCount(int confirmedCount) {
        this.confirmedCount = confirmedCount;
    }

    public int getRacCount() {
        return racCount;
    }

    public void setRacCount(int racCount) {
        this.racCount = racCount;
    }

    public int getWlCount() {
        return wlCount;
    }

    public void setWlCount(int wlCount) {
        this.wlCount = wlCount;
    }

    public int getFailedCount() {
        return failedCount;
    }

    public void setFailedCount(int failedCount) {
        this.failedCount = failedCount;
    }

    public int getDuplicateAttempts() {
        return duplicateAttempts;
    }

    public void setDuplicateAttempts(int duplicateAttempts) {
        this.duplicateAttempts = duplicateAttempts;
    }

    public long getTotalTimeMs() {
        return totalTimeMs;
    }

    public void setTotalTimeMs(long totalTimeMs) {
        this.totalTimeMs = totalTimeMs;
    }

    public double getAvgTimeMs() {
        return avgTimeMs;
    }

    public void setAvgTimeMs(double avgTimeMs) {
        this.avgTimeMs = avgTimeMs;
    }

    public long getMaxTimeMs() {
        return maxTimeMs;
    }

    public void setMaxTimeMs(long maxTimeMs) {
        this.maxTimeMs = maxTimeMs;
    }

    public double getSuccessRatePct() {
        return successRatePct;
    }

    public void setSuccessRatePct(double successRatePct) {
        this.successRatePct = successRatePct;
    }

    public double getSeatUtilizationPct() {
        return seatUtilizationPct;
    }

    public void setSeatUtilizationPct(double seatUtilizationPct) {
        this.seatUtilizationPct = seatUtilizationPct;
    }

    public boolean isInventoryConsistent() {
        return inventoryConsistent;
    }

    public void setInventoryConsistent(boolean inventoryConsistent) {
        this.inventoryConsistent = inventoryConsistent;
    }

    public String getThreadSummary() {
        return threadSummary;
    }

    public void setThreadSummary(String threadSummary) {
        this.threadSummary = threadSummary;
    }

    public String getRecordedAt() {
        return recordedAt;
    }

    public void setRecordedAt(String recordedAt) {
        this.recordedAt = recordedAt;
    }
}
