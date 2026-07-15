package com.example.app.dto;

import com.example.app.entity.Transactions;
import lombok.Getter;

import java.time.Instant;

@Getter
public class TransactionResponse {
    private Long transactionId;
    private Instant timestamp;
    private double amount;
    private TransactionType type;
    private Status status;
    public TransactionResponse(Transactions transaction) {
        this.transactionId = transaction.getTransactionId();
        this.timestamp = transaction.getTimestamp();
        this.amount = transaction.getAmount();
        this.type = transaction.getType();
        this.status = transaction.getStatus();
    }
}