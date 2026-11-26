package com.tatkalx.concurrency;

import java.util.LinkedList;

/**
 * Generic bounded Producer-Consumer buffer using intrinsic monitor locks,
 * guarded blocks, wait(), and notifyAll() strictly per the Java syllabus.
 */
public class ProducerConsumerBookingBuffer<T> {

    private final LinkedList<T> buffer = new LinkedList<>();
    private final int capacity;
    private boolean producerFinished = false;

    public ProducerConsumerBookingBuffer(int capacity) {
        this.capacity = Math.max(1, capacity);
    }

    public synchronized void produce(T item) throws InterruptedException {
        while (buffer.size() >= capacity) {
            wait();
        }
        buffer.addLast(item);
        notifyAll();
    }

    public synchronized T consume() throws InterruptedException {
        while (buffer.isEmpty() && !producerFinished) {
            wait();
        }
        if (buffer.isEmpty()) {
            return null;
        }
        T item = buffer.removeFirst();
        notifyAll();
        return item;
    }

    public synchronized void markProducerFinished() {
        this.producerFinished = true;
        notifyAll();
    }

    public synchronized int size() {
        return buffer.size();
    }
}
