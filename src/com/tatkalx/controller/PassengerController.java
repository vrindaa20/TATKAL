package com.tatkalx.controller;

import com.tatkalx.exception.BookingValidationException;
import com.tatkalx.model.Berth;
import com.tatkalx.model.BookingRequest;
import com.tatkalx.model.Notification;
import com.tatkalx.model.Passenger;
import com.tatkalx.model.PnrRecord;
import com.tatkalx.model.Refund;
import com.tatkalx.model.Route;
import com.tatkalx.model.SeatInventory;
import com.tatkalx.model.Ticket;
import com.tatkalx.model.TrainSchedule;
import com.tatkalx.model.TrainStop;
import com.tatkalx.model.User;
import com.tatkalx.service.BookingOutcome;
import com.tatkalx.simulation.SimulationSessionContext;
import com.tatkalx.utility.ConsoleFormatter;
import com.tatkalx.utility.IdGenerator;
import com.tatkalx.utility.InputValidator;
import com.tatkalx.utility.TimeUtils;

import java.util.List;

/**
 * Handles all 14 Passenger Menu operations in the terminal UI.
 */
public class PassengerController {

    private final TerminalSessionContext app;

    public PassengerController(TerminalSessionContext app) {
        this.app = app;
    }

    public void showMenu() {
        boolean running = true;
        while (running) {
            Passenger current = app.getLoggedInPassenger();
            String userBadge = current == null
                    ? "Guest (Not Logged In)"
                    : current.getFullName() + " (ID:" + current.getPassengerId()
                      + ", Wallet: Rs " + String.format("%.2f", current.getWalletBalance()) + ")";

            ConsoleFormatter.printBanner("TATKAL-X PASSENGER PORTAL | " + userBadge);
            System.out.println("  1.  Register                     8.  Check PNR");
            System.out.println("  2.  Login                        9.  View RAC Status");
            System.out.println("  3.  Search Trains               10.  View Waiting List");
            System.out.println("  4.  View Routes                 11.  Cancel Ticket");
            System.out.println("  5.  Check Seat Availability     12.  Check Refund Status");
            System.out.println("  6.  View Tatkal Countdown       13.  View Booking History");
            System.out.println("  7.  Book Ticket                 14.  View Notifications");
            System.out.println("  15. Manage / Verify Profile      0.  Back to Main Portal");
            ConsoleFormatter.printDivider();

            int choice = app.readInt("  Select option: ");
            switch (choice) {
                case 1: handleRegister(); break;
                case 2: handleLogin(); break;
                case 3: handleSearchTrains(); break;
                case 4: handleViewRoutes(); break;
                case 5: handleCheckAvailability(); break;
                case 6: handleTatkalCountdown(); break;
                case 7: handleBookTicket(); break;
                case 8: handleCheckPnr(); break;
                case 9: handleViewRacStatus(); break;
                case 10: handleViewWaitingList(); break;
                case 11: handleCancelTicket(); break;
                case 12: handleCheckRefundStatus(); break;
                case 13: handleViewBookingHistory(); break;
                case 14: handleViewNotifications(); break;
                case 15: handleProfileAndVerification(); break;
                case 0: running = false; break;
                default: System.out.println("  [!] Invalid option. Choose 0..15.");
            }
        }
    }

