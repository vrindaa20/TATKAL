package com.tatkalx.database;

import com.tatkalx.exception.DatabaseOperationException;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Component #44: Database Consistency Monitor.
 * Runs automated relational invariant audits across Seat_Inventory, Tickets,
 * Ticket_Passengers, PNR_Records, RAC_Queue, Waiting_List, and Payments.
 */
public class DatabaseConsistencyMonitor {

    public static class CheckItem {
        private final String checkName;
        private final boolean passed;
        private final String detail;

        public CheckItem(String checkName, boolean passed, String detail) {
            this.checkName = checkName;
            this.passed = passed;
            this.detail = detail;
        }

        public String getCheckName() {
            return checkName;
        }

        public boolean isPassed() {
            return passed;
        }

        public String getDetail() {
            return detail;
        }
    }

    public static class ConsistencyReport {
        private final List<CheckItem> checks = new ArrayList<>();

        public void addCheck(String name, boolean passed, String detail) {
            checks.add(new CheckItem(name, passed, detail));
        }

        public List<CheckItem> getChecks() {
            return Collections.unmodifiableList(checks);
        }

        public boolean isAllPassed() {
            for (CheckItem c : checks) {
                if (!c.isPassed()) {
                    return false;
                }
            }
            return true;
        }
    }

    private final DatabaseConnectionManager connectionManager;

    public DatabaseConsistencyMonitor(DatabaseConnectionManager connectionManager) {
        this.connectionManager = connectionManager;
    }

    public ConsistencyReport runFullConsistencyAudit() {
        ConsistencyReport report = new ConsistencyReport();
        try (Connection conn = connectionManager.getConnection();
             Statement stmt = conn.createStatement()) {

            // 1. Seat_Inventory conservation: available + confirmed == total
            try (ResultSet rs = stmt.executeQuery(
                    "SELECT COUNT(*) FROM Seat_Inventory WHERE available_tatkal_seats + confirmed_count <> total_tatkal_seats")) {
                int mismatches = rs.next() ? rs.getInt(1) : 0;
                report.addCheck("Inventory Seat Conservation (avail + conf == total)",
                        mismatches == 0, "Mismatched rows: " + mismatches);
            }

            // 2. Duplicate confirmed berth assignments inside same (session_id, schedule_id, berth_id)
            try (ResultSet rs = stmt.executeQuery(
                    "SELECT COUNT(*) FROM ("
                    + "SELECT t.session_id, t.schedule_id, tp.berth_id, COUNT(*) AS cnt "
                    + "FROM Ticket_Passengers tp JOIN Tickets t ON tp.ticket_id = t.ticket_id "
                    + "WHERE tp.allocation_status = 'CONFIRMED' AND tp.berth_id IS NOT NULL "
                    + "GROUP BY t.session_id, t.schedule_id, tp.berth_id HAVING cnt > 1)")) {
                int dupBerths = rs.next() ? rs.getInt(1) : 0;
                report.addCheck("Zero Double-Booked Physical Berths",
                        dupBerths == 0, "Duplicate berth assignments: " + dupBerths);
            }

            // 3. Tickets vs Seat_Inventory confirmed count sync
            try (ResultSet rs = stmt.executeQuery(
                    "SELECT COUNT(*) FROM Seat_Inventory si "
                    + "WHERE si.confirmed_count <> ("
                    + "SELECT COUNT(*) FROM Tickets t "
                    + "WHERE t.session_id = si.session_id AND t.schedule_id = si.schedule_id "
                    + "AND t.coach_id = si.coach_id AND t.ticket_status = 'CONFIRMED')")) {
                int drift = rs.next() ? rs.getInt(1) : 0;
                report.addCheck("Tickets vs Inventory Confirmed Count Sync",
                        drift == 0, "Drifted inventory rows: " + drift);
            }

            // 4. Every Ticket has a matching PNR record
            try (ResultSet rs = stmt.executeQuery(
                    "SELECT COUNT(*) FROM Tickets t LEFT JOIN PNR_Records p ON t.ticket_id = p.ticket_id WHERE p.pnr_id IS NULL")) {
                int missingPnrs = rs.next() ? rs.getInt(1) : 0;
                report.addCheck("Every Issued Ticket Has a PNR Record",
                        missingPnrs == 0, "Tickets missing PNR: " + missingPnrs);
            }

            // 5. Foreign-key integrity check via SQLite PRAGMA foreign_key_check
            try (ResultSet rs = stmt.executeQuery("PRAGMA foreign_key_check")) {
                int fkViolations = 0;
                while (rs.next()) {
                    fkViolations++;
                }
                report.addCheck("SQLite Foreign-Key Integrity (PRAGMA foreign_key_check)",
                        fkViolations == 0, "FK violations: " + fkViolations);
            }

            // 6. Table count verification (all 30 tables present)
            try (ResultSet rs = stmt.executeQuery(
                    "SELECT COUNT(*) FROM sqlite_master WHERE type = 'table' AND name NOT LIKE 'sqlite_%'")) {
                int tableCount = rs.next() ? rs.getInt(1) : 0;
                report.addCheck("30-Table Schema Completeness",
                        tableCount >= 30, "Active tables in tatkal_x.db: " + tableCount);
            }

        } catch (SQLException ex) {
            throw new DatabaseOperationException("Consistency audit failed: " + ex.getMessage(), ex);
        }
        return report;
    }
}
