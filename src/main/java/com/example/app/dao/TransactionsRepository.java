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

    @Query("SELECT t FROM Transactions t WHERE t.userId = :userId OR t.receiverId = :userId ORDER BY t.timestamp DESC")
    List<Transactions> findByUserIdOrderByTimestampDesc(long userId);

    // Calculates today's total amount for a specific transaction and balance type.
    @Query("SELECT COALESCE(SUM(t.amount), 0) FROM Transactions t WHERE t.userId = :userId AND t.transactionType = :transactionType AND t.balanceType = :balanceType AND t.timestamp >= :startOfDay")
    Double getDailyTransactionSum(
            @Param("userId") Long userId,
            @Param("transactionType") TransactionType transactionType,
            @Param("balanceType") BalanceType balanceType,
            @Param("startOfDay") Instant startOfDay
    );
}
