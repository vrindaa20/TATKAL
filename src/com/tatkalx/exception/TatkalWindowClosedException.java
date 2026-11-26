package com.tatkalx.exception;

/**
 * Checked exception thrown when a booking is attempted before 10:00 AM or after window closure.
 */
public class TatkalWindowClosedException extends TatkalException {

    public TatkalWindowClosedException(String message) {
        super("ERR_WINDOW_CLOSED", message);
    }
}
