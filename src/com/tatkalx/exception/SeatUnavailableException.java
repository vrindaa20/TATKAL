package com.tatkalx.exception;

/**
 * Checked exception thrown when confirmed, RAC, and waiting list quotas are all exhausted.
 */
public class SeatUnavailableException extends TatkalException {

    public SeatUnavailableException(String message) {
        super("ERR_NO_SEATS", message);
    }
}
