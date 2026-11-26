package com.tatkalx.dao;

import com.tatkalx.database.DatabaseConnectionManager;
import com.tatkalx.exception.DatabaseOperationException;
import com.tatkalx.model.Berth;
import com.tatkalx.model.Coach;
import com.tatkalx.model.SeatInventory;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.List;

/**
 * JDBC DAO for session-isolated Seat_Inventory records.
 */
public class SeatInventoryDao {

    private final DatabaseConnectionManager connectionManager;

    public SeatInventoryDao(DatabaseConnectionManager connectionManager) {
        this.connectionManager = connectionManager;
    }

    public long createSessionInventory(long sessionId, long scheduleId, Coach coach) {
        return connectionManager.executeWriteTransaction(conn -> {
            String sql = "INSERT INTO Seat_Inventory (session_id, schedule_id, coach_id, total_tatkal_seats, "
                    + "available_tatkal_seats, confirmed_count, rac_capacity, rac_allocated, wl_capacity, wl_allocated) "
                    + "VALUES (?, ?, ?, ?, ?, 0, ?, 0, ?, 0)";
            try (PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
                ps.setLong(1, sessionId);
                ps.setLong(2, scheduleId);
                ps.setInt(3, coach.getCoachId());
                ps.setInt(4, coach.getTatkalQuotaBerths());
                ps.setInt(5, coach.getTatkalQuotaBerths());
                ps.setInt(6, coach.getRacQuota());
                ps.setInt(7, coach.getWlQuota());
                ps.executeUpdate();
                try (ResultSet rs = ps.getGeneratedKeys()) {
                    if (rs.next()) {
                        return rs.getLong(1);
                    }
                    throw new SQLException("Failed to create Seat_Inventory row.");
                }
            }
        });
    }

    public SeatInventory findInventory(long sessionId, long scheduleId, Coach coach, List<Berth> coachBerths) {
        String sql = "SELECT inventory_id, total_tatkal_seats, available_tatkal_seats, confirmed_count, "
                + "rac_capacity, rac_allocated, wl_capacity, wl_allocated, version_num "
                + "FROM Seat_Inventory WHERE session_id = ? AND schedule_id = ? AND coach_id = ?";
        try (Connection conn = connectionManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, sessionId);
            ps.setLong(2, scheduleId);
            ps.setInt(3, coach.getCoachId());
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) {
                    return null;
                }
                SeatInventory inv = new SeatInventory(sessionId, scheduleId, coach, coachBerths);
                inv.setInventoryId(rs.getLong("inventory_id"));
                inv.setTotalTatkalSeats(rs.getInt("total_tatkal_seats"));
                inv.setAvailableTatkalSeats(rs.getInt("available_tatkal_seats"));
                inv.setConfirmedCount(rs.getInt("confirmed_count"));
                inv.setRacCapacity(rs.getInt("rac_capacity"));
                inv.setRacAllocated(rs.getInt("rac_allocated"));
                inv.setWlCapacity(rs.getInt("wl_capacity"));
                inv.setWlAllocated(rs.getInt("wl_allocated"));

                // Sync physical berth pool with already-booked berths in Ticket_Passengers
                String occSql = "SELECT tp.berth_id FROM Ticket_Passengers tp "
                        + "JOIN Tickets t ON tp.ticket_id = t.ticket_id "
                        + "WHERE t.session_id = ? AND t.schedule_id = ? AND t.coach_id = ? "
                        + "AND tp.allocation_status = 'CONFIRMED' AND tp.berth_id IS NOT NULL";
                try (PreparedStatement occPs = conn.prepareStatement(occSql)) {
                    occPs.setLong(1, sessionId);
                    occPs.setLong(2, scheduleId);
                    occPs.setInt(3, coach.getCoachId());
                    try (ResultSet occRs = occPs.executeQuery()) {
                        java.util.Set<Integer> occupiedIds = new java.util.HashSet<>();
                        while (occRs.next()) {
                            occupiedIds.add(occRs.getInt("berth_id"));
                        }
                        if (!occupiedIds.isEmpty()) {
                            java.util.Iterator<Berth> it = inv.getAvailableBerthPool().iterator();
                            while (it.hasNext()) {
                                Berth b = it.next();
                                if (occupiedIds.contains(b.getBerthId())) {
                                    it.remove();
                                    inv.getOccupiedBerthIds().add(b.getBerthId());
                                }
                            }
                        }
                    }
                }
                return inv;
            }
        } catch (SQLException ex) {
            throw new DatabaseOperationException("Failed to fetch Seat_Inventory", ex);
        }
    }

    public void syncInventorySnapshot(Connection conn, SeatInventory inv) throws SQLException {
        String sql = "UPDATE Seat_Inventory SET available_tatkal_seats = ?, confirmed_count = ?, "
                + "rac_allocated = ?, wl_allocated = ?, version_num = version_num + 1 "
                + "WHERE session_id = ? AND schedule_id = ? AND coach_id = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, inv.getAvailableTatkalSeats());
            ps.setInt(2, inv.getConfirmedCount());
            ps.setInt(3, inv.getRacAllocated());
            ps.setInt(4, inv.getWlAllocated());
            ps.setLong(5, inv.getSessionId());
            ps.setLong(6, inv.getScheduleId());
            ps.setInt(7, inv.getCoachId());
            ps.executeUpdate();
        }
    }
}
