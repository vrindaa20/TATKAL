package com.tatkalx.dao;

import com.tatkalx.database.DatabaseConnectionManager;
import com.tatkalx.exception.DatabaseOperationException;
import com.tatkalx.model.Berth;
import com.tatkalx.model.BookingEvent;
import com.tatkalx.model.BookingRequest;
import com.tatkalx.model.Passenger;
import com.tatkalx.model.Payment;
import com.tatkalx.model.PnrRecord;
import com.tatkalx.model.SeatInventory;
import com.tatkalx.model.Ticket;
import com.tatkalx.service.BookingOutcome;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Types;
import java.util.ArrayList;
import java.util.List;

/**
 * JDBC DAO for Booking_Requests, Tickets, Ticket_Passengers, PNR_Records,
 * RAC_Queue, and Waiting_List inside atomic SQLite transactions.
 */
public class BookingDao {

    private final DatabaseConnectionManager connectionManager;
    private final SeatInventoryDao seatInventoryDao;

    public BookingDao(DatabaseConnectionManager connectionManager) {
        this.connectionManager = connectionManager;
        this.seatInventoryDao = new SeatInventoryDao(connectionManager);
    }

    /**
     * Atomically persists a complete BookingOutcome (Request, Inventory sync, Ticket,
     * Ticket_Passengers, PNR, RAC/WL queue entry, Payment, Transaction, and Booking_Events).
     */
    public void persistBookingOutcomeAtomically(BookingOutcome outcome,
                                                Passenger passenger,
                                                SeatInventory inventory,
                                                List<Berth> assignedBerths) {
        connectionManager.executeWriteTransaction(conn -> {
            BookingRequest req = outcome.getRequest();
            upsertBookingRequest(conn, req);

            if (inventory != null) {
                seatInventoryDao.syncInventorySnapshot(conn, inventory);
            }

            if (outcome.isSuccess() && outcome.getTicket() != null) {
                Ticket t = outcome.getTicket();
                long dbTicketId = insertTicket(conn, t);
                t.setTicketId(dbTicketId);

                Berth firstBerth = (assignedBerths != null && !assignedBerths.isEmpty()) ? assignedBerths.get(0) : null;
                insertTicketPassenger(conn, dbTicketId, passenger, firstBerth, t.getSeatLabel(), t.getTicketStatus());

                if (outcome.getPnrRecord() != null) {
                    outcome.getPnrRecord().setTicketId(dbTicketId);
                    insertPnrRecord(conn, outcome.getPnrRecord());
                }

                if ("RAC".equals(t.getTicketStatus())) {
                    insertRacEntry(conn, t);
                } else if ("WL".equals(t.getTicketStatus())) {
                    insertWlEntry(conn, t);
                }

                if (outcome.getPayment() != null) {
                    Payment p = outcome.getPayment();
                    long dbPayId = insertPayment(conn, p);
                    p.setPaymentId(dbPayId);
                    insertDebitTransaction(conn, t.getSessionId(), passenger.getUserId(), dbPayId, p.getAmount(), passenger.getWalletBalance());
                }
            }

            insertBookingEvents(conn, outcome.getEvents());
            return null;
        });
    }

