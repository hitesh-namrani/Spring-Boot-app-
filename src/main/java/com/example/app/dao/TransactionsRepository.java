package com.example.app.dao;

import com.example.app.dto.BalanceType;
import com.example.app.dto.TransactionType;
import com.example.app.entity.Transactions;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;

public interface TransactionsRepository extends JpaRepository<Transactions, Long> {
    // 1. Find all transactions by a user, sorted latest to oldest
    List<Transactions> findByClientIdOrderByTimestampDesc(long clientId);
    @Query("SELECT COALESCE(SUM(t.amount), 0) FROM Transactions t WHERE t.clientId = :clientId AND t.transactionType = :transactionType AND t.balanceType = :balanceType AND t.timestamp >= :startOfDay")
    Double getDailyTransactionSum(
            @Param("clientId") Long clientId,
            @Param("transactionType") TransactionType transactionType,
            @Param("balanceType") BalanceType balanceType,
            @Param("startOfDay") Instant startOfDay
    );
}
