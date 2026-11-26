package com.tatkalx.dao;

import com.tatkalx.database.DatabaseConnectionManager;
import com.tatkalx.exception.DatabaseOperationException;
import com.tatkalx.model.Berth;
import com.tatkalx.model.Coach;
import com.tatkalx.model.Route;
import com.tatkalx.model.Station;
import com.tatkalx.model.Train;
import com.tatkalx.model.TrainSchedule;
import com.tatkalx.model.TrainStop;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

/**
 * JDBC DAO for Railway master tables: Stations, Trains, Routes, Train_Stops,
 * Train_Schedules, Coaches, and Berths.
 */
public class RailwayDao {

    private final DatabaseConnectionManager connectionManager;

    public RailwayDao(DatabaseConnectionManager connectionManager) {
        this.connectionManager = connectionManager;
    }

    public int insertStation(Station station) {
        return connectionManager.executeWriteTransaction(conn -> {
            String sql = "INSERT INTO Stations (station_code, station_name, city, state, railway_zone) VALUES (?, ?, ?, ?, ?)";
            try (PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
                ps.setString(1, station.getStationCode());
                ps.setString(2, station.getStationName());
                ps.setString(3, station.getCity());
                ps.setString(4, station.getState());
                ps.setString(5, station.getRailwayZone());
                ps.executeUpdate();
                try (ResultSet rs = ps.getGeneratedKeys()) {
                    if (rs.next()) {
                        station.setStationId(rs.getInt(1));
                        return station.getStationId();
                    }
                    throw new SQLException("Failed to get station_id");
                }
            }
        });
    }

