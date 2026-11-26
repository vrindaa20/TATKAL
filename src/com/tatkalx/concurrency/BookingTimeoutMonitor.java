package com.tatkalx.concurrency;

import com.tatkalx.utility.TimeUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Daemon monitor demonstrating sleep() and interrupt() for periodic Tatkal timeout sweeps.
 */
public class BookingTimeoutMonitor implements Runnable {

    private final long sweepIntervalMs;
    private volatile boolean active = true;
    private int sweepCycles = 0;
    private final List<String> sweepLogs = new ArrayList<>();

    public BookingTimeoutMonitor(long sweepIntervalMs) {
        this.sweepIntervalMs = Math.max(10L, sweepIntervalMs);
    }

    @Override
    public void run() {
        while (active && !Thread.currentThread().isInterrupted()) {
            synchronized (this) {
                sweepCycles++;
                sweepLogs.add(TimeUtils.nowTimeWithMillis() + " [TimeoutDaemon] Sweep #" + sweepCycles + " completed.");
            }
            try {
                Thread.sleep(sweepIntervalMs);
            } catch (InterruptedException ie) {
                Thread.currentThread().interrupt();
                synchronized (this) {
                    sweepLogs.add(TimeUtils.nowTimeWithMillis() + " [TimeoutDaemon] Interrupted cleanly; shutting down.");
                }
                break;
            }
        }
    }

    public void stopMonitor() {
        this.active = false;
    }

    public synchronized int getSweepCycles() {
        return sweepCycles;
    }

    public synchronized List<String> getSweepLogs() {
        return Collections.unmodifiableList(new ArrayList<>(sweepLogs));
    }
}
