package com.tatkalx.investigation;

import com.tatkalx.dao.ForensicDao;
import com.tatkalx.filehandling.FileLogManager;
import com.tatkalx.model.BookingEvent;
import com.tatkalx.simulation.SimulationSessionContext;
import com.tatkalx.utility.ConsoleFormatter;

import java.io.IOException;
import java.util.Collections;
import java.util.List;

/**
 * Component #33: Booking Forensic Reconstruction ("Booking Crime Scene").
 * Reconstructs the exact chronological lifecycle of any Booking ID from SQLite,
 * FileReader logs, or active SimulationSessionContext without fabricating events.
 */
public class ForensicReconstructor {

    private final ForensicDao forensicDao;
    private final FileLogManager fileLogManager;

    public ForensicReconstructor(ForensicDao forensicDao, FileLogManager fileLogManager) {
        this.forensicDao = forensicDao;
        this.fileLogManager = fileLogManager;
    }

    public List<BookingEvent> loadTimeline(String bookingId, SimulationSessionContext optionalContext) {
        if (optionalContext != null) {
            List<BookingEvent> memEvents = optionalContext.getEventsForBooking(bookingId);
            if (!memEvents.isEmpty()) {
                return memEvents;
            }
        }

        if (forensicDao != null) {
            List<BookingEvent> dbEvents = forensicDao.findEventsByBookingId(bookingId);
            if (!dbEvents.isEmpty()) {
                return dbEvents;
            }
        }

        if (fileLogManager != null) {
            try {
                List<BookingEvent> fileEvents = fileLogManager.readBookingEventsFromFile(bookingId);
                if (!fileEvents.isEmpty()) {
                    return fileEvents;
                }
            } catch (IOException ex) {
                fileLogManager.appendErrorLog("ForensicReconstructor", "File read error: " + ex.getMessage());
            }
        }
        return Collections.emptyList();
    }

    public void printForensicReport(String bookingId, List<BookingEvent> events, String sourceLabel) {
        ConsoleFormatter.printBanner("BOOKING FORENSIC REPORT (CRIME SCENE RECONSTRUCTION)");
        if (events == null || events.isEmpty()) {
            System.out.println("  No recorded events found for Booking ID: " + bookingId);
            return;
        }

        BookingEvent first = events.get(0);
        BookingEvent last = events.get(events.size() - 1);

        System.out.println("  Booking ID   : " + bookingId);
        System.out.println("  Session ID   : " + first.getSessionId());
        System.out.println("  Passenger ID : " + first.getPassengerId());
        System.out.println("  Worker Thread: " + last.getThreadName());
        System.out.println("  Data Source  : " + sourceLabel);
        ConsoleFormatter.printDivider();

        for (BookingEvent ev : events) {
            System.out.printf("  %s | #%-2d | %-24s | %s%n",
                    ev.getEventTimestamp(), ev.getEventSeq(), ev.getEventType(), ev.getEventDescription());
            System.out.printf("      -> Thread=%-15s | Status=%-10s | AvailSeats=%s | QPos=%s | Alloc=%s | Pay=%s%n",
                    ev.getThreadName(),
                    ev.getRequestStatus(),
                    ev.getAvailableSeatsSnapshot() == null ? "N/A" : ev.getAvailableSeatsSnapshot(),
                    ev.getQueuePositionSnapshot() == null ? "N/A" : ev.getQueuePositionSnapshot(),
                    ev.getAllocationResult() == null ? "-" : ev.getAllocationResult(),
                    ev.getPaymentResult() == null ? "-" : ev.getPaymentResult());
        }

        ConsoleFormatter.printDivider();
        System.out.println("  Final Status : " + last.getRequestStatus());
        System.out.println("================================================================================");
    }
}
