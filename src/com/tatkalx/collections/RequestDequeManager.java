package com.tatkalx.collections;

import com.tatkalx.model.BookingRequest;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Deque;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Queue;
import java.util.Set;

/**
 * Manages incoming booking requests using:
 * - Deque<BookingRequest> (double-ended queue) supporting normal tail enqueue and urgent/retry front requeue
 * - Queue<BookingRequest> view for standard FIFO processors
 * - LinkedHashSet<String> to maintain ordered unique request IDs and prevent duplicate enqueueing
 */
public class RequestDequeManager {

    private final Deque<BookingRequest> requestDeque = new ArrayDeque<>();
    private final Set<String> enqueuedOrderIds = new LinkedHashSet<>();

    /**
     * Enqueues a standard request at the tail of the Deque.
     */
    public synchronized boolean enqueueNormal(BookingRequest request) {
        if (request == null || request.getBookingId() == null) {
            return false;
        }
        if (!enqueuedOrderIds.add(request.getBookingId())) {
            return false;
        }
        request.setQueuePosition(requestDeque.size() + 1);
        request.setStatus("QUEUED");
        return requestDeque.offerLast(request);
    }

    /**
     * Re-inserts a retry or urgent request at the front (head) of the Deque.
     */
    public synchronized boolean enqueueFrontPriority(BookingRequest request) {
        if (request == null || request.getBookingId() == null) {
            return false;
        }
        enqueuedOrderIds.add(request.getBookingId());
        request.setQueuePosition(1);
        request.setStatus("QUEUED");
        return requestDeque.offerFirst(request);
    }

    public synchronized BookingRequest pollNext() {
        return requestDeque.pollFirst();
    }

    public synchronized BookingRequest peekNext() {
        return requestDeque.peekFirst();
    }

    public synchronized BookingRequest peekLast() {
        return requestDeque.peekLast();
    }

    /**
     * Exposes a Queue<BookingRequest> snapshot for sequential FIFO universe processing.
     */
    public synchronized Queue<BookingRequest> asFifoQueueSnapshot() {
        return new ArrayDeque<>(requestDeque);
    }

    public synchronized Set<String> getOrderedUniqueEnqueuedIds() {
        return Collections.unmodifiableSet(new LinkedHashSet<>(enqueuedOrderIds));
    }

    public synchronized List<BookingRequest> snapshotList() {
        return new ArrayList<>(requestDeque);
    }

    public synchronized int size() {
        return requestDeque.size();
    }
}
