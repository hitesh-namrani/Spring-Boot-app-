package com.example.app.dto;

public enum TransactionType {
    Deposit("Deposit"),
    Withdraw("Withdraw");
    private final String type;
    TransactionType(String type) {
        this.type = type;
    }
}
