package com.tatkalx.exception;

/**
 * Checked base exception for all business-rule and domain errors in TATKAL-X.
 */
public class TatkalException extends Exception {

    private final String errorCode;

    public TatkalException(String errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
    }

    public TatkalException(String errorCode, String message, Throwable cause) {
        super(message, cause);
        this.errorCode = errorCode;
    }

    public String getErrorCode() {
        return errorCode;
    }
}
