package com.tatkalx.model;

/**
 * Captures a real-time worker thread lifecycle sample aligned with Thread_Logs.
 */
public class ThreadLog {

    private long threadLogId;
    private long sessionId;
    private String bookingId;
    private long threadIdNum;
    private String threadName;
    private int threadPriority;
    private String threadState;
    private String lockTarget;
    private String actionDetail;
    private String loggedAt;

    public ThreadLog() {
    }

    public ThreadLog(long sessionId, String bookingId, long threadIdNum, String threadName,
                     int threadPriority, String threadState, String lockTarget,
                     String actionDetail, String loggedAt) {
        this.sessionId = sessionId;
        this.bookingId = bookingId;
        this.threadIdNum = threadIdNum;
        this.threadName = threadName;
        this.threadPriority = threadPriority;
        this.threadState = threadState;
        this.lockTarget = lockTarget;
        this.actionDetail = actionDetail;
        this.loggedAt = loggedAt;
    }

    public long getThreadLogId() {
        return threadLogId;
    }

    public void setThreadLogId(long threadLogId) {
        this.threadLogId = threadLogId;
    }

    public long getSessionId() {
        return sessionId;
    }

    public void setSessionId(long sessionId) {
        this.sessionId = sessionId;
    }

    public String getBookingId() {
        return bookingId;
    }

    public void setBookingId(String bookingId) {
        this.bookingId = bookingId;
    }

    public long getThreadIdNum() {
        return threadIdNum;
    }

    public void setThreadIdNum(long threadIdNum) {
        this.threadIdNum = threadIdNum;
    }

    public String getThreadName() {
        return threadName;
    }

    public void setThreadName(String threadName) {
        this.threadName = threadName;
    }

    public int getThreadPriority() {
        return threadPriority;
    }

    public void setThreadPriority(int threadPriority) {
        this.threadPriority = threadPriority;
    }

    public String getThreadState() {
        return threadState;
    }

    public void setThreadState(String threadState) {
        this.threadState = threadState;
    }

    public String getLockTarget() {
        return lockTarget;
    }

    public void setLockTarget(String lockTarget) {
        this.lockTarget = lockTarget;
    }

    public String getActionDetail() {
        return actionDetail;
    }

    public void setActionDetail(String actionDetail) {
        this.actionDetail = actionDetail;
    }

    public String getLoggedAt() {
        return loggedAt;
    }

    public void setLoggedAt(String loggedAt) {
        this.loggedAt = loggedAt;
    }

    @Override
    public String toString() {
        return String.format("%s | %-16s (prio=%d) | state=%-13s | %s",
                loggedAt, threadName, threadPriority, threadState, actionDetail);
    }
}
