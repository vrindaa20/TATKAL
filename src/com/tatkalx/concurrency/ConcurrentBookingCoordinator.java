package com.tatkalx.concurrency;

import com.tatkalx.collections.PriorityPolicyComparators;
import com.tatkalx.model.BookingRequest;
import com.tatkalx.service.BerthAllocationService;
import com.tatkalx.service.NotificationService;
import com.tatkalx.service.PaymentService;
import com.tatkalx.simulation.SimulationSessionContext;

import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Coordinates Universe C (Concurrent Booking):
 * Spawns N real Threads, captures NEW/WAITING/BLOCKED/RUNNABLE/TERMINATED states via
 * ThreadExecutionMonitor daemon, releases all threads at virtual 10:00:00 AM, and calls join().
 */
public class ConcurrentBookingCoordinator {

    private final PaymentService paymentService;
    private final NotificationService notificationService;

    private ThreadExecutionMonitor lastMonitor;
    private RaceConditionDetector lastRaceDetector;

    public ConcurrentBookingCoordinator(PaymentService paymentService, NotificationService notificationService) {
        this.paymentService = paymentService;
        this.notificationService = notificationService;
    }

    public long executeConcurrentRace(SimulationSessionContext context,
                                      List<BookingRequest> requests) {
        // Hold virtual clock at 09:59:59 so all worker threads line up in WAITING state before 10:00:00
        context.getVirtualClock().setVirtualTime(LocalTime.of(9, 59, 59));

        BerthAllocationService allocationService = new BerthAllocationService(paymentService, notificationService);
        RaceConditionDetector raceDetector = new RaceConditionDetector();
        this.lastRaceDetector = raceDetector;

        List<Thread> workerThreads = new ArrayList<>(requests.size());
        List<String> bookingIds = new ArrayList<>(requests.size());

        for (int i = 0; i < requests.size(); i++) {
            BookingRequest req = requests.get(i);
            req.setQueuePosition(i + 1);
            req.setStatus("QUEUED");
            bookingIds.add(req.getBookingId());

            BookingWorkerTask task = new BookingWorkerTask(req, context, allocationService, raceDetector, "UPI");
            Thread worker = new Thread(task, String.format("TX-Worker-%03d", i + 1));

            // Map passenger category to Thread priority (1..10)
            int catWeight = PriorityPolicyComparators.categoryWeight(req.getPriorityCategory());
            if (catWeight >= 4) {
                worker.setPriority(Thread.MAX_PRIORITY);
            } else if (catWeight == 3) {
                worker.setPriority(8);
            } else {
                worker.setPriority(Thread.NORM_PRIORITY);
            }
            workerThreads.add(worker);
        }

        ThreadExecutionMonitor monitor = new ThreadExecutionMonitor(
                context.getSessionId(), workerThreads, bookingIds, 4L);
        this.lastMonitor = monitor;

        // Capture NEW state before start()
        monitor.captureSnapshot("Pre-Start (NEW)");

        Thread monitorDaemon = new Thread(monitor, "TX-Monitor-Daemon");
        monitorDaemon.setDaemon(true);
        monitorDaemon.start();

        // Start all worker threads -> they enter WAITING inside TatkalVirtualClock.awaitWindowOpen()
        for (Thread t : workerThreads) {
            t.start();
        }

        try {
            Thread.sleep(25L);
        } catch (InterruptedException ie) {
            Thread.currentThread().interrupt();
        }
        monitor.captureSnapshot("Pre-10AM Gate (WAITING)");

        // Open 10:00:00 AM virtual clock -> notifyAll() wakes every thread simultaneously!
        long raceStartMs = System.currentTimeMillis();
        context.getVirtualClock().openWindowNow();

        // Wait for every competing thread to finish via join()
        for (Thread t : workerThreads) {
            try {
                t.join();
            } catch (InterruptedException ie) {
                Thread.currentThread().interrupt();
            }
        }
        long raceElapsedMs = System.currentTimeMillis() - raceStartMs;

        monitor.stopMonitoring();
        monitorDaemon.interrupt();
        monitor.captureSnapshot("Post-Join (TERMINATED)");

        return raceElapsedMs;
    }

    public ThreadExecutionMonitor getLastMonitor() {
        return lastMonitor;
    }

    public RaceConditionDetector getLastRaceDetector() {
        return lastRaceDetector;
    }
}
