package com.tatkalx.controller;

import com.tatkalx.utility.ConsoleFormatter;

/**
 * Top-level Terminal Portal router connecting Passenger Portal, Administrator Panel,
 * and system diagnostics.
 */
public class TatkalAppController {

    private final TerminalSessionContext context;
    private final PassengerController passengerController;
    private final AdminController adminController;

    public TatkalAppController(TerminalSessionContext context) {
        this.context = context;
        this.passengerController = new PassengerController(context);
        this.adminController = new AdminController(context);
    }

    public void start() {
        boolean running = true;
        while (running) {
            ConsoleFormatter.printBanner("TATKAL-X | THE RAILWAY BOOKING RACE & FORENSIC SIMULATOR");
            System.out.println("  Virtual Clock: " + context.getLiveClock().getCountdownBanner());
            System.out.println("  Live Inventory: " + context.getLiveSessionContext().getSeatInventory());
            ConsoleFormatter.printDivider();
            System.out.println("  1. Passenger Portal (14+ Operations)");
            System.out.println("  2. Administrator Control & Forensic Panel (24 Operations)");
            System.out.println("  3. Run Quick Database Consistency Audit");
            System.out.println("  0. Exit TATKAL-X");
            ConsoleFormatter.printDivider();

            int choice = context.readInt("  Select Portal: ");
            switch (choice) {
                case 1:
                    passengerController.showMenu();
                    break;
                case 2:
                    adminController.showMenu();
                    break;
                case 3:
                    context.getConsistencyMonitor().runFullConsistencyAudit().getChecks()
                            .forEach(c -> System.out.printf("  [%-4s] %s (%s)%n",
                                    c.isPassed() ? "PASS" : "FAIL", c.getCheckName(), c.getDetail()));
                    break;
                case 0:
                    running = false;
                    System.out.println("\n  Shutting down TATKAL-X cleanly. Goodbye!");
                    break;
                default:
                    System.out.println("  [!] Invalid choice. Enter 0, 1, 2, or 3.");
            }
        }
    }
}
