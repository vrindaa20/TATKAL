package com.tatkalx.concurrency;

import com.tatkalx.model.ThreadLog;
import com.tatkalx.utility.TimeUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * Daemon Thread monitor that samples worker Thread states (NEW, RUNNABLE, BLOCKED,
 * WAITING, TIMED_WAITING, TERMINATED) during concurrent Tatkal execution.
 */
public class ThreadExecutionMonitor implements Runnable {

    private final long sessionId;
    private final List<Thread> watchedThreads;
    private final List<String> bookingIds;
    private final long sampleIntervalMs;

    private volatile boolean monitoring = true;
    private final List<ThreadLog> capturedLogs = new ArrayList<>();
    private final Map<Thread.State, Integer> stateHistogram = new EnumMap<>(Thread.State.class);

    public ThreadExecutionMonitor(long sessionId,
                                  List<Thread> watchedThreads,
                                  List<String> bookingIds,
                                  long sampleIntervalMs) {
        this.sessionId = sessionId;
        this.watchedThreads = watchedThreads;
        this.bookingIds = bookingIds;
        this.sampleIntervalMs = Math.max(2L, sampleIntervalMs);
        for (Thread.State state : Thread.State.values()) {
            stateHistogram.put(state, 0);
        }
    }

    public synchronized void captureSnapshot(String phaseDetail) {
        String now = TimeUtils.nowTimestamp();
        for (int i = 0; i < watchedThreads.size(); i++) {
            Thread t = watchedThreads.get(i);
            Thread.State state = t.getState();
            stateHistogram.put(state, stateHistogram.get(state) + 1);

            // Store detailed ThreadLog rows for non-terminated samples or key lifecycle transitions
            if (capturedLogs.size() < 400 || state == Thread.State.BLOCKED || state == Thread.State.WAITING) {
                String bookingId = (bookingIds != null && i < bookingIds.size()) ? bookingIds.get(i) : null;
                capturedLogs.add(new ThreadLog(
                        sessionId,
                        bookingId,
                        t.threadId(),
                        t.getName(),
                        t.getPriority(),
                        state.name(),
                        state == Thread.State.BLOCKED ? "SeatInventory.monitorLock" : "TatkalVirtualClock",
                        phaseDetail,
                        now
                ));
            }
        }
    }

    @Override
    public void run() {
        while (monitoring && !Thread.currentThread().isInterrupted()) {
            captureSnapshot("Daemon Periodic Sample");
            try {
                Thread.sleep(sampleIntervalMs);
            } catch (InterruptedException ie) {
                Thread.currentThread().interrupt();
                break;
            }
        }
    }

    public void stopMonitoring() {
        this.monitoring = false;
    }

    public synchronized List<ThreadLog> getCapturedLogs() {
        return Collections.unmodifiableList(new ArrayList<>(capturedLogs));
    }

    public synchronized Map<Thread.State, Integer> getStateHistogram() {
        return Collections.unmodifiableMap(new EnumMap<>(stateHistogram));
    }
}
