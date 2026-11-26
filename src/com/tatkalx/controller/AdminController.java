package com.tatkalx.controller;

import com.tatkalx.collections.PriorityPolicyComparators;
import com.tatkalx.database.DatabaseConsistencyMonitor;
import com.tatkalx.filehandling.CsvImportResult;
import com.tatkalx.investigation.FailureLabResult;
import com.tatkalx.investigation.FailureLabService;
import com.tatkalx.model.Admin;
import com.tatkalx.model.Berth;
import com.tatkalx.model.BookingEvent;
import com.tatkalx.model.BookingRequest;
import com.tatkalx.model.Coach;
import com.tatkalx.model.Passenger;
import com.tatkalx.model.Payment;
import com.tatkalx.model.Refund;
import com.tatkalx.model.Route;
import com.tatkalx.model.SeatInventory;
import com.tatkalx.model.SimulationResult;
import com.tatkalx.model.Station;
import com.tatkalx.model.ThreadLog;
import com.tatkalx.model.Train;
import com.tatkalx.model.User;
import com.tatkalx.simulation.ConcurrentUniverse;
import com.tatkalx.simulation.PriorityUniverse;
import com.tatkalx.simulation.SequentialUniverse;
import com.tatkalx.simulation.SimulationSessionContext;
import com.tatkalx.simulation.StrategyComparisonEngine;
import com.tatkalx.utility.ConsoleFormatter;

import java.io.IOException;
import java.time.LocalTime;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

/**
 * Handles all 24 Administrator Control Panel operations in the terminal UI.
 */
public class AdminController {

    private final TerminalSessionContext app;

    public AdminController(TerminalSessionContext app) {
        this.app = app;
    }

    public void showMenu() {
        boolean running = true;
        while (running) {
            Admin admin = app.getLoggedInAdmin();
            String badge = admin == null ? "NOT AUTHENTICATED (Use Option 1: admin_tx / admin123)"
                    : admin.getFullName() + " [" + admin.getEmployeeCode() + "]";

            ConsoleFormatter.printBanner("TATKAL-X ADMIN CONTROL & FORENSIC PANEL | " + badge);
            System.out.println("  1.  Admin Login                  13. Monitor Thread Execution");
            System.out.println("  2.  Manage Trains                14. View Booking Queues");
            System.out.println("  3.  Manage Stations              15. Investigate Booking ID (Crime Scene)");
            System.out.println("  4.  Manage Routes                16. Replay Booking");
            System.out.println("  5.  Manage Coaches               17. Run Booking Failure Lab");
            System.out.println("  6.  Manage Berths                18. View RAC and Waiting List");
            System.out.println("  7.  Configure Tatkal Window      19. View Payments");
            System.out.println("  8.  Generate Booking Requests    20. View Refunds");
            System.out.println("  9.  Run Sequential Simulation    21. View Database Consistency");
            System.out.println("  10. Run Priority Simulation      22. Generate Booking Reports (Streams)");
            System.out.println("  11. Run Concurrent Simulation    23. Export CSV Files & Byte Backup");
            System.out.println("  12. Compare All Three Strategies 24. View Audit Logs");
            System.out.println("   0. Back to Main Portal");
            ConsoleFormatter.printDivider();

            int choice = app.readInt("  Select Admin option: ");
            if (choice != 1 && choice != 0 && app.getLoggedInAdmin() == null) {
                System.out.println("  [!] Admin login required first (Option 1: admin_tx / admin123).");
                continue;
            }

            switch (choice) {
                case 1: handleAdminLogin(); break;
                case 2: handleManageTrains(); break;
                case 3: handleManageStations(); break;
                case 4: handleManageRoutes(); break;
                case 5: handleManageCoaches(); break;
                case 6: handleManageBerths(); break;
                case 7: handleConfigureClock(); break;
                case 8: handleGenerateRequests(); break;
                case 9: handleRunSequential(); break;
                case 10: handleRunPriority(); break;
                case 11: handleRunConcurrent(); break;
                case 12: handleCompareAllThree(); break;
                case 13: handleMonitorThreads(); break;
                case 14: handleViewQueues(); break;
                case 15: handleInvestigateBooking(); break;
                case 16: handleReplayBooking(); break;
                case 17: handleFailureLab(); break;
                case 18: handleViewRacAndWl(); break;
                case 19: handleViewPayments(); break;
                case 20: handleViewRefunds(); break;
                case 21: handleDatabaseConsistency(); break;
                case 22: handleGenerateReports(); break;
                case 23: handleExportCsvAndBackup(); break;
                case 24: handleViewAuditLogs(); break;
                case 0: running = false; break;
                default: System.out.println("  [!] Invalid option. Choose 0..24.");
            }
        }
    }

