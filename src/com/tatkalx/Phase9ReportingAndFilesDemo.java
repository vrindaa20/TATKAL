package com.tatkalx;

import com.tatkalx.collections.PriorityPolicyComparators;
import com.tatkalx.filehandling.BinaryAuditBackupService;
import com.tatkalx.filehandling.CsvImportExportService;
import com.tatkalx.filehandling.CsvImportResult;
import com.tatkalx.model.BookingRequest;
import com.tatkalx.model.Coach;
import com.tatkalx.model.Passenger;
import com.tatkalx.model.PnrRecord;
import com.tatkalx.model.Refund;
import com.tatkalx.model.SeatInventory;
import com.tatkalx.model.SimulationResult;
import com.tatkalx.model.ThreadLog;
import com.tatkalx.model.TrainSchedule;
import com.tatkalx.reporting.ReportFormatter;
import com.tatkalx.reporting.StreamAnalyticsService;
import com.tatkalx.service.CancellationRefundService;
import com.tatkalx.service.NotificationService;
import com.tatkalx.service.PaymentService;
import com.tatkalx.service.QueuePromotionService;
import com.tatkalx.service.RailwayManagementService;
import com.tatkalx.simulation.SimulationSessionContext;
import com.tatkalx.simulation.StrategyComparisonEngine;
import com.tatkalx.simulation.WorkloadGenerator;
import com.tatkalx.utility.ConsoleFormatter;

import java.util.List;

/**
 * Phase 9 Verification Runner:
 * Exercises CSV Import (FileReader + StringTokenizer + validation), Stream API Analytics,
 * CSV Exports (FileWriter), and Binary Byte-Stream Backup (FileInputStream / FileOutputStream).
 */
public class Phase9ReportingAndFilesDemo {

    public static void main(String[] args) throws Exception {
        ConsoleFormatter.printBanner("TATKAL-X | PHASE 9: REPORTING & FILE HANDLING LAB");

        // 1. Sample CSV Generation & Validated Import (FileReader + StringTokenizer)
        CsvImportExportService csvService = new CsvImportExportService();
        String sampleCsvPath = "data/sample_passengers.csv";
        csvService.createSamplePassengersCsv(sampleCsvPath);
        CsvImportResult importResult = csvService.importPassengersFromCsv(sampleCsvPath);

        ConsoleFormatter.printSubHeader("1. CSV Import Results (FileReader + StringTokenizer + Validation)");
        System.out.println("  Valid Imported Passengers : " + importResult.getValidPassengers().size());
        for (Passenger p : importResult.getValidPassengers()) {
            System.out.println("   + " + p);
        }
        System.out.println("  Rejected Invalid Rows     : " + importResult.getInvalidRows().size());
        for (String err : importResult.getInvalidRows()) {
            System.out.println("   ! " + err);
        }

        // 2. Run 3-Universe Simulation + Cancel 1 WL ticket to generate rich Analytics & Refunds
        RailwayManagementService railway = new RailwayManagementService();
        railway.loadDefaultRailwayData();
        TrainSchedule schedule = railway.getSchedule(1L);
        Coach coach = new Coach(1, 1, "B1", "3A", 72, 8, 3, 5, 1.45);
        railway.addCoachWithAutoBerths(coach);
        SeatInventory templateInv = railway.createIsolatedInventory(0L, 1L, "3A");

        WorkloadGenerator generator = new WorkloadGenerator();
        List<Passenger> pax = generator.generatePassengers(25);
        List<BookingRequest> reqs = generator.generateRequests(pax, 1L, "3A", true);

        PaymentService pay = new PaymentService();
        pay.setSimulatedLatencyMs(1L);
        NotificationService notify = new NotificationService();

        StrategyComparisonEngine engine = new StrategyComparisonEngine(pay, notify);
        List<SimulationResult> simResults = engine.compareAllThreeUniverses(
                schedule, coach, templateInv, pax, reqs,
                PriorityPolicyComparators.MEDICAL_SENIOR_FIRST, "MEDICAL_SENIOR_FIRST");

        SimulationSessionContext concurrentCtx = engine.getLastUniverseC().getContext();
        // Cancel one WL ticket in Universe C so Refunds report is populated
        CancellationRefundService canceller = new CancellationRefundService(new QueuePromotionService(notify), notify);
        for (PnrRecord pnr : concurrentCtx.getPnrDirectory().getAllSortedPnrs()) {
            if ("WL".equals(pnr.getCurrentStatus())) {
                Refund ref = canceller.cancelTicketByPnr(pnr.getPnrNumber(), "Demo WL refund", false, concurrentCtx);
                System.out.println("\n  [Generated Sample Refund]: " + ref);
                break;
            }
        }

        // 3. Stream API Analytics
        StreamAnalyticsService analytics = new StreamAnalyticsService();
        ReportFormatter formatter = new ReportFormatter(analytics);
        formatter.printFullAnalyticsReport(
                concurrentCtx.getAllRequestsSnapshot(),
                concurrentCtx.getAllPayments(),
                concurrentCtx.getAllRefunds());
        System.out.println("  Verified Passenger Names Sorted (Method Ref): "
                + analytics.getVerifiedPassengerNamesSorted(importResult.getValidPassengers()));

        // 4. Export 5 CSV Reports via FileWriter
        ConsoleFormatter.printSubHeader("4. Exporting CSV Reports via FileWriter");
        int paxCount = csvService.exportPassengersToCsv(importResult.getValidPassengers(), "exports/passengers_export.csv");
        int bookCount = csvService.exportBookingHistoryToCsv(concurrentCtx.getAllRequestsSnapshot(), "exports/booking_history_export.csv");
        int refCount = csvService.exportRefundReportToCsv(concurrentCtx.getAllRefunds(), "exports/refund_report_export.csv");
        List<ThreadLog> threadLogs = engine.getLastUniverseC().getMonitor().getCapturedLogs();
        int threadCount = csvService.exportThreadLogsToCsv(threadLogs, "exports/thread_logs_export.csv");
        int simCount = csvService.exportSimulationComparisonToCsv(simResults, "exports/simulation_comparison.csv");

        System.out.println("  exports/passengers_export.csv      -> " + paxCount + " rows");
        System.out.println("  exports/booking_history_export.csv -> " + bookCount + " rows");
        System.out.println("  exports/refund_report_export.csv   -> " + refCount + " rows");
        System.out.println("  exports/thread_logs_export.csv     -> " + threadCount + " rows");
        System.out.println("  exports/simulation_comparison.csv  -> " + simCount + " rows");

        // 5. Byte Stream Binary Backup (FileInputStream / FileOutputStream)
        ConsoleFormatter.printSubHeader("5. Byte-Stream Binary Backup (FileInputStream / FileOutputStream)");
        BinaryAuditBackupService backupService = new BinaryAuditBackupService();
        String backupTarget = "exports/tatkal_db_snapshot.bin";
        long bytes = backupService.createBinaryBackup("tatkal_x.db", backupTarget);
        boolean identical = backupService.verifyByteIdentity("tatkal_x.db", backupTarget);
        System.out.println("  Backed up tatkal_x.db -> " + backupTarget + " (" + bytes + " bytes), byte-verified=" + identical);
    }
}
