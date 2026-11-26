package com.tatkalx;

import com.tatkalx.collections.PriorityPolicyComparators;
import com.tatkalx.dao.BookingDao;
import com.tatkalx.dao.FinancialDao;
import com.tatkalx.dao.ForensicDao;
import com.tatkalx.dao.RailwayDao;
import com.tatkalx.dao.UserPassengerDao;
import com.tatkalx.database.DatabaseConnectionManager;
import com.tatkalx.database.DatabaseConsistencyMonitor;
import com.tatkalx.database.DatabaseInitializer;
import com.tatkalx.filehandling.CsvImportExportService;
import com.tatkalx.filehandling.CsvImportResult;
import com.tatkalx.filehandling.FileLogManager;
import com.tatkalx.investigation.BookingExecutionLogger;
import com.tatkalx.investigation.FailureLabResult;
import com.tatkalx.investigation.FailureLabService;
import com.tatkalx.investigation.ForensicReconstructor;
import com.tatkalx.model.Berth;
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
import com.tatkalx.simulation.SimulationSessionContext;
import com.tatkalx.simulation.StrategyComparisonEngine;
import com.tatkalx.simulation.WorkloadGenerator;
import com.tatkalx.utility.ConsoleFormatter;
import com.tatkalx.utility.IdGenerator;

import java.time.LocalTime;
import java.util.Arrays;
import java.util.List;

/**
 * Phase 10 Automated Integration Test Suite (Plain Java, No External Test Framework).
 * Verifies all 26 required scenarios end-to-end.
 */
public class Phase10IntegrationTest {

    private static int passed = 0;
    private static int failed = 0;

    private static void check(String label, boolean condition) {
        if (condition) {
            passed++;
            System.out.printf("  [PASS] %s%n", label);
        } else {
            failed++;
            System.out.printf("  [FAIL] %s%n", label);
        }
    }

