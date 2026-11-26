package com.tatkalx.collections;

import com.tatkalx.model.BookingRequest;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.PriorityQueue;

/**
 * Configurable Comparator policies and PriorityQueue manager for Universe B (Priority-Based Booking).
 */
public class PriorityPolicyComparators {

    @FunctionalInterface
    public interface PriorityPolicy extends Comparator<BookingRequest> {
        // Inherits int compare(BookingRequest a, BookingRequest b)
    }

    /**
     * Policy 1: Priority category (MEDICAL > SENIOR_CITIZEN > DEFENCE > NORMAL),
     * then age descending, then earliest requestTimestamp.
     */
    public static final Comparator<BookingRequest> MEDICAL_SENIOR_FIRST = new Comparator<BookingRequest>() {
        @Override
        public int compare(BookingRequest a, BookingRequest b) {
            int weightCmp = Integer.compare(categoryWeight(b.getPriorityCategory()), categoryWeight(a.getPriorityCategory()));
            if (weightCmp != 0) {
                return weightCmp;
            }
            int ageCmp = Integer.compare(b.getPassengerAge(), a.getPassengerAge());
            if (ageCmp != 0) {
                return ageCmp;
            }
            return a.compareTo(b);
        }
    };

    /**
     * Policy 2: Explicit pre-computed priorityScore descending, tie-broken by natural timestamp order.
     */
    public static final Comparator<BookingRequest> SCORE_DESCENDING = (a, b) -> {
        int scoreCmp = Integer.compare(b.getPriorityScore(), a.getPriorityScore());
        if (scoreCmp != 0) {
            return scoreCmp;
        }
        return a.compareTo(b);
    };

    /**
     * Policy 3: Smallest party size first (maximizes number of satisfied requests), then timestamp.
     */
    public static final Comparator<BookingRequest> SMALLEST_PARTY_FIRST = (a, b) -> {
        int seatCmp = Integer.compare(a.getRequestedSeats(), b.getRequestedSeats());
        if (seatCmp != 0) {
            return seatCmp;
        }
        return a.compareTo(b);
    };

    public static int categoryWeight(String category) {
        if (category == null) {
            return 1;
        }
        switch (category.toUpperCase()) {
            case "MEDICAL":
                return 4;
            case "SENIOR_CITIZEN":
                return 3;
            case "DEFENCE":
                return 2;
            default:
                return 1;
        }
    }

    /**
     * Builds a PriorityQueue ordered by the chosen Comparator and drains items in priority order.
     */
    public static List<BookingRequest> orderRequestsByPolicy(List<BookingRequest> requests,
                                                             Comparator<BookingRequest> policy) {
        PriorityQueue<BookingRequest> pq = new PriorityQueue<>(Math.max(1, requests.size()), policy);
        pq.addAll(requests);

        List<BookingRequest> ordered = new ArrayList<>(pq.size());
        while (!pq.isEmpty()) {
            ordered.add(pq.poll());
        }
        return ordered;
    }
}
