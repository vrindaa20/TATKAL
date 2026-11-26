package com.tatkalx;

import com.tatkalx.collections.PriorityPolicyComparators;
import com.tatkalx.concurrency.BookingTimeoutMonitor;
import com.tatkalx.concurrency.ProducerConsumerBookingBuffer;
import com.tatkalx.concurrency.RaceConditionDetector;
import com.tatkalx.concurrency.ThreadExecutionMonitor;
import com.tatkalx.model.BookingRequest;
import com.tatkalx.model.Coach;
import com.tatkalx.model.Passenger;
import com.tatkalx.model.SeatInventory;
import com.tatkalx.model.SimulationResult;
import com.tatkalx.model.ThreadLog;
import com.tatkalx.model.TrainSchedule;
import com.tatkalx.service.NotificationService;
import com.tatkalx.service.PaymentService;
import com.tatkalx.service.RailwayManagementService;
import com.tatkalx.simulation.StrategyComparisonEngine;
import com.tatkalx.simulation.WorkloadGenerator;
import com.tatkalx.utility.ConsoleFormatter;

import java.util.List;
import java.util.Map;

/**
 * Phase 6 Verification Runner:
 * Demonstrates Producer-Consumer coordination, Daemon Timeout Monitor + interrupt(),
 * 300-thread concurrent Tatkal race, Thread Lifecycle state histogram, Race Condition Detection,
 * and full 3-Universe Comparison (Sequential vs Priority vs Concurrent).
 */
public class Phase6MultithreadingDemo {

    public static void main(String[] args) throws InterruptedException {
        ConsoleFormatter.printBanner("TATKAL-X | PHASE 6: MULTITHREADING & 3-UNIVERSE RACE LAB");

        // 1. Producer-Consumer Coordination (wait / notifyAll)
        ConsoleFormatter.printSubHeader("1. Producer-Consumer Coordination (wait / notifyAll)");
        ProducerConsumerBookingBuffer<String> pcBuffer = new ProducerConsumerBookingBuffer<>(3);
        Thread producer = new Thread(() -> {
            try {
                for (int i = 1; i <= 5; i++) {
                    String item = "REQ-" + i;
                    pcBuffer.produce(item);
                    System.out.println("  [Producer] Enqueued " + item);
                }
                pcBuffer.markProducerFinished();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }, "PC-Producer");

        Thread consumer = new Thread(() -> {
            try {
                String item;
                while ((item = pcBuffer.consume()) != null) {
                    System.out.println("  [Consumer] Processed " + item);
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }, "PC-Consumer");

        producer.start();
        consumer.start();
        producer.join();
        consumer.join();

        // 2. Daemon Timeout Monitor + sleep() + interrupt()
        ConsoleFormatter.printSubHeader("2. Daemon Timeout Monitor (sleep + interrupt)");
        BookingTimeoutMonitor timeoutMonitor = new BookingTimeoutMonitor(15L);
        Thread daemonThread = new Thread(timeoutMonitor, "Timeout-Daemon");
        daemonThread.setDaemon(true);
        daemonThread.start();
        Thread.sleep(50L);
        timeoutMonitor.stopMonitor();
        daemonThread.interrupt();
        daemonThread.join();
        for (String log : timeoutMonitor.getSweepLogs()) {
            System.out.println("  " + log);
        }

        // 3. Full 3-Universe Race with 300 Requests Competing for 24 Tatkal Seats (+ 6 RAC + 12 WL)
        ConsoleFormatter.printSubHeader("3. Running 300-Passenger Identical Workload Across Universes A, B, and C");
        RailwayManagementService railway = new RailwayManagementService();
        railway.loadDefaultRailwayData();
        Coach raceCoach = new Coach(10, 2, "B2", "3A", 72, 24, 6, 12, 1.45);
        railway.addTrain(new com.tatkalx.model.Train(2, "12954", "August Kranti Rajdhani", "RAJDHANI", 1, true, "ACTIVE"));
        TrainSchedule raceSchedule = new TrainSchedule(
                2L, 2, 1, "12954", "August Kranti Rajdhani", 1377,
                "2026-11-26", "17:15", "09:10", "10:00:00", "11:00:00", "SCHEDULED");
        railway.addSchedule(raceSchedule);
        railway.addCoachWithAutoBerths(raceCoach);
        SeatInventory templateInventory = railway.createIsolatedInventory(0L, 2L, "3A");

        WorkloadGenerator generator = new WorkloadGenerator();
        List<Passenger> passengers = generator.generatePassengers(300);
        List<BookingRequest> requests = generator.generateRequests(passengers, 2L, "3A", true);

        PaymentService paymentService = new PaymentService();
        paymentService.setSimulatedLatencyMs(2L);
        NotificationService notificationService = new NotificationService();

        StrategyComparisonEngine engine = new StrategyComparisonEngine(paymentService, notificationService);
        List<SimulationResult> results = engine.compareAllThreeUniverses(
                raceSchedule, raceCoach, templateInventory, passengers, requests,
                PriorityPolicyComparators.MEDICAL_SENIOR_FIRST, "MEDICAL_SENIOR_FIRST");

        StrategyComparisonEngine.printComparisonReport(results);

        // 4. Thread Lifecycle & Contention Evidence from Universe C
        ConsoleFormatter.printSubHeader("4. Universe C Thread Lifecycle States & Race Condition Detector");
        ThreadExecutionMonitor monitor = engine.getLastUniverseC().getMonitor();
        for (Map.Entry<Thread.State, Integer> entry : monitor.getStateHistogram().entrySet()) {
            System.out.printf("  State %-14s : %d samples%n", entry.getKey(), entry.getValue());
        }

        System.out.println("\n  Sample Thread Logs (first 6):");
        List<ThreadLog> logs = monitor.getCapturedLogs();
        for (int i = 0; i < Math.min(6, logs.size()); i++) {
            System.out.println("  " + logs.get(i));
        }

        RaceConditionDetector detector = engine.getLastUniverseC().getRaceDetector();
        System.out.println("\n  Contention Collisions Detected & Safely Serialized: " + detector.getTotalContentionCollisions());
        System.out.println("  Last-Seat Race Attempts Safely Blocked: " + detector.getLastSeatRaceAttempts());
        for (int i = 0; i < Math.min(5, detector.getCollisionTraces().size()); i++) {
            System.out.println("   * " + detector.getCollisionTraces().get(i));
        }

        System.out.println("\n  Actual First 8 Concurrent Execution Outcomes:");
        List<String> execOrder = engine.getLastUniverseC().getContext().getExecutionOrderLog();
        for (int i = 0; i < Math.min(8, execOrder.size()); i++) {
            System.out.println("   -> " + execOrder.get(i));
        }
    }
}