    private void handleAdminLogin() {
        ConsoleFormatter.printSubHeader("1. Admin Login (Default: admin_tx / admin123)");
        String user = app.readLine("  Admin Username: ");
        String pass = app.readLine("  Password: ");
        User u = app.getUserPassengerDao().authenticate(user, pass);
        if (u instanceof Admin) {
            app.setLoggedInAdmin((Admin) u);
            app.getForensicDao().insertAuditLog(u.getUserId(), 1L, "AdminAuth", "LOGIN", "Admins",
                    String.valueOf(((Admin) u).getAdminId()), "Admin logged in");
            System.out.println("  [OK] Authenticated: " + u.getRoleDescription());
        } else {
            System.out.println("  [!] Invalid administrator credentials.");
        }
    }

    private void handleManageTrains() {
        ConsoleFormatter.printSubHeader("2. Manage Trains");
        for (Train t : app.getRailwayDao().findAllTrains()) {
            System.out.println("  #" + t.getTrainId() + " | " + t + " | Coaches=" + t.getTotalCoaches());
        }
        String add = app.readLine("  Add a new train? (y/n): ");
        if ("y".equalsIgnoreCase(add)) {
            String num = app.readLine("  Train Number: ");
            String name = app.readLine("  Train Name: ");
            String type = app.readLine("  Type (RAJDHANI/SHATABDI/DURONTO/SUPERFAST/EXPRESS): ").toUpperCase();
            int coaches = app.readInt("  Total Coaches: ");
            int id = app.getRailwayDao().insertTrain(new Train(0, num, name, type, coaches, true, "ACTIVE"));
            System.out.println("  [OK] Inserted Train ID=" + id);
        }
    }

    private void handleManageStations() {
        ConsoleFormatter.printSubHeader("3. Manage Stations");
        for (Station s : app.getRailwayDao().findAllStations()) {
            System.out.println("  #" + s.getStationId() + " | " + s + " | Zone=" + s.getRailwayZone());
        }
        String add = app.readLine("  Add a new station? (y/n): ");
        if ("y".equalsIgnoreCase(add)) {
            String code = app.readLine("  Code (e.g. ADI): ").toUpperCase();
            String name = app.readLine("  Name: ");
            String city = app.readLine("  City: ");
            String state = app.readLine("  State: ");
            String zone = app.readLine("  Zone: ").toUpperCase();
            int id = app.getRailwayDao().insertStation(new Station(0, code, name, city, state, zone));
            System.out.println("  [OK] Inserted Station ID=" + id);
        }
    }

    private void handleManageRoutes() {
        ConsoleFormatter.printSubHeader("4. Manage Routes & Schedules");
        for (Route r : app.getRailwayDao().findAllRoutes()) {
            System.out.println("  Route #" + r.getRouteId() + " [" + r.getRouteCode() + "] " + r.getTotalDistanceKm() + " km");
        }
        String add = app.readLine("  Add new route? (y/n): ");
        if ("y".equalsIgnoreCase(add)) {
            String code = app.readLine("  Route Code: ");
            int orig = app.readInt("  Origin Station ID: ");
            int dest = app.readInt("  Destination Station ID: ");
            int dist = app.readInt("  Distance (km): ");
            int rid = app.getRailwayDao().insertRoute(code, orig, dest, dist);
            System.out.println("  [OK] Created Route ID=" + rid);
        }
    }

