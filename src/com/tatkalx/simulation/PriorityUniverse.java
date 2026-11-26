package com.tatkalx.simulation;

import com.tatkalx.collections.PriorityPolicyComparators;
import com.tatkalx.model.BookingRequest;
import com.tatkalx.model.SimulationResult;
import com.tatkalx.service.BerthAllocationService;
import com.tatkalx.service.NotificationService;
import com.tatkalx.service.PaymentService;
import com.tatkalx.service.PriorityBookingProcessor;

import java.util.Comparator;
import java.util.PriorityQueue;

/**
 * UNIVERSE B: PRIORITY-BASED BOOKING
 * Uses PriorityQueue with a configurable Comparator policy (Comparable tie-breaker).
 */
public class PriorityUniverse extends AbstractSimulationUniverse {

    private final PaymentService paymentService;
    private final NotificationService notificationService;
    private Comparator<BookingRequest> priorityPolicy;
    private String policyName;

    public PriorityUniverse(PaymentService paymentService,
                            NotificationService notificationService,
                            Comparator<BookingRequest> priorityPolicy,
                            String policyName) {
        this.paymentService = paymentService;
        this.notificationService = notificationService;
        this.priorityPolicy = priorityPolicy == null
                ? PriorityPolicyComparators.MEDICAL_SENIOR_FIRST
                : priorityPolicy;
        this.policyName = policyName == null ? "MEDICAL_SENIOR_FIRST" : policyName;
    }

    public void setPriorityPolicy(Comparator<BookingRequest> priorityPolicy, String policyName) {
        this.priorityPolicy = priorityPolicy;
        this.policyName = policyName;
    }

    @Override
    public String getUniverseName() {
        return "UNIVERSE B (PriorityQueue: " + policyName + ")";
    }

    @Override
    public SimulationResult runSimulation() {
        context.getVirtualClock().openWindowNow();

        PriorityQueue<BookingRequest> priorityQueue = new PriorityQueue<>(
                Math.max(1, preparedRequests.size()), priorityPolicy);

        for (BookingRequest req : preparedRequests) {
            req.setPriorityScore(PriorityBookingProcessor.computePriorityScore(req));
            req.setStatus("QUEUED");
            priorityQueue.offer(req);
        }

        PriorityBookingProcessor processor = new PriorityBookingProcessor(
                new BerthAllocationService(paymentService, notificationService));

        long startMs = System.currentTimeMillis();
        int processingOrder = 1;
        while (!priorityQueue.isEmpty()) {
            BookingRequest next = priorityQueue.poll();
            next.setQueuePosition(processingOrder++);
            processor.processBooking(next, context);
        }
        long elapsedMs = System.currentTimeMillis() - startMs;

        return computeMetrics(elapsedMs, "Single-Threaded PriorityQueue (" + policyName + ")");
    }
}
