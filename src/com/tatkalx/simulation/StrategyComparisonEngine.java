package com.tatkalx.simulation;

import com.tatkalx.collections.PriorityPolicyComparators;
import com.tatkalx.model.BookingRequest;
import com.tatkalx.model.Coach;
import com.tatkalx.model.Passenger;
import com.tatkalx.model.SeatInventory;
import com.tatkalx.model.SimulationResult;
import com.tatkalx.model.TrainSchedule;
import com.tatkalx.service.NotificationService;
import com.tatkalx.service.PaymentService;
import com.tatkalx.utility.ConsoleFormatter;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Executes Universe A (Sequential), Universe B (Priority), and Universe C (Concurrent)
 * on identical cloned passenger/request datasets and isolated inventories, then renders
 * an empirical comparison report.
 */
public class StrategyComparisonEngine {

    private final PaymentService paymentService;
    private final NotificationService notificationService;

    private SequentialUniverse lastUniverseA;
    private PriorityUniverse lastUniverseB;
    private ConcurrentUniverse lastUniverseC;

    public StrategyComparisonEngine(PaymentService paymentService, NotificationService notificationService) {
        this.paymentService = paymentService;
        this.notificationService = notificationService;
    }

    public List<SimulationResult> compareAllThreeUniverses(TrainSchedule schedule,
                                                           Coach coach,
                                                           SeatInventory templateInventory,
                                                           List<Passenger> passengers,
                                                           List<BookingRequest> templateRequests,
                                                           Comparator<BookingRequest> priorityPolicy,
                                                           String policyName) {
        List<SimulationResult> results = new ArrayList<>(3);

        lastUniverseA = new SequentialUniverse(paymentService, notificationService);
        lastUniverseA.prepareUniverse(schedule, coach, templateInventory, passengers, templateRequests);
        results.add(lastUniverseA.runSimulation());

        lastUniverseB = new PriorityUniverse(
                paymentService,
                notificationService,
                priorityPolicy == null ? PriorityPolicyComparators.MEDICAL_SENIOR_FIRST : priorityPolicy,
                policyName == null ? "MEDICAL_SENIOR_FIRST" : policyName
        );
        lastUniverseB.prepareUniverse(schedule, coach, templateInventory, passengers, templateRequests);
        results.add(lastUniverseB.runSimulation());

        lastUniverseC = new ConcurrentUniverse(paymentService, notificationService);
        lastUniverseC.prepareUniverse(schedule, coach, templateInventory, passengers, templateRequests);
        results.add(lastUniverseC.runSimulation());

        return results;
    }

    public static void printComparisonReport(List<SimulationResult> results) {
        ConsoleFormatter.printBanner("TATKAL-X | THREE BOOKING UNIVERSES COMPARATIVE REPORT");
        System.out.printf("%-34s | %4s | %4s | %3s | %3s | %4s | %3s | %7s | %7s | %6s | %6s | %6s | %s%n",
                "Universe", "Req", "Conf", "RAC", "WL", "Fail", "Dup", "TotalMs", "AvgMs", "MaxMs", "Succ%", "Util%", "Consistent");
        ConsoleFormatter.printDivider();
        for (SimulationResult r : results) {
            System.out.printf("%-34s | %4d | %4d | %3d | %3d | %4d | %3d | %7d | %7.2f | %6d | %5.1f%% | %5.1f%% | %-10s%n",
                    truncate(r.getUniverseName(), 34),
                    r.getTotalRequests(),
                    r.getConfirmedCount(),
                    r.getRacCount(),
                    r.getWlCount(),
                    r.getFailedCount(),
                    r.getDuplicateAttempts(),
                    r.getTotalTimeMs(),
                    r.getAvgTimeMs(),
                    r.getMaxTimeMs(),
                    r.getSuccessRatePct(),
                    r.getSeatUtilizationPct(),
                    r.isInventoryConsistent() ? "YES" : "NO");
            System.out.println("   -> Thread Info: " + r.getThreadSummary());
        }
        ConsoleFormatter.printDivider();
    }

    private static String truncate(String s, int max) {
        if (s == null) {
            return "";
        }
        return s.length() <= max ? s : s.substring(0, max - 3) + "...";
    }

    public SequentialUniverse getLastUniverseA() {
        return lastUniverseA;
    }

    public PriorityUniverse getLastUniverseB() {
        return lastUniverseB;
    }

    public ConcurrentUniverse getLastUniverseC() {
        return lastUniverseC;
    }
}