    private void handleManageCoaches() {
        ConsoleFormatter.printSubHeader("5. Manage Coaches");
        int trainId = app.readInt("  Enter Train ID (default 1): ");
        for (Coach c : app.getRailwayDao().findCoachesByTrainId(trainId)) {
            System.out.println("  Coach #" + c.getCoachId() + " | " + c);
        }
        String add = app.readLine("  Add new coach to this train? (y/n): ");
        if ("y".equalsIgnoreCase(add)) {
            String code = app.readLine("  Coach Code (e.g. B2): ").toUpperCase();
            String cls = app.readLine("  Class (2A/3A/SL/CC): ").toUpperCase();
            int total = app.readInt("  Total Berths: ");
            int tatkal = app.readInt("  Tatkal Quota Berths: ");
            int rac = app.readInt("  RAC Quota: ");
            int wl = app.readInt("  WL Quota: ");
            double fare = app.readDouble("  Base Fare per km: ");
            int cid = app.getRailwayDao().insertCoachWithBerths(
                    new Coach(0, trainId, code, cls, total, tatkal, rac, wl, fare));
            System.out.println("  [OK] Created Coach ID=" + cid + " with " + total + " berths.");
        }
    }

    private void handleManageBerths() {
        ConsoleFormatter.printSubHeader("6. Inspect Coach Berths");
        int coachId = app.readInt("  Coach ID (e.g. 1): ");
        String code = app.readLine("  Coach Code label (e.g. B1): ");
        List<Berth> berths = app.getRailwayDao().findBerthsByCoachId(coachId, code);
        System.out.println("  Total Berths: " + berths.size());
        for (int i = 0; i < Math.min(16, berths.size()); i++) {
            Berth b = berths.get(i);
            System.out.printf("  %-14s | Tatkal=%-5s | RACShared=%s%n",
                    b.getFormattedLabel(), b.isTatkalReserved(), b.isRacShared());
        }
    }

    private void handleConfigureClock() {
        ConsoleFormatter.printSubHeader("7. Configure Tatkal Virtual Clock");
        System.out.println("  Current: " + app.getLiveClock().getCountdownBanner());
        System.out.println("  1) Jump to 09:59:55 (Pre-Window)  2) Open 10:00:00 AM Now  3) Close Window Now");
        int opt = app.readInt("  Choose: ");
        if (opt == 1) {
            app.getLiveClock().setVirtualTime(LocalTime.of(9, 59, 55));
        } else if (opt == 2) {
            app.getLiveClock().openWindowNow();
        } else if (opt == 3) {
            app.getLiveClock().closeWindowNow();
        }
        System.out.println("  Updated: " + app.getLiveClock().getCountdownBanner());
    }

    private void handleGenerateRequests() {
        ConsoleFormatter.printSubHeader("8. Generate Booking Request Workload");
        int count = app.readInt("  Number of competing passengers (e.g. 200): ");
        String dupStr = app.readLine("  Include duplicate passenger attack? (y/n): ");
        boolean includeDup = "y".equalsIgnoreCase(dupStr);
        List<Passenger> pax = app.getWorkloadGenerator().generatePassengers(count);
        List<BookingRequest> reqs = app.getWorkloadGenerator().generateRequests(pax, 1L, "3A", includeDup);
        app.setGeneratedSimPassengers(pax);
        app.setGeneratedSimRequests(reqs);
        System.out.println("  [OK] Generated " + pax.size() + " passengers & " + reqs.size() + " cloned-ready requests.");
    }

    private void ensureWorkloadReady() {
        if (app.getGeneratedSimRequests().isEmpty()) {
            List<Passenger> pax = app.getWorkloadGenerator().generatePassengers(150);
            app.setGeneratedSimPassengers(pax);
            app.setGeneratedSimRequests(app.getWorkloadGenerator().generateRequests(pax, 1L, "3A", true));
            System.out.println("  [Info] Auto-generated default 150-passenger workload.");
        }
    }

    private void handleRunSequential() {
        ConsoleFormatter.printSubHeader("9. Universe A: Sequential FIFO Simulation");
        ensureWorkloadReady();
        SimulationSessionContext live = app.getLiveSessionContext();
        SeatInventory template = app.getRailwayDao() != null
                ? new SeatInventory(0L, live.getSchedule().getScheduleId(), live.getCoach(),
                app.getRailwayDao().findBerthsByCoachId(live.getCoach().getCoachId(), live.getCoach().getCoachCode()))
                : live.getSeatInventory().cloneForSession(0L);

        SequentialUniverse univA = new SequentialUniverse(app.getPaymentService(), app.getNotificationService());
        univA.prepareUniverse(live.getSchedule(), live.getCoach(), template,
                app.getGeneratedSimPassengers(), app.getGeneratedSimRequests());
        SimulationResult res = univA.runSimulation();
        app.setLastUniverseA(univA);
        persistSimulationResult(res, "SEQUENTIAL", 2, app.getGeneratedSimRequests().size(), live.getSchedule().getScheduleId());
        StrategyComparisonEngine.printComparisonReport(java.util.Collections.singletonList(res));
    }

