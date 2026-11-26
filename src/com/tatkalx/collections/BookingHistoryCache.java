package com.tatkalx.collections;

import com.tatkalx.model.BookingRequest;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;

/**
 * Tracks booking history and chronological summaries using:
 * - LinkedHashMap<String, BookingRequest> to preserve exact arrival/insertion order
 * - TreeMap<String, List<BookingRequest>> keyed by timestamp bucket for sorted time-window summaries
 */
public class BookingHistoryCache {

    private final LinkedHashMap<String, BookingRequest> orderedHistory = new LinkedHashMap<>();
    private final TreeMap<String, List<BookingRequest>> chronologicalBuckets = new TreeMap<>();

    public synchronized void recordBooking(BookingRequest request) {
        if (request == null || request.getBookingId() == null) {
            return;
        }
        orderedHistory.put(request.getBookingId(), request);

        String timeKey = request.getRequestTimestamp() == null ? "UNKNOWN" : request.getRequestTimestamp();
        List<BookingRequest> bucket = chronologicalBuckets.get(timeKey);
        if (bucket == null) {
            bucket = new ArrayList<>();
            chronologicalBuckets.put(timeKey, bucket);
        }
        bucket.add(request);
    }

    public synchronized BookingRequest findByBookingId(String bookingId) {
        return orderedHistory.get(bookingId);
    }

    /**
     * Returns booking history in exact insertion order (via LinkedHashMap).
     */
    public synchronized List<BookingRequest> getInsertionOrderedHistory() {
        return Collections.unmodifiableList(new ArrayList<>(orderedHistory.values()));
    }

    /**
     * Returns a sorted timestamp -> count summary map (via TreeMap).
     */
    public synchronized Map<String, Integer> getChronologicalSummaryCounts() {
        Map<String, Integer> summary = new LinkedHashMap<>();
        for (Map.Entry<String, List<BookingRequest>> entry : chronologicalBuckets.entrySet()) {
            summary.put(entry.getKey(), entry.getValue().size());
        }
        return summary;
    }

    /**
     * Demonstrates TreeMap subMap() to slice bookings inside a time window [startTime, endTime].
     */
    public synchronized List<BookingRequest> getBookingsBetweenTimestamps(String startTimeInclusive, String endTimeInclusive) {
        SortedMap<String, List<BookingRequest>> window =
                chronologicalBuckets.subMap(startTimeInclusive, true, endTimeInclusive, true);
        List<BookingRequest> result = new ArrayList<>();
        for (List<BookingRequest> bucket : window.values()) {
            result.addAll(bucket);
        }
        return result;
    }
}