    private void handleRegister() {
        ConsoleFormatter.printSubHeader("1. Passenger Registration");
        try {
            String username = app.readLine("  Username: ");
            String password = app.readLine("  Password: ");
            String fullName = app.readLine("  Full Name: ");
            String email = app.readLine("  Email: ");
            String phone = app.readLine("  Phone (10 digits): ");
            int age = app.readInt("  Age: ");
            String gender = app.readLine("  Gender (M/F/O): ").toUpperCase();
            String idType = app.readLine("  ID Proof Type (AADHAAR/PAN/PASSPORT/VOTER_ID): ").toUpperCase();
            String idNumber = app.readLine("  ID Proof Number: ");
            String category = app.readLine("  Priority Category (NORMAL/SENIOR_CITIZEN/MEDICAL/DEFENCE): ").toUpperCase();
            String pref = app.readLine("  Berth Preference (LB/MB/UB/SL/SU/ANY): ").toUpperCase();

            InputValidator.validatePassengerRegistration(username, fullName, email, phone, age, idNumber);
            Passenger pax = new Passenger(0, 0, username, fullName, email, phone,
                    age, gender, idType, idNumber, true, category, pref, 15000.0);
            pax.setPasswordHash(password.isEmpty() ? "pass123" : password);

            int paxId = app.getUserPassengerDao().insert(pax);
            app.getLiveSessionContext().getPassengerRegistry().registerPassenger(pax);
            app.setLoggedInPassenger(pax);
            System.out.println("  [OK] Registered & verified Passenger ID=" + paxId + " (" + fullName + ").");
        } catch (BookingValidationException | RuntimeException ex) {
            System.out.println("  [ERROR] Registration failed: " + ex.getMessage());
        }
    }

    private void handleLogin() {
        ConsoleFormatter.printSubHeader("2. Passenger Login (Demo: aarav_99 / pass123)");
        String username = app.readLine("  Username: ");
        String password = app.readLine("  Password: ");
        User user = app.getUserPassengerDao().authenticate(username, password);
        if (user instanceof Passenger) {
            app.setLoggedInPassenger((Passenger) user);
            System.out.println("  [OK] Logged in as " + user.getFullName() + " | " + user.getRoleDescription());
        } else {
            System.out.println("  [!] Invalid passenger credentials.");
        }
    }

    private void handleSearchTrains() {
        ConsoleFormatter.printSubHeader("3. Search Available Train Schedules");
        String query = app.readLine("  Filter by train number/name (or press Enter for all): ").toLowerCase();
        List<TrainSchedule> schedules = app.getRailwayDao().findAllSchedules();
        for (TrainSchedule ts : schedules) {
            if (query.isEmpty() || ts.toString().toLowerCase().contains(query)) {
                System.out.printf("  Schedule #%d | %s | Tatkal Window: %s - %s | Status: %s%n",
                        ts.getScheduleId(), ts, ts.getTatkalOpenTime(), ts.getTatkalCloseTime(), ts.getScheduleStatus());
            }
        }
    }

    private void handleViewRoutes() {
        ConsoleFormatter.printSubHeader("4. Train Routes & Stops");
        List<Route> routes = app.getRailwayDao().findAllRoutes();
        for (Route r : routes) {
            System.out.println("  Route: " + r.getRouteCode() + " (Total " + r.getTotalDistanceKm() + " km)");
            for (TrainStop stop : r.getStops()) {
                System.out.println("    -> " + stop);
            }
        }
    }

    private void handleCheckAvailability() {
        ConsoleFormatter.printSubHeader("5. Live Tatkal Seat Availability");
        long scheduleId = app.readLong("  Enter Schedule ID (default 1): ");
        String coachClass = app.readLine("  Enter Coach Class (3A/2A): ").toUpperCase();
        app.hydrateLiveContext(scheduleId, coachClass);
        SeatInventory inv = app.getLiveSessionContext().getSeatInventory();
        System.out.println("  " + inv);
    }

    private void handleTatkalCountdown() {
        ConsoleFormatter.printSubHeader("6. Tatkal Virtual Clock Status");
        System.out.println("  " + app.getLiveClock().getCountdownBanner());
        if (!app.getLiveClock().isBookingAllowed()) {
            String openNow = app.readLine("  Open 10:00 AM Tatkal window now for testing? (y/n): ");
            if ("y".equalsIgnoreCase(openNow)) {
                app.getLiveClock().openWindowNow();
                System.out.println("  [OK] " + app.getLiveClock().getCountdownBanner());
            }
        }
    }

