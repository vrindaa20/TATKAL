package com.tatkalx.database;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

/**
 * Automatically initializes all 30 normalized tables, 9 indexes, and idempotent seed data
 * in tatkal_x.db when TATKAL-X launches.
 */
public class DatabaseInitializer {

    private final DatabaseConnectionManager connectionManager;

    public DatabaseInitializer(DatabaseConnectionManager connectionManager) {
        this.connectionManager = connectionManager;
    }

    public void initializeDatabase() {
        connectionManager.executeWriteTransaction(conn -> {
            createTablesAndIndexes(conn);
            seedInitialData(conn);
            syncIdCounters(conn);
            return null;
        });
    }

    private void syncIdCounters(Connection conn) throws SQLException {
        long maxPnr = 4521000000L;
        long maxTicket = 0L;
        long maxSession = 10L;
        long maxBooking = 1000L;

        try (Statement st = conn.createStatement()) {
            try (ResultSet rs = st.executeQuery("SELECT MAX(CAST(pnr_number AS INTEGER)) FROM PNR_Records")) {
                if (rs.next() && rs.getLong(1) > maxPnr) {
                    maxPnr = rs.getLong(1);
                }
            }
            try (ResultSet rs = st.executeQuery("SELECT COALESCE(MAX(ticket_id), 0) FROM Tickets")) {
                if (rs.next()) {
                    maxTicket = rs.getLong(1);
                }
            }
            try (ResultSet rs = st.executeQuery("SELECT COALESCE(MAX(session_id), 10) FROM Simulation_Sessions")) {
                if (rs.next()) {
                    maxSession = rs.getLong(1);
                }
            }
            try (ResultSet rs = st.executeQuery("SELECT COUNT(*) FROM Booking_Requests")) {
                if (rs.next()) {
                    maxBooking = 1000L + rs.getLong(1);
                }
            }
        }
        com.tatkalx.utility.IdGenerator.syncFromDbMaximums(maxBooking, maxPnr, maxTicket, maxSession);
    }

