package com.tatkalx.service;

import com.tatkalx.model.BookingRequest;
import com.tatkalx.simulation.SimulationSessionContext;

/**
 * FIFO Booking Processor: processes booking requests strictly in arrival order
 * by delegating to the atomic BerthAllocationService.
 */
public class FifoBookingProcessor implements BookingProcessor {

    private final BerthAllocationService allocationService;

    public FifoBookingProcessor(BerthAllocationService allocationService) {
        this.allocationService = allocationService;
    }

    @Override
    public BookingOutcome processBooking(BookingRequest request, SimulationSessionContext context) {
        return allocationService.allocateAndConfirm(request, context, "UPI");
    }
}
