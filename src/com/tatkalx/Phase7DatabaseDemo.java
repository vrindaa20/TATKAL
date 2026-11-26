package com.tatkalx;

import com.tatkalx.dao.BookingDao;
import com.tatkalx.dao.FinancialDao;
import com.tatkalx.dao.ForensicDao;
import com.tatkalx.dao.RailwayDao;
import com.tatkalx.dao.SeatInventoryDao;
import com.tatkalx.dao.UserPassengerDao;
import com.tatkalx.database.DatabaseConnectionManager;
import com.tatkalx.database.DatabaseConsistencyMonitor;
import com.tatkalx.database.DatabaseInitializer;
import com.tatkalx.model.Berth;
import com.tatkalx.model.BookingEvent;
import com.tatkalx.model.BookingRequest;
import com.tatkalx.model.Coach;
import com.tatkalx.model.Passenger;
import com.tatkalx.model.PnrRecord;
import com.tatkalx.model.SeatInventory;
import com.tatkalx.model.TrainSchedule;
import com.tatkalx.service.BerthAllocationService;
import com.tatkalx.service.BookingOutcome;
import com.tatkalx.service.NotificationService;
import com.tatkalx.service.PaymentService;
import com.tatkalx.service.TatkalVirtualClock;
import com.tatkalx.simulation.SimulationSessionContext;
import com.tatkalx.utility.ConsoleFormatter;
import com.tatkalx.utility.IdGenerator;

import java.util.List;

/**
 * Phase 7 Verification Runner:
 * Initializes tatkal_x.db (30 tables), performs CRUD + ResultSet mapping across DAOs,
 * executes an atomic booking transaction in SQLite, reconstructs Booking_Events from SQLite,
 * and runs DatabaseConsistencyMonitor across all 6 relational invariants.
 */
public class Phase7DatabaseDemo {

    public static void main(String[] args) throws Exception {
        ConsoleFormatter.printBanner("TATKAL-X | PHASE 7: SQLITE JDBC & TRANSACTIONAL PERSISTENCE LAB");

        DatabaseConnectionManager dbManager = new DatabaseConnectionManager();
        DatabaseInitializer initializer = new DatabaseInitializer(dbManager);
        initializer.initializeDatabase();
        System.out.println("[1] Initialized tatkal_x.db cleanly.");

        UserPassengerDao userDao = new UserPassengerDao(dbManager);
        RailwayDao railwayDao = new RailwayDao(dbManager);
        SeatInventoryDao invDao = new SeatInventoryDao(dbManager);
        BookingDao bookingDao = new BookingDao(dbManager);
        FinancialDao financialDao = new FinancialDao(dbManager);
        ForensicDao forensicDao = new ForensicDao(dbManager);

        // 1. Insert a new Passenger via JDBC Transaction (Users + Passengers)
        ConsoleFormatter.printSubHeader("2. Passenger Registration CRUD & Authentication via JDBC");
        String uniqueSuffix = String.valueOf(System.currentTimeMillis() % 100000);
        Passenger newPax = new Passenger(
                0, 0, "rohan_" + uniqueSuffix, "Rohan Kulkarni",
                "rohan" + uniqueSuffix + "@mail.in", "98111" + String.format("%05d", Integer.parseInt(uniqueSuffix)),
                34, "M", "PASSPORT", "PASS-" + uniqueSuffix, true, "DEFENCE", "LB", 18000.0);
        newPax.setPasswordHash("pass123");
        int newPaxId = userDao.insert(newPax);
        Passenger loadedPax = userDao.findById(newPaxId);
        System.out.println("  Inserted & Loaded from SQLite: " + loadedPax);
        System.out.println("  Authenticated Admin: " + userDao.authenticate("admin_tx", "admin123").getRoleDescription());

        // 2. Load Railway Schedule, Coach & Live Session Inventory from SQLite
        ConsoleFormatter.printSubHeader("3. Railway Master & Live Seat_Inventory Hydration");
        List<TrainSchedule> schedules = railwayDao.findAllSchedules();
        TrainSchedule schedule = schedules.get(0);
        List<Coach> coaches = railwayDao.findCoachesByTrainId(schedule.getTrainId());
        Coach coach3A = coaches.get(0);
        List<Berth> berths = railwayDao.findBerthsByCoachId(coach3A.getCoachId(), coach3A.getCoachCode());
        SeatInventory liveInventory = invDao.findInventory(1L, schedule.getScheduleId(), coach3A, berths);
        System.out.println("  Schedule: " + schedule);
        System.out.println("  Live Inventory before booking: " + liveInventory);

        // 3. Execute Booking via Phase 5 Engine & Persist Atomically to SQLite
        ConsoleFormatter.printSubHeader("4. Atomic Booking Transaction in tatkal_x.db");
        TatkalVirtualClock clock = new TatkalVirtualClock();
        clock.openWindowNow();
        SimulationSessionContext ctx = new SimulationSessionContext(1L, "LIVE", schedule, coach3A, liveInventory, clock);
        for (Passenger p : userDao.findAll()) {
            ctx.getPassengerRegistry().registerPassenger(p);
        }

        PaymentService paymentService = new PaymentService();
        paymentService.setSimulatedLatencyMs(2L);
        BerthAllocationService allocator = new BerthAllocationService(paymentService, new NotificationService());

        String bookingId = IdGenerator.nextBookingId() + "-DB" + uniqueSuffix;
        BookingRequest req = new BookingRequest(
                bookingId, 1L, loadedPax.getPassengerId(), loadedPax.getFullName(),
                loadedPax.getAge(), loadedPax.getPriorityCategory(),
                schedule.getScheduleId(), "3A", 1, "10:00:01.000");

        BookingOutcome outcome = allocator.allocateAndConfirm(req, ctx, "UPI");
        List<Berth> allocated = liveInventory.getAllocatedBerthsByTicket().get(outcome.getTicket().getTicketId());
        bookingDao.persistBookingOutcomeAtomically(outcome, loadedPax, liveInventory, allocated);

        PnrRecord dbPnr = bookingDao.findPnrByNumber(outcome.getPnrRecord().getPnrNumber());
        System.out.println("  Persisted PNR Lookup from SQLite: " + dbPnr);
        System.out.println("  SQLite Payments count: " + financialDao.findAllPayments().size());

        // 4. Query Booking_Events from SQLite
        ConsoleFormatter.printSubHeader("5. Booking_Events Reconstructed from SQLite for " + bookingId);
        List<BookingEvent> dbEvents = forensicDao.findEventsByBookingId(bookingId);
        for (BookingEvent ev : dbEvents) {
            System.out.println("  " + ev);
        }

        // 5. Run Database Consistency Monitor across all 6 relational invariants
        ConsoleFormatter.printSubHeader("6. Database Consistency Monitor Audit");
        forensicDao.insertAuditLog(1, 1L, "DatabaseDemo", "CONSISTENCY_AUDIT", "Seat_Inventory", "ALL", "Verified Phase 7 persistence");
        DatabaseConsistencyMonitor monitor = new DatabaseConsistencyMonitor(dbManager);
        DatabaseConsistencyMonitor.ConsistencyReport report = monitor.runFullConsistencyAudit();
        for (DatabaseConsistencyMonitor.CheckItem item : report.getChecks()) {
            System.out.printf("  [%-4s] %-52s | %s%n",
                    item.isPassed() ? "PASS" : "FAIL", item.getCheckName(), item.getDetail());
        }
        System.out.println("  OVERALL DATABASE CONSISTENCY: " + (report.isAllPassed() ? "CLEAN & CONSISTENT" : "CORRUPTION DETECTED"));
    }
}