    private void handleRunPriority() {
        ConsoleFormatter.printSubHeader("10. Universe B: PriorityQueue Simulation");
        ensureWorkloadReady();
        System.out.println("  Policies: 1) MEDICAL_SENIOR_FIRST  2) SCORE_DESCENDING  3) SMALLEST_PARTY_FIRST");
        int pChoice = app.readInt("  Choose policy (1..3): ");
        Comparator<BookingRequest> policy = PriorityPolicyComparators.MEDICAL_SENIOR_FIRST;
        String policyName = "MEDICAL_SENIOR_FIRST";
        if (pChoice == 2) {
            policy = PriorityPolicyComparators.SCORE_DESCENDING;
            policyName = "SCORE_DESCENDING";
        } else if (pChoice == 3) {
            policy = PriorityPolicyComparators.SMALLEST_PARTY_FIRST;
            policyName = "SMALLEST_PARTY_FIRST";
        }

        SimulationSessionContext live = app.getLiveSessionContext();
        SeatInventory template = new SeatInventory(0L, live.getSchedule().getScheduleId(), live.getCoach(),
                app.getRailwayDao().findBerthsByCoachId(live.getCoach().getCoachId(), live.getCoach().getCoachCode()));
        PriorityUniverse univB = new PriorityUniverse(app.getPaymentService(), app.getNotificationService(), policy, policyName);
        univB.prepareUniverse(live.getSchedule(), live.getCoach(), template,
                app.getGeneratedSimPassengers(), app.getGeneratedSimRequests());
        SimulationResult res = univB.runSimulation();
        app.setLastUniverseB(univB);
        persistSimulationResult(res, "PRIORITY", 3, app.getGeneratedSimRequests().size(), live.getSchedule().getScheduleId());
        StrategyComparisonEngine.printComparisonReport(java.util.Collections.singletonList(res));
    }

    private void handleRunConcurrent() {
        ConsoleFormatter.printSubHeader("11. Universe C: Concurrent Multi-Threaded Simulation");
        ensureWorkloadReady();
        SimulationSessionContext live = app.getLiveSessionContext();
        SeatInventory template = new SeatInventory(0L, live.getSchedule().getScheduleId(), live.getCoach(),
                app.getRailwayDao().findBerthsByCoachId(live.getCoach().getCoachId(), live.getCoach().getCoachCode()));

        ConcurrentUniverse univC = new ConcurrentUniverse(app.getPaymentService(), app.getNotificationService());
        univC.prepareUniverse(live.getSchedule(), live.getCoach(), template,
                app.getGeneratedSimPassengers(), app.getGeneratedSimRequests());
        SimulationResult res = univC.runSimulation();
        app.setLastUniverseC(univC);
        persistSimulationResult(res, "CONCURRENT", 4, app.getGeneratedSimRequests().size(), live.getSchedule().getScheduleId());
        StrategyComparisonEngine.printComparisonReport(java.util.Collections.singletonList(res));
    }

