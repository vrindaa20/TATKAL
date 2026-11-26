package com.tatkalx;

import com.tatkalx.controller.TatkalAppController;
import com.tatkalx.controller.TerminalSessionContext;

/**
 * Main entry point for TATKAL-X: The Railway Booking Race & Forensic Intelligence Simulator.
 */
public class Main {

    public static void main(String[] args) {
        TerminalSessionContext context = new TerminalSessionContext();
        TatkalAppController app = new TatkalAppController(context);
        app.start();
    }
}
