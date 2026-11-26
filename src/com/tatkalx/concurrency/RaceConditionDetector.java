package com.tatkalx.concurrency;

import com.tatkalx.model.SeatInventory;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Detects and records real concurrency contention during Universe C execution:
 * compares optimistic availability observed BEFORE acquiring the monitor lock against
 * actual availability INSIDE the synchronized critical section.
 */
public class RaceConditionDetector {

    private int totalContentionCollisions = 0;
    private int lastSeatRaceAttempts = 0;
    private final List<String> collisionTraces = new ArrayList<>();

    public synchronized void recordContentionCheck(String bookingId,
                                                   String threadName,
                                                   int preLockAvailable,
                                                   SeatInventory inventoryInsideLock) {
        int insideLockAvailable = inventoryInsideLock.getAvailableTatkalSeats();
        if (preLockAvailable != insideLockAvailable) {
            totalContentionCollisions++;
            if (preLockAvailable > 0 && insideLockAvailable == 0) {
                lastSeatRaceAttempts++;
            }
            if (collisionTraces.size() < 25) {
                collisionTraces.add(String.format(
                        "%s [%s] saw avail=%d before lock, but inside synchronized lock avail=%d (race prevented)",
                        bookingId, threadName, preLockAvailable, insideLockAvailable));
            }
        }
    }

    public synchronized int getTotalContentionCollisions() {
        return totalContentionCollisions;
    }

    public synchronized int getLastSeatRaceAttempts() {
        return lastSeatRaceAttempts;
    }

    public synchronized List<String> getCollisionTraces() {
        return Collections.unmodifiableList(new ArrayList<>(collisionTraces));
    }
}
