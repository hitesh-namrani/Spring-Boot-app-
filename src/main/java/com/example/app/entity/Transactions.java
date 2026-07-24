package com.example.app.entity;

import com.example.app.dto.BalanceType;
import com.example.app.dto.Status;
import com.example.app.dto.TransactionType;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

//Represents a wallet transaction record.
@Entity
@Getter
@Setter
@NoArgsConstructor
public class Transactions {
    // Unique identifier for the transaction.
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long transactionId;

    // Identifier of the user associated with this transaction.
    @Column(nullable = false)
    private long userId;//Foreign key from User entity

    // Time when the transaction was created.
    @Column(nullable = false, updatable = false)
    private Instant timestamp;

    // Amount involved in the transaction.
    @Column(nullable = false)
    private Double amount;

    // Type of transaction performed.
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TransactionType transactionType;

    // Balance category affected by the transaction.
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private BalanceType balanceType;

    // Result status of the transaction.
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Status status;

    @Column (nullable = true)
    private Long receiverId;

    // Automatically sets the transaction timestamp before saving.
    @PrePersist
    protected void onCreate() {
        this.timestamp = Instant.now();
    }

    // Creates a transaction record with the provided details.
    public Transactions(long userId, Double amount, TransactionType transactionType, BalanceType balanceType, Status status) {
        this.userId = userId;
        this.amount = amount;
        this.transactionType = transactionType;
        this.balanceType = balanceType;
        this.status = status;
        this.receiverId = null;
    }
    public Transactions(long userId, Double amount, TransactionType transactionType, BalanceType balanceType, Status status,Long receiverId) {
        this.userId = userId;
        this.amount = amount;
        this.transactionType = transactionType;
        this.balanceType = balanceType;
        this.status = status;
        this.receiverId=receiverId;
    }

}
