package com.tatkalx.dao;

import com.tatkalx.database.DatabaseConnectionManager;
import com.tatkalx.exception.DatabaseOperationException;
import com.tatkalx.model.Cancellation;
import com.tatkalx.model.Payment;
import com.tatkalx.model.Refund;
import com.tatkalx.model.SeatInventory;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

/**
 * JDBC DAO for Payments, Cancellations, Refunds, and financial Transactions.
 */
public class FinancialDao {

    private final DatabaseConnectionManager connectionManager;
    private final SeatInventoryDao seatInventoryDao;

    public FinancialDao(DatabaseConnectionManager connectionManager) {
        this.connectionManager = connectionManager;
        this.seatInventoryDao = new SeatInventoryDao(connectionManager);
    }

    public void persistCancellationAndRefundAtomically(Cancellation cancellation,
                                                       Refund refund,
                                                       long ticketId,
                                                       String pnrNumber,
                                                       SeatInventory updatedInventory,
                                                       int userId,
                                                       double walletBalanceAfter) {
        connectionManager.executeWriteTransaction(conn -> {
            // 1. Update Ticket, Ticket_Passengers, and PNR_Records
            try (PreparedStatement psT = conn.prepareStatement(
                    "UPDATE Tickets SET ticket_status = 'CANCELLED', updated_at = datetime('now', 'localtime') WHERE ticket_id = ?")) {
                psT.setLong(1, ticketId);
                psT.executeUpdate();
            }
            try (PreparedStatement psTp = conn.prepareStatement(
                    "UPDATE Ticket_Passengers SET allocation_status = 'CANCELLED', berth_id = NULL WHERE ticket_id = ?")) {
                psTp.setLong(1, ticketId);
                psTp.executeUpdate();
            }
            if (pnrNumber != null) {
                try (PreparedStatement psPnr = conn.prepareStatement(
                        "UPDATE PNR_Records SET current_status = 'CANCELLED' WHERE pnr_number = ?")) {
                    psPnr.setString(1, pnrNumber);
                    psPnr.executeUpdate();
                }
            }

            if (updatedInventory != null) {
                seatInventoryDao.syncInventorySnapshot(conn, updatedInventory);
            }

            // 2. Insert Cancellation
            long dbCancelId;
            String cancelSql = "INSERT INTO Cancellations (ticket_id, session_id, cancelled_by_user_id, previous_status, cancellation_reason, cancelled_at) "
                    + "VALUES (?, ?, ?, ?, ?, ?)";
            try (PreparedStatement psC = conn.prepareStatement(cancelSql, Statement.RETURN_GENERATED_KEYS)) {
                psC.setLong(1, ticketId);
                psC.setLong(2, cancellation.getSessionId());
                psC.setInt(3, userId);
                psC.setString(4, cancellation.getPreviousStatus());
                psC.setString(5, cancellation.getCancellationReason());
                psC.setString(6, cancellation.getCancelledAt());
                psC.executeUpdate();
                try (ResultSet rs = psC.getGeneratedKeys()) {
                    if (!rs.next()) {
                        throw new SQLException("Failed to get cancellation_id.");
                    }
                    dbCancelId = rs.getLong(1);
                    cancellation.setCancellationId(dbCancelId);
                }
            }

            // 3. Insert Refund if paymentId exists
            if (refund != null && refund.getPaymentId() > 0) {
                long dbRefundId;
                String refSql = "INSERT INTO Refunds (cancellation_id, payment_id, original_amount, deduction_amount, refund_amount, refund_status, processed_at) "
                        + "VALUES (?, ?, ?, ?, ?, ?, ?)";
                try (PreparedStatement psR = conn.prepareStatement(refSql, Statement.RETURN_GENERATED_KEYS)) {
                    psR.setLong(1, dbCancelId);
                    psR.setLong(2, refund.getPaymentId());
                    psR.setDouble(3, refund.getOriginalAmount());
                    psR.setDouble(4, refund.getDeductionAmount());
                    psR.setDouble(5, refund.getRefundAmount());
                    psR.setString(6, refund.getRefundStatus());
                    psR.setString(7, refund.getProcessedAt());
                    psR.executeUpdate();
                    try (ResultSet rs = psR.getGeneratedKeys()) {
                        if (rs.next()) {
                            dbRefundId = rs.getLong(1);
                            refund.setRefundId(dbRefundId);
                            if (refund.getRefundAmount() > 0) {
                                try (PreparedStatement psTx = conn.prepareStatement(
                                        "INSERT INTO Transactions (session_id, user_id, payment_id, refund_id, txn_type, amount, balance_after, txn_status) "
                                                + "VALUES (?, ?, ?, ?, 'CREDIT_REFUND', ?, ?, 'COMMITTED')")) {
                                    psTx.setLong(1, cancellation.getSessionId());
                                    psTx.setInt(2, userId);
                                    psTx.setLong(3, refund.getPaymentId());
                                    psTx.setLong(4, dbRefundId);
                                    psTx.setDouble(5, refund.getRefundAmount());
                                    psTx.setDouble(6, walletBalanceAfter);
                                    psTx.executeUpdate();
                                }
                            }
                        }
                    }
                }
            }
            return null;
        });
    }

    public List<Payment> findAllPayments() {
        List<Payment> list = new ArrayList<>();
        String sql = "SELECT payment_id, booking_id, session_id, passenger_id, amount, payment_method, "
                + "gateway_ref, payment_status, latency_ms, initiated_at, completed_at FROM Payments ORDER BY payment_id DESC";
        try (Connection conn = connectionManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                list.add(new Payment(
                        rs.getLong("payment_id"),
                        rs.getString("booking_id"),
                        rs.getLong("session_id"),
                        rs.getInt("passenger_id"),
                        rs.getDouble("amount"),
                        rs.getString("payment_method"),
                        rs.getString("gateway_ref"),
                        rs.getString("payment_status"),
                        rs.getLong("latency_ms"),
                        rs.getString("initiated_at"),
                        rs.getString("completed_at")));
            }
            return list;
        } catch (SQLException ex) {
            throw new DatabaseOperationException("Error querying Payments", ex);
        }
    }

    public List<Refund> findAllRefunds() {
        List<Refund> list = new ArrayList<>();
        String sql = "SELECT refund_id, cancellation_id, payment_id, original_amount, deduction_amount, "
                + "refund_amount, refund_status, processed_at FROM Refunds ORDER BY refund_id DESC";
        try (Connection conn = connectionManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                list.add(new Refund(
                        rs.getLong("refund_id"),
                        rs.getLong("cancellation_id"),
                        rs.getLong("payment_id"),
                        rs.getDouble("original_amount"),
                        rs.getDouble("deduction_amount"),
                        rs.getDouble("refund_amount"),
                        rs.getString("refund_status"),
                        rs.getString("processed_at")));
            }
            return list;
        } catch (SQLException ex) {
            throw new DatabaseOperationException("Error querying Refunds", ex);
        }
    }
}
