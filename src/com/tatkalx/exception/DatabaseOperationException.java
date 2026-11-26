package com.tatkalx.exception;

/**
 * Unchecked exception thrown when a JDBC / SQLite database operation fails.
 */
public class DatabaseOperationException extends TatkalRuntimeException {

    public DatabaseOperationException(String message) {
        super("ERR_DB_OPERATION", message);
    }

    public DatabaseOperationException(String message, Throwable cause) {
        super("ERR_DB_OPERATION", message, cause);
    }
}
