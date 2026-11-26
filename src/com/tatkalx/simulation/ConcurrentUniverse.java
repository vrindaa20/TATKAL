package com.tatkalx.simulation;

import com.tatkalx.concurrency.ConcurrentBookingCoordinator;
import com.tatkalx.concurrency.RaceConditionDetector;
import com.tatkalx.concurrency.ThreadExecutionMonitor;
import com.tatkalx.model.SimulationResult;
import com.tatkalx.service.NotificationService;
import com.tatkalx.service.PaymentService;

/**
 * UNIVERSE C: CONCURRENT BOOKING
 * Spawns hundreds/thousands of real Java Threads competing simultaneously at 10:00 AM
 * over shared SeatInventory protected by synchronized critical sections.
 */
public class ConcurrentUniverse extends AbstractSimulationUniverse {

    private final ConcurrentBookingCoordinator coordinator;

    public ConcurrentUniverse(PaymentService paymentService, NotificationService notificationService) {
        this.coordinator = new ConcurrentBookingCoordinator(paymentService, notificationService);
    }

    @Override
    public String getUniverseName() {
        return "UNIVERSE C (Concurrent Threads)";
    }

    @Override
    public SimulationResult runSimulation() {
        long elapsedMs = coordinator.executeConcurrentRace(context, preparedRequests);
        RaceConditionDetector detector = coordinator.getLastRaceDetector();
        int collisions = detector == null ? 0 : detector.getTotalContentionCollisions();
        int lastSeatRaces = detector == null ? 0 : detector.getLastSeatRaceAttempts();

        String summary = String.format("%d Threads | ContentionCollisions=%d | LastSeatRaces=%d",
                preparedRequests.size(), collisions, lastSeatRaces);
        return computeMetrics(elapsedMs, summary);
    }

    public ThreadExecutionMonitor getMonitor() {
        return coordinator.getLastMonitor();
    }

    public RaceConditionDetector getRaceDetector() {
        return coordinator.getLastRaceDetector();
    }
}
