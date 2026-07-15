package com.example.app.entity;

import com.example.app.dto.Status;
import com.example.app.dto.TransactionType;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

@Entity
@Getter
@Setter
@NoArgsConstructor
public class Transactions {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long transactionId;

    @Column(nullable = false)
    private long clientId;//Foreign key from Client entity

    @Column(nullable = false,updatable = false)
    private Instant timestamp;

    @Column(nullable = false)
    private Double amount;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TransactionType type;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Status status;

    @PrePersist
    protected void onCreate() {
        this.timestamp = Instant.now();
    }

    public Transactions(long clientId, Double amount, TransactionType type, Status status) {
        this.clientId = clientId;
        this.amount = amount;
        this.type = type;
        this.status = status;
    }

}
