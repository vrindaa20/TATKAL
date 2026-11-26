package com.tatkalx.exception;

/**
 * Checked exception thrown on invalid login credentials or unauthorized role actions.
 */
public class AuthenticationException extends TatkalException {

    public AuthenticationException(String message) {
        super("ERR_AUTH_FAILED", message);
    }
}