    private void handleCompareAllThree() {
        ConsoleFormatter.printSubHeader("12. Compare All Three Universes (A vs B vs C)");
        ensureWorkloadReady();
        SimulationSessionContext live = app.getLiveSessionContext();
        SeatInventory template = new SeatInventory(0L, live.getSchedule().getScheduleId(), live.getCoach(),
                app.getRailwayDao().findBerthsByCoachId(live.getCoach().getCoachId(), live.getCoach().getCoachCode()));

        StrategyComparisonEngine engine = app.createComparisonEngine();
        List<SimulationResult> results = engine.compareAllThreeUniverses(
                live.getSchedule(), live.getCoach(), template,
                app.getGeneratedSimPassengers(), app.getGeneratedSimRequests(),
                PriorityPolicyComparators.MEDICAL_SENIOR_FIRST, "MEDICAL_SENIOR_FIRST");

        app.setLastUniverseA(engine.getLastUniverseA());
        app.setLastUniverseB(engine.getLastUniverseB());
        app.setLastUniverseC(engine.getLastUniverseC());
        app.getLastComparisonResults().clear();
        app.getLastComparisonResults().addAll(results);

        persistSimulationResult(results.get(0), "SEQUENTIAL", 2, app.getGeneratedSimRequests().size(), live.getSchedule().getScheduleId());
        persistSimulationResult(results.get(1), "PRIORITY", 3, app.getGeneratedSimRequests().size(), live.getSchedule().getScheduleId());
        persistSimulationResult(results.get(2), "CONCURRENT", 4, app.getGeneratedSimRequests().size(), live.getSchedule().getScheduleId());

        StrategyComparisonEngine.printComparisonReport(results);
    }

    private void persistSimulationResult(SimulationResult res, String type, int strategyId, int reqCount, long scheduleId) {
        String code = type + "-" + System.currentTimeMillis();
        long dbSessionId = app.getSimulationDao().createSimulationSession(code, type, strategyId, scheduleId, reqCount);
        res.setSessionId(dbSessionId);
        app.getSimulationDao().saveSimulationResult(res);
    }

    private void handleMonitorThreads() {
        ConsoleFormatter.printSubHeader("13. Thread Execution Monitor");
        if (app.getLastUniverseC() == null || app.getLastUniverseC().getMonitor() == null) {
            System.out.println("  [!] Run Concurrent Simulation (Option 11 or 12) first.");
            return;
        }
        for (Map.Entry<Thread.State, Integer> e : app.getLastUniverseC().getMonitor().getStateHistogram().entrySet()) {
            System.out.printf("  State %-14s : %d samples%n", e.getKey(), e.getValue());
        }
        List<ThreadLog> logs = app.getLastUniverseC().getMonitor().getCapturedLogs();
        System.out.println("  First 10 Thread Samples:");
        for (int i = 0; i < Math.min(10, logs.size()); i++) {
            System.out.println("   " + logs.get(i));
        }
    }

    private void handleViewQueues() {
        ConsoleFormatter.printSubHeader("14. View Booking Queues");
        SimulationSessionContext live = app.getLiveSessionContext();
        System.out.println("  Live History Size : " + live.getAllRequestsSnapshot().size());
        System.out.println("  Active RAC Count  : " + live.getWaitingListManager().getRacSnapshot().size());
        System.out.println("  Active WL Count   : " + live.getWaitingListManager().getWlSnapshot().size());
    }

    private void handleInvestigateBooking() {
        ConsoleFormatter.printSubHeader("15. Investigate Booking ID (Crime Scene Reconstruction)");
        String bookingId = app.readLine("  Enter Booking ID (e.g. TX1001): ");
        SimulationSessionContext ctx = app.getLastUniverseC() != null ? app.getLastUniverseC().getContext() : app.getLiveSessionContext();
        List<BookingEvent> events = app.getForensicReconstructor().loadTimeline(bookingId, ctx);
        if (events.isEmpty() && app.getLiveSessionContext() != null) {
            events = app.getForensicReconstructor().loadTimeline(bookingId, app.getLiveSessionContext());
        }
        app.getForensicReconstructor().printForensicReport(bookingId, events, "SQLite + FileLog + SessionContext");
    }

    private void handleReplayBooking() {
        ConsoleFormatter.printSubHeader("16. Replay Booking Lifecycle");
        String bookingId = app.readLine("  Enter Booking ID to replay: ");
        SimulationSessionContext ctx = app.getLastUniverseC() != null ? app.getLastUniverseC().getContext() : app.getLiveSessionContext();
        List<BookingEvent> events = app.getForensicReconstructor().loadTimeline(bookingId, ctx);
        app.getReplayEngine().replayBooking(bookingId, events, 50L);
    }

