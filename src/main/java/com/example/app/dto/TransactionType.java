package com.example.app.dto;

public enum TransactionType {
    DEPOSIT("Deposit"),
    WITHDRAW("Withdraw"),
    TRANSFER("Transfer");
    private final String type;

    TransactionType(String type) {
        this.type = type;
    }
}