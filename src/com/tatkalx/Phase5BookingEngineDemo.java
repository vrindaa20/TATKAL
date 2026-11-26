package com.tatkalx;

import com.tatkalx.collections.PriorityPolicyComparators;
import com.tatkalx.exception.BookingValidationException;
import com.tatkalx.model.BookingEvent;
import com.tatkalx.model.BookingRequest;
import com.tatkalx.model.Coach;
import com.tatkalx.model.Passenger;
import com.tatkalx.model.Refund;
import com.tatkalx.model.SeatInventory;
import com.tatkalx.model.SimulationResult;
import com.tatkalx.model.TrainSchedule;
import com.tatkalx.service.BerthAllocationService;
import com.tatkalx.service.BookingOutcome;
import com.tatkalx.service.CancellationRefundService;
import com.tatkalx.service.NotificationService;
import com.tatkalx.service.PaymentService;
import com.tatkalx.service.QueuePromotionService;
import com.tatkalx.service.RailwayManagementService;
import com.tatkalx.service.TatkalVirtualClock;
import com.tatkalx.simulation.PriorityUniverse;
import com.tatkalx.simulation.SequentialUniverse;
import com.tatkalx.simulation.SimulationSessionContext;
import com.tatkalx.simulation.WorkloadGenerator;
import com.tatkalx.utility.ConsoleFormatter;
import com.tatkalx.utility.IdGenerator;

import java.time.LocalTime;
import java.util.List;

/**
 * Phase 5 Verification Runner:
 * Tests TatkalVirtualClock, Berth Allocation, Payment Rollback, RAC/WL Cascade Promotion,
 * Cancellation/Refunds, and Universe A (Sequential) vs Universe B (Priority).
 */
public class Phase5BookingEngineDemo {

