package com.tatkalx.controller;

import com.tatkalx.dao.BookingDao;
import com.tatkalx.dao.FinancialDao;
import com.tatkalx.dao.ForensicDao;
import com.tatkalx.dao.RailwayDao;
import com.tatkalx.dao.SeatInventoryDao;
import com.tatkalx.dao.SimulationDao;
import com.tatkalx.dao.UserPassengerDao;
import com.tatkalx.database.DatabaseConnectionManager;
import com.tatkalx.database.DatabaseConsistencyMonitor;
import com.tatkalx.database.DatabaseInitializer;
import com.tatkalx.exception.BookingValidationException;
import com.tatkalx.exception.DuplicateBookingException;
import com.tatkalx.filehandling.BinaryAuditBackupService;
import com.tatkalx.filehandling.CsvImportExportService;
import com.tatkalx.filehandling.FileLogManager;
import com.tatkalx.investigation.BookingExecutionLogger;
import com.tatkalx.investigation.BookingReplayEngine;
import com.tatkalx.investigation.FailureLabService;
import com.tatkalx.investigation.ForensicReconstructor;
import com.tatkalx.model.Admin;
import com.tatkalx.model.Berth;
import com.tatkalx.model.BookingRequest;
import com.tatkalx.model.Coach;
import com.tatkalx.model.Passenger;
import com.tatkalx.model.Payment;
import com.tatkalx.model.PnrRecord;
import com.tatkalx.model.SeatInventory;
import com.tatkalx.model.SimulationResult;
import com.tatkalx.model.Ticket;
import com.tatkalx.model.TrainSchedule;
import com.tatkalx.reporting.ReportFormatter;
import com.tatkalx.reporting.StreamAnalyticsService;
import com.tatkalx.service.BerthAllocationService;
import com.tatkalx.service.CancellationRefundService;
import com.tatkalx.service.NotificationService;
import com.tatkalx.service.PaymentService;
import com.tatkalx.service.QueuePromotionService;
import com.tatkalx.service.TatkalVirtualClock;
import com.tatkalx.simulation.ConcurrentUniverse;
import com.tatkalx.simulation.PriorityUniverse;
import com.tatkalx.simulation.SequentialUniverse;
import com.tatkalx.simulation.SimulationSessionContext;
import com.tatkalx.simulation.StrategyComparisonEngine;
import com.tatkalx.simulation.WorkloadGenerator;

import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

/**
 * Central dependency container and shared state for the TATKAL-X Terminal Application.
 * Hydrates live SQLite state on launch and coordinates services, DAOs, and simulation runs.
 */
public class TerminalSessionContext {

    private final Scanner scanner = new Scanner(System.in);

    private final DatabaseConnectionManager dbManager;
    private final DatabaseConsistencyMonitor consistencyMonitor;
    private final UserPassengerDao userPassengerDao;
    private final RailwayDao railwayDao;
    private final SeatInventoryDao seatInventoryDao;
    private final BookingDao bookingDao;
    private final FinancialDao financialDao;
    private final ForensicDao forensicDao;
    private final SimulationDao simulationDao;

    private final TatkalVirtualClock liveClock = new TatkalVirtualClock();
    private final PaymentService paymentService = new PaymentService();
    private final NotificationService notificationService = new NotificationService();
    private final BerthAllocationService allocationService;
    private final QueuePromotionService queuePromotionService;
    private final CancellationRefundService cancellationService;

    private final FileLogManager fileLogManager = new FileLogManager();
    private final BookingExecutionLogger executionLogger;
    private final ForensicReconstructor forensicReconstructor;
    private final BookingReplayEngine replayEngine;
    private final FailureLabService failureLabService;

    private final CsvImportExportService csvService = new CsvImportExportService();
    private final BinaryAuditBackupService backupService = new BinaryAuditBackupService();
    private final StreamAnalyticsService analyticsService = new StreamAnalyticsService();
    private final ReportFormatter reportFormatter;
    private final WorkloadGenerator workloadGenerator = new WorkloadGenerator();

    private SimulationSessionContext liveSessionContext;
    private Passenger loggedInPassenger;
    private Admin loggedInAdmin;

    // Simulation Lab working state
    private List<Passenger> generatedSimPassengers = new ArrayList<>();
    private List<BookingRequest> generatedSimRequests = new ArrayList<>();
    private SequentialUniverse lastUniverseA;
    private PriorityUniverse lastUniverseB;
    private ConcurrentUniverse lastUniverseC;
    private final List<SimulationResult> lastComparisonResults = new ArrayList<>();

