package com.example.app.exception;

import lombok.Getter;

@Getter
public class WalletException extends Exception {

    private final String errorCode;

    public WalletException(String errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
    }
}