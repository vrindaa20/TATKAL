package com.tatkalx.dao;

import com.tatkalx.database.DatabaseConnectionManager;
import com.tatkalx.exception.DatabaseOperationException;
import com.tatkalx.model.BookingEvent;
import com.tatkalx.model.ThreadLog;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.util.ArrayList;
import java.util.List;

/**
 * JDBC DAO for Forensic Investigation tables: Booking_Events, Thread_Logs,
 * Failure_Records, and Audit_Logs.
 */
public class ForensicDao {

    private final DatabaseConnectionManager connectionManager;

    public ForensicDao(DatabaseConnectionManager connectionManager) {
        this.connectionManager = connectionManager;
    }

    public List<BookingEvent> findEventsByBookingId(String bookingId) {
        List<BookingEvent> events = new ArrayList<>();
        String sql = "SELECT event_id, booking_id, session_id, passenger_id, thread_name, event_seq, "
                + "event_type, event_description, request_status, available_seats_snapshot, "
                + "queue_position_snapshot, allocation_result, payment_result, event_timestamp "
                + "FROM Booking_Events WHERE booking_id = ? ORDER BY event_seq ASC";
        try (Connection conn = connectionManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, bookingId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Integer avail = rs.getObject("available_seats_snapshot") == null ? null : rs.getInt("available_seats_snapshot");
                    Integer qpos = rs.getObject("queue_position_snapshot") == null ? null : rs.getInt("queue_position_snapshot");
                    BookingEvent ev = new BookingEvent(
                            rs.getString("booking_id"),
                            rs.getLong("session_id"),
                            rs.getInt("passenger_id"),
                            rs.getString("thread_name"),
                            rs.getInt("event_seq"),
                            rs.getString("event_type"),
                            rs.getString("event_description"),
                            rs.getString("request_status"),
                            avail,
                            qpos,
                            rs.getString("allocation_result"),
                            rs.getString("payment_result"),
                            rs.getString("event_timestamp"));
                    ev.setEventId(rs.getLong("event_id"));
                    events.add(ev);
                }
            }
            return events;
        } catch (SQLException ex) {
            throw new DatabaseOperationException("Failed to reconstruct booking events for " + bookingId, ex);
        }
    }

    public void insertThreadLogsBatch(List<ThreadLog> logs) {
        if (logs == null || logs.isEmpty()) {
            return;
        }
        connectionManager.executeWriteTransaction(conn -> {
            String sql = "INSERT INTO Thread_Logs (session_id, booking_id, thread_id_num, thread_name, "
                    + "thread_priority, thread_state, lock_target, action_detail, logged_at) "
                    + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)";
            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                for (ThreadLog log : logs) {
                    ps.setLong(1, log.getSessionId());
                    if (log.getBookingId() == null) {
                        ps.setNull(2, Types.VARCHAR);
                    } else {
                        ps.setString(2, log.getBookingId());
                    }
                    ps.setLong(3, log.getThreadIdNum());
                    ps.setString(4, log.getThreadName());
                    ps.setInt(5, log.getThreadPriority());
                    ps.setString(6, log.getThreadState());
                    ps.setString(7, log.getLockTarget());
                    ps.setString(8, log.getActionDetail());
                    ps.setString(9, log.getLoggedAt());
                    ps.addBatch();
                }
                ps.executeBatch();
            }
            return null;
        });
    }

    public void insertFailureRecord(long sessionId, String bookingId, String scenarioCode,
                                    String threadName, String errorClass, String errorMessage,
                                    String recoveryAction, boolean inventoryVerified) {
        connectionManager.executeWriteTransaction(conn -> {
            String sql = "INSERT INTO Failure_Records (session_id, booking_id, scenario_code, thread_name, "
                    + "error_class, error_message, recovery_action, inventory_verified) VALUES (?, ?, ?, ?, ?, ?, ?, ?)";
            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setLong(1, sessionId);
                ps.setString(2, bookingId);
                ps.setString(3, scenarioCode);
                ps.setString(4, threadName);
                ps.setString(5, errorClass);
                ps.setString(6, errorMessage);
                ps.setString(7, recoveryAction);
                ps.setInt(8, inventoryVerified ? 1 : 0);
                ps.executeUpdate();
            }
            return null;
        });
    }

    public void insertAuditLog(Integer userId, Long sessionId, String moduleName,
                               String actionName, String targetEntity, String targetId, String details) {
        connectionManager.executeWriteTransaction(conn -> {
            String sql = "INSERT INTO Audit_Logs (user_id, session_id, module_name, action_name, target_entity, target_id, details) "
                    + "VALUES (?, ?, ?, ?, ?, ?, ?)";
            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                if (userId == null) {
                    ps.setNull(1, Types.INTEGER);
                } else {
                    ps.setInt(1, userId);
                }
                if (sessionId == null) {
                    ps.setNull(2, Types.INTEGER);
                } else {
                    ps.setLong(2, sessionId);
                }
                ps.setString(3, moduleName);
                ps.setString(4, actionName);
                ps.setString(5, targetEntity);
                ps.setString(6, targetId);
                ps.setString(7, details);
                ps.executeUpdate();
            }
            return null;
        });
    }

    public List<String> findRecentAuditLogs(int limit) {
        List<String> logs = new ArrayList<>();
        String sql = "SELECT audit_id, module_name, action_name, target_entity, target_id, details, logged_at "
                + "FROM Audit_Logs ORDER BY audit_id DESC LIMIT ?";
        try (Connection conn = connectionManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, limit);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    logs.add(String.format("#%d [%s] %s/%s -> %s(%s): %s",
                            rs.getLong("audit_id"),
                            rs.getString("logged_at"),
                            rs.getString("module_name"),
                            rs.getString("action_name"),
                            rs.getString("target_entity"),
                            rs.getString("target_id"),
                            rs.getString("details")));
                }
            }
            return logs;
        } catch (SQLException ex) {
            throw new DatabaseOperationException("Error reading Audit_Logs", ex);
        }
    }
}
