package com.tatkalx.service;

import com.tatkalx.model.Berth;
import com.tatkalx.model.SeatInventory;
import com.tatkalx.model.Ticket;
import com.tatkalx.simulation.SimulationSessionContext;
import com.tatkalx.utility.TimeUtils;

/**
 * Handles automatic FIFO queue promotion when a Confirmed berth is vacated:
 * 1) RAC-1 promotes to CONFIRMED and receives the freed physical berth.
 * 2) WL-1 cascades into the vacated RAC slot.
 */
public class QueuePromotionService {

    private final NotificationService notificationService;

    public QueuePromotionService(NotificationService notificationService) {
        this.notificationService = notificationService;
    }

    /**
     * Must be invoked while holding SeatInventory's monitorLock (or acquires it safely).
     */
    public Ticket promoteNextAfterBerthFreed(SimulationSessionContext context, Berth freedBerth) {
        SeatInventory inventory = context.getSeatInventory();
        synchronized (inventory.getMonitorLock()) {
            if (context.getWaitingListManager().getRacSnapshot().isEmpty()) {
                return null;
            }
            if (inventory.getAvailableBerthPool().isEmpty()) {
                return null;
            }

            // Take the freed berth (or head of pool) to assign to RAC-1
            Berth assignedBerth = freedBerth;
            if (assignedBerth == null || !inventory.getAvailableBerthPool().remove(assignedBerth)) {
                assignedBerth = inventory.getAvailableBerthPool().removeFirst();
            }

            boolean hadWaitingList = !context.getWaitingListManager().getWlSnapshot().isEmpty();
            Ticket promoted = context.getWaitingListManager().promoteOnConfirmedVacancy(assignedBerth.getFormattedLabel());
            if (promoted == null) {
                inventory.getAvailableBerthPool().addFirst(assignedBerth);
                return null;
            }

            // Update SeatInventory counters atomically
            inventory.setAvailableTatkalSeats(inventory.getAvailableTatkalSeats() - 1);
            inventory.setConfirmedCount(inventory.getConfirmedCount() + 1);
            inventory.getOccupiedBerthIds().add(assignedBerth.getBerthId());
            inventory.getAllocatedBerthsByTicket().put(promoted.getTicketId(), java.util.Collections.singletonList(assignedBerth));
            inventory.setRacAllocated(context.getWaitingListManager().getRacSnapshot().size());
            inventory.setWlAllocated(context.getWaitingListManager().getWlSnapshot().size());
            inventory.incrementVersion();
            promoted.setUpdatedAt(TimeUtils.nowTimestamp());

            // Update PNR status for promoted RAC -> CONFIRMED
            for (com.tatkalx.model.PnrRecord pnr : context.getPnrDirectory().getAllSortedPnrs()) {
                if (pnr.getTicketId() == promoted.getTicketId()) {
                    context.getPnrDirectory().updateStatus(pnr.getPnrNumber(), "CONFIRMED");
                    break;
                }
            }

            notificationService.sendNotification(
                    context.getSessionId(),
                    promoted.getPassengerId(),
                    promoted.getBookingId(),
                    "RAC Promoted to CONFIRMED",
                    "Your ticket " + promoted.getBookingId() + " has been promoted to " + assignedBerth.getFormattedLabel()
            );

            // If WL-1 also cascaded to RAC, update its PNR & notify
            if (hadWaitingList) {
                for (Ticket racTicket : context.getWaitingListManager().getRacSnapshot()) {
                    if (racTicket.getQueuePosition() == context.getWaitingListManager().getRacSnapshot().size()) {
                        for (com.tatkalx.model.PnrRecord pnr : context.getPnrDirectory().getAllSortedPnrs()) {
                            if (pnr.getTicketId() == racTicket.getTicketId()) {
                                context.getPnrDirectory().updateStatus(pnr.getPnrNumber(), "RAC");
                                break;
                            }
                        }
                        notificationService.sendNotification(
                                context.getSessionId(),
                                racTicket.getPassengerId(),
                                racTicket.getBookingId(),
                                "WL Promoted to RAC",
                                "Your ticket " + racTicket.getBookingId() + " moved from Waiting List to " + racTicket.getSeatLabel()
                        );
                    }
                }
            }

            return promoted;
        }
    }
}
