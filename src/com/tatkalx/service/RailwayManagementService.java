package com.tatkalx.service;

import com.tatkalx.model.Berth;
import com.tatkalx.model.Coach;
import com.tatkalx.model.Route;
import com.tatkalx.model.SeatInventory;
import com.tatkalx.model.Station;
import com.tatkalx.model.Train;
import com.tatkalx.model.TrainSchedule;
import com.tatkalx.model.TrainStop;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Manages railway master data (Stations, Trains, Routes, Schedules, Coaches, Berths)
 * and creates isolated SeatInventory clones for simulations and live sessions.
 */
public class RailwayManagementService {

    private final Map<Integer, Station> stationsById = new HashMap<>();
    private final Map<Integer, Train> trainsById = new HashMap<>();
    private final Map<Integer, Route> routesById = new HashMap<>();
    private final Map<Long, TrainSchedule> schedulesById = new HashMap<>();
    private final Map<Integer, List<Coach>> coachesByTrainId = new HashMap<>();
    private final Map<Integer, List<Berth>> berthsByCoachId = new HashMap<>();

    public void addStation(Station station) {
        stationsById.put(station.getStationId(), station);
    }

    public void addTrain(Train train) {
        trainsById.put(train.getTrainId(), train);
    }

    public void addRoute(Route route) {
        routesById.put(route.getRouteId(), route);
    }

    public void addSchedule(TrainSchedule schedule) {
        schedulesById.put(schedule.getScheduleId(), schedule);
    }

    public void addCoachWithAutoBerths(Coach coach) {
        List<Coach> trainCoaches = coachesByTrainId.get(coach.getTrainId());
        if (trainCoaches == null) {
            trainCoaches = new ArrayList<>();
            coachesByTrainId.put(coach.getTrainId(), trainCoaches);
        }
        trainCoaches.add(coach);

        String[] cycle3A = {"LB", "MB", "UB", "LB", "MB", "UB", "SL", "SU"};
        List<Berth> generated = new ArrayList<>();
        for (int num = 1; num <= coach.getTotalBerths(); num++) {
            String berthType = cycle3A[(num - 1) % cycle3A.length];
            boolean tatkalReserved = num <= coach.getTatkalQuotaBerths();
            boolean racShared = "SL".equals(berthType) && !tatkalReserved;
            int berthId = coach.getCoachId() * 1000 + num;
            generated.add(new Berth(berthId, coach.getCoachId(), coach.getCoachCode(),
                    num, berthType, tatkalReserved, racShared));
        }
        berthsByCoachId.put(coach.getCoachId(), generated);
    }

    public SeatInventory createIsolatedInventory(long sessionId, long scheduleId, String coachClass) {
        TrainSchedule schedule = schedulesById.get(scheduleId);
        if (schedule == null) {
            throw new IllegalArgumentException("Unknown scheduleId: " + scheduleId);
        }
        List<Coach> coaches = coachesByTrainId.get(schedule.getTrainId());
        if (coaches == null) {
            throw new IllegalArgumentException("No coaches configured for trainId: " + schedule.getTrainId());
        }
        for (Coach coach : coaches) {
            if (coach.getCoachClass().equalsIgnoreCase(coachClass)) {
                List<Berth> berths = berthsByCoachId.get(coach.getCoachId());
                return new SeatInventory(sessionId, scheduleId, coach, berths);
            }
        }
        throw new IllegalArgumentException("No coach of class " + coachClass + " found for schedule " + scheduleId);
    }

    public Coach findCoach(int trainId, String coachClass) {
        List<Coach> coaches = coachesByTrainId.get(trainId);
        if (coaches == null) {
            return null;
        }
        for (Coach c : coaches) {
            if (c.getCoachClass().equalsIgnoreCase(coachClass)) {
                return c;
            }
        }
        return null;
    }

    public TrainSchedule getSchedule(long scheduleId) {
        return schedulesById.get(scheduleId);
    }

    public Route getRoute(int routeId) {
        return routesById.get(routeId);
    }

    public Train getTrain(int trainId) {
        return trainsById.get(trainId);
    }

    public List<TrainSchedule> getAllSchedules() {
        return Collections.unmodifiableList(new ArrayList<>(schedulesById.values()));
    }

    /**
     * Populates a realistic Mumbai Rajdhani (12952) NDLS -> BCT default railway setup.
     */
    public void loadDefaultRailwayData() {
        if (!trainsById.isEmpty()) {
            return;
        }
        Station ndls = new Station(1, "NDLS", "New Delhi", "New Delhi", "Delhi", "NR");
        Station kota = new Station(2, "KOTA", "Kota Junction", "Kota", "Rajasthan", "WCR");
        Station bct = new Station(3, "BCT", "Mumbai Central", "Mumbai", "Maharashtra", "WR");
        addStation(ndls);
        addStation(kota);
        addStation(bct);

        Train rajdhani = new Train(1, "12952", "Mumbai Rajdhani Express", "RAJDHANI", 2, true, "ACTIVE");
        addTrain(rajdhani);

        Route route = new Route(1, "NDLS-BCT-RAJ", 1, 3, 1386);
        route.addStop(new TrainStop(1, 1, 1, "NDLS", "New Delhi", 1, 0, 0, 0));
        route.addStop(new TrainStop(2, 1, 2, "KOTA", "Kota Junction", 2, 465, 290, 295));
        route.addStop(new TrainStop(3, 1, 3, "BCT", "Mumbai Central", 3, 1386, 940, 940));
        addRoute(route);

        TrainSchedule schedule = new TrainSchedule(
                1L, 1, 1, "12952", "Mumbai Rajdhani Express", 1386,
                "2026-11-26", "16:55", "08:35", "10:00:00", "11:00:00", "SCHEDULED"
        );
        addSchedule(schedule);

        addCoachWithAutoBerths(new Coach(1, 1, "B1", "3A", 72, 12, 4, 8, 1.45));
        addCoachWithAutoBerths(new Coach(2, 1, "A1", "2A", 48, 8, 2, 4, 2.15));
    }
}
