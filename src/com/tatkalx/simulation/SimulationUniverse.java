package com.tatkalx.simulation;

import com.tatkalx.model.BookingRequest;
import com.tatkalx.model.Coach;
import com.tatkalx.model.Passenger;
import com.tatkalx.model.SeatInventory;
import com.tatkalx.model.SimulationResult;
import com.tatkalx.model.TrainSchedule;

import java.util.List;

/**
 * Contract for independent Tatkal simulation universes (Sequential, Priority, Concurrent).
 */
public interface SimulationUniverse {

    String getUniverseName();

    void prepareUniverse(TrainSchedule schedule,
                         Coach coach,
                         SeatInventory templateInventory,
                         List<Passenger> passengers,
                         List<BookingRequest> templateRequests);

    SimulationResult runSimulation();

    SimulationSessionContext getContext();
}
