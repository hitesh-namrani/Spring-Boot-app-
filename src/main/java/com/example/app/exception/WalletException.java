package com.example.app.exception;

import lombok.Getter;
/*
Custom exception class for wallet-related errors.
Stores an application-specific error code along with the exception message.
*/

@Getter
public class WalletException extends Exception {
    // Application-specific error code that identifies the type of wallet exception.

    private final String errorCode;
    /*
    Creates a new WalletException with the specified
    error code and error message.
    param errorCode the application-specific error code
    param message the detailed error message
    */

    public WalletException(String errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
    }
}
