package com.tatkalx.collections;

import com.tatkalx.model.Ticket;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.ListIterator;

/**
 * Manages RAC and Waiting List queues using LinkedList<Ticket> and ListIterator<Ticket>.
 * LinkedList provides O(1) FIFO head removal (pollFirst) during promotions and O(1) tail append (addLast).
 * ListIterator enables clean traversal and position renumbering after promotions or mid-queue cancellations.
 */
public class WaitingListManager {

    private final LinkedList<Ticket> racQueue = new LinkedList<>();
    private final LinkedList<Ticket> waitingList = new LinkedList<>();

    public synchronized void enqueueRac(Ticket ticket) {
        ticket.setTicketStatus("RAC");
        int pos = racQueue.size() + 1;
        ticket.setQueuePosition(pos);
        ticket.setSeatLabel("RAC-" + pos);
        racQueue.addLast(ticket);
    }

    public synchronized void enqueueWaitingList(Ticket ticket) {
        ticket.setTicketStatus("WL");
        int pos = waitingList.size() + 1;
        ticket.setQueuePosition(pos);
        ticket.setSeatLabel("WL-" + pos);
        waitingList.addLast(ticket);
    }

    /**
     * Promotes RAC-1 to CONFIRMED and cascades WL-1 -> RAC in strict FIFO order.
     * Returns the promoted RAC ticket (or null if RAC was empty).
     */
    public synchronized Ticket promoteOnConfirmedVacancy(String freedSeatLabel) {
        if (racQueue.isEmpty()) {
            return null;
        }
        Ticket promotedToConfirmed = racQueue.removeFirst();
        promotedToConfirmed.setTicketStatus("CONFIRMED");
        promotedToConfirmed.setQueuePosition(0);
        promotedToConfirmed.setSeatLabel(freedSeatLabel);

        // Cascade WL-1 into the vacated RAC slot if waiting list is non-empty
        if (!waitingList.isEmpty()) {
            Ticket promotedToRac = waitingList.removeFirst();
            promotedToRac.setTicketStatus("RAC");
            racQueue.addLast(promotedToRac);
        }

        renumberQueue(racQueue, "RAC-");
        renumberQueue(waitingList, "WL-");
        return promotedToConfirmed;
    }

    /**
     * Cancels a specific ticket inside RAC or WL using ListIterator and renumbers remaining entries.
     */
    public synchronized boolean cancelFromQueues(long ticketId) {
        boolean removed = removeByTicketId(racQueue, ticketId);
        if (removed) {
            if (!waitingList.isEmpty()) {
                Ticket wlFirst = waitingList.removeFirst();
                wlFirst.setTicketStatus("RAC");
                racQueue.addLast(wlFirst);
            }
            renumberQueue(racQueue, "RAC-");
            renumberQueue(waitingList, "WL-");
            return true;
        }
        removed = removeByTicketId(waitingList, ticketId);
        if (removed) {
            renumberQueue(waitingList, "WL-");
        }
        return removed;
    }

    private boolean removeByTicketId(LinkedList<Ticket> queue, long ticketId) {
        ListIterator<Ticket> listIterator = queue.listIterator();
        while (listIterator.hasNext()) {
            Ticket t = listIterator.next();
            if (t.getTicketId() == ticketId) {
                t.setTicketStatus("CANCELLED");
                listIterator.remove();
                return true;
            }
        }
        return false;
    }

    /**
     * Uses ListIterator to renumber queue positions sequentially from 1..N.
     */
    private void renumberQueue(LinkedList<Ticket> queue, String prefix) {
        ListIterator<Ticket> it = queue.listIterator();
        int position = 1;
        while (it.hasNext()) {
            Ticket t = it.next();
            t.setQueuePosition(position);
            t.setSeatLabel(prefix + position);
            position++;
        }
    }

    /**
     * Demonstrates reverse traversal via ListIterator starting from the tail of the Waiting List.
     */
    public synchronized List<Ticket> getWaitingListReverseSnapshot() {
        List<Ticket> reversed = new ArrayList<>();
        ListIterator<Ticket> it = waitingList.listIterator(waitingList.size());
        while (it.hasPrevious()) {
            reversed.add(it.previous());
        }
        return reversed;
    }

    public synchronized List<Ticket> getRacSnapshot() {
        return Collections.unmodifiableList(new ArrayList<>(racQueue));
    }

    public synchronized List<Ticket> getWlSnapshot() {
        return Collections.unmodifiableList(new ArrayList<>(waitingList));
    }
}