    private void handleBookTicket() {
        ConsoleFormatter.printSubHeader("7. Book Tatkal Ticket");
        Passenger pax = ensureLoggedInPassenger();
        if (pax == null) {
            return;
        }
        long scheduleId = app.readLong("  Schedule ID (e.g. 1): ");
        String coachClass = app.readLine("  Coach Class (3A/2A): ").toUpperCase();
        int seats = app.readInt("  Number of Seats (1..4): ");
        String method = app.readLine("  Payment Method (UPI/WALLET/CARD/NETBANKING): ").toUpperCase();

        app.hydrateLiveContext(scheduleId, coachClass);
        SimulationSessionContext ctx = app.getLiveSessionContext();

        String bookingId = IdGenerator.nextBookingId();
        BookingRequest req = new BookingRequest(
                bookingId, ctx.getSessionId(), pax.getPassengerId(), pax.getFullName(),
                pax.getAge(), pax.getPriorityCategory(), scheduleId, coachClass,
                seats, TimeUtils.nowTimeWithMillis());

        BookingOutcome outcome = app.getAllocationService().allocateAndConfirm(req, ctx, method);
        List<Berth> assignedBerths = outcome.getTicket() == null
                ? null : ctx.getSeatInventory().getAllocatedBerthsByTicket().get(outcome.getTicket().getTicketId());
        app.getBookingDao().persistBookingOutcomeAtomically(outcome, pax, ctx.getSeatInventory(), assignedBerths);
        app.getExecutionLogger().logBookingEventsToFile(outcome.getEvents());
        if ("WALLET".equalsIgnoreCase(method) && outcome.isSuccess()) {
            app.getUserPassengerDao().update(pax);
        }

        if (outcome.isSuccess()) {
            System.out.println("  [CONFIRMED/QUEUED] Booking ID: " + bookingId
                    + " | PNR: " + outcome.getPnrRecord().getPnrNumber()
                    + " | Status: " + outcome.getOutcomeStatus()
                    + " | Seat: " + outcome.getTicket().getSeatLabel());
        } else {
            System.out.println("  [REJECTED] Booking ID: " + bookingId
                    + " | Status: " + outcome.getOutcomeStatus()
                    + " | Reason: " + outcome.getMessage());
        }
    }

    private void handleCheckPnr() {
        ConsoleFormatter.printSubHeader("8. Check PNR Status");
        String pnrNum = app.readLine("  Enter 10-digit PNR: ");
        PnrRecord pnr = app.getBookingDao().findPnrByNumber(pnrNum);
        if (pnr == null) {
            System.out.println("  [!] PNR not found: " + pnrNum);
        } else {
            System.out.println("  " + pnr);
        }
    }

    private void handleViewRacStatus() {
        ConsoleFormatter.printSubHeader("9. Live RAC Queue Status");
        List<Ticket> rac = app.getLiveSessionContext().getWaitingListManager().getRacSnapshot();
        if (rac.isEmpty()) {
            System.out.println("  RAC queue is currently empty.");
        } else {
            for (Ticket t : rac) {
                System.out.println("  " + t);
            }
        }
    }

    private void handleViewWaitingList() {
        ConsoleFormatter.printSubHeader("10. Live Waiting List Status");
        List<Ticket> wl = app.getLiveSessionContext().getWaitingListManager().getWlSnapshot();
        if (wl.isEmpty()) {
            System.out.println("  Waiting List is currently empty.");
        } else {
            for (Ticket t : wl) {
                System.out.println("  " + t);
            }
        }
    }

