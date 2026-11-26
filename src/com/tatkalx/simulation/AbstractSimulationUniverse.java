package com.tatkalx.simulation;

import com.tatkalx.exception.BookingValidationException;
import com.tatkalx.model.BookingRequest;
import com.tatkalx.model.Coach;
import com.tatkalx.model.Passenger;
import com.tatkalx.model.SeatInventory;
import com.tatkalx.model.SimulationResult;
import com.tatkalx.model.TrainSchedule;
import com.tatkalx.service.TatkalVirtualClock;
import com.tatkalx.utility.IdGenerator;
import com.tatkalx.utility.TimeUtils;

import java.util.ArrayList;
import java.util.List;

/**
 * Shared base class for Universe A, Universe B, and Universe C.
 * Guarantees isolated inventories, cloned passenger/request inputs, and empirical metric calculation.
 */
public abstract class AbstractSimulationUniverse implements SimulationUniverse {

    protected SimulationSessionContext context;
    protected List<BookingRequest> preparedRequests = new ArrayList<>();

    @Override
    public void prepareUniverse(TrainSchedule schedule,
                                Coach coach,
                                SeatInventory templateInventory,
                                List<Passenger> passengers,
                                List<BookingRequest> templateRequests) {
        long sessionId = IdGenerator.nextSessionId();
        SeatInventory isolatedInventory = templateInventory.cloneForSession(sessionId);
        TatkalVirtualClock clock = new TatkalVirtualClock();

        this.context = new SimulationSessionContext(
                sessionId, getUniverseName(), schedule, coach, isolatedInventory, clock);

        for (Passenger p : passengers) {
            Passenger clone = new Passenger(
                    p.getUserId(), p.getPassengerId(), p.getUsername(), p.getFullName(),
                    p.getEmail(), p.getPhone(), p.getAge(), p.getGender(),
                    p.getIdProofType(), p.getIdProofNumber(), p.isVerified(),
                    p.getPriorityCategory(), p.getBerthPreference(), p.getWalletBalance()
            );
            try {
                context.getPassengerRegistry().registerPassenger(clone);
            } catch (BookingValidationException ignored) {
                // Skip duplicate passenger templates if test dataset includes duplicates
            }
        }

        this.preparedRequests = new ArrayList<>(templateRequests.size());
        for (BookingRequest req : templateRequests) {
            this.preparedRequests.add(req.copyForSession(sessionId));
        }
    }

    protected SimulationResult computeMetrics(long totalWallTimeMs, String threadSummary) {
        int confirmed = 0;
        int rac = 0;
        int wl = 0;
        int failed = 0;
        long sumLatency = 0L;
        long maxLatency = 0L;

        for (BookingRequest req : preparedRequests) {
            String status = req.getStatus();
            if ("CONFIRMED".equals(status)) {
                confirmed++;
            } else if ("RAC".equals(status)) {
                rac++;
            } else if ("WL".equals(status)) {
                wl++;
            } else {
                failed++;
            }
            sumLatency += req.getProcessingTimeMs();
            if (req.getProcessingTimeMs() > maxLatency) {
                maxLatency = req.getProcessingTimeMs();
            }
        }

        int total = preparedRequests.size();
        double avgLatency = total == 0 ? 0.0 : (double) sumLatency / total;
        double successRate = total == 0 ? 0.0 : ((confirmed + rac + wl) * 100.0) / total;

        SeatInventory inv = context.getSeatInventory();
        double seatUtil = inv.getTotalTatkalSeats() == 0
                ? 0.0
                : (inv.getConfirmedCount() * 100.0) / inv.getTotalTatkalSeats();

        SimulationResult result = new SimulationResult();
        result.setSessionId(context.getSessionId());
        result.setUniverseName(getUniverseName());
        result.setTotalRequests(total);
        result.setConfirmedCount(confirmed);
        result.setRacCount(rac);
        result.setWlCount(wl);
        result.setFailedCount(failed);
        result.setDuplicateAttempts(context.getDuplicateAttempts());
        result.setTotalTimeMs(totalWallTimeMs);
        result.setAvgTimeMs(Math.round(avgLatency * 100.0) / 100.0);
        result.setMaxTimeMs(maxLatency);
        result.setSuccessRatePct(Math.round(successRate * 100.0) / 100.0);
        result.setSeatUtilizationPct(Math.round(seatUtil * 100.0) / 100.0);
        result.setInventoryConsistent(inv.isConsistent());
        result.setThreadSummary(threadSummary);
        result.setRecordedAt(TimeUtils.nowTimestamp());
        return result;
    }

    @Override
    public SimulationSessionContext getContext() {
        return context;
    }
}
