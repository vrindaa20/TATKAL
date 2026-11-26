package com.tatkalx.exception;

/**
 * Unchecked base exception for unexpected infrastructure or internal invariant failures.
 */
public class TatkalRuntimeException extends RuntimeException {

    private final String errorCode;

    public TatkalRuntimeException(String errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
    }

    public TatkalRuntimeException(String errorCode, String message, Throwable cause) {
        super(message, cause);
        this.errorCode = errorCode;
    }

    public String getErrorCode() {
        return errorCode;
    }
}
