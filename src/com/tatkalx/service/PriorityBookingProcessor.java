package com.tatkalx.service;

import com.tatkalx.collections.PriorityPolicyComparators;
import com.tatkalx.model.BookingRequest;
import com.tatkalx.simulation.SimulationSessionContext;

/**
 * Priority-Based Booking Processor: computes transparent priority scores
 * before delegating to BerthAllocationService.
 */
public class PriorityBookingProcessor implements BookingProcessor {

    private final BerthAllocationService allocationService;

    public PriorityBookingProcessor(BerthAllocationService allocationService) {
        this.allocationService = allocationService;
    }

    public static int computePriorityScore(BookingRequest request) {
        int baseWeight = PriorityPolicyComparators.categoryWeight(request.getPriorityCategory()) * 100;
        int ageBonus = request.getPassengerAge() >= 60 ? 50 : (request.getPassengerAge() / 5);
        return baseWeight + ageBonus;
    }

    @Override
    public BookingOutcome processBooking(BookingRequest request, SimulationSessionContext context) {
        request.setPriorityScore(computePriorityScore(request));
        return allocationService.allocateAndConfirm(request, context, "WALLET");
    }
}
