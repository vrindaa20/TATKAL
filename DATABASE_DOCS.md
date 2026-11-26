# TATKAL-X Database Documentation (`tatkal_x.db`)

## 1. Engine & Configuration
- **DBMS:** SQLite 3 via `sqlite-jdbc-3.46.1.0.jar` (raw JDBC, zero ORM)
- **Connection URL:** `jdbc:sqlite:tatkal_x.db`
- **PRAGMAs enabled on every connection (`DatabaseConnectionManager`):**
  - `PRAGMA foreign_keys = ON;` (enforces all foreign-key constraints)
  - `PRAGMA journal_mode = WAL;` (allows concurrent readers while serializing writes)
  - `PRAGMA busy_timeout = 5000;` (prevents transient lock failures)

## 2. Concurrency & Single-Writer Coordination
SQLite allows multiple concurrent readers (`SELECT`) and one active writer (`INSERT`/`UPDATE`/`DELETE`) at a time.
- Business seat invariants are guarded in memory by `synchronized(SeatInventory.getMonitorLock())`.
- Multi-statement SQLite persistence (`BookingDao`, `FinancialDao`, `UserPassengerDao`) executes inside `DatabaseConnectionManager.executeWriteTransaction(...)`, which acquires `writeGateLock`, disables `autoCommit`, executes parameterized `PreparedStatement` batches, and calls `commit()` or `rollback()`.

## 3. Normalization Summary
- **1NF:** Multi-valued traveler lists are stored in `Ticket_Passengers`; route stop lists are stored in `Train_Stops`.
- **2NF:** Surrogate primary keys (`INTEGER PRIMARY KEY AUTOINCREMENT`) eliminate partial dependencies; natural keys use explicit `UNIQUE` constraints.
- **3NF:** Station, Train, Route, User, and Strategy details live in dedicated parent tables without transitive duplication.

## 4. Complete 30-Table Directory

| Group | Tables |
|---|---|
| **Identity & Access** | `Users`, `Passengers`, `Admins` |
| **Railway Topology** | `Stations`, `Trains`, `Routes`, `Train_Stops`, `Train_Schedules`, `Coaches`, `Berths` |
| **Simulation & Inventory** | `Booking_Strategies`, `Simulation_Sessions`, `Seat_Inventory`, `Simulation_Results`, `System_Configurations` |
| **Booking & Queues** | `Booking_Requests`, `Tickets`, `Ticket_Passengers`, `PNR_Records`, `RAC_Queue`, `Waiting_List` |
| **Financials** | `Payments`, `Cancellations`, `Refunds`, `Transactions` |
| **Forensics & Audit** | `Booking_Events`, `Thread_Logs`, `Notifications`, `Failure_Records`, `Audit_Logs` |

## 5. Simulation Isolation Guarantee
Every simulation universe creates its own `Simulation_Sessions` row (`session_id`) and cloned `Seat_Inventory` rows (`UNIQUE(session_id, schedule_id, coach_id)`), ensuring Universe A, Universe B, Universe C, Failure Lab, and Live bookings never contaminate each other.