    private void createTablesAndIndexes(Connection conn) throws SQLException {
        String[] ddlStatements = {
            // 1. Users
            "CREATE TABLE IF NOT EXISTS Users ("
            + "user_id INTEGER PRIMARY KEY AUTOINCREMENT, "
            + "username TEXT NOT NULL UNIQUE, "
            + "password_hash TEXT NOT NULL, "
            + "full_name TEXT NOT NULL, "
            + "email TEXT NOT NULL UNIQUE, "
            + "phone TEXT NOT NULL, "
            + "role TEXT NOT NULL CHECK (role IN ('PASSENGER', 'ADMIN')), "
            + "is_active INTEGER NOT NULL DEFAULT 1 CHECK (is_active IN (0, 1)), "
            + "created_at TEXT NOT NULL DEFAULT (datetime('now', 'localtime')))",

            // 2. Passengers
            "CREATE TABLE IF NOT EXISTS Passengers ("
            + "passenger_id INTEGER PRIMARY KEY AUTOINCREMENT, "
            + "user_id INTEGER NOT NULL UNIQUE, "
            + "age INTEGER NOT NULL CHECK (age BETWEEN 1 AND 120), "
            + "gender TEXT NOT NULL CHECK (gender IN ('M', 'F', 'O')), "
            + "id_proof_type TEXT NOT NULL CHECK (id_proof_type IN ('AADHAAR', 'PAN', 'PASSPORT', 'VOTER_ID')), "
            + "id_proof_number TEXT NOT NULL UNIQUE, "
            + "is_verified INTEGER NOT NULL DEFAULT 0 CHECK (is_verified IN (0, 1)), "
            + "priority_category TEXT NOT NULL DEFAULT 'NORMAL' CHECK (priority_category IN ('NORMAL', 'SENIOR_CITIZEN', 'MEDICAL', 'DEFENCE')), "
            + "berth_preference TEXT NOT NULL DEFAULT 'ANY' CHECK (berth_preference IN ('LB', 'MB', 'UB', 'SL', 'SU', 'ANY')), "
            + "wallet_balance REAL NOT NULL DEFAULT 10000.0 CHECK (wallet_balance >= 0), "
            + "FOREIGN KEY (user_id) REFERENCES Users(user_id) ON DELETE CASCADE)",

            // 3. Admins
            "CREATE TABLE IF NOT EXISTS Admins ("
            + "admin_id INTEGER PRIMARY KEY AUTOINCREMENT, "
            + "user_id INTEGER NOT NULL UNIQUE, "
            + "employee_code TEXT NOT NULL UNIQUE, "
            + "department TEXT NOT NULL, "
            + "clearance_level INTEGER NOT NULL DEFAULT 1 CHECK (clearance_level BETWEEN 1 AND 5), "
            + "last_audit_at TEXT, "
            + "FOREIGN KEY (user_id) REFERENCES Users(user_id) ON DELETE CASCADE)",

            // 4. Stations
            "CREATE TABLE IF NOT EXISTS Stations ("
            + "station_id INTEGER PRIMARY KEY AUTOINCREMENT, "
            + "station_code TEXT NOT NULL UNIQUE, "
            + "station_name TEXT NOT NULL, "
            + "city TEXT NOT NULL, "
            + "state TEXT NOT NULL, "
            + "railway_zone TEXT NOT NULL)",

            // 5. Trains
            "CREATE TABLE IF NOT EXISTS Trains ("
            + "train_id INTEGER PRIMARY KEY AUTOINCREMENT, "
            + "train_number TEXT NOT NULL UNIQUE, "
            + "train_name TEXT NOT NULL, "
            + "train_type TEXT NOT NULL CHECK (train_type IN ('RAJDHANI', 'SHATABDI', 'DURONTO', 'SUPERFAST', 'EXPRESS')), "
            + "total_coaches INTEGER NOT NULL CHECK (total_coaches > 0), "
            + "is_tatkal_enabled INTEGER NOT NULL DEFAULT 1 CHECK (is_tatkal_enabled IN (0, 1)), "
            + "status TEXT NOT NULL DEFAULT 'ACTIVE' CHECK (status IN ('ACTIVE', 'MAINTENANCE', 'CANCELLED')))",

            // 6. Routes
            "CREATE TABLE IF NOT EXISTS Routes ("
            + "route_id INTEGER PRIMARY KEY AUTOINCREMENT, "
            + "route_code TEXT NOT NULL UNIQUE, "
            + "origin_station_id INTEGER NOT NULL, "
            + "destination_station_id INTEGER NOT NULL, "
            + "total_distance_km INTEGER NOT NULL CHECK (total_distance_km > 0), "
            + "FOREIGN KEY (origin_station_id) REFERENCES Stations(station_id), "
            + "FOREIGN KEY (destination_station_id) REFERENCES Stations(station_id), "
            + "CHECK (origin_station_id <> destination_station_id))",

            // 7. Train_Stops
            "CREATE TABLE IF NOT EXISTS Train_Stops ("
            + "stop_id INTEGER PRIMARY KEY AUTOINCREMENT, "
            + "route_id INTEGER NOT NULL, "
            + "station_id INTEGER NOT NULL, "
            + "stop_order INTEGER NOT NULL CHECK (stop_order >= 1), "
            + "distance_from_origin_km INTEGER NOT NULL CHECK (distance_from_origin_km >= 0), "
            + "arrival_offset_min INTEGER NOT NULL CHECK (arrival_offset_min >= 0), "
            + "departure_offset_min INTEGER NOT NULL CHECK (departure_offset_min >= arrival_offset_min), "
            + "FOREIGN KEY (route_id) REFERENCES Routes(route_id) ON DELETE CASCADE, "
            + "FOREIGN KEY (station_id) REFERENCES Stations(station_id), "
            + "UNIQUE (route_id, stop_order), UNIQUE (route_id, station_id))",

            // 8. Train_Schedules
            "CREATE TABLE IF NOT EXISTS Train_Schedules ("
            + "schedule_id INTEGER PRIMARY KEY AUTOINCREMENT, "
            + "train_id INTEGER NOT NULL, "
            + "route_id INTEGER NOT NULL, "
            + "journey_date TEXT NOT NULL, "
            + "departure_time TEXT NOT NULL, "
            + "arrival_time TEXT NOT NULL, "
            + "tatkal_open_time TEXT NOT NULL DEFAULT '10:00:00', "
            + "tatkal_close_time TEXT NOT NULL DEFAULT '11:00:00', "
            + "schedule_status TEXT NOT NULL DEFAULT 'SCHEDULED' CHECK (schedule_status IN ('SCHEDULED', 'CHARTED', 'DEPARTED', 'CANCELLED')), "
            + "FOREIGN KEY (train_id) REFERENCES Trains(train_id), "
            + "FOREIGN KEY (route_id) REFERENCES Routes(route_id), "
            + "UNIQUE (train_id, journey_date))",

            // 9. Coaches
            "CREATE TABLE IF NOT EXISTS Coaches ("
            + "coach_id INTEGER PRIMARY KEY AUTOINCREMENT, "
            + "train_id INTEGER NOT NULL, "
            + "coach_code TEXT NOT NULL, "
            + "coach_class TEXT NOT NULL CHECK (coach_class IN ('2A', '3A', 'SL', 'CC')), "
            + "total_berths INTEGER NOT NULL CHECK (total_berths > 0), "
            + "tatkal_quota_berths INTEGER NOT NULL CHECK (tatkal_quota_berths >= 0 AND tatkal_quota_berths <= total_berths), "
            + "rac_quota INTEGER NOT NULL DEFAULT 0 CHECK (rac_quota >= 0), "
            + "wl_quota INTEGER NOT NULL DEFAULT 0 CHECK (wl_quota >= 0), "
            + "base_fare_per_km REAL NOT NULL CHECK (base_fare_per_km > 0), "
            + "FOREIGN KEY (train_id) REFERENCES Trains(train_id) ON DELETE CASCADE, "
            + "UNIQUE (train_id, coach_code))",

            // 10. Berths
            "CREATE TABLE IF NOT EXISTS Berths ("
            + "berth_id INTEGER PRIMARY KEY AUTOINCREMENT, "
            + "coach_id INTEGER NOT NULL, "
            + "berth_number INTEGER NOT NULL CHECK (berth_number > 0), "
            + "berth_type TEXT NOT NULL CHECK (berth_type IN ('LB', 'MB', 'UB', 'SL', 'SU', 'WS', 'AS')), "
            + "is_tatkal_reserved INTEGER NOT NULL DEFAULT 1 CHECK (is_tatkal_reserved IN (0, 1)), "
            + "is_rac_shared INTEGER NOT NULL DEFAULT 0 CHECK (is_rac_shared IN (0, 1)), "
            + "FOREIGN KEY (coach_id) REFERENCES Coaches(coach_id) ON DELETE CASCADE, "
            + "UNIQUE (coach_id, berth_number))",

            // 11. Booking_Strategies
            "CREATE TABLE IF NOT EXISTS Booking_Strategies ("
            + "strategy_id INTEGER PRIMARY KEY AUTOINCREMENT, "
            + "strategy_code TEXT NOT NULL UNIQUE, "
            + "strategy_name TEXT NOT NULL, "
            + "universe_label TEXT NOT NULL CHECK (universe_label IN ('LIVE', 'UNIVERSE_A', 'UNIVERSE_B', 'UNIVERSE_C', 'FAILURE_LAB')), "
            + "description TEXT NOT NULL, "
            + "priority_rule TEXT)",

            // 12. Simulation_Sessions
            "CREATE TABLE IF NOT EXISTS Simulation_Sessions ("
            + "session_id INTEGER PRIMARY KEY AUTOINCREMENT, "
            + "session_code TEXT NOT NULL UNIQUE, "
            + "session_type TEXT NOT NULL CHECK (session_type IN ('LIVE', 'SEQUENTIAL', 'PRIORITY', 'CONCURRENT', 'FAILURE_LAB')), "
            + "strategy_id INTEGER NOT NULL, "
            + "schedule_id INTEGER NOT NULL, "
            + "total_requests INTEGER NOT NULL DEFAULT 0 CHECK (total_requests >= 0), "
            + "virtual_open_time TEXT NOT NULL DEFAULT '10:00:00', "
            + "started_at TEXT NOT NULL DEFAULT (datetime('now', 'localtime')), "
            + "ended_at TEXT, "
            + "status TEXT NOT NULL DEFAULT 'RUNNING' CHECK (status IN ('CREATED', 'RUNNING', 'COMPLETED', 'FAILED')), "
            + "FOREIGN KEY (strategy_id) REFERENCES Booking_Strategies(strategy_id), "
            + "FOREIGN KEY (schedule_id) REFERENCES Train_Schedules(schedule_id))",

            // 13. Seat_Inventory
            "CREATE TABLE IF NOT EXISTS Seat_Inventory ("
            + "inventory_id INTEGER PRIMARY KEY AUTOINCREMENT, "
            + "session_id INTEGER NOT NULL, "
            + "schedule_id INTEGER NOT NULL, "
            + "coach_id INTEGER NOT NULL, "
            + "total_tatkal_seats INTEGER NOT NULL CHECK (total_tatkal_seats >= 0), "
            + "available_tatkal_seats INTEGER NOT NULL CHECK (available_tatkal_seats >= 0 AND available_tatkal_seats <= total_tatkal_seats), "
            + "confirmed_count INTEGER NOT NULL DEFAULT 0 CHECK (confirmed_count >= 0), "
            + "rac_capacity INTEGER NOT NULL DEFAULT 0 CHECK (rac_capacity >= 0), "
            + "rac_allocated INTEGER NOT NULL DEFAULT 0 CHECK (rac_allocated >= 0 AND rac_allocated <= rac_capacity), "
            + "wl_capacity INTEGER NOT NULL DEFAULT 0 CHECK (wl_capacity >= 0), "
            + "wl_allocated INTEGER NOT NULL DEFAULT 0 CHECK (wl_allocated >= 0 AND wl_allocated <= wl_capacity), "
            + "version_num INTEGER NOT NULL DEFAULT 1, "
            + "FOREIGN KEY (session_id) REFERENCES Simulation_Sessions(session_id) ON DELETE CASCADE, "
            + "FOREIGN KEY (schedule_id) REFERENCES Train_Schedules(schedule_id), "
            + "FOREIGN KEY (coach_id) REFERENCES Coaches(coach_id), "
            + "UNIQUE (session_id, schedule_id, coach_id), "
            + "CHECK (available_tatkal_seats + confirmed_count = total_tatkal_seats))",

            // 14. Booking_Requests
            "CREATE TABLE IF NOT EXISTS Booking_Requests ("
            + "booking_id TEXT PRIMARY KEY, "
            + "session_id INTEGER NOT NULL, "
            + "passenger_id INTEGER NOT NULL, "
            + "schedule_id INTEGER NOT NULL, "
            + "coach_class TEXT NOT NULL CHECK (coach_class IN ('2A', '3A', 'SL', 'CC')), "
            + "requested_seats INTEGER NOT NULL DEFAULT 1 CHECK (requested_seats BETWEEN 1 AND 4), "
            + "priority_score INTEGER NOT NULL DEFAULT 0, "
            + "request_timestamp TEXT NOT NULL, "
            + "queue_entered_at TEXT, "
            + "queue_position INTEGER, "
            + "thread_name TEXT, "
            + "processing_time_ms INTEGER DEFAULT 0 CHECK (processing_time_ms >= 0), "
            + "status TEXT NOT NULL DEFAULT 'QUEUED' CHECK (status IN ('CREATED', 'QUEUED', 'PROCESSING', 'CONFIRMED', 'RAC', 'WL', 'FAILED', 'TIMEOUT', 'CANCELLED')), "
            + "failure_reason TEXT, "
            + "FOREIGN KEY (session_id) REFERENCES Simulation_Sessions(session_id) ON DELETE CASCADE, "
            + "FOREIGN KEY (passenger_id) REFERENCES Passengers(passenger_id), "
            + "FOREIGN KEY (schedule_id) REFERENCES Train_Schedules(schedule_id))",

            // 15. Tickets
            "CREATE TABLE IF NOT EXISTS Tickets ("
            + "ticket_id INTEGER PRIMARY KEY AUTOINCREMENT, "
            + "booking_id TEXT NOT NULL UNIQUE, "
            + "session_id INTEGER NOT NULL, "
            + "passenger_id INTEGER NOT NULL, "
            + "schedule_id INTEGER NOT NULL, "
            + "coach_id INTEGER NOT NULL, "
            + "ticket_status TEXT NOT NULL CHECK (ticket_status IN ('CONFIRMED', 'RAC', 'WL', 'CANCELLED')), "
            + "total_fare REAL NOT NULL CHECK (total_fare >= 0), "
            + "booked_at TEXT NOT NULL DEFAULT (datetime('now', 'localtime')), "
            + "updated_at TEXT, "
            + "FOREIGN KEY (booking_id) REFERENCES Booking_Requests(booking_id) ON DELETE CASCADE, "
            + "FOREIGN KEY (session_id) REFERENCES Simulation_Sessions(session_id) ON DELETE CASCADE, "
            + "FOREIGN KEY (passenger_id) REFERENCES Passengers(passenger_id), "
            + "FOREIGN KEY (schedule_id) REFERENCES Train_Schedules(schedule_id), "
            + "FOREIGN KEY (coach_id) REFERENCES Coaches(coach_id))",

            // 16. Ticket_Passengers
            "CREATE TABLE IF NOT EXISTS Ticket_Passengers ("
            + "tp_id INTEGER PRIMARY KEY AUTOINCREMENT, "
            + "ticket_id INTEGER NOT NULL, "
            + "traveler_name TEXT NOT NULL, "
            + "traveler_age INTEGER NOT NULL CHECK (traveler_age BETWEEN 1 AND 120), "
            + "traveler_gender TEXT NOT NULL CHECK (traveler_gender IN ('M', 'F', 'O')), "
            + "berth_id INTEGER, "
            + "assigned_seat_label TEXT, "
            + "allocation_status TEXT NOT NULL CHECK (allocation_status IN ('CONFIRMED', 'RAC', 'WL', 'CANCELLED')), "
            + "FOREIGN KEY (ticket_id) REFERENCES Tickets(ticket_id) ON DELETE CASCADE, "
            + "FOREIGN KEY (berth_id) REFERENCES Berths(berth_id))",

            // 17. PNR_Records
            "CREATE TABLE IF NOT EXISTS PNR_Records ("
            + "pnr_id INTEGER PRIMARY KEY AUTOINCREMENT, "
            + "pnr_number TEXT NOT NULL UNIQUE, "
            + "ticket_id INTEGER NOT NULL UNIQUE, "
            + "session_id INTEGER NOT NULL, "
            + "current_status TEXT NOT NULL CHECK (current_status IN ('CONFIRMED', 'RAC', 'WL', 'CANCELLED')), "
            + "chart_prepared INTEGER NOT NULL DEFAULT 0 CHECK (chart_prepared IN (0, 1)), "
            + "generated_at TEXT NOT NULL DEFAULT (datetime('now', 'localtime')), "
            + "FOREIGN KEY (ticket_id) REFERENCES Tickets(ticket_id) ON DELETE CASCADE, "
            + "FOREIGN KEY (session_id) REFERENCES Simulation_Sessions(session_id) ON DELETE CASCADE)",

            // 18. RAC_Queue
            "CREATE TABLE IF NOT EXISTS RAC_Queue ("
            + "rac_id INTEGER PRIMARY KEY AUTOINCREMENT, "
            + "session_id INTEGER NOT NULL, "
            + "schedule_id INTEGER NOT NULL, "
            + "coach_id INTEGER NOT NULL, "
            + "ticket_id INTEGER NOT NULL UNIQUE, "
            + "rac_number INTEGER NOT NULL CHECK (rac_number >= 1), "
            + "queue_status TEXT NOT NULL DEFAULT 'ACTIVE' CHECK (queue_status IN ('ACTIVE', 'PROMOTED', 'CANCELLED')), "
            + "enqueued_at TEXT NOT NULL DEFAULT (datetime('now', 'localtime')), "
            + "promoted_at TEXT, "
            + "FOREIGN KEY (session_id) REFERENCES Simulation_Sessions(session_id) ON DELETE CASCADE, "
            + "FOREIGN KEY (schedule_id) REFERENCES Train_Schedules(schedule_id), "
            + "FOREIGN KEY (coach_id) REFERENCES Coaches(coach_id), "
            + "FOREIGN KEY (ticket_id) REFERENCES Tickets(ticket_id) ON DELETE CASCADE)",

            // 19. Waiting_List
            "CREATE TABLE IF NOT EXISTS Waiting_List ("
            + "wl_id INTEGER PRIMARY KEY AUTOINCREMENT, "
            + "session_id INTEGER NOT NULL, "
            + "schedule_id INTEGER NOT NULL, "
            + "coach_id INTEGER NOT NULL, "
            + "ticket_id INTEGER NOT NULL UNIQUE, "
            + "wl_number INTEGER NOT NULL CHECK (wl_number >= 1), "
            + "queue_status TEXT NOT NULL DEFAULT 'ACTIVE' CHECK (queue_status IN ('ACTIVE', 'PROMOTED', 'CANCELLED')), "
            + "enqueued_at TEXT NOT NULL DEFAULT (datetime('now', 'localtime')), "
            + "promoted_at TEXT, "
            + "FOREIGN KEY (session_id) REFERENCES Simulation_Sessions(session_id) ON DELETE CASCADE, "
            + "FOREIGN KEY (schedule_id) REFERENCES Train_Schedules(schedule_id), "
            + "FOREIGN KEY (coach_id) REFERENCES Coaches(coach_id), "
            + "FOREIGN KEY (ticket_id) REFERENCES Tickets(ticket_id) ON DELETE CASCADE)",

            // 20. Payments
            "CREATE TABLE IF NOT EXISTS Payments ("
            + "payment_id INTEGER PRIMARY KEY AUTOINCREMENT, "
            + "booking_id TEXT NOT NULL, "
            + "session_id INTEGER NOT NULL, "
            + "passenger_id INTEGER NOT NULL, "
            + "amount REAL NOT NULL CHECK (amount >= 0), "
            + "payment_method TEXT NOT NULL CHECK (payment_method IN ('UPI', 'WALLET', 'CARD', 'NETBANKING')), "
            + "gateway_ref TEXT NOT NULL UNIQUE, "
            + "payment_status TEXT NOT NULL CHECK (payment_status IN ('INITIATED', 'SUCCESS', 'FAILED', 'TIMEOUT', 'REFUNDED')), "
            + "latency_ms INTEGER NOT NULL DEFAULT 0 CHECK (latency_ms >= 0), "
            + "initiated_at TEXT NOT NULL, "
            + "completed_at TEXT, "
            + "FOREIGN KEY (booking_id) REFERENCES Booking_Requests(booking_id) ON DELETE CASCADE, "
            + "FOREIGN KEY (session_id) REFERENCES Simulation_Sessions(session_id) ON DELETE CASCADE, "
            + "FOREIGN KEY (passenger_id) REFERENCES Passengers(passenger_id))",

            // 21. Cancellations
            "CREATE TABLE IF NOT EXISTS Cancellations ("
            + "cancellation_id INTEGER PRIMARY KEY AUTOINCREMENT, "
            + "ticket_id INTEGER NOT NULL UNIQUE, "
            + "session_id INTEGER NOT NULL, "
            + "cancelled_by_user_id INTEGER NOT NULL, "
            + "previous_status TEXT NOT NULL CHECK (previous_status IN ('CONFIRMED', 'RAC', 'WL')), "
            + "cancellation_reason TEXT NOT NULL, "
            + "cancelled_at TEXT NOT NULL DEFAULT (datetime('now', 'localtime')), "
            + "FOREIGN KEY (ticket_id) REFERENCES Tickets(ticket_id) ON DELETE CASCADE, "
            + "FOREIGN KEY (session_id) REFERENCES Simulation_Sessions(session_id) ON DELETE CASCADE, "
            + "FOREIGN KEY (cancelled_by_user_id) REFERENCES Users(user_id))",

            // 22. Refunds
            "CREATE TABLE IF NOT EXISTS Refunds ("
            + "refund_id INTEGER PRIMARY KEY AUTOINCREMENT, "
            + "cancellation_id INTEGER NOT NULL UNIQUE, "
            + "payment_id INTEGER NOT NULL, "
            + "original_amount REAL NOT NULL CHECK (original_amount >= 0), "
            + "deduction_amount REAL NOT NULL CHECK (deduction_amount >= 0), "
            + "refund_amount REAL NOT NULL CHECK (refund_amount >= 0 AND refund_amount <= original_amount), "
            + "refund_status TEXT NOT NULL CHECK (refund_status IN ('CALCULATED', 'PROCESSED', 'NO_REFUND')), "
            + "processed_at TEXT NOT NULL DEFAULT (datetime('now', 'localtime')), "
            + "FOREIGN KEY (cancellation_id) REFERENCES Cancellations(cancellation_id) ON DELETE CASCADE, "
            + "FOREIGN KEY (payment_id) REFERENCES Payments(payment_id))",

            // 23. Transactions
            "CREATE TABLE IF NOT EXISTS Transactions ("
            + "txn_id INTEGER PRIMARY KEY AUTOINCREMENT, "
            + "session_id INTEGER NOT NULL, "
            + "user_id INTEGER NOT NULL, "
            + "payment_id INTEGER, "
            + "refund_id INTEGER, "
            + "txn_type TEXT NOT NULL CHECK (txn_type IN ('DEBIT_BOOKING', 'CREDIT_REFUND', 'REVERSAL')), "
            + "amount REAL NOT NULL CHECK (amount >= 0), "
            + "balance_after REAL NOT NULL CHECK (balance_after >= 0), "
            + "txn_status TEXT NOT NULL CHECK (txn_status IN ('COMMITTED', 'ROLLED_BACK')), "
            + "created_at TEXT NOT NULL DEFAULT (datetime('now', 'localtime')), "
            + "FOREIGN KEY (session_id) REFERENCES Simulation_Sessions(session_id) ON DELETE CASCADE, "
            + "FOREIGN KEY (user_id) REFERENCES Users(user_id), "
            + "FOREIGN KEY (payment_id) REFERENCES Payments(payment_id), "
            + "FOREIGN KEY (refund_id) REFERENCES Refunds(refund_id))",

            // 24. Booking_Events
            "CREATE TABLE IF NOT EXISTS Booking_Events ("
            + "event_id INTEGER PRIMARY KEY AUTOINCREMENT, "
            + "booking_id TEXT NOT NULL, "
            + "session_id INTEGER NOT NULL, "
            + "passenger_id INTEGER NOT NULL, "
            + "thread_name TEXT NOT NULL, "
            + "event_seq INTEGER NOT NULL CHECK (event_seq >= 1), "
            + "event_type TEXT NOT NULL, "
            + "event_description TEXT NOT NULL, "
            + "request_status TEXT NOT NULL, "
            + "available_seats_snapshot INTEGER, "
            + "queue_position_snapshot INTEGER, "
            + "allocation_result TEXT, "
            + "payment_result TEXT, "
            + "event_timestamp TEXT NOT NULL, "
            + "FOREIGN KEY (booking_id) REFERENCES Booking_Requests(booking_id) ON DELETE CASCADE, "
            + "FOREIGN KEY (session_id) REFERENCES Simulation_Sessions(session_id) ON DELETE CASCADE, "
            + "FOREIGN KEY (passenger_id) REFERENCES Passengers(passenger_id), "
            + "UNIQUE (booking_id, event_seq))",

            // 25. Thread_Logs
            "CREATE TABLE IF NOT EXISTS Thread_Logs ("
            + "thread_log_id INTEGER PRIMARY KEY AUTOINCREMENT, "
            + "session_id INTEGER NOT NULL, "
            + "booking_id TEXT, "
            + "thread_id_num INTEGER NOT NULL, "
            + "thread_name TEXT NOT NULL, "
            + "thread_priority INTEGER NOT NULL CHECK (thread_priority BETWEEN 1 AND 10), "
            + "thread_state TEXT NOT NULL CHECK (thread_state IN ('NEW', 'RUNNABLE', 'BLOCKED', 'WAITING', 'TIMED_WAITING', 'TERMINATED')), "
            + "lock_target TEXT, "
            + "action_detail TEXT NOT NULL, "
            + "logged_at TEXT NOT NULL, "
            + "FOREIGN KEY (session_id) REFERENCES Simulation_Sessions(session_id) ON DELETE CASCADE, "
            + "FOREIGN KEY (booking_id) REFERENCES Booking_Requests(booking_id) ON DELETE SET NULL)",

            // 26. Simulation_Results
            "CREATE TABLE IF NOT EXISTS Simulation_Results ("
            + "result_id INTEGER PRIMARY KEY AUTOINCREMENT, "
            + "session_id INTEGER NOT NULL UNIQUE, "
            + "total_requests INTEGER NOT NULL, "
            + "confirmed_count INTEGER NOT NULL, "
            + "rac_count INTEGER NOT NULL, "
            + "wl_count INTEGER NOT NULL, "
            + "failed_count INTEGER NOT NULL, "
            + "duplicate_attempts INTEGER NOT NULL DEFAULT 0, "
            + "total_time_ms INTEGER NOT NULL, "
            + "avg_time_ms REAL NOT NULL, "
            + "max_time_ms INTEGER NOT NULL, "
            + "success_rate_pct REAL NOT NULL, "
            + "seat_utilization_pct REAL NOT NULL, "
            + "inventory_consistent INTEGER NOT NULL CHECK (inventory_consistent IN (0, 1)), "
            + "recorded_at TEXT NOT NULL DEFAULT (datetime('now', 'localtime')), "
            + "FOREIGN KEY (session_id) REFERENCES Simulation_Sessions(session_id) ON DELETE CASCADE)",

            // 27. System_Configurations
            "CREATE TABLE IF NOT EXISTS System_Configurations ("
            + "config_key TEXT PRIMARY KEY, "
            + "config_value TEXT NOT NULL, "
            + "value_type TEXT NOT NULL CHECK (value_type IN ('STRING', 'INTEGER', 'DOUBLE', 'BOOLEAN', 'TIME')), "
            + "description TEXT NOT NULL, "
            + "updated_at TEXT NOT NULL DEFAULT (datetime('now', 'localtime')))",

            // 28. Notifications
            "CREATE TABLE IF NOT EXISTS Notifications ("
            + "notification_id INTEGER PRIMARY KEY AUTOINCREMENT, "
            + "session_id INTEGER NOT NULL, "
            + "passenger_id INTEGER NOT NULL, "
            + "booking_id TEXT, "
            + "title TEXT NOT NULL, "
            + "message TEXT NOT NULL, "
            + "channel TEXT NOT NULL DEFAULT 'TERMINAL' CHECK (channel IN ('TERMINAL', 'SMS_SIM', 'EMAIL_SIM')), "
            + "is_read INTEGER NOT NULL DEFAULT 0 CHECK (is_read IN (0, 1)), "
            + "created_at TEXT NOT NULL DEFAULT (datetime('now', 'localtime')), "
            + "FOREIGN KEY (session_id) REFERENCES Simulation_Sessions(session_id) ON DELETE CASCADE, "
            + "FOREIGN KEY (passenger_id) REFERENCES Passengers(passenger_id), "
            + "FOREIGN KEY (booking_id) REFERENCES Booking_Requests(booking_id) ON DELETE SET NULL)",

            // 29. Failure_Records
            "CREATE TABLE IF NOT EXISTS Failure_Records ("
            + "failure_id INTEGER PRIMARY KEY AUTOINCREMENT, "
            + "session_id INTEGER NOT NULL, "
            + "booking_id TEXT, "
            + "scenario_code TEXT NOT NULL, "
            + "thread_name TEXT NOT NULL, "
            + "error_class TEXT NOT NULL, "
            + "error_message TEXT NOT NULL, "
            + "recovery_action TEXT NOT NULL, "
            + "inventory_verified INTEGER NOT NULL CHECK (inventory_verified IN (0, 1)), "
            + "occurred_at TEXT NOT NULL DEFAULT (datetime('now', 'localtime')), "
            + "FOREIGN KEY (session_id) REFERENCES Simulation_Sessions(session_id) ON DELETE CASCADE, "
            + "FOREIGN KEY (booking_id) REFERENCES Booking_Requests(booking_id) ON DELETE SET NULL)",

            // 30. Audit_Logs
            "CREATE TABLE IF NOT EXISTS Audit_Logs ("
            + "audit_id INTEGER PRIMARY KEY AUTOINCREMENT, "
            + "user_id INTEGER, "
            + "session_id INTEGER, "
            + "module_name TEXT NOT NULL, "
            + "action_name TEXT NOT NULL, "
            + "target_entity TEXT NOT NULL, "
            + "target_id TEXT NOT NULL, "
            + "details TEXT NOT NULL, "
            + "logged_at TEXT NOT NULL DEFAULT (datetime('now', 'localtime')), "
            + "FOREIGN KEY (user_id) REFERENCES Users(user_id), "
            + "FOREIGN KEY (session_id) REFERENCES Simulation_Sessions(session_id) ON DELETE SET NULL)",

            // Indexes
            "CREATE INDEX IF NOT EXISTS idx_booking_requests_session_status ON Booking_Requests(session_id, status)",
            "CREATE INDEX IF NOT EXISTS idx_booking_requests_passenger ON Booking_Requests(passenger_id, schedule_id)",
            "CREATE INDEX IF NOT EXISTS idx_tickets_session_schedule ON Tickets(session_id, schedule_id, ticket_status)",
            "CREATE INDEX IF NOT EXISTS idx_pnr_lookup ON PNR_Records(pnr_number)",
            "CREATE INDEX IF NOT EXISTS idx_rac_active_order ON RAC_Queue(session_id, schedule_id, queue_status, rac_number)",
            "CREATE INDEX IF NOT EXISTS idx_wl_active_order ON Waiting_List(session_id, schedule_id, queue_status, wl_number)",
            "CREATE INDEX IF NOT EXISTS idx_booking_events_forensic ON Booking_Events(booking_id, event_seq)",
            "CREATE INDEX IF NOT EXISTS idx_thread_logs_session ON Thread_Logs(session_id, thread_state)",
            "CREATE INDEX IF NOT EXISTS idx_notifications_passenger ON Notifications(passenger_id, is_read)"
        };

        try (Statement stmt = conn.createStatement()) {
            for (String sql : ddlStatements) {
                stmt.execute(sql);
            }
        }
    }

