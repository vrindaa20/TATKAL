package com.tatkalx;

import com.tatkalx.dao.BookingDao;
import com.tatkalx.dao.ForensicDao;
import com.tatkalx.dao.RailwayDao;
import com.tatkalx.dao.SeatInventoryDao;
import com.tatkalx.dao.UserPassengerDao;
import com.tatkalx.database.DatabaseConnectionManager;
import com.tatkalx.database.DatabaseInitializer;
import com.tatkalx.filehandling.FileLogManager;
import com.tatkalx.investigation.BookingExecutionLogger;
import com.tatkalx.investigation.BookingReplayEngine;
import com.tatkalx.investigation.FailureLabResult;
import com.tatkalx.investigation.FailureLabService;
import com.tatkalx.investigation.ForensicReconstructor;
import com.tatkalx.model.Berth;
import com.tatkalx.model.BookingEvent;
import com.tatkalx.model.BookingRequest;
import com.tatkalx.model.Coach;
import com.tatkalx.model.Passenger;
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
 * Phase 8 Verification Runner:
 * Exercises FileWriter/FileReader BookingExecutionLogger, Crime Scene ForensicReconstructor,
 * BookingReplayEngine, and all 8 scenarios in FailureLabService.
 */
public class Phase8ForensicDemo {

    public static void main(String[] args) throws Exception {
        ConsoleFormatter.printBanner("TATKAL-X | PHASE 8: FORENSIC INVESTIGATION & FAILURE LAB");

        DatabaseConnectionManager dbManager = new DatabaseConnectionManager();
        new DatabaseInitializer(dbManager).initializeDatabase();

        UserPassengerDao userDao = new UserPassengerDao(dbManager);
        RailwayDao railwayDao = new RailwayDao(dbManager);
        SeatInventoryDao invDao = new SeatInventoryDao(dbManager);
        BookingDao bookingDao = new BookingDao(dbManager);
        ForensicDao forensicDao = new ForensicDao(dbManager);

        FileLogManager fileLogManager = new FileLogManager();
        BookingExecutionLogger execLogger = new BookingExecutionLogger(fileLogManager, forensicDao);
        ForensicReconstructor reconstructor = new ForensicReconstructor(forensicDao, fileLogManager);
        BookingReplayEngine replayEngine = new BookingReplayEngine(reconstructor);

        // 1. Create a real booking and write events to BOTH SQLite and logs/booking_events.log
        TrainSchedule schedule = railwayDao.findAllSchedules().get(0);
        Coach coach3A = railwayDao.findCoachesByTrainId(schedule.getTrainId()).get(0);
        List<Berth> berths = railwayDao.findBerthsByCoachId(coach3A.getCoachId(), coach3A.getCoachCode());
        SeatInventory liveInv = invDao.findInventory(1L, schedule.getScheduleId(), coach3A, berths);

        TatkalVirtualClock clock = new TatkalVirtualClock();
        clock.openWindowNow();
        SimulationSessionContext ctx = new SimulationSessionContext(1L, "LIVE", schedule, coach3A, liveInv, clock);
        List<Passenger> allPax = userDao.findAll();
        for (Passenger p : allPax) {
            ctx.getPassengerRegistry().registerPassenger(p);
        }
        Passenger targetPax = allPax.get(1);

        PaymentService pay = new PaymentService();
        pay.setSimulatedLatencyMs(2L);
        BerthAllocationService alloc = new BerthAllocationService(pay, new NotificationService());

        String bookingId = IdGenerator.nextBookingId() + "-FR" + (System.currentTimeMillis() % 10000);
        BookingRequest req = new BookingRequest(bookingId, 1L, targetPax.getPassengerId(),
                targetPax.getFullName(), targetPax.getAge(), targetPax.getPriorityCategory(),
                schedule.getScheduleId(), "3A", 1, "10:00:02.500");

        BookingOutcome outcome = alloc.allocateAndConfirm(req, ctx, "UPI");
        if (outcome.isSuccess()) {
            List<Berth> allocated = liveInv.getAllocatedBerthsByTicket().get(outcome.getTicket().getTicketId());
            bookingDao.persistBookingOutcomeAtomically(outcome, targetPax, liveInv, allocated);
        } else {
            bookingDao.persistBookingOutcomeAtomically(outcome, targetPax, liveInv, null);
        }
        execLogger.logBookingEventsToFile(outcome.getEvents());

        // 2. Reconstruct Crime Scene strictly from FileReader + StringTokenizer (proving file persistence)
        List<BookingEvent> fileEvents = fileLogManager.readBookingEventsFromFile(bookingId);
        reconstructor.printForensicReport(bookingId, fileEvents, "logs/booking_events.log (FileReader + StringTokenizer)");

        // 3. Replay booking step-by-step from SQLite ForensicDao
        List<BookingEvent> dbEvents = reconstructor.loadTimeline(bookingId, null);
        replayEngine.replayBooking(bookingId, dbEvents, 20L);

        // 4. Execute all 8 Failure Lab Scenarios
        FailureLabService failureLab = new FailureLabService(dbManager, forensicDao, execLogger);
        List<FailureLabResult> labResults = failureLab.runAllEightScenarios();
        FailureLabService.printFailureLabReport(labResults);
    }
}