    public static void main(String[] args) throws Exception {
        ConsoleFormatter.printBanner("TATKAL-X | PHASE 10 END-TO-END 26-SCENARIO VERIFICATION SUITE");

        DatabaseConnectionManager dbManager = new DatabaseConnectionManager();
        new DatabaseInitializer(dbManager).initializeDatabase();
        UserPassengerDao userDao = new UserPassengerDao(dbManager);
        RailwayDao railwayDao = new RailwayDao(dbManager);
        BookingDao bookingDao = new BookingDao(dbManager);
        FinancialDao financialDao = new FinancialDao(dbManager);
        ForensicDao forensicDao = new ForensicDao(dbManager);

        // 1-3: Registration, Auth, Train Search
        String tag = String.valueOf(System.currentTimeMillis() % 100000);
        Passenger regPax = new Passenger(0, 0, "test_" + tag, "Test Passenger",
                "test" + tag + "@tatkalx.in", "97000" + String.format("%05d", Integer.parseInt(tag)),
                28, "M", "AADHAAR", "TST-" + tag, true, "NORMAL", "LB", 12000.0);
        regPax.setPasswordHash("pass123");
        int newId = userDao.insert(regPax);
        check("1. Passenger registration persisted in SQLite", newId > 0);
        check("2. Authentication succeeds for registered passenger & admin",
                userDao.authenticate("test_" + tag, "pass123") != null
                        && userDao.authenticate("admin_tx", "admin123") != null);
        check("3. Train searching returns seeded schedules", !railwayDao.findAllSchedules().isEmpty());

        // 4-6: Tatkal Window Timing (Before 10 AM, At 10 AM, After Window Closes)
        RailwayManagementService rms = new RailwayManagementService();
        rms.loadDefaultRailwayData();
        TrainSchedule schedule = rms.getSchedule(1L);
        Coach tightCoach = new Coach(50, 1, "B5", "3A", 10, 2, 1, 1, 1.45);
        SeatInventory tightInv = new SeatInventory(901L, 1L, tightCoach, Arrays.asList(
                new Berth(5001, 50, "B5", 1, "LB", true, false),
                new Berth(5002, 50, "B5", 2, "SL", true, false)
        ));
        TatkalVirtualClock clock = new TatkalVirtualClock();
        clock.setVirtualTime(LocalTime.of(9, 59, 50));

        SimulationSessionContext ctx = new SimulationSessionContext(901L, "TEST", schedule, tightCoach, tightInv, clock);
        WorkloadGenerator gen = new WorkloadGenerator();
        List<Passenger> paxList = gen.generatePassengers(6);
        for (Passenger p : paxList) {
            ctx.getPassengerRegistry().registerPassenger(p);
        }

        PaymentService paySvc = new PaymentService();
        paySvc.setSimulatedLatencyMs(1L);
        NotificationService notifySvc = new NotificationService();
        BerthAllocationService alloc = new BerthAllocationService(paySvc, notifySvc);

        BookingOutcome pre10 = alloc.allocateAndConfirm(
                new BookingRequest(IdGenerator.nextBookingId(), 901L, 1, "Passenger-1", 21, "NORMAL", 1L, "3A", 1, "09:59:50"),
                ctx, "UPI");
        check("4. Booking before 10:00 AM rejected", !pre10.isSuccess());

        clock.openWindowNow();
        BookingOutcome o1 = alloc.allocateAndConfirm(
                new BookingRequest(IdGenerator.nextBookingId(), 901L, 1, "Passenger-1", 21, "NORMAL", 1L, "3A", 1, "10:00:00"),
                ctx, "WALLET");
        BookingOutcome o2 = alloc.allocateAndConfirm(
                new BookingRequest(IdGenerator.nextBookingId(), 901L, 2, "Passenger-2", 22, "NORMAL", 1L, "3A", 1, "10:00:01"),
                ctx, "WALLET");
        BookingOutcome o3Rac = alloc.allocateAndConfirm(
                new BookingRequest(IdGenerator.nextBookingId(), 901L, 3, "Passenger-3", 23, "NORMAL", 1L, "3A", 1, "10:00:02"),
                ctx, "WALLET");
        BookingOutcome o4Wl = alloc.allocateAndConfirm(
                new BookingRequest(IdGenerator.nextBookingId(), 901L, 4, "Passenger-4", 63, "SENIOR_CITIZEN", 1L, "3A", 1, "10:00:03"),
                ctx, "WALLET");

        check("5 & 7. Booking at 10 AM confirms seats", "CONFIRMED".equals(o1.getOutcomeStatus()) && "CONFIRMED".equals(o2.getOutcomeStatus()));
        check("8. RAC allocation when confirmed quota full", "RAC".equals(o3Rac.getOutcomeStatus()));
        check("9. Waiting-list allocation when RAC full", "WL".equals(o4Wl.getOutcomeStatus()));

        clock.closeWindowNow();
        BookingOutcome afterClose = alloc.allocateAndConfirm(
                new BookingRequest(IdGenerator.nextBookingId(), 901L, 5, "Passenger-5", 25, "NORMAL", 1L, "3A", 1, "11:01:00"),
                ctx, "UPI");
        check("6. Booking after Tatkal window closes rejected", !afterClose.isSuccess());

        // 12, 14-18: Duplicate, Cancel, Refund, RAC->CONFIRMED, WL->RAC, Payment Failure
        clock.openWindowNow();
        BookingOutcome dup = alloc.allocateAndConfirm(
                new BookingRequest(IdGenerator.nextBookingId(), 901L, 1, "Passenger-1", 21, "NORMAL", 1L, "3A", 1, "10:00:04"),
                ctx, "UPI");
        check("12. Duplicate passenger booking blocked", !dup.isSuccess());

        CancellationRefundService canceller = new CancellationRefundService(new QueuePromotionService(notifySvc), notifySvc);
        Refund confRefund = canceller.cancelTicketByPnr(o1.getPnrRecord().getPnrNumber(), "Cancel test", false, ctx);
        check("14-17. Cancel + Refund + RAC->CONFIRMED + WL->RAC promotion",
                confRefund.getRefundAmount() == 0.0
                        && "CONFIRMED".equals(ctx.getPnrDirectory().lookupPnr(o3Rac.getPnrRecord().getPnrNumber()).getCurrentStatus())
                        && "RAC".equals(ctx.getPnrDirectory().lookupPnr(o4Wl.getPnrRecord().getPnrNumber()).getCurrentStatus()));

        paySvc.setForceNextFailure(true);
        int availBeforeFail = tightInv.getAvailableTatkalSeats();
        BookingOutcome payFail = alloc.allocateAndConfirm(
                new BookingRequest(IdGenerator.nextBookingId(), 901L, 6, "Passenger-6", 26, "NORMAL", 1L, "3A", 1, "10:00:05"),
                ctx, "UPI");
        check("18. Payment failure rolls back seat without leak",
                !payFail.isSuccess() && tightInv.getAvailableTatkalSeats() == availBeforeFail);

        // 10-11, 13, 19-20, 25: Failure Lab + Concurrent 3-Universe Comparison
        FailureLabService lab = new FailureLabService(dbManager, forensicDao,
                new BookingExecutionLogger(new FileLogManager(), forensicDao));
        List<FailureLabResult> labResults = lab.runAllEightScenarios();
        boolean allLabConsistent = labResults.stream().allMatch(FailureLabResult::isInventoryConsistent);
        check("10,11,13,19,20. Failure Lab (Last-seat, Thread interrupt, Sync, Concurrent) consistent", allLabConsistent && labResults.size() == 8);

        StrategyComparisonEngine cmp = new StrategyComparisonEngine(paySvc, notifySvc);
        List<Passenger> racePax = gen.generatePassengers(100);
        List<BookingRequest> raceReqs = gen.generateRequests(racePax, 1L, "3A", true);
        Coach raceCoach = new Coach(1, 1, "B1", "3A", 72, 12, 4, 8, 1.45);
        rms.addCoachWithAutoBerths(raceCoach);
        List<SimulationResult> simResults = cmp.compareAllThreeUniverses(
                schedule, raceCoach, rms.createIsolatedInventory(0L, 1L, "3A"),
                racePax, raceReqs, PriorityPolicyComparators.MEDICAL_SENIOR_FIRST, "MEDICAL_SENIOR_FIRST");
        check("25. 3-Universe Simulation Comparison all consistent",
                simResults.size() == 3 && simResults.stream().allMatch(SimulationResult::isInventoryConsistent));

        // 21-24, 26: DB Connectivity, CRUD, CSV Import/Export, Forensics, DB Consistency
        check("21-22. JDBC CRUD & Queries operational",
                bookingDao.findTicketsByPassenger(newId) != null && financialDao.findAllPayments() != null);
        CsvImportExportService csv = new CsvImportExportService();
        csv.createSamplePassengersCsv("data/sample_passengers.csv");
        CsvImportResult csvRes = csv.importPassengersFromCsv("data/sample_passengers.csv");
        check("23. CSV import validates valid & rejects invalid rows",
                csvRes.getValidPassengers().size() == 3 && csvRes.getInvalidRows().size() == 2);

        ForensicReconstructor reconstructor = new ForensicReconstructor(forensicDao, new FileLogManager());
        check("24. Booking Forensic Reconstruction captures real timeline",
                !reconstructor.loadTimeline(o1.getRequest().getBookingId(), ctx).isEmpty());

        DatabaseConsistencyMonitor.ConsistencyReport dbReport = new DatabaseConsistencyMonitor(dbManager).runFullConsistencyAudit();
        check("26. Database Consistency Monitor passes all 6 checks", dbReport.isAllPassed());

        ConsoleFormatter.printDivider();
        System.out.printf("  PHASE 10 TEST SUMMARY: PASSED=%d | FAILED=%d%n", passed, failed);
        if (failed > 0) {
            System.exit(1);
        }
    }
}
