package com.tatkalx.dao;

import com.tatkalx.database.DatabaseConnectionManager;
import com.tatkalx.exception.DatabaseOperationException;
import com.tatkalx.model.Notification;
import com.tatkalx.model.SimulationResult;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

/**
 * JDBC DAO for Simulation_Sessions, Simulation_Results, Notifications, and System_Configurations.
 */
public class SimulationDao {

    private final DatabaseConnectionManager connectionManager;

    public SimulationDao(DatabaseConnectionManager connectionManager) {
        this.connectionManager = connectionManager;
    }

    public long createSimulationSession(String sessionCode, String sessionType,
                                        int strategyId, long scheduleId, int totalRequests) {
        return connectionManager.executeWriteTransaction(conn -> {
            String sql = "INSERT INTO Simulation_Sessions (session_code, session_type, strategy_id, schedule_id, total_requests, status) "
                    + "VALUES (?, ?, ?, ?, ?, 'RUNNING')";
            try (PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
                ps.setString(1, sessionCode);
                ps.setString(2, sessionType);
                ps.setInt(3, strategyId);
                ps.setLong(4, scheduleId);
                ps.setInt(5, totalRequests);
                ps.executeUpdate();
                try (ResultSet rs = ps.getGeneratedKeys()) {
                    if (rs.next()) {
                        return rs.getLong(1);
                    }
                    throw new SQLException("Failed to create Simulation_Sessions row.");
                }
            }
        });
    }

    public void saveSimulationResult(SimulationResult r) {
        connectionManager.executeWriteTransaction(conn -> {
            String updateSession = "UPDATE Simulation_Sessions SET ended_at = datetime('now', 'localtime'), status = 'COMPLETED' WHERE session_id = ?";
            try (PreparedStatement psS = conn.prepareStatement(updateSession)) {
                psS.setLong(1, r.getSessionId());
                psS.executeUpdate();
            }

            String sql = "INSERT OR REPLACE INTO Simulation_Results (session_id, total_requests, confirmed_count, rac_count, "
                    + "wl_count, failed_count, duplicate_attempts, total_time_ms, avg_time_ms, max_time_ms, "
                    + "success_rate_pct, seat_utilization_pct, inventory_consistent) "
                    + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setLong(1, r.getSessionId());
                ps.setInt(2, r.getTotalRequests());
                ps.setInt(3, r.getConfirmedCount());
                ps.setInt(4, r.getRacCount());
                ps.setInt(5, r.getWlCount());
                ps.setInt(6, r.getFailedCount());
                ps.setInt(7, r.getDuplicateAttempts());
                ps.setLong(8, r.getTotalTimeMs());
                ps.setDouble(9, r.getAvgTimeMs());
                ps.setLong(10, r.getMaxTimeMs());
                ps.setDouble(11, r.getSuccessRatePct());
                ps.setDouble(12, r.getSeatUtilizationPct());
                ps.setInt(13, r.isInventoryConsistent() ? 1 : 0);
                ps.executeUpdate();
            }
            return null;
        });
    }

    public List<SimulationResult> findAllSimulationResults() {
        List<SimulationResult> list = new ArrayList<>();
        String sql = "SELECT sr.result_id, sr.session_id, ss.session_type, ss.session_code, sr.total_requests, "
                + "sr.confirmed_count, sr.rac_count, sr.wl_count, sr.failed_count, sr.duplicate_attempts, "
                + "sr.total_time_ms, sr.avg_time_ms, sr.max_time_ms, sr.success_rate_pct, "
                + "sr.seat_utilization_pct, sr.inventory_consistent, sr.recorded_at "
                + "FROM Simulation_Results sr JOIN Simulation_Sessions ss ON sr.session_id = ss.session_id "
                + "ORDER BY sr.result_id DESC";
        try (Connection conn = connectionManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                SimulationResult r = new SimulationResult();
                r.setResultId(rs.getLong("result_id"));
                r.setSessionId(rs.getLong("session_id"));
                r.setUniverseName(rs.getString("session_type") + " (" + rs.getString("session_code") + ")");
                r.setTotalRequests(rs.getInt("total_requests"));
                r.setConfirmedCount(rs.getInt("confirmed_count"));
                r.setRacCount(rs.getInt("rac_count"));
                r.setWlCount(rs.getInt("wl_count"));
                r.setFailedCount(rs.getInt("failed_count"));
                r.setDuplicateAttempts(rs.getInt("duplicate_attempts"));
                r.setTotalTimeMs(rs.getLong("total_time_ms"));
                r.setAvgTimeMs(rs.getDouble("avg_time_ms"));
                r.setMaxTimeMs(rs.getLong("max_time_ms"));
                r.setSuccessRatePct(rs.getDouble("success_rate_pct"));
                r.setSeatUtilizationPct(rs.getDouble("seat_utilization_pct"));
                r.setInventoryConsistent(rs.getInt("inventory_consistent") == 1);
                r.setRecordedAt(rs.getString("recorded_at"));
                list.add(r);
            }
            return list;
        } catch (SQLException ex) {
            throw new DatabaseOperationException("Error querying Simulation_Results", ex);
        }
    }

    public void insertNotification(Notification n) {
        connectionManager.executeWriteTransaction(conn -> {
            String sql = "INSERT INTO Notifications (session_id, passenger_id, booking_id, title, message, channel, is_read) "
                    + "VALUES (?, ?, ?, ?, ?, ?, ?)";
            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setLong(1, n.getSessionId());
                ps.setInt(2, n.getPassengerId());
                ps.setString(3, n.getBookingId());
                ps.setString(4, n.getTitle());
                ps.setString(5, n.getMessage());
                ps.setString(6, n.getChannel());
                ps.setInt(7, n.isRead() ? 1 : 0);
                ps.executeUpdate();
            }
            return null;
        });
    }

    public List<Notification> findNotificationsByPassenger(int passengerId) {
        List<Notification> list = new ArrayList<>();
        String sql = "SELECT notification_id, session_id, passenger_id, booking_id, title, message, channel, is_read, created_at "
                + "FROM Notifications WHERE passenger_id = ? ORDER BY notification_id DESC";
        try (Connection conn = connectionManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, passengerId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(new Notification(
                            rs.getLong("notification_id"),
                            rs.getLong("session_id"),
                            rs.getInt("passenger_id"),
                            rs.getString("booking_id"),
                            rs.getString("title"),
                            rs.getString("message"),
                            rs.getString("channel"),
                            rs.getInt("is_read") == 1,
                            rs.getString("created_at")));
                }
            }
            return list;
        } catch (SQLException ex) {
            throw new DatabaseOperationException("Error querying Notifications", ex);
        }
    }
}
