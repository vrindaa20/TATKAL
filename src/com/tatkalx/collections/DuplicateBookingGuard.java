package com.tatkalx.collections;

import com.tatkalx.exception.DuplicateBookingException;

import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Set;

/**
 * Prevents duplicate passenger bookings on the same train schedule within a session.
 * - HashSet<String> gives O(1) membership checks for active keys.
 * - LinkedHashSet<String> preserves exact chronological order of blocked duplicate attempts for forensic audit.
 */
public class DuplicateBookingGuard {

    private final Set<String> activeBookingKeys = new HashSet<>();
    private final Set<String> rejectedDuplicateAuditKeys = new LinkedHashSet<>();

    /**
     * Builds a composite uniqueness key scoped to (sessionId, scheduleId, passengerId).
     */
    public static String buildKey(long sessionId, long scheduleId, int passengerId) {
        return sessionId + ":" + scheduleId + ":" + passengerId;
    }

    /**
     * Atomically checks and registers a passenger booking key.
     * Throws DuplicateBookingException if already present.
     */
    public synchronized void acquireOrThrow(long sessionId, long scheduleId, int passengerId)
            throws DuplicateBookingException {
        String key = buildKey(sessionId, scheduleId, passengerId);
        if (!activeBookingKeys.add(key)) {
            rejectedDuplicateAuditKeys.add(key);
            throw new DuplicateBookingException(
                    key,
                    "Duplicate booking attempt blocked for passengerId=" + passengerId
                            + " on scheduleId=" + scheduleId + " (session=" + sessionId + ")"
            );
        }
    }

    /**
     * Releases a booking key when a booking is cancelled or fails before ticket issuance.
     */
    public synchronized boolean releaseKey(long sessionId, long scheduleId, int passengerId) {
        return activeBookingKeys.remove(buildKey(sessionId, scheduleId, passengerId));
    }

    public synchronized boolean hasActiveBooking(long sessionId, long scheduleId, int passengerId) {
        return activeBookingKeys.contains(buildKey(sessionId, scheduleId, passengerId));
    }

    public synchronized Set<String> getOrderedRejectedAuditKeys() {
        return Collections.unmodifiableSet(new LinkedHashSet<>(rejectedDuplicateAuditKeys));
    }

    public synchronized int getActiveCount() {
        return activeBookingKeys.size();
    }
}
