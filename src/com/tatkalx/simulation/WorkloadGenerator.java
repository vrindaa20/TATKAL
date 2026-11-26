package com.tatkalx.simulation;

import com.tatkalx.model.BookingRequest;
import com.tatkalx.model.Passenger;
import com.tatkalx.utility.IdGenerator;

import java.util.ArrayList;
import java.util.List;

/**
 * Generates deterministic, realistic passenger and booking-request workloads
 * that can be cloned identically across Universe A, Universe B, and Universe C.
 */
public class WorkloadGenerator {

    private static final String[] CATEGORIES = {"NORMAL", "NORMAL", "NORMAL", "SENIOR_CITIZEN", "DEFENCE", "MEDICAL"};
    private static final String[] PREFS = {"LB", "MB", "UB", "SL", "SU", "ANY"};

    public List<Passenger> generatePassengers(int count) {
        List<Passenger> passengers = new ArrayList<>(count);
        for (int i = 1; i <= count; i++) {
            String cat = CATEGORIES[(i - 1) % CATEGORIES.length];
            int age = "SENIOR_CITIZEN".equals(cat) ? (60 + (i % 18)) : (20 + (i % 35));
            String gender = (i % 2 == 0) ? "F" : "M";
            String pref = PREFS[(i - 1) % PREFS.length];

            passengers.add(new Passenger(
                    100 + i,
                    i,
                    "pax_user_" + i,
                    "Passenger-" + i,
                    "pax" + i + "@tatkalx.in",
                    String.format("98%08d", i),
                    age,
                    gender,
                    "AADHAAR",
                    String.format("AAD-%06d", i),
                    true,
                    cat,
                    pref,
                    25000.0
            ));
        }
        return passengers;
    }

    public List<BookingRequest> generateRequests(List<Passenger> passengers,
                                                 long scheduleId,
                                                 String coachClass,
                                                 boolean includeDuplicateAttempt) {
        List<BookingRequest> requests = new ArrayList<>();
        for (int i = 0; i < passengers.size(); i++) {
            Passenger p = passengers.get(i);
            String ts = String.format("10:00:%02d.%03d", (i / 10) % 60, (i * 37) % 1000);
            requests.add(new BookingRequest(
                    IdGenerator.nextBookingId(),
                    0L,
                    p.getPassengerId(),
                    p.getFullName(),
                    p.getAge(),
                    p.getPriorityCategory(),
                    scheduleId,
                    coachClass,
                    1,
                    ts
            ));
        }

        if (includeDuplicateAttempt && !passengers.isEmpty()) {
            Passenger first = passengers.get(0);
            requests.add(new BookingRequest(
                    IdGenerator.nextBookingId(),
                    0L,
                    first.getPassengerId(),
                    first.getFullName(),
                    first.getAge(),
                    first.getPriorityCategory(),
                    scheduleId,
                    coachClass,
                    1,
                    "10:00:05.999"
            ));
        }
        return requests;
    }
}