    public TerminalSessionContext() {
        this.dbManager = new DatabaseConnectionManager();
        new DatabaseInitializer(dbManager).initializeDatabase();
        this.consistencyMonitor = new DatabaseConsistencyMonitor(dbManager);

        this.userPassengerDao = new UserPassengerDao(dbManager);
        this.railwayDao = new RailwayDao(dbManager);
        this.seatInventoryDao = new SeatInventoryDao(dbManager);
        this.bookingDao = new BookingDao(dbManager);
        this.financialDao = new FinancialDao(dbManager);
        this.forensicDao = new ForensicDao(dbManager);
        this.simulationDao = new SimulationDao(dbManager);

        this.paymentService.setSimulatedLatencyMs(5L);
        this.allocationService = new BerthAllocationService(paymentService, notificationService);
        this.queuePromotionService = new QueuePromotionService(notificationService);
        this.cancellationService = new CancellationRefundService(queuePromotionService, notificationService);

        this.executionLogger = new BookingExecutionLogger(fileLogManager, forensicDao);
        this.forensicReconstructor = new ForensicReconstructor(forensicDao, fileLogManager);
        this.replayEngine = new BookingReplayEngine(forensicReconstructor);
        this.failureLabService = new FailureLabService(dbManager, forensicDao, executionLogger);
        this.reportFormatter = new ReportFormatter(analyticsService);

        hydrateLiveContext(1L, "3A");
    }

    /**
     * Hydrates the live in-memory SimulationSessionContext from persisted tatkal_x.db rows.
     */
    public void hydrateLiveContext(long scheduleId, String coachClass) {
        List<TrainSchedule> schedules = railwayDao.findAllSchedules();
        if (schedules.isEmpty()) {
            return;
        }
        TrainSchedule targetSchedule = schedules.get(0);
        for (TrainSchedule ts : schedules) {
            if (ts.getScheduleId() == scheduleId) {
                targetSchedule = ts;
                break;
            }
        }

        List<Coach> coaches = railwayDao.findCoachesByTrainId(targetSchedule.getTrainId());
        if (coaches.isEmpty()) {
            return;
        }
        Coach targetCoach = coaches.get(0);
        for (Coach c : coaches) {
            if (c.getCoachClass().equalsIgnoreCase(coachClass)) {
                targetCoach = c;
                break;
            }
        }

        List<Berth> berths = railwayDao.findBerthsByCoachId(targetCoach.getCoachId(), targetCoach.getCoachCode());
        SeatInventory inv = seatInventoryDao.findInventory(1L, targetSchedule.getScheduleId(), targetCoach, berths);
        if (inv == null) {
            seatInventoryDao.createSessionInventory(1L, targetSchedule.getScheduleId(), targetCoach);
            inv = seatInventoryDao.findInventory(1L, targetSchedule.getScheduleId(), targetCoach, berths);
        }

        SimulationSessionContext ctx = new SimulationSessionContext(
                1L, "LIVE", targetSchedule, targetCoach, inv, liveClock);

        for (Passenger p : userPassengerDao.findAll()) {
            try {
                ctx.getPassengerRegistry().registerPassenger(p);
            } catch (BookingValidationException ignored) {
            }
        }
        for (BookingRequest r : bookingDao.findRequestsBySession(1L)) {
            ctx.getHistoryCache().recordBooking(r);
        }
        for (Ticket t : bookingDao.findTicketsBySession(1L)) {
            ctx.recordTicket(t);
            if (!"CANCELLED".equalsIgnoreCase(t.getTicketStatus()) && t.getScheduleId() == targetSchedule.getScheduleId()) {
                try {
                    ctx.getDuplicateGuard().acquireOrThrow(1L, t.getScheduleId(), t.getPassengerId());
                } catch (DuplicateBookingException ignored) {
                }
            }
            if ("RAC".equalsIgnoreCase(t.getTicketStatus())) {
                ctx.getWaitingListManager().enqueueRac(t);
            } else if ("WL".equalsIgnoreCase(t.getTicketStatus())) {
                ctx.getWaitingListManager().enqueueWaitingList(t);
            }
        }
        for (PnrRecord pnr : bookingDao.findPnrsBySession(1L)) {
            ctx.getPnrDirectory().registerPnr(pnr);
        }
        for (Payment p : financialDao.findAllPayments()) {
            ctx.recordPayment(p);
        }

        this.liveSessionContext = ctx;
    }

