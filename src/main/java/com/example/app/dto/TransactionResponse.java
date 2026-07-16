package com.example.app.dto;

import com.example.app.entity.Transactions;
import lombok.Getter;

import java.time.Instant;

@Getter
public class TransactionResponse {
    private final Long transactionId;
    private final Instant timestamp;
    private final double amount;
    private final TransactionType transactionType;
    private final BalanceType balanceType;
    private final Status status;
    public TransactionResponse(Transactions transaction) {
        this.transactionId = transaction.getTransactionId();
        this.timestamp = transaction.getTimestamp();
        this.amount = transaction.getAmount();
        this.transactionType = transaction.getTransactionType();
        this.balanceType=transaction.getBalanceType();
        this.status = transaction.getStatus();
    }
}