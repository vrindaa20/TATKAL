package com.tatkalx.concurrency;

import com.tatkalx.exception.TatkalWindowClosedException;
import com.tatkalx.model.BookingRequest;
import com.tatkalx.model.SeatInventory;
import com.tatkalx.service.BerthAllocationService;
import com.tatkalx.service.BookingOutcome;
import com.tatkalx.simulation.SimulationSessionContext;

/**
 * Runnable worker representing a single competing passenger request at 10:00 AM.
 * Waits on TatkalVirtualClock (WAITING state), wakes on notifyAll(), checks pre-lock availability,
 * and enters BerthAllocationService's synchronized critical section.
 */
public class BookingWorkerTask implements Runnable {

    private final BookingRequest request;
    private final SimulationSessionContext context;
    private final BerthAllocationService allocationService;
    private final RaceConditionDetector raceDetector;
    private final String paymentMethod;
    private BookingOutcome outcome;

    public BookingWorkerTask(BookingRequest request,
                             SimulationSessionContext context,
                             BerthAllocationService allocationService,
                             RaceConditionDetector raceDetector,
                             String paymentMethod) {
        this.request = request;
        this.context = context;
        this.allocationService = allocationService;
        this.raceDetector = raceDetector;
        this.paymentMethod = paymentMethod;
    }

    @Override
    public void run() {
        try {
            // 1. Wait at the 10:00:00 AM virtual clock barrier
            context.getVirtualClock().awaitWindowOpen();

            // 2. Capture optimistic availability BEFORE entering synchronized lock (to prove contention)
            SeatInventory inv = context.getSeatInventory();
            int preLockAvail = inv.getAvailableTatkalSeats();

            // 3. Synchronized check inside RaceConditionDetector & atomic allocation
            synchronized (inv.getMonitorLock()) {
                if (raceDetector != null) {
                    raceDetector.recordContentionCheck(
                            request.getBookingId(), Thread.currentThread().getName(), preLockAvail, inv);
                }
            }

            this.outcome = allocationService.allocateAndConfirm(request, context, paymentMethod);

        } catch (InterruptedException ie) {
            Thread.currentThread().interrupt();
            request.setStatus("FAILED");
            request.setFailureReason("Worker thread interrupted before completion.");
            context.recordExecutionOrder(Thread.currentThread().getName() + " -> " + request.getBookingId() + " [INTERRUPTED]");
        } catch (TatkalWindowClosedException closedEx) {
            request.setStatus("FAILED");
            request.setFailureReason(closedEx.getMessage());
        }
    }

    public BookingOutcome getOutcome() {
        return outcome;
    }
}
