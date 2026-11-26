package com.tatkalx;

import com.tatkalx.collections.BookingHistoryCache;
import com.tatkalx.collections.DuplicateBookingGuard;
import com.tatkalx.collections.GenericRepository;
import com.tatkalx.collections.PassengerRegistry;
import com.tatkalx.collections.PnrDirectory;
import com.tatkalx.collections.PriorityPolicyComparators;
import com.tatkalx.collections.RequestDequeManager;
import com.tatkalx.collections.WaitingListManager;
import com.tatkalx.exception.BookingValidationException;
import com.tatkalx.exception.DuplicateBookingException;
import com.tatkalx.model.Admin;
import com.tatkalx.model.BookingRequest;
import com.tatkalx.model.Passenger;
import com.tatkalx.model.PnrRecord;
import com.tatkalx.model.Ticket;
import com.tatkalx.model.User;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Queue;

/**
 * Phase 4 Verification Runner: Exercises every Collection, Iterator, ListIterator,
 * Comparable, Comparator, Collections utility, and Generics wildcard with domain objects.
 */
public class Phase4CollectionsDemo {

    public static void main(String[] args) {
        System.out.println("=================================================================");
        System.out.println("  TATKAL-X | PHASE 4: COLLECTIONS & GENERICS VERIFICATION LAB");
        System.out.println("=================================================================\n");

        // 1. ArrayList + HashMap + HashSet + Iterator (PassengerRegistry)
        PassengerRegistry registry = new PassengerRegistry();
        Passenger p1 = new Passenger(2, 101, "aarav_99", "Aarav Sharma", "aarav@mail.in", "9876543210", 27, "M", "AADHAAR", "AAD-1001", true, "NORMAL", "SL", 15000);
        Passenger p2 = new Passenger(3, 102, "meera_k", "Meera Krishnan", "meera@mail.in", "9876543211", 66, "F", "PAN", "PAN-2002", true, "SENIOR_CITIZEN", "LB", 20000);
        Passenger p3 = new Passenger(4, 103, "rohan_m", "Rohan Mehta", "rohan@mail.in", "9876543212", 41, "M", "PASSPORT", "PAS-3003", true, "MEDICAL", "LB", 12000);
        Passenger p4 = new Passenger(5, 104, "ghost_u", "Unverified User", "ghost@mail.in", "9876543213", 22, "M", "VOTER_ID", "VOT-4004", false, "NORMAL", "UB", 5000);

        try {
            registry.registerPassenger(p2);
            registry.registerPassenger(p1);
            registry.registerPassenger(p3);
            registry.registerPassenger(p4);
            // Attempt duplicate ID proof
            registry.registerPassenger(new Passenger(6, 105, "fake_u", "Fake", "f@m.in", "900", 30, "M", "PAN", "PAN-2002", true, "NORMAL", "ANY", 1000));
        } catch (BookingValidationException ex) {
            System.out.println("[1. PassengerRegistry] Caught expected duplicate check: " + ex.getMessage());
        }

        System.out.println("[1. HashMap Lookup id=102]: " + registry.findById(102));
        System.out.println("[1. Sorted by Age Desc (Comparator)]:");
        for (Passenger p : registry.getAllSortedByAgeDesc()) {
            System.out.println("    " + p);
        }
        int purged = registry.purgeUnverifiedPassengers();
        System.out.println("[1. Iterator Safe Purge] Removed unverified count = " + purged + ", remaining = " + registry.count() + "\n");

        // 2. HashSet + LinkedHashSet (DuplicateBookingGuard)
        DuplicateBookingGuard guard = new DuplicateBookingGuard();
        try {
            guard.acquireOrThrow(1L, 1L, 101);
            guard.acquireOrThrow(1L, 1L, 102);
            guard.acquireOrThrow(1L, 1L, 101); // Duplicate!
        } catch (DuplicateBookingException ex) {
            System.out.println("[2. DuplicateBookingGuard] Blocked: " + ex.getMessage());
        }
        System.out.println("[2. LinkedHashSet Audit Rejected Keys]: " + guard.getOrderedRejectedAuditKeys() + "\n");

        // 3. Deque + Queue + LinkedHashSet (RequestDequeManager)
        BookingRequest r1 = new BookingRequest("TX1001", 1L, 101, "Aarav Sharma", 27, "NORMAL", 1L, "3A", 2, "10:00:00.120");
        BookingRequest r2 = new BookingRequest("TX1002", 1L, 102, "Meera Krishnan", 66, "SENIOR_CITIZEN", 1L, "3A", 1, "10:00:00.050");
        BookingRequest r3 = new BookingRequest("TX1003", 1L, 103, "Rohan Mehta", 41, "MEDICAL", 1L, "3A", 1, "10:00:01.010");

        RequestDequeManager dequeManager = new RequestDequeManager();
        dequeManager.enqueueNormal(r1);
        dequeManager.enqueueNormal(r2);
        dequeManager.enqueueFrontPriority(r3); // Urgent front insertion
        System.out.println("[3. RequestDequeManager] Head peek=" + dequeManager.peekNext().getBookingId()
                + ", Tail peek=" + dequeManager.peekLast().getBookingId());
        Queue<BookingRequest> fifoSnapshot = dequeManager.asFifoQueueSnapshot();
        System.out.println("[3. Queue Snapshot size]=" + fifoSnapshot.size() + ", LinkedHashSet IDs=" + dequeManager.getOrderedUniqueEnqueuedIds() + "\n");

        // 4. PriorityQueue + Comparator (PriorityPolicyComparators)
        List<BookingRequest> priorityOrdered = PriorityPolicyComparators.orderRequestsByPolicy(
                Arrays.asList(r1, r2, r3),
                PriorityPolicyComparators.MEDICAL_SENIOR_FIRST
        );
        System.out.println("[4. PriorityQueue (MEDICAL_SENIOR_FIRST)]:");
        for (BookingRequest req : priorityOrdered) {
            System.out.println("    " + req);
        }
        System.out.println();

        // 5. LinkedHashMap + TreeMap (BookingHistoryCache)
        BookingHistoryCache historyCache = new BookingHistoryCache();
        historyCache.recordBooking(r1);
        historyCache.recordBooking(r2);
        historyCache.recordBooking(r3);
        System.out.println("[5. LinkedHashMap Insertion Order]:");
        for (BookingRequest br : historyCache.getInsertionOrderedHistory()) {
            System.out.println("    " + br.getBookingId() + " @ " + br.getRequestTimestamp());
        }
        System.out.println("[5. TreeMap Sorted Time Summary]: " + historyCache.getChronologicalSummaryCounts());
        System.out.println("[5. TreeMap subMap 10:00:00..10:00:00.999 count]: "
                + historyCache.getBookingsBetweenTimestamps("10:00:00", "10:00:00.999").size() + "\n");

        // 6. TreeSet + HashMap (PnrDirectory)
        PnrDirectory pnrDirectory = new PnrDirectory();
        pnrDirectory.registerPnr(new PnrRecord(1, "4521000099", 1, 1L, "CONFIRMED", false, "10:00:01"));
        pnrDirectory.registerPnr(new PnrRecord(2, "4521000011", 2, 1L, "RAC", false, "10:00:02"));
        pnrDirectory.registerPnr(new PnrRecord(3, "4521000055", 3, 1L, "WL", false, "10:00:03"));
        System.out.println("[6. TreeSet Sorted PNRs]:");
        for (PnrRecord pnr : pnrDirectory.getAllSortedPnrs()) {
            System.out.println("    " + pnr);
        }
        System.out.println("[6. HashMap Lookup 4521000055]: " + pnrDirectory.lookupPnr("4521000055") + "\n");

        // 7. LinkedList + ListIterator (WaitingListManager)
        WaitingListManager wlManager = new WaitingListManager();
        Ticket tRac1 = new Ticket(501, "TX1004", 1L, 201, 1L, 1, "B1", "RAC", 1, "", 1950, "10:00:04");
        Ticket tWl1 = new Ticket(502, "TX1005", 1L, 202, 1L, 1, "B1", "WL", 1, "", 1950, "10:00:05");
        Ticket tWl2 = new Ticket(503, "TX1006", 1L, 203, 1L, 1, "B1", "WL", 2, "", 1950, "10:00:06");
        wlManager.enqueueRac(tRac1);
        wlManager.enqueueWaitingList(tWl1);
        wlManager.enqueueWaitingList(tWl2);

        System.out.println("[7. Before Promotion] RAC=" + wlManager.getRacSnapshot() + " | WL=" + wlManager.getWlSnapshot());
        Ticket promoted = wlManager.promoteOnConfirmedVacancy("B1-19 (SL)");
        System.out.println("[7. Promoted to Confirmed]: " + promoted);
        System.out.println("[7. After Cascade Promotion] RAC=" + wlManager.getRacSnapshot() + " | WL=" + wlManager.getWlSnapshot());
        System.out.println("[7. Reverse WL via ListIterator]: " + wlManager.getWaitingListReverseSnapshot() + "\n");

        // 8. Generics (Bounded Types, Generic Methods, ? extends, ? super, Collections.binarySearch)
        GenericRepository<PnrRecord> pnrRepo = new GenericRepository<>();
        pnrRepo.add(new PnrRecord(1, "4521000099", 1, 1L, "CONFIRMED", false, "10:00:01"));
        pnrRepo.add(new PnrRecord(2, "4521000011", 2, 1L, "RAC", false, "10:00:02"));
        int idx = pnrRepo.binarySearchSorted(new PnrRecord(0, "4521000099", 0, 0, "", false, ""));
        System.out.println("[8. Generics binarySearch index for 4521000099]: " + idx);

        List<User> mixedUsers = new ArrayList<>();
        mixedUsers.add(p1);
        mixedUsers.add(new Admin(1, 1, "admin_tx", "Chief Controller", "admin@tatkalx.in", "999", "ADM-01", "Forensics", 5));
        System.out.println("[8. Wildcard (? extends User) descriptions]: " + GenericRepository.extractRoleDescriptions(mixedUsers));

        List<User> superDestination = new ArrayList<>();
        GenericRepository.addVerifiedPassengersToTarget(registry.getAllSortedById(), superDestination);
        System.out.println("[8. Wildcard (? super Passenger) copied verified count]: " + superDestination.size());
    }
}