    private void upsertBookingRequest(Connection conn, BookingRequest req) throws SQLException {
        String sql = "INSERT OR REPLACE INTO Booking_Requests "
                + "(booking_id, session_id, passenger_id, schedule_id, coach_class, requested_seats, "
                + "priority_score, request_timestamp, queue_entered_at, queue_position, thread_name, "
                + "processing_time_ms, status, failure_reason) "
                + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, req.getBookingId());
            ps.setLong(2, req.getSessionId());
            ps.setInt(3, req.getPassengerId());
            ps.setLong(4, req.getScheduleId());
            ps.setString(5, req.getCoachClass());
            ps.setInt(6, req.getRequestedSeats());
            ps.setInt(7, req.getPriorityScore());
            ps.setString(8, req.getRequestTimestamp());
            ps.setString(9, req.getQueueEnteredAt());
            ps.setInt(10, req.getQueuePosition());
            ps.setString(11, req.getThreadName());
            ps.setLong(12, req.getProcessingTimeMs());
            ps.setString(13, req.getStatus());
            ps.setString(14, req.getFailureReason());
            ps.executeUpdate();
        }
    }

    private long insertTicket(Connection conn, Ticket t) throws SQLException {
        String sql = "INSERT INTO Tickets (booking_id, session_id, passenger_id, schedule_id, coach_id, ticket_status, total_fare, booked_at) "
                + "VALUES (?, ?, ?, ?, ?, ?, ?, ?)";
        try (PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, t.getBookingId());
            ps.setLong(2, t.getSessionId());
            ps.setInt(3, t.getPassengerId());
            ps.setLong(4, t.getScheduleId());
            ps.setInt(5, t.getCoachId());
            ps.setString(6, t.getTicketStatus());
            ps.setDouble(7, t.getTotalFare());
            ps.setString(8, t.getBookedAt());
            ps.executeUpdate();
            try (ResultSet rs = ps.getGeneratedKeys()) {
                if (rs.next()) {
                    return rs.getLong(1);
                }
                throw new SQLException("Failed to get generated ticket_id.");
            }
        }
    }

    private void insertTicketPassenger(Connection conn, long ticketId, Passenger p,
                                       Berth berth, String seatLabel, String status) throws SQLException {
        String sql = "INSERT INTO Ticket_Passengers (ticket_id, traveler_name, traveler_age, traveler_gender, "
                + "berth_id, assigned_seat_label, allocation_status) VALUES (?, ?, ?, ?, ?, ?, ?)";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, ticketId);
            ps.setString(2, p.getFullName());
            ps.setInt(3, p.getAge());
            ps.setString(4, p.getGender());
            if (berth == null) {
                ps.setNull(5, Types.INTEGER);
            } else {
                ps.setInt(5, berth.getBerthId());
            }
            ps.setString(6, seatLabel);
            ps.setString(7, status);
            ps.executeUpdate();
        }
    }

    private void insertPnrRecord(Connection conn, PnrRecord pnr) throws SQLException {
        String sql = "INSERT INTO PNR_Records (pnr_number, ticket_id, session_id, current_status, chart_prepared, generated_at) "
                + "VALUES (?, ?, ?, ?, ?, ?)";
        try (PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, pnr.getPnrNumber());
            ps.setLong(2, pnr.getTicketId());
            ps.setLong(3, pnr.getSessionId());
            ps.setString(4, pnr.getCurrentStatus());
            ps.setInt(5, pnr.isChartPrepared() ? 1 : 0);
            ps.setString(6, pnr.getGeneratedAt());
            ps.executeUpdate();
            try (ResultSet rs = ps.getGeneratedKeys()) {
                if (rs.next()) {
                    pnr.setPnrId(rs.getLong(1));
                }
            }
        }
    }

    private void insertRacEntry(Connection conn, Ticket t) throws SQLException {
        String sql = "INSERT INTO RAC_Queue (session_id, schedule_id, coach_id, ticket_id, rac_number, queue_status) "
                + "VALUES (?, ?, ?, ?, ?, 'ACTIVE')";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, t.getSessionId());
            ps.setLong(2, t.getScheduleId());
            ps.setInt(3, t.getCoachId());
            ps.setLong(4, t.getTicketId());
            ps.setInt(5, Math.max(1, t.getQueuePosition()));
            ps.executeUpdate();
        }
    }

    private void insertWlEntry(Connection conn, Ticket t) throws SQLException {
        String sql = "INSERT INTO Waiting_List (session_id, schedule_id, coach_id, ticket_id, wl_number, queue_status) "
                + "VALUES (?, ?, ?, ?, ?, 'ACTIVE')";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, t.getSessionId());
            ps.setLong(2, t.getScheduleId());
            ps.setInt(3, t.getCoachId());
            ps.setLong(4, t.getTicketId());
            ps.setInt(5, Math.max(1, t.getQueuePosition()));
            ps.executeUpdate();
        }
    }

    private long insertPayment(Connection conn, Payment p) throws SQLException {
        String sql = "INSERT INTO Payments (booking_id, session_id, passenger_id, amount, payment_method, "
                + "gateway_ref, payment_status, latency_ms, initiated_at, completed_at) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
        try (PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, p.getBookingId());
            ps.setLong(2, p.getSessionId());
            ps.setInt(3, p.getPassengerId());
            ps.setDouble(4, p.getAmount());
            ps.setString(5, p.getPaymentMethod());
            ps.setString(6, p.getGatewayRef());
            ps.setString(7, p.getPaymentStatus());
            ps.setLong(8, p.getLatencyMs());
            ps.setString(9, p.getInitiatedAt());
            ps.setString(10, p.getCompletedAt());
            ps.executeUpdate();
            try (ResultSet rs = ps.getGeneratedKeys()) {
                if (rs.next()) {
                    return rs.getLong(1);
                }
                throw new SQLException("Failed to get payment_id.");
            }
        }
    }

    private void insertDebitTransaction(Connection conn, long sessionId, int userId,
                                        long paymentId, double amount, double balanceAfter) throws SQLException {
        String sql = "INSERT INTO Transactions (session_id, user_id, payment_id, refund_id, txn_type, amount, balance_after, txn_status) "
                + "VALUES (?, ?, ?, NULL, 'DEBIT_BOOKING', ?, ?, 'COMMITTED')";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, sessionId);
            ps.setInt(2, userId);
            ps.setLong(3, paymentId);
            ps.setDouble(4, amount);
            ps.setDouble(5, balanceAfter);
            ps.executeUpdate();
        }
    }

    private void insertBookingEvents(Connection conn, List<BookingEvent> events) throws SQLException {
        if (events == null || events.isEmpty()) {
            return;
        }
        String sql = "INSERT OR IGNORE INTO Booking_Events (booking_id, session_id, passenger_id, thread_name, "
                + "event_seq, event_type, event_description, request_status, available_seats_snapshot, "
                + "queue_position_snapshot, allocation_result, payment_result, event_timestamp) "
                + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            for (BookingEvent ev : events) {
                ps.setString(1, ev.getBookingId());
                ps.setLong(2, ev.getSessionId());
                ps.setInt(3, ev.getPassengerId());
                ps.setString(4, ev.getThreadName());
                ps.setInt(5, ev.getEventSeq());
                ps.setString(6, ev.getEventType());
                ps.setString(7, ev.getEventDescription());
                ps.setString(8, ev.getRequestStatus());
                if (ev.getAvailableSeatsSnapshot() == null) {
                    ps.setNull(9, Types.INTEGER);
                } else {
                    ps.setInt(9, ev.getAvailableSeatsSnapshot());
                }
                if (ev.getQueuePositionSnapshot() == null) {
                    ps.setNull(10, Types.INTEGER);
                } else {
                    ps.setInt(10, ev.getQueuePositionSnapshot());
                }
                ps.setString(11, ev.getAllocationResult());
                ps.setString(12, ev.getPaymentResult());
                ps.setString(13, ev.getEventTimestamp());
                ps.addBatch();
            }
            ps.executeBatch();
        }
    }

    public PnrRecord findPnrByNumber(String pnrNumber) {
        String sql = "SELECT pnr_id, pnr_number, ticket_id, session_id, current_status, chart_prepared, generated_at "
                + "FROM PNR_Records WHERE pnr_number = ?";
        try (Connection conn = connectionManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, pnrNumber);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) {
                    return null;
                }
                return new PnrRecord(
                        rs.getLong("pnr_id"),
                        rs.getString("pnr_number"),
                        rs.getLong("ticket_id"),
                        rs.getLong("session_id"),
                        rs.getString("current_status"),
                        rs.getInt("chart_prepared") == 1,
                        rs.getString("generated_at"));
            }
        } catch (SQLException ex) {
            throw new DatabaseOperationException("PNR lookup failed for " + pnrNumber, ex);
        }
    }

    public List<Ticket> findTicketsByPassenger(int passengerId) {
        List<Ticket> tickets = new ArrayList<>();
        String sql = "SELECT t.ticket_id, t.booking_id, t.session_id, t.passenger_id, t.schedule_id, t.coach_id, "
                + "c.coach_code, t.ticket_status, t.total_fare, t.booked_at, "
                + "COALESCE((SELECT assigned_seat_label FROM Ticket_Passengers tp WHERE tp.ticket_id = t.ticket_id LIMIT 1), 'N/A') AS seat_label "
                + "FROM Tickets t JOIN Coaches c ON t.coach_id = c.coach_id "
                + "WHERE t.passenger_id = ? ORDER BY t.ticket_id DESC";
        try (Connection conn = connectionManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, passengerId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    tickets.add(new Ticket(
                            rs.getLong("ticket_id"),
                            rs.getString("booking_id"),
                            rs.getLong("session_id"),
                            rs.getInt("passenger_id"),
                            rs.getLong("schedule_id"),
                            rs.getInt("coach_id"),
                            rs.getString("coach_code"),
                            rs.getString("ticket_status"),
                            0,
                            rs.getString("seat_label"),
                            rs.getDouble("total_fare"),
                            rs.getString("booked_at")));
                }
            }
            return tickets;
        } catch (SQLException ex) {
            throw new DatabaseOperationException("Error querying passenger tickets", ex);
        }
    }

    public List<BookingRequest> findRequestsBySession(long sessionId) {
        List<BookingRequest> list = new ArrayList<>();
        String sql = "SELECT br.booking_id, br.session_id, br.passenger_id, u.full_name, p.age, p.priority_category, "
                + "br.schedule_id, br.coach_class, br.requested_seats, br.priority_score, br.request_timestamp, "
                + "br.queue_position, br.thread_name, br.processing_time_ms, br.status, br.failure_reason "
                + "FROM Booking_Requests br "
                + "JOIN Passengers p ON br.passenger_id = p.passenger_id "
                + "JOIN Users u ON p.user_id = u.user_id "
                + "WHERE br.session_id = ? ORDER BY br.request_timestamp";
        try (Connection conn = connectionManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, sessionId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    BookingRequest req = new BookingRequest(
                            rs.getString("booking_id"),
                            rs.getLong("session_id"),
                            rs.getInt("passenger_id"),
                            rs.getString("full_name"),
                            rs.getInt("age"),
                            rs.getString("priority_category"),
                            rs.getLong("schedule_id"),
                            rs.getString("coach_class"),
                            rs.getInt("requested_seats"),
                            rs.getString("request_timestamp"));
                    req.setPriorityScore(rs.getInt("priority_score"));
                    req.setQueuePosition(rs.getInt("queue_position"));
                    req.setThreadName(rs.getString("thread_name"));
                    req.setProcessingTimeMs(rs.getLong("processing_time_ms"));
                    req.setStatus(rs.getString("status"));
                    req.setFailureReason(rs.getString("failure_reason"));
                    list.add(req);
                }
            }
            return list;
        } catch (SQLException ex) {
            throw new DatabaseOperationException("Error querying booking requests", ex);
        }
    }

    public List<Ticket> findTicketsBySession(long sessionId) {
        List<Ticket> tickets = new ArrayList<>();
        String sql = "SELECT t.ticket_id, t.booking_id, t.session_id, t.passenger_id, t.schedule_id, t.coach_id, "
                + "c.coach_code, t.ticket_status, t.total_fare, t.booked_at, "
                + "COALESCE((SELECT assigned_seat_label FROM Ticket_Passengers tp WHERE tp.ticket_id = t.ticket_id LIMIT 1), 'N/A') AS seat_label "
                + "FROM Tickets t JOIN Coaches c ON t.coach_id = c.coach_id "
                + "WHERE t.session_id = ? ORDER BY t.ticket_id ASC";
        try (Connection conn = connectionManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, sessionId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    tickets.add(new Ticket(
                            rs.getLong("ticket_id"),
                            rs.getString("booking_id"),
                            rs.getLong("session_id"),
                            rs.getInt("passenger_id"),
                            rs.getLong("schedule_id"),
                            rs.getInt("coach_id"),
                            rs.getString("coach_code"),
                            rs.getString("ticket_status"),
                            0,
                            rs.getString("seat_label"),
                            rs.getDouble("total_fare"),
                            rs.getString("booked_at")));
                }
            }
            return tickets;
        } catch (SQLException ex) {
            throw new DatabaseOperationException("Error querying session tickets", ex);
        }
    }

    public List<PnrRecord> findPnrsBySession(long sessionId) {
        List<PnrRecord> list = new ArrayList<>();
        String sql = "SELECT pnr_id, pnr_number, ticket_id, session_id, current_status, chart_prepared, generated_at "
                + "FROM PNR_Records WHERE session_id = ? ORDER BY pnr_id";
        try (Connection conn = connectionManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, sessionId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(new PnrRecord(
                            rs.getLong("pnr_id"),
                            rs.getString("pnr_number"),
                            rs.getLong("ticket_id"),
                            rs.getLong("session_id"),
                            rs.getString("current_status"),
                            rs.getInt("chart_prepared") == 1,
                            rs.getString("generated_at")));
                }
            }
            return list;
        } catch (SQLException ex) {
            throw new DatabaseOperationException("Error querying PNR_Records", ex);
        }
    }
}
