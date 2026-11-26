package com.tatkalx.service;

import com.tatkalx.model.BookingRequest;
import com.tatkalx.simulation.SimulationSessionContext;

/**
 * Polymorphic booking processing strategy contract shared across FIFO, Priority, and Concurrent processors.
 */
public interface BookingProcessor {

    BookingOutcome processBooking(BookingRequest request, SimulationSessionContext context);
}
