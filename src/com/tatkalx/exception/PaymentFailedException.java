package com.tatkalx.exception;

/**
 * Checked exception thrown when payment gateway authorization fails or wallet funds are insufficient.
 */
public class PaymentFailedException extends TatkalException {

    public PaymentFailedException(String message) {
        super("ERR_PAYMENT_FAILED", message);
    }
}
