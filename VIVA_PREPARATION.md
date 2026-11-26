# TATKAL-X University Viva Preparation Guide

### 1. Why did you create three Booking Universes instead of a single booking loop?
To empirically measure how processing strategy affects Tatkal outcomes on identical data:
- **Universe A (`SequentialUniverse`):** Single-threaded `ArrayList` + `Queue` + `Iterator` FIFO baseline.
- **Universe B (`PriorityUniverse`):** `PriorityQueue` + `Comparator` (`MEDICAL > SENIOR_CITIZEN > DEFENCE > NORMAL`), showing how priority policies reorder who receives scarce berths.
- **Universe C (`ConcurrentUniverse`):** Hundreds of real `Thread`/`Runnable` workers released simultaneously at `10:00:00 AM` via `wait()`/`notifyAll()`, proving thread-safe synchronization is faster in wall time while incurring lock contention (`RaceConditionDetector`).

### 2. How do you prevent race conditions and double-booked berths without `ReentrantLock` or `AtomicInteger`?
Using core Java intrinsic monitor locks (`synchronized (inventory.getMonitorLock())`):
- The check-then-act sequence (`availableTatkalSeats >= needed` -> pick `Berth` -> decrement `availableTatkalSeats` -> increment `confirmedCount`) executes atomically inside `synchronized`.
- Payment runs outside the inventory lock so gateway latency doesn't stall other threads; if payment fails or times out, `rollbackProvisionalHold()` re-acquires the lock and restores the berth.
- `DuplicateBookingGuard` uses a synchronized `HashSet<String>` (`sessionId:scheduleId:passengerId`) to block concurrent double bookings.

### 3. How are `wait()` and `notifyAll()` used in TATKAL-X?
1. **`TatkalVirtualClock.awaitWindowOpen()`:** All worker threads start at `09:59:59` and wait inside a `while (BEFORE_WINDOW)` guarded block (`WAITING` state). When `openWindowNow()` runs at `10:00:00`, `notifyAll()` wakes every thread simultaneously.
2. **`ProducerConsumerBookingBuffer`:** Producer waits while `buffer.size() >= capacity`; consumers wait while `buffer.isEmpty()`; `notifyAll()` coordinates handoffs.

### 4. Why did you choose specific Collections in `com.tatkalx.collections`?
- **`ArrayList` + `HashMap` (`PassengerRegistry`):** Fast indexed iteration/sorting + `O(1)` lookup by ID.
- **`HashSet` + `LinkedHashSet` (`DuplicateBookingGuard`):** `O(1)` duplicate detection + ordered forensic log of blocked attempts.
- **`TreeSet` (`PnrDirectory`):** Automatically keeps PNRs sorted (`Comparable<PnrRecord>`) for `subSet()` range queries.
- **`LinkedHashMap` + `TreeMap` (`BookingHistoryCache`):** Preserves insertion order while bucketing sorted timestamp slices.
- **`LinkedList` + `ListIterator` (`WaitingListManager`):** `O(1)` head promotion (`RAC-1 -> CONFIRMED`, `WL-1 -> RAC`) and safe mid-queue removal + sequential renumbering.
- **`PriorityQueue` (`PriorityUniverse`) & `Deque` (`RequestDequeManager`):** Configurable comparator ordering and head/tail buffering.

### 5. How does Booking Crime Scene Reconstruction work?
`BerthAllocationService` records real `BookingEvent` snapshots (timestamp, thread name, event sequence, available seats, queue position, allocation/payment outcome). They are persisted to SQLite (`Booking_Events`) and `logs/booking_events.log` (`FileWriter`). `ForensicReconstructor` queries SQLite or parses the log via `FileReader` + `StringTokenizer`, and `BookingReplayEngine` replays each event step by step.

### 6. How do you handle SQLite's single-writer limitation under multithreading?
`DatabaseConnectionManager` enables `PRAGMA journal_mode = WAL;` and `PRAGMA busy_timeout = 5000;`, and routes all multi-table writes through a synchronized `writeGateLock` with `conn.setAutoCommit(false)` -> `commit()` / `rollback()`.
