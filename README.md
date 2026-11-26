# TATKAL-X: The Railway Booking Race & Forensic Intelligence Simulator

**TATKAL-X** is a terminal-based Java + SQLite simulation, benchmarking, and forensic investigation platform modeled after IRCTC's 10:00 AM Tatkal railway reservation rush.

Instead of a simple CRUD form, TATKAL-X answers:
> *What actually happens when hundreds of passengers compete for limited railway seats at precisely the same time, and how can we prove that the reservation system made correct decisions?*

---

## Key Capabilities (44 Functional Components)
1. **Three Isolated Booking Universes:**
   - **Universe A (Sequential):** `ArrayList` + `Queue` + `Iterator` FIFO baseline.
   - **Universe B (Priority-Based):** `PriorityQueue` + configurable `Comparator` policies (`MEDICAL_SENIOR_FIRST`, `SCORE_DESCENDING`, `SMALLEST_PARTY_FIRST`).
   - **Universe C (Concurrent):** Hundreds of real `Thread`/`Runnable` workers synchronized on a virtual `10:00:00 AM` `TatkalVirtualClock` (`wait()`/`notifyAll()`), competing inside `synchronized` critical sections monitored by daemon `ThreadExecutionMonitor`.
2. **Booking Crime Scene Reconstruction & Replay:** Every booking logs millisecond-level events to SQLite (`Booking_Events`) and `logs/booking_events.log` (`FileWriter`/`FileReader`), allowing full timeline reconstruction and step-by-step replay by `Booking ID`.
3. **Booking Failure Lab (8 Scenarios):** Fault injection for last-seat competition, post-hold payment crashes, booking timeouts, duplicate booking attacks, thread interruption, simulated SQLite connection failure, cancellation during concurrent booking, and 121-thread shared inventory contention.
4. **30-Table SQLite Database (`tatkal_x.db`):** Auto-initialized via raw JDBC (`PreparedStatement`, `ResultSet`, WAL mode, foreign keys, atomic transactions) plus `DatabaseConsistencyMonitor`.
5. **Stream API Analytics & File I/O:** Lambda/Stream pipelines (`filter`, `map`, `sorted`, `collect`, method references), `FileReader` + `StringTokenizer` CSV import with row validation, `FileWriter` CSV exports, and `FileInputStream`/`FileOutputStream` byte-stream backups.

---

## Project Structure
- `src/com/tatkalx/Main.java` — Application bootstrapper & terminal portal entry point
- `src/com/tatkalx/model/` — Domain POJOs (`User`, `Passenger`, `Admin`, `Train`, `Coach`, `Berth`, `SeatInventory`, `BookingRequest`, `Ticket`, `PnrRecord`, etc.)
- `src/com/tatkalx/exception/` — Custom checked (`TatkalException`) & unchecked (`TatkalRuntimeException`) hierarchy
- `src/com/tatkalx/collections/` — `ArrayList`, `LinkedList`, `HashSet`, `LinkedHashSet`, `TreeSet`, `HashMap`, `LinkedHashMap`, `TreeMap`, `PriorityQueue`, `Deque`, `Iterator`, `ListIterator`, `GenericRepository<T extends Comparable<T>>`
- `src/com/tatkalx/service/` — `TatkalVirtualClock`, `BerthAllocationService`, `QueuePromotionService`, `CancellationRefundService`, `PaymentService`, `FifoBookingProcessor`, `PriorityBookingProcessor`
- `src/com/tatkalx/concurrency/` — `BookingWorkerTask`, `ConcurrentBookingCoordinator`, `ThreadExecutionMonitor`, `BookingTimeoutMonitor`, `RaceConditionDetector`, `ProducerConsumerBookingBuffer`
- `src/com/tatkalx/simulation/` — `SequentialUniverse`, `PriorityUniverse`, `ConcurrentUniverse`, `WorkloadGenerator`, `StrategyComparisonEngine`
- `src/com/tatkalx/database/` — `DatabaseConnectionManager`, `DatabaseInitializer`, `DatabaseConsistencyMonitor`
- `src/com/tatkalx/dao/` — `GenericDao<T, ID>` and 7 SQLite JDBC DAOs
- `src/com/tatkalx/investigation/` — `ForensicReconstructor`, `BookingExecutionLogger`, `BookingReplayEngine`, `FailureLabService`
- `src/com/tatkalx/filehandling/` — `CsvImportExportService`, `FileLogManager`, `BinaryAuditBackupService`
- `src/com/tatkalx/reporting/` — `StreamAnalyticsService`, `ReportFormatter`
- `src/com/tatkalx/controller/` — `TatkalAppController`, `PassengerController` (14+ options), `AdminController` (24 options)

---

## Setup, Compile & Run Instructions

### Prerequisites
- **Java JDK 17+** (tested on JDK 26)
- **SQLite JDBC JARs** included in `lib/`:
  - `lib/sqlite-jdbc-3.46.1.0.jar`
  - `lib/slf4j-api-2.0.16.jar`
  - `lib/slf4j-nop-2.0.16.jar`

### Compile the Entire Project (Windows PowerShell)
```powershell
New-Item -ItemType Directory -Force -Path out | Out-Null
javac -encoding UTF-8 -d out -cp "lib/*" -sourcepath src src\com\tatkalx\Main.java src\com\tatkalx\Phase10IntegrationTest.java
```
*(If Oracle's `javapath` shim hangs in your terminal, invoke your JDK directly, e.g. `& "C:\Program Files\Java\jdk-26.0.1\bin\javac.exe"`.)*

### Run the Full Interactive Application
```powershell
java --enable-native-access=ALL-UNNAMED -cp "out;lib/*" com.tatkalx.Main
```

### Run the Automated Integration Test Suite
```powershell
java --enable-native-access=ALL-UNNAMED -cp "out;lib/*" com.tatkalx.Phase10IntegrationTest
```

### Default Login Credentials
- **Passenger:** `aarav_99` / `pass123` (or `meera_k` / `pass123`)
- **Administrator:** `admin_tx` / `admin123`
