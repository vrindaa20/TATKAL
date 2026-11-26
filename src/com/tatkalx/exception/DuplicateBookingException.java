package com.tatkalx.exception;

/**
 * Checked exception thrown when a passenger attempts a duplicate booking on the same schedule.
 */
public class DuplicateBookingException extends TatkalException {

    private final String duplicateKey;

    public DuplicateBookingException(String duplicateKey, String message) {
        super("ERR_DUPLICATE_BOOKING", message);
        this.duplicateKey = duplicateKey;
    }

    public String getDuplicateKey() {
        return duplicateKey;
    }
}
