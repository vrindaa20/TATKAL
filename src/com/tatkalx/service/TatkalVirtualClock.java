package com.tatkalx.service;

import com.tatkalx.exception.TatkalWindowClosedException;
import com.tatkalx.utility.TimeUtils;

import java.time.Duration;
import java.time.LocalTime;

/**
 * Virtual Tatkal Clock modeling the 10:00:00 AM - 11:00:00 AM Tatkal booking window.
 * Uses synchronized guarded blocks (wait / notifyAll) so competing worker threads can
 * block until precisely 10:00:00 AM and wake up simultaneously when the gate opens.
 */
public class TatkalVirtualClock {

    public enum WindowState {
        BEFORE_WINDOW,
        OPEN,
        CLOSED
    }

    private LocalTime currentVirtualTime;
    private LocalTime openTime;
    private LocalTime closeTime;
    private boolean aborted = false;

    public TatkalVirtualClock() {
        this.openTime = LocalTime.of(10, 0, 0);
        this.closeTime = LocalTime.of(11, 0, 0);
        this.currentVirtualTime = LocalTime.of(9, 59, 55);
    }

    public synchronized void configureWindow(LocalTime openTime, LocalTime closeTime) {
        this.openTime = openTime;
        this.closeTime = closeTime;
    }

    public synchronized void setVirtualTime(LocalTime newTime) {
        this.currentVirtualTime = newTime;
        this.aborted = false;
        if (getWindowState() == WindowState.OPEN) {
            notifyAll();
        }
    }

    /**
     * Immediately jumps virtual clock to 10:00:00 AM and notifies all waiting booking threads.
     */
    public synchronized void openWindowNow() {
        this.aborted = false;
        this.currentVirtualTime = openTime;
        notifyAll();
    }

    public synchronized void closeWindowNow() {
        this.currentVirtualTime = closeTime.plusSeconds(1);
        this.aborted = true;
        notifyAll();
    }

    public synchronized WindowState getWindowState() {
        if (currentVirtualTime.isBefore(openTime)) {
            return WindowState.BEFORE_WINDOW;
        }
        if (!currentVirtualTime.isAfter(closeTime)) {
            return WindowState.OPEN;
        }
        return WindowState.CLOSED;
    }

    public synchronized boolean isBookingAllowed() {
        return getWindowState() == WindowState.OPEN;
    }

    public synchronized void assertWindowOpen() throws TatkalWindowClosedException {
        WindowState state = getWindowState();
        if (state == WindowState.BEFORE_WINDOW) {
            throw new TatkalWindowClosedException(
                    "Tatkal window has not opened yet! Current virtual time: "
                            + TimeUtils.formatTime(currentVirtualTime)
                            + " (Opens at " + TimeUtils.formatTime(openTime) + ")");
        }
        if (state == WindowState.CLOSED) {
            throw new TatkalWindowClosedException(
                    "Tatkal booking window is CLOSED! Current virtual time: "
                            + TimeUtils.formatTime(currentVirtualTime)
                            + " (Closed at " + TimeUtils.formatTime(closeTime) + ")");
        }
    }

    /**
     * Guarded block: competing threads wait here until virtual time reaches openTime (10:00:00).
     */
    public synchronized void awaitWindowOpen() throws InterruptedException, TatkalWindowClosedException {
        while (getWindowState() == WindowState.BEFORE_WINDOW && !aborted) {
            wait();
        }
        if (getWindowState() == WindowState.CLOSED || aborted) {
            throw new TatkalWindowClosedException("Tatkal window closed before request could start.");
        }
    }

    public synchronized String getCountdownBanner() {
        WindowState state = getWindowState();
        if (state == WindowState.BEFORE_WINDOW) {
            long secondsLeft = Duration.between(currentVirtualTime, openTime).getSeconds();
            return String.format("Virtual Time: %s | Tatkal Opens In: %02d sec (%s)",
                    TimeUtils.formatTime(currentVirtualTime), secondsLeft, TimeUtils.formatTime(openTime));
        }
        if (state == WindowState.OPEN) {
            long secondsRemaining = Duration.between(currentVirtualTime, closeTime).getSeconds();
            return String.format("Virtual Time: %s | TATKAL WINDOW OPEN (Closes in %d sec)",
                    TimeUtils.formatTime(currentVirtualTime), secondsRemaining);
        }
        return String.format("Virtual Time: %s | TATKAL WINDOW CLOSED (Closed at %s)",
                TimeUtils.formatTime(currentVirtualTime), TimeUtils.formatTime(closeTime));
    }

    public synchronized LocalTime getCurrentVirtualTime() {
        return currentVirtualTime;
    }

    public synchronized LocalTime getOpenTime() {
        return openTime;
    }

    public synchronized LocalTime getCloseTime() {
        return closeTime;
    }
}
