package com.tatkalx.investigation;

import com.tatkalx.model.BookingEvent;
import com.tatkalx.utility.ConsoleFormatter;

import java.util.List;

/**
 * Component #38: Booking Replay Engine.
 * Replays a recorded booking's lifecycle step-by-step from creation to final state.
 */
public class BookingReplayEngine {

    private final ForensicReconstructor reconstructor;

    public BookingReplayEngine(ForensicReconstructor reconstructor) {
        this.reconstructor = reconstructor;
    }

    public void replayBooking(String bookingId, long stepDelayMs) {
        List<BookingEvent> events = reconstructor.loadTimeline(bookingId, null);
        replayBooking(bookingId, events, stepDelayMs);
    }

    public void replayBooking(String bookingId, List<BookingEvent> events, long stepDelayMs) {
        ConsoleFormatter.printBanner("BOOKING REPLAY ENGINE | TARGET: " + bookingId);
        if (events == null || events.isEmpty()) {
            System.out.println("  Cannot replay: no events recorded for " + bookingId);
            return;
        }

        for (int i = 0; i < events.size(); i++) {
            BookingEvent ev = events.get(i);
            System.out.printf("  [STEP %d/%d] %s | Thread=%-14s | %-24s -> %s (Status=%s, Avail=%s)%n",
                    i + 1,
                    events.size(),
                    ev.getEventTimestamp(),
                    ev.getThreadName(),
                    ev.getEventType(),
                    ev.getEventDescription(),
                    ev.getRequestStatus(),
                    ev.getAvailableSeatsSnapshot() == null ? "N/A" : ev.getAvailableSeatsSnapshot());

            if (stepDelayMs > 0 && i < events.size() - 1) {
                try {
                    Thread.sleep(stepDelayMs);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    System.out.println("  [Replay interrupted]");
                    break;
                }
            }
        }
        System.out.println("  [REPLAY COMPLETE] Final Outcome: " + events.get(events.size() - 1).getRequestStatus());
        ConsoleFormatter.printDivider();
    }
}