    private void handleCancelTicket() {
        ConsoleFormatter.printSubHeader("11. Cancel Ticket");
        String pnrNum = app.readLine("  Enter PNR Number to cancel: ");
        String reason = app.readLine("  Cancellation Reason: ");
        try {
            SimulationSessionContext ctx = app.getLiveSessionContext();
            PnrRecord pnr = ctx.getPnrDirectory().lookupPnr(pnrNum);
            if (pnr == null) {
                System.out.println("  [!] PNR not found in live session.");
                return;
            }
            Ticket ticket = ctx.findTicketById(pnr.getTicketId());
            Passenger owner = ctx.getPassengerRegistry().findById(ticket.getPassengerId());
            Refund refund = app.getCancellationService().cancelTicketByPnr(pnrNum, reason, false, ctx);
            com.tatkalx.model.Cancellation cancelModel = new com.tatkalx.model.Cancellation(
                    refund.getCancellationId(), ticket.getTicketId(), ctx.getSessionId(),
                    owner == null ? 1 : owner.getUserId(), "CONFIRMED", reason, TimeUtils.nowTimestamp());
            app.getFinancialDao().persistCancellationAndRefundAtomically(
                    cancelModel, refund, ticket.getTicketId(), pnrNum,
                    ctx.getSeatInventory(), owner == null ? 1 : owner.getUserId(),
                    owner == null ? 0.0 : owner.getWalletBalance());
            if (owner != null) {
                app.getUserPassengerDao().update(owner);
            }
            System.out.println("  [OK] Cancelled PNR " + pnrNum + " -> " + refund);
        } catch (BookingValidationException ex) {
            System.out.println("  [ERROR] " + ex.getMessage());
        }
    }

    private void handleCheckRefundStatus() {
        ConsoleFormatter.printSubHeader("12. Refund Status Ledger");
        List<Refund> refunds = app.getFinancialDao().findAllRefunds();
        if (refunds.isEmpty()) {
            System.out.println("  No refunds recorded yet.");
        } else {
            for (Refund r : refunds) {
                System.out.println("  " + r);
            }
        }
    }

    private void handleViewBookingHistory() {
        ConsoleFormatter.printSubHeader("13. Passenger Booking History");
        Passenger pax = ensureLoggedInPassenger();
        if (pax == null) {
            return;
        }
        List<Ticket> tickets = app.getBookingDao().findTicketsByPassenger(pax.getPassengerId());
        if (tickets.isEmpty()) {
            System.out.println("  No tickets booked yet for " + pax.getFullName());
        } else {
            for (Ticket t : tickets) {
                System.out.println("  " + t);
            }
        }
    }

    private void handleViewNotifications() {
        ConsoleFormatter.printSubHeader("14. Passenger Notifications");
        Passenger pax = ensureLoggedInPassenger();
        if (pax == null) {
            return;
        }
        List<Notification> memList = app.getNotificationService().getNotificationsForPassenger(pax.getPassengerId());
        List<Notification> dbList = app.getSimulationDao().findNotificationsByPassenger(pax.getPassengerId());
        if (memList.isEmpty() && dbList.isEmpty()) {
            System.out.println("  No notifications yet.");
        } else {
            for (Notification n : memList) {
                System.out.println("  " + n);
            }
            for (Notification n : dbList) {
                System.out.println("  " + n);
            }
        }
    }

    private void handleProfileAndVerification() {
        ConsoleFormatter.printSubHeader("15. Passenger Profile & Verification");
        Passenger pax = ensureLoggedInPassenger();
        if (pax == null) {
            return;
        }
        System.out.println("  Current Profile: " + pax);
        String newPref = app.readLine("  New Berth Preference (LB/MB/UB/SL/SU/ANY, Enter to keep): ");
        if (!newPref.isEmpty()) {
            pax.setBerthPreference(newPref.toUpperCase());
        }
        pax.setVerified(true);
        app.getUserPassengerDao().update(pax);
        System.out.println("  [OK] Profile updated & verified.");
    }

    private Passenger ensureLoggedInPassenger() {
        if (app.getLoggedInPassenger() == null) {
            System.out.println("  [!] Please login first (Option 2) or register (Option 1).");
        }
        return app.getLoggedInPassenger();
    }
}
