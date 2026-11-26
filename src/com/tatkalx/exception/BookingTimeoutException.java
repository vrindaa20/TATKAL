package com.tatkalx.exception;

/**
 * Checked exception thrown when a booking or payment exceeds the allowed Tatkal timeout threshold.
 */
public class BookingTimeoutException extends TatkalException {

    public BookingTimeoutException(String message) {
        super("ERR_BOOKING_TIMEOUT", message);
    }
}
