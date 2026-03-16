package com.vaultx.exception;

public class VaultXException extends Exception {
    public VaultXException(String message) {
        super(message);
    }

    public VaultXException(String message, Throwable cause) {
        super(message, cause);
    }
}
