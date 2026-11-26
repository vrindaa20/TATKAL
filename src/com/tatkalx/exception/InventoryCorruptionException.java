package com.tatkalx.exception;

/**
 * Unchecked exception thrown if database or seat inventory invariants are violated.
 */
public class InventoryCorruptionException extends TatkalRuntimeException {

    public InventoryCorruptionException(String message) {
        super("ERR_INVENTORY_CORRUPT", message);
    }
}