    public List<Station> findAllStations() {
        List<Station> list = new ArrayList<>();
        String sql = "SELECT station_id, station_code, station_name, city, state, railway_zone FROM Stations ORDER BY station_id";
        try (Connection conn = connectionManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                list.add(new Station(
                        rs.getInt("station_id"),
                        rs.getString("station_code"),
                        rs.getString("station_name"),
                        rs.getString("city"),
                        rs.getString("state"),
                        rs.getString("railway_zone")));
            }
            return list;
        } catch (SQLException ex) {
            throw new DatabaseOperationException("Error querying Stations", ex);
        }
    }

    public int insertTrain(Train train) {
        return connectionManager.executeWriteTransaction(conn -> {
            String sql = "INSERT INTO Trains (train_number, train_name, train_type, total_coaches, is_tatkal_enabled, status) "
                    + "VALUES (?, ?, ?, ?, ?, ?)";
            try (PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
                ps.setString(1, train.getTrainNumber());
                ps.setString(2, train.getTrainName());
                ps.setString(3, train.getTrainType());
                ps.setInt(4, train.getTotalCoaches());
                ps.setInt(5, train.isTatkalEnabled() ? 1 : 0);
                ps.setString(6, train.getStatus());
                ps.executeUpdate();
                try (ResultSet rs = ps.getGeneratedKeys()) {
                    if (rs.next()) {
                        train.setTrainId(rs.getInt(1));
                        return train.getTrainId();
                    }
                    throw new SQLException("Failed to get train_id");
                }
            }
        });
    }

    public List<Train> findAllTrains() {
        List<Train> list = new ArrayList<>();
        String sql = "SELECT train_id, train_number, train_name, train_type, total_coaches, is_tatkal_enabled, status "
                + "FROM Trains ORDER BY train_id";
        try (Connection conn = connectionManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                list.add(new Train(
                        rs.getInt("train_id"),
                        rs.getString("train_number"),
                        rs.getString("train_name"),
                        rs.getString("train_type"),
                        rs.getInt("total_coaches"),
                        rs.getInt("is_tatkal_enabled") == 1,
                        rs.getString("status")));
            }
            return list;
        } catch (SQLException ex) {
            throw new DatabaseOperationException("Error querying Trains", ex);
        }
    }

    public List<TrainSchedule> findAllSchedules() {
        List<TrainSchedule> list = new ArrayList<>();
        String sql = "SELECT ts.schedule_id, ts.train_id, ts.route_id, t.train_number, t.train_name, "
                + "r.total_distance_km, ts.journey_date, ts.departure_time, ts.arrival_time, "
                + "ts.tatkal_open_time, ts.tatkal_close_time, ts.schedule_status "
                + "FROM Train_Schedules ts "
                + "JOIN Trains t ON ts.train_id = t.train_id "
                + "JOIN Routes r ON ts.route_id = r.route_id ORDER BY ts.schedule_id";
        try (Connection conn = connectionManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                list.add(new TrainSchedule(
                        rs.getLong("schedule_id"),
                        rs.getInt("train_id"),
                        rs.getInt("route_id"),
                        rs.getString("train_number"),
                        rs.getString("train_name"),
                        rs.getInt("total_distance_km"),
                        rs.getString("journey_date"),
                        rs.getString("departure_time"),
                        rs.getString("arrival_time"),
                        rs.getString("tatkal_open_time"),
                        rs.getString("tatkal_close_time"),
                        rs.getString("schedule_status")));
            }
            return list;
        } catch (SQLException ex) {
            throw new DatabaseOperationException("Error querying Train_Schedules", ex);
        }
    }

    public List<TrainStop> findStopsByRouteId(int routeId) {
        List<TrainStop> stops = new ArrayList<>();
        String sql = "SELECT ts.stop_id, ts.route_id, ts.station_id, s.station_code, s.station_name, "
                + "ts.stop_order, ts.distance_from_origin_km, ts.arrival_offset_min, ts.departure_offset_min "
                + "FROM Train_Stops ts JOIN Stations s ON ts.station_id = s.station_id "
                + "WHERE ts.route_id = ? ORDER BY ts.stop_order";
        try (Connection conn = connectionManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, routeId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    stops.add(new TrainStop(
                            rs.getInt("stop_id"),
                            rs.getInt("route_id"),
                            rs.getInt("station_id"),
                            rs.getString("station_code"),
                            rs.getString("station_name"),
                            rs.getInt("stop_order"),
                            rs.getInt("distance_from_origin_km"),
                            rs.getInt("arrival_offset_min"),
                            rs.getInt("departure_offset_min")));
                }
            }
            return stops;
        } catch (SQLException ex) {
            throw new DatabaseOperationException("Error querying Train_Stops", ex);
        }
    }

    public List<Route> findAllRoutes() {
        List<Route> routes = new ArrayList<>();
        String sql = "SELECT route_id, route_code, origin_station_id, destination_station_id, total_distance_km FROM Routes ORDER BY route_id";
        try (Connection conn = connectionManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                Route r = new Route(
                        rs.getInt("route_id"),
                        rs.getString("route_code"),
                        rs.getInt("origin_station_id"),
                        rs.getInt("destination_station_id"),
                        rs.getInt("total_distance_km"));
                for (TrainStop stop : findStopsByRouteId(r.getRouteId())) {
                    r.addStop(stop);
                }
                routes.add(r);
            }
            return routes;
        } catch (SQLException ex) {
            throw new DatabaseOperationException("Error querying Routes", ex);
        }
    }

    public List<Coach> findCoachesByTrainId(int trainId) {
        List<Coach> coaches = new ArrayList<>();
        String sql = "SELECT coach_id, train_id, coach_code, coach_class, total_berths, "
                + "tatkal_quota_berths, rac_quota, wl_quota, base_fare_per_km FROM Coaches WHERE train_id = ? ORDER BY coach_id";
        try (Connection conn = connectionManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, trainId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    coaches.add(new Coach(
                            rs.getInt("coach_id"),
                            rs.getInt("train_id"),
                            rs.getString("coach_code"),
                            rs.getString("coach_class"),
                            rs.getInt("total_berths"),
                            rs.getInt("tatkal_quota_berths"),
                            rs.getInt("rac_quota"),
                            rs.getInt("wl_quota"),
                            rs.getDouble("base_fare_per_km")));
                }
            }
            return coaches;
        } catch (SQLException ex) {
            throw new DatabaseOperationException("Error querying Coaches", ex);
        }
    }

    public List<Berth> findBerthsByCoachId(int coachId, String coachCode) {
        List<Berth> berths = new ArrayList<>();
        String sql = "SELECT berth_id, coach_id, berth_number, berth_type, is_tatkal_reserved, is_rac_shared "
                + "FROM Berths WHERE coach_id = ? ORDER BY berth_number";
        try (Connection conn = connectionManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, coachId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    berths.add(new Berth(
                            rs.getInt("berth_id"),
                            rs.getInt("coach_id"),
                            coachCode,
                            rs.getInt("berth_number"),
                            rs.getString("berth_type"),
                            rs.getInt("is_tatkal_reserved") == 1,
                            rs.getInt("is_rac_shared") == 1));
                }
            }
            return berths;
        } catch (SQLException ex) {
            throw new DatabaseOperationException("Error querying Berths", ex);
        }
    }

    public int insertRoute(String routeCode, int originStationId, int destStationId, int distanceKm) {
        return connectionManager.executeWriteTransaction(conn -> {
            String sql = "INSERT INTO Routes (route_code, origin_station_id, destination_station_id, total_distance_km) VALUES (?, ?, ?, ?)";
            try (PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
                ps.setString(1, routeCode);
                ps.setInt(2, originStationId);
                ps.setInt(3, destStationId);
                ps.setInt(4, distanceKm);
                ps.executeUpdate();
                try (ResultSet rs = ps.getGeneratedKeys()) {
                    if (rs.next()) {
                        int routeId = rs.getInt(1);
                        String stopSql = "INSERT INTO Train_Stops (route_id, station_id, stop_order, distance_from_origin_km, arrival_offset_min, departure_offset_min) VALUES (?, ?, ?, ?, ?, ?)";
                        try (PreparedStatement psStop = conn.prepareStatement(stopSql)) {
                            psStop.setInt(1, routeId);
                            psStop.setInt(2, originStationId);
                            psStop.setInt(3, 1);
                            psStop.setInt(4, 0);
                            psStop.setInt(5, 0);
                            psStop.setInt(6, 0);
                            psStop.addBatch();

                            psStop.setInt(1, routeId);
                            psStop.setInt(2, destStationId);
                            psStop.setInt(3, 2);
                            psStop.setInt(4, distanceKm);
                            psStop.setInt(5, distanceKm / 2);
                            psStop.setInt(6, distanceKm / 2);
                            psStop.addBatch();
                            psStop.executeBatch();
                        }
                        return routeId;
                    }
                    throw new SQLException("Failed to get route_id");
                }
            }
        });
    }

    public long insertSchedule(int trainId, int routeId, String journeyDate, String depTime, String arrTime) {
        return connectionManager.executeWriteTransaction(conn -> {
            String sql = "INSERT INTO Train_Schedules (train_id, route_id, journey_date, departure_time, arrival_time) VALUES (?, ?, ?, ?, ?)";
            try (PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
                ps.setInt(1, trainId);
                ps.setInt(2, routeId);
                ps.setString(3, journeyDate);
                ps.setString(4, depTime);
                ps.setString(5, arrTime);
                ps.executeUpdate();
                try (ResultSet rs = ps.getGeneratedKeys()) {
                    if (rs.next()) {
                        return rs.getLong(1);
                    }
                    throw new SQLException("Failed to get schedule_id");
                }
            }
        });
    }

    public int insertCoachWithBerths(Coach coach) {
        return connectionManager.executeWriteTransaction(conn -> {
            String sql = "INSERT INTO Coaches (train_id, coach_code, coach_class, total_berths, tatkal_quota_berths, rac_quota, wl_quota, base_fare_per_km) "
                    + "VALUES (?, ?, ?, ?, ?, ?, ?, ?)";
            int coachId;
            try (PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
                ps.setInt(1, coach.getTrainId());
                ps.setString(2, coach.getCoachCode());
                ps.setString(3, coach.getCoachClass());
                ps.setInt(4, coach.getTotalBerths());
                ps.setInt(5, coach.getTatkalQuotaBerths());
                ps.setInt(6, coach.getRacQuota());
                ps.setInt(7, coach.getWlQuota());
                ps.setDouble(8, coach.getBaseFarePerKm());
                ps.executeUpdate();
                try (ResultSet rs = ps.getGeneratedKeys()) {
                    if (!rs.next()) {
                        throw new SQLException("Failed to get coach_id");
                    }
                    coachId = rs.getInt(1);
                    coach.setCoachId(coachId);
                }
            }

            String[] cycle = {"LB", "MB", "UB", "LB", "MB", "UB", "SL", "SU"};
            String berthSql = "INSERT INTO Berths (coach_id, berth_number, berth_type, is_tatkal_reserved, is_rac_shared) VALUES (?, ?, ?, ?, ?)";
            try (PreparedStatement psB = conn.prepareStatement(berthSql)) {
                for (int num = 1; num <= coach.getTotalBerths(); num++) {
                    String type = cycle[(num - 1) % cycle.length];
                    psB.setInt(1, coachId);
                    psB.setInt(2, num);
                    psB.setString(3, type);
                    psB.setInt(4, num <= coach.getTatkalQuotaBerths() ? 1 : 0);
                    psB.setInt(5, ("SL".equals(type) && num > coach.getTatkalQuotaBerths()) ? 1 : 0);
                    psB.addBatch();
                }
                psB.executeBatch();
            }
            return coachId;
        });
    }
}
