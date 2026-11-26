package com.tatkalx.collections;

import com.tatkalx.exception.BookingValidationException;
import com.tatkalx.model.Passenger;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Manages Passenger records using:
 * - ArrayList<Passenger> for ordered storage, indexing, and sorting
 * - HashMap<Integer, Passenger> for O(1) lookup by passengerId
 * - HashSet<String> for O(1) duplicate username and ID-proof detection
 * - Iterator<Passenger> for safe traversal and removal during cleanup
 */
public class PassengerRegistry {

    private final ArrayList<Passenger> passengerList = new ArrayList<>();
    private final HashMap<Integer, Passenger> passengerById = new HashMap<>();
    private final Set<String> registeredIdProofs = new HashSet<>();
    private final Set<String> registeredUsernames = new HashSet<>();

    /**
     * Registers a passenger after verifying uniqueness in HashSets.
     */
    public void registerPassenger(Passenger passenger) throws BookingValidationException {
        if (passenger == null) {
            throw new BookingValidationException("Passenger object cannot be null.");
        }
        String usernameKey = passenger.getUsername() == null ? "" : passenger.getUsername().trim().toLowerCase();
        String idProofKey = passenger.getIdProofNumber() == null ? "" : passenger.getIdProofNumber().trim().toUpperCase();

        if (usernameKey.isEmpty() || idProofKey.isEmpty()) {
            throw new BookingValidationException("Username and ID proof number are required.");
        }
        if (registeredUsernames.contains(usernameKey)) {
            throw new BookingValidationException("Duplicate username detected: " + passenger.getUsername());
        }
        if (registeredIdProofs.contains(idProofKey)) {
            throw new BookingValidationException("Duplicate ID proof detected: " + passenger.getIdProofNumber());
        }

        registeredUsernames.add(usernameKey);
        registeredIdProofs.add(idProofKey);
        passengerList.add(passenger);
        passengerById.put(passenger.getPassengerId(), passenger);
    }

    /**
     * O(1) average-time lookup by passengerId via HashMap.
     */
    public Passenger findById(int passengerId) {
        return passengerById.get(passengerId);
    }

    /**
     * Returns a defensive copy sorted by natural ordering (passengerId via Comparable).
     */
    public List<Passenger> getAllSortedById() {
        List<Passenger> copy = new ArrayList<>(passengerList);
        Collections.sort(copy);
        return copy;
    }

    /**
     * Returns passengers sorted by age descending, then name ascending using a Comparator.
     */
    public List<Passenger> getAllSortedByAgeDesc() {
        List<Passenger> copy = new ArrayList<>(passengerList);
        Collections.sort(copy, new Comparator<Passenger>() {
            @Override
            public int compare(Passenger p1, Passenger p2) {
                int ageCmp = Integer.compare(p2.getAge(), p1.getAge());
                if (ageCmp != 0) {
                    return ageCmp;
                }
                return p1.getFullName().compareToIgnoreCase(p2.getFullName());
            }
        });
        return copy;
    }

    /**
     * Uses Iterator.remove() to safely purge unverified passengers across ArrayList, HashMap, and HashSets.
     * Direct list.remove() inside a for-each loop would throw ConcurrentModificationException.
     */
    public int purgeUnverifiedPassengers() {
        int removedCount = 0;
        Iterator<Passenger> iterator = passengerList.iterator();
        while (iterator.hasNext()) {
            Passenger current = iterator.next();
            if (!current.isVerified()) {
                iterator.remove();
                passengerById.remove(current.getPassengerId());
                if (current.getUsername() != null) {
                    registeredUsernames.remove(current.getUsername().trim().toLowerCase());
                }
                if (current.getIdProofNumber() != null) {
                    registeredIdProofs.remove(current.getIdProofNumber().trim().toUpperCase());
                }
                removedCount++;
            }
        }
        return removedCount;
    }

    public int count() {
        return passengerList.size();
    }

    public Map<Integer, Passenger> getLookupMapSnapshot() {
        return Collections.unmodifiableMap(new HashMap<>(passengerById));
    }
}