    private void handleFailureLab() {
        ConsoleFormatter.printSubHeader("17. Booking Failure Lab (8 Scenarios)");
        System.out.println("  1-8: Run specific scenario | 9: Run ALL 8 Scenarios");
        int sc = app.readInt("  Select scenario: ");
        if (sc == 9) {
            List<FailureLabResult> all = app.getFailureLabService().runAllEightScenarios();
            FailureLabService.printFailureLabReport(all);
        } else if (sc >= 1 && sc <= 8) {
            FailureLabResult one = app.getFailureLabService().runScenario(sc);
            FailureLabService.printFailureLabReport(java.util.Collections.singletonList(one));
        } else {
            System.out.println("  [!] Invalid selection.");
        }
    }

    private void handleViewRacAndWl() {
        ConsoleFormatter.printSubHeader("18. RAC & Waiting List Inspection");
        SimulationSessionContext ctx = app.getLiveSessionContext();
        System.out.println("  RAC Entries: " + ctx.getWaitingListManager().getRacSnapshot());
        System.out.println("  WL  Entries: " + ctx.getWaitingListManager().getWlSnapshot());
    }

    private void handleViewPayments() {
        ConsoleFormatter.printSubHeader("19. Persisted Payments (SQLite)");
        for (Payment p : app.getFinancialDao().findAllPayments()) {
            System.out.println("  " + p);
        }
    }

    private void handleViewRefunds() {
        ConsoleFormatter.printSubHeader("20. Persisted Refunds (SQLite)");
        for (Refund r : app.getFinancialDao().findAllRefunds()) {
            System.out.println("  " + r);
        }
    }

    private void handleDatabaseConsistency() {
        ConsoleFormatter.printSubHeader("21. Database Consistency Monitor");
        DatabaseConsistencyMonitor.ConsistencyReport report = app.getConsistencyMonitor().runFullConsistencyAudit();
        for (DatabaseConsistencyMonitor.CheckItem item : report.getChecks()) {
            System.out.printf("  [%-4s] %-52s | %s%n",
                    item.isPassed() ? "PASS" : "FAIL", item.getCheckName(), item.getDetail());
        }
    }

    private void handleGenerateReports() {
        ConsoleFormatter.printSubHeader("22. Stream API Analytical Reports");
        SimulationSessionContext target = app.getLastUniverseC() != null
                ? app.getLastUniverseC().getContext() : app.getLiveSessionContext();
        app.getReportFormatter().printFullAnalyticsReport(
                target.getAllRequestsSnapshot(), target.getAllPayments(), target.getAllRefunds());
    }

    private void handleExportCsvAndBackup() {
        ConsoleFormatter.printSubHeader("23. CSV Import/Export & Binary Byte-Stream Backup");
        try {
            String samplePath = "data/sample_passengers.csv";
            app.getCsvService().createSamplePassengersCsv(samplePath);
            CsvImportResult imp = app.getCsvService().importPassengersFromCsv(samplePath);
            System.out.println("  CSV Sample Imported Valid=" + imp.getValidPassengers().size()
                    + ", Invalid=" + imp.getInvalidRows().size());

            SimulationSessionContext target = app.getLastUniverseC() != null
                    ? app.getLastUniverseC().getContext() : app.getLiveSessionContext();
            app.getCsvService().exportPassengersToCsv(app.getUserPassengerDao().findAll(), "exports/passengers_export.csv");
            app.getCsvService().exportBookingHistoryToCsv(target.getAllRequestsSnapshot(), "exports/booking_history_export.csv");
            app.getCsvService().exportRefundReportToCsv(app.getFinancialDao().findAllRefunds(), "exports/refund_report_export.csv");
            if (!app.getLastComparisonResults().isEmpty()) {
                app.getCsvService().exportSimulationComparisonToCsv(app.getLastComparisonResults(), "exports/simulation_comparison.csv");
            }
            long bytes = app.getBackupService().createBinaryBackup("tatkal_x.db", "exports/tatkal_db_snapshot.bin");
            System.out.println("  [OK] CSV files written to exports/ & DB backed up (" + bytes + " bytes).");
        } catch (IOException ex) {
            System.out.println("  [ERROR] File export failed: " + ex.getMessage());
        }
    }

    private void handleViewAuditLogs() {
        ConsoleFormatter.printSubHeader("24. Recent System Audit Logs");
        for (String line : app.getForensicDao().findRecentAuditLogs(20)) {
            System.out.println("  " + line);
        }
    }
}
