package com.tatkalx.exception;

/**
 * Checked exception thrown when user input or booking request fields fail validation.
 */
public class BookingValidationException extends TatkalException {

    public BookingValidationException(String message) {
        super("ERR_VALIDATION", message);
    }
}