    public static void main(String[] args) throws BookingValidationException {
        ConsoleFormatter.printBanner("TATKAL-X | PHASE 5: BOOKING ENGINE & UNIVERSE A/B VERIFICATION");

        RailwayManagementService railway = new RailwayManagementService();
        railway.loadDefaultRailwayData();
        TrainSchedule schedule = railway.getSchedule(1L);

        // Make small isolated coach for clear RAC/WL/Cancellation walkthrough: 2 Confirmed, 1 RAC, 2 WL
        Coach demoCoach = new Coach(99, 1, "B9", "3A", 10, 2, 1, 2, 1.45);
        railway.addCoachWithAutoBerths(demoCoach);
        // Use demoCoach directly for fine-grained cascade test
        SeatInventory tightInv = new SeatInventory(1L, 1L, demoCoach,
                java.util.Arrays.asList(
                        new com.tatkalx.model.Berth(99001, 99, "B9", 1, "LB", true, false),
                        new com.tatkalx.model.Berth(99002, 99, "B9", 2, "SL", true, false)
                ));

        TatkalVirtualClock clock = new TatkalVirtualClock();
        clock.setVirtualTime(LocalTime.of(9, 59, 57));

        SimulationSessionContext liveCtx = new SimulationSessionContext(
                1L, "LIVE_TEST", schedule, demoCoach, tightInv, clock);

        WorkloadGenerator generator = new WorkloadGenerator();
        List<Passenger> testPax = generator.generatePassengers(6);
        for (Passenger p : testPax) {
            liveCtx.getPassengerRegistry().registerPassenger(p);
        }

        PaymentService paymentService = new PaymentService();
        paymentService.setSimulatedLatencyMs(2L);
        NotificationService notificationService = new NotificationService();
        BerthAllocationService allocator = new BerthAllocationService(paymentService, notificationService);
        QueuePromotionService promoter = new QueuePromotionService(notificationService);
        CancellationRefundService canceller = new CancellationRefundService(promoter, notificationService);

        // Test 1: Booking before 10:00 AM
        ConsoleFormatter.printSubHeader("1. Tatkal Virtual Clock Guard (09:59:57 vs 10:00:00)");
        System.out.println("  " + clock.getCountdownBanner());
        BookingOutcome earlyOutcome = allocator.allocateAndConfirm(
                new BookingRequest(IdGenerator.nextBookingId(), 1L, 1, "Passenger-1", 21, "NORMAL", 1L, "3A", 1, "09:59:57"),
                liveCtx, "UPI");
        System.out.println("  Early booking result: " + earlyOutcome.getOutcomeStatus() + " -> " + earlyOutcome.getMessage());

        // Open clock at 10:00:00 AM
        clock.openWindowNow();
        System.out.println("  " + clock.getCountdownBanner());

        // Test 2: Payment failure rollback (must NOT leak seat!)
        ConsoleFormatter.printSubHeader("2. Payment Failure Rollback Integrity Check");
        paymentService.setForceNextFailure(true);
        BookingOutcome failPay = allocator.allocateAndConfirm(
                new BookingRequest(IdGenerator.nextBookingId(), 1L, 1, "Passenger-1", 21, "NORMAL", 1L, "3A", 1, "10:00:00.100"),
                liveCtx, "UPI");
        System.out.println("  Payment fail status: " + failPay.getOutcomeStatus() + " | Inventory: " + liveCtx.getSeatInventory());

        // Test 3: Fill Confirmed (2), RAC (1), WL (2), and Reject 6th (SeatUnavailableException)
        ConsoleFormatter.printSubHeader("3. Confirmed -> RAC -> WL -> Exhausted Flow");
        BookingOutcome[] outcomes = new BookingOutcome[6];
        for (int i = 0; i < 6; i++) {
            Passenger p = testPax.get(i);
            outcomes[i] = allocator.allocateAndConfirm(
                    new BookingRequest(IdGenerator.nextBookingId(), 1L, p.getPassengerId(), p.getFullName(),
                            p.getAge(), p.getPriorityCategory(), 1L, "3A", 1, "10:00:0" + i + ".200"),
                    liveCtx, "WALLET");
            String pnrStr = outcomes[i].getPnrRecord() == null ? "NONE" : outcomes[i].getPnrRecord().getPnrNumber();
            String seatStr = outcomes[i].getTicket() == null ? outcomes[i].getMessage() : outcomes[i].getTicket().getSeatLabel();
            System.out.printf("  Pax-%d -> %-9s | PNR: %-10s | %s%n",
                    p.getPassengerId(), outcomes[i].getOutcomeStatus(), pnrStr, seatStr);
        }

        // Test 4: Duplicate passenger prevention
        BookingOutcome dupOutcome = allocator.allocateAndConfirm(
                new BookingRequest(IdGenerator.nextBookingId(), 1L, 1, "Passenger-1", 21, "NORMAL", 1L, "3A", 1, "10:00:09.000"),
                liveCtx, "UPI");
        System.out.println("  Duplicate attempt by Pax-1: " + dupOutcome.getOutcomeStatus() + " -> " + dupOutcome.getMessage());

        // Test 5: Cancel Confirmed Ticket #1 -> Refund Rs 0 + RAC-1 -> CONFIRMED + WL-1 -> RAC!
        ConsoleFormatter.printSubHeader("4. Ticket Cancellation, Refund & Automatic RAC/WL Promotion");
        String pnrToCancel = outcomes[0].getPnrRecord().getPnrNumber();
        Refund refund = canceller.cancelTicketByPnr(pnrToCancel, "Change of plans", false, liveCtx);
        System.out.println("  Cancelled PNR " + pnrToCancel + " -> " + refund);
        System.out.println("  Promoted RAC-1 PNR (" + outcomes[2].getPnrRecord().getPnrNumber() + ") status: "
                + liveCtx.getPnrDirectory().lookupPnr(outcomes[2].getPnrRecord().getPnrNumber()));
        System.out.println("  Promoted WL-1  PNR (" + outcomes[3].getPnrRecord().getPnrNumber() + ") status: "
                + liveCtx.getPnrDirectory().lookupPnr(outcomes[3].getPnrRecord().getPnrNumber()));
        System.out.println("  Final Tight Inventory: " + liveCtx.getSeatInventory());

        // Sample Forensic Events captured for Pax-1
        ConsoleFormatter.printSubHeader("5. Sample Captured Events for Booking " + outcomes[0].getRequest().getBookingId());
        for (BookingEvent ev : liveCtx.getEventsForBooking(outcomes[0].getRequest().getBookingId())) {
            System.out.println("  " + ev);
        }

        // Test 6: Universe A (Sequential) vs Universe B (Priority) on 25 requests vs 12 seats!
        ConsoleFormatter.printSubHeader("6. Universe A (Sequential) vs Universe B (Priority) on Identical 25-Request Workload");
        Coach uniCoach = new Coach(1, 1, "B1", "3A", 72, 12, 4, 8, 1.45);
        railway.addCoachWithAutoBerths(uniCoach);
        SeatInventory templateInv = railway.createIsolatedInventory(0L, 1L, "3A");

        List<Passenger> batchPax = generator.generatePassengers(25);
        List<BookingRequest> batchReqs = generator.generateRequests(batchPax, 1L, "3A", true);

        PaymentService simPay = new PaymentService();
        simPay.setSimulatedLatencyMs(1L);
        NotificationService simNotify = new NotificationService();

        SequentialUniverse universeA = new SequentialUniverse(simPay, simNotify);
        universeA.prepareUniverse(schedule, uniCoach, templateInv, batchPax, batchReqs);
        SimulationResult resA = universeA.runSimulation();

        PriorityUniverse universeB = new PriorityUniverse(
                simPay, simNotify, PriorityPolicyComparators.MEDICAL_SENIOR_FIRST, "MEDICAL_SENIOR_FIRST");
        universeB.prepareUniverse(schedule, uniCoach, templateInv, batchPax, batchReqs);
        SimulationResult resB = universeB.runSimulation();

        printComparisonRow(resA);
        printComparisonRow(resB);
    }

    private static void printComparisonRow(SimulationResult r) {
        System.out.printf("  %-42s | Req=%d | Conf=%d | RAC=%d | WL=%d | Fail=%d | Dup=%d | Time=%dms | Consistent=%s%n",
                r.getUniverseName(), r.getTotalRequests(), r.getConfirmedCount(), r.getRacCount(),
                r.getWlCount(), r.getFailedCount(), r.getDuplicateAttempts(), r.getTotalTimeMs(), r.isInventoryConsistent());
    }
}