    private void seedInitialData(Connection conn) throws SQLException {
        // Seed only if Users table is empty
        try (Statement checkStmt = conn.createStatement();
             ResultSet rs = checkStmt.executeQuery("SELECT COUNT(*) FROM Users")) {
            if (rs.next() && rs.getInt(1) > 0) {
                return;
            }
        }

        String[] seedSql = {
            "INSERT INTO Users (user_id, username, password_hash, full_name, email, phone, role) VALUES "
            + "(1, 'admin_tx', 'admin123', 'Chief Reservation Controller', 'admin@tatkalx.in', '9999900001', 'ADMIN'), "
            + "(2, 'aarav_99', 'pass123',  'Aarav Sharma', 'aarav@mail.in', '9876543210', 'PASSENGER'), "
            + "(3, 'meera_k',  'pass123',  'Meera Krishnan', 'meera@mail.in', '9876543211', 'PASSENGER')",

            "INSERT INTO Admins (user_id, employee_code, department, clearance_level) VALUES "
            + "(1, 'IRCTC-ADM-01', 'Forensics & Concurrency Control', 5)",

            "INSERT INTO Passengers (user_id, age, gender, id_proof_type, id_proof_number, is_verified, priority_category, berth_preference, wallet_balance) VALUES "
            + "(2, 27, 'M', 'AADHAAR', 'AADHAAR-4411-8822', 1, 'NORMAL', 'SL', 15000.0), "
            + "(3, 64, 'F', 'PAN', 'PAN-MK9021Z', 1, 'SENIOR_CITIZEN', 'LB', 20000.0)",

            "INSERT INTO Stations (station_id, station_code, station_name, city, state, railway_zone) VALUES "
            + "(1, 'NDLS', 'New Delhi', 'New Delhi', 'Delhi', 'NR'), "
            + "(2, 'KOTA', 'Kota Junction', 'Kota', 'Rajasthan', 'WCR'), "
            + "(3, 'BCT',  'Mumbai Central', 'Mumbai', 'Maharashtra', 'WR')",

            "INSERT INTO Trains (train_id, train_number, train_name, train_type, total_coaches, is_tatkal_enabled) VALUES "
            + "(1, '12952', 'Mumbai Rajdhani Express', 'RAJDHANI', 2, 1)",

            "INSERT INTO Routes (route_id, route_code, origin_station_id, destination_station_id, total_distance_km) VALUES "
            + "(1, 'NDLS-BCT-RAJ', 1, 3, 1386)",

            "INSERT INTO Train_Stops (route_id, station_id, stop_order, distance_from_origin_km, arrival_offset_min, departure_offset_min) VALUES "
            + "(1, 1, 1, 0, 0, 0), (1, 2, 2, 465, 290, 295), (1, 3, 3, 1386, 940, 940)",

            "INSERT INTO Train_Schedules (schedule_id, train_id, route_id, journey_date, departure_time, arrival_time, tatkal_open_time, tatkal_close_time) VALUES "
            + "(1, 1, 1, '2026-11-26', '16:55', '08:35', '10:00:00', '11:00:00')",

            "INSERT INTO Coaches (coach_id, train_id, coach_code, coach_class, total_berths, tatkal_quota_berths, rac_quota, wl_quota, base_fare_per_km) VALUES "
            + "(1, 1, 'B1', '3A', 72, 24, 6, 15, 1.45), "
            + "(2, 1, 'A1', '2A', 48, 16, 4, 10, 2.15)",

            "INSERT INTO Booking_Strategies (strategy_id, strategy_code, strategy_name, universe_label, description, priority_rule) VALUES "
            + "(1, 'LIVE_SYNC', 'Live Synchronized Tatkal', 'LIVE', 'Real-time terminal booking', 'FIFO + Synchronized'), "
            + "(2, 'UNIV_A', 'Sequential FIFO Universe', 'UNIVERSE_A', 'Single-threaded ArrayList/Queue/Iterator', 'Arrival Timestamp'), "
            + "(3, 'UNIV_B', 'Priority Queue Universe', 'UNIVERSE_B', 'PriorityQueue using Comparator', 'Category > Age > Timestamp'), "
            + "(4, 'UNIV_C', 'Concurrent Race Universe', 'UNIVERSE_C', 'Multi-threaded Runnable workers', 'OS Thread Contention'), "
            + "(5, 'FAIL_LAB', 'Booking Failure Lab', 'FAILURE_LAB', 'Fault injection & recovery testing', 'Scenario Specific')",

            "INSERT INTO System_Configurations (config_key, config_value, value_type, description) VALUES "
            + "('TATKAL_OPEN_TIME', '10:00:00', 'TIME', 'Virtual Tatkal booking opening hour'), "
            + "('TATKAL_CLOSE_TIME', '11:00:00', 'TIME', 'Virtual Tatkal booking closing hour'), "
            + "('PAYMENT_TIMEOUT_MS', '3000', 'INTEGER', 'Max payment wait before seat release'), "
            + "('TATKAL_SURCHARGE_RATE', '0.30', 'DOUBLE', '30% Tatkal dynamic surcharge')",

            "INSERT INTO Simulation_Sessions (session_id, session_code, session_type, strategy_id, schedule_id, total_requests, status) VALUES "
            + "(1, 'LIVE-DEFAULT-01', 'LIVE', 1, 1, 0, 'RUNNING')",

            "INSERT INTO Seat_Inventory (session_id, schedule_id, coach_id, total_tatkal_seats, available_tatkal_seats, confirmed_count, rac_capacity, rac_allocated, wl_capacity, wl_allocated) VALUES "
            + "(1, 1, 1, 24, 24, 0, 6, 0, 15, 0), "
            + "(1, 1, 2, 16, 16, 0, 4, 0, 10, 0)"
        };

        try (Statement stmt = conn.createStatement()) {
            for (String sql : seedSql) {
                stmt.execute(sql);
            }
        }

        // Generate physical Berths for Coach B1 (1..72) and Coach A1 (1..48)
        String insertBerthSql = "INSERT INTO Berths (coach_id, berth_number, berth_type, is_tatkal_reserved, is_rac_shared) VALUES (?, ?, ?, ?, ?)";
        String[] cycle3A = {"LB", "MB", "UB", "LB", "MB", "UB", "SL", "SU"};
        String[] cycle2A = {"LB", "UB", "LB", "UB", "SL", "SU"};

        try (PreparedStatement ps = conn.prepareStatement(insertBerthSql)) {
            for (int num = 1; num <= 72; num++) {
                String type = cycle3A[(num - 1) % cycle3A.length];
                ps.setInt(1, 1);
                ps.setInt(2, num);
                ps.setString(3, type);
                ps.setInt(4, num <= 24 ? 1 : 0);
                ps.setInt(5, ("SL".equals(type) && num > 24 && num <= 48) ? 1 : 0);
                ps.addBatch();
            }
            for (int num = 1; num <= 48; num++) {
                String type = cycle2A[(num - 1) % cycle2A.length];
                ps.setInt(1, 2);
                ps.setInt(2, num);
                ps.setString(3, type);
                ps.setInt(4, num <= 16 ? 1 : 0);
                ps.setInt(5, ("SL".equals(type) && num > 16 && num <= 32) ? 1 : 0);
                ps.addBatch();
            }
            ps.executeBatch();
        }
    }
}
