package com.example.app.dao;

import com.example.app.entity.Transactions;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.Instant;
import java.util.List;

public interface TransactionsRepository extends JpaRepository<Transactions, Long> {
    // 1. Find all transactions by a user, sorted latest to oldest
    List<Transactions> findByClientIdOrderByTimestampDesc(long clientId);
}
