package com.example.app.dto;

import com.example.app.entity.Transactions;
import com.example.app.entity.User;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Getter;

import java.time.Instant;

@Getter
@JsonInclude(JsonInclude.Include.NON_NULL)
public class TransactionResponse {
    private final Long transactionId;
    private final Instant timestamp;
    private final double amount;
    private final TransactionType transactionType;
    private final BalanceType balanceType;
    private final Status status;
    private final String transferDirection;

    public TransactionResponse(Transactions transaction, User user) {
        this.transactionId = transaction.getTransactionId();
        this.timestamp = transaction.getTimestamp();
        this.amount = transaction.getAmount();
        this.transactionType = transaction.getTransactionType();
        this.balanceType = transaction.getBalanceType();
        this.status = transaction.getStatus();
        if(transaction.getTransactionType()==TransactionType.TRANSFER){
            if(transaction.getUserId()==user.getId()){
                this.transferDirection="SENT";
            }
            else{
                this.transferDirection="RECEIVED";
            }
        }
        else{
            transferDirection=null;
        }
    }
}