    public int readInt(String prompt) {
        while (true) {
            System.out.print(prompt);
            String line = scanner.nextLine().trim();
            try {
                return Integer.parseInt(line);
            } catch (NumberFormatException ex) {
                System.out.println("  [!] Please enter a valid integer.");
            }
        }
    }

    public long readLong(String prompt) {
        while (true) {
            System.out.print(prompt);
            String line = scanner.nextLine().trim();
            try {
                return Long.parseLong(line);
            } catch (NumberFormatException ex) {
                System.out.println("  [!] Please enter a valid number.");
            }
        }
    }

    public double readDouble(String prompt) {
        while (true) {
            System.out.print(prompt);
            String line = scanner.nextLine().trim();
            try {
                return Double.parseDouble(line);
            } catch (NumberFormatException ex) {
                System.out.println("  [!] Please enter a valid decimal number.");
            }
        }
    }

    public String readLine(String prompt) {
        System.out.print(prompt);
        return scanner.nextLine().trim();
    }

    // Getters & Setters
    public DatabaseConnectionManager getDbManager() { return dbManager; }
    public DatabaseConsistencyMonitor getConsistencyMonitor() { return consistencyMonitor; }
    public UserPassengerDao getUserPassengerDao() { return userPassengerDao; }
    public RailwayDao getRailwayDao() { return railwayDao; }
    public SeatInventoryDao getSeatInventoryDao() { return seatInventoryDao; }
    public BookingDao getBookingDao() { return bookingDao; }
    public FinancialDao getFinancialDao() { return financialDao; }
    public ForensicDao getForensicDao() { return forensicDao; }
    public SimulationDao getSimulationDao() { return simulationDao; }
    public TatkalVirtualClock getLiveClock() { return liveClock; }
    public PaymentService getPaymentService() { return paymentService; }
    public NotificationService getNotificationService() { return notificationService; }
    public BerthAllocationService getAllocationService() { return allocationService; }
    public CancellationRefundService getCancellationService() { return cancellationService; }
    public FileLogManager getFileLogManager() { return fileLogManager; }
    public BookingExecutionLogger getExecutionLogger() { return executionLogger; }
    public ForensicReconstructor getForensicReconstructor() { return forensicReconstructor; }
    public BookingReplayEngine getReplayEngine() { return replayEngine; }
    public FailureLabService getFailureLabService() { return failureLabService; }
    public CsvImportExportService getCsvService() { return csvService; }
    public BinaryAuditBackupService getBackupService() { return backupService; }
    public StreamAnalyticsService getAnalyticsService() { return analyticsService; }
    public ReportFormatter getReportFormatter() { return reportFormatter; }
    public WorkloadGenerator getWorkloadGenerator() { return workloadGenerator; }
    public SimulationSessionContext getLiveSessionContext() { return liveSessionContext; }
    public Passenger getLoggedInPassenger() { return loggedInPassenger; }
    public void setLoggedInPassenger(Passenger loggedInPassenger) { this.loggedInPassenger = loggedInPassenger; }
    public Admin getLoggedInAdmin() { return loggedInAdmin; }
    public void setLoggedInAdmin(Admin loggedInAdmin) { this.loggedInAdmin = loggedInAdmin; }
    public List<Passenger> getGeneratedSimPassengers() { return generatedSimPassengers; }
    public void setGeneratedSimPassengers(List<Passenger> generatedSimPassengers) { this.generatedSimPassengers = generatedSimPassengers; }
    public List<BookingRequest> getGeneratedSimRequests() { return generatedSimRequests; }
    public void setGeneratedSimRequests(List<BookingRequest> generatedSimRequests) { this.generatedSimRequests = generatedSimRequests; }
    public SequentialUniverse getLastUniverseA() { return lastUniverseA; }
    public void setLastUniverseA(SequentialUniverse lastUniverseA) { this.lastUniverseA = lastUniverseA; }
    public PriorityUniverse getLastUniverseB() { return lastUniverseB; }
    public void setLastUniverseB(PriorityUniverse lastUniverseB) { this.lastUniverseB = lastUniverseB; }
    public ConcurrentUniverse getLastUniverseC() { return lastUniverseC; }
    public void setLastUniverseC(ConcurrentUniverse lastUniverseC) { this.lastUniverseC = lastUniverseC; }
    public List<SimulationResult> getLastComparisonResults() { return lastComparisonResults; }

    public StrategyComparisonEngine createComparisonEngine() {
        return new StrategyComparisonEngine(paymentService, notificationService);
    }
}
