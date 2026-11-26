package com.tatkalx.simulation;

import com.tatkalx.model.BookingRequest;
import com.tatkalx.model.SimulationResult;
import com.tatkalx.service.BerthAllocationService;
import com.tatkalx.service.FifoBookingProcessor;
import com.tatkalx.service.NotificationService;
import com.tatkalx.service.PaymentService;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.Queue;

/**
 * UNIVERSE A: SEQUENTIAL BOOKING
 * Uses ArrayList, Queue, and Iterator to process booking requests strictly one after another.
 */
public class SequentialUniverse extends AbstractSimulationUniverse {

    private final PaymentService paymentService;
    private final NotificationService notificationService;

    public SequentialUniverse(PaymentService paymentService, NotificationService notificationService) {
        this.paymentService = paymentService;
        this.notificationService = notificationService;
    }

    @Override
    public String getUniverseName() {
        return "UNIVERSE A (Sequential FIFO)";
    }

    @Override
    public SimulationResult runSimulation() {
        context.getVirtualClock().openWindowNow();

        ArrayList<BookingRequest> stagingList = new ArrayList<>(preparedRequests);
        Queue<BookingRequest> fifoQueue = new ArrayDeque<>();
        Iterator<BookingRequest> stagingIterator = stagingList.iterator();
        int position = 1;
        while (stagingIterator.hasNext()) {
            BookingRequest req = stagingIterator.next();
            req.setQueuePosition(position++);
            req.setStatus("QUEUED");
            fifoQueue.offer(req);
        }

        FifoBookingProcessor processor = new FifoBookingProcessor(
                new BerthAllocationService(paymentService, notificationService));

        long startMs = System.currentTimeMillis();
        while (!fifoQueue.isEmpty()) {
            BookingRequest next = fifoQueue.poll();
            processor.processBooking(next, context);
        }
        long elapsedMs = System.currentTimeMillis() - startMs;

        return computeMetrics(elapsedMs, "Single-Threaded (main) FIFO Queue");
    }
}
