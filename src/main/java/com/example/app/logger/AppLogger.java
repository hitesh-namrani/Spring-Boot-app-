package com.example.app.logger;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/*
Utility class responsible for logging application events
such as transactions and errors.
Uses the SLF4J logging framework for consistent logging.
*/

@Component
public class AppLogger {

    // Creates a logger instance for this class.
    private static final Logger logger = LoggerFactory.getLogger(AppLogger.class);

    /*
    Logs successful transaction-related activities.
    @param username Username of the client performing the transaction
    @param action   Type of transaction (REGISTER, DEPOSIT, WITHDRAWAL, etc.)
    @param amount   Transaction amount
    */
    public void logTransaction(String username, String action, double amount) {
        // Record transaction details at INFO level.
        logger.info("TRANSACTION | User: {} | Action: {} | Amount: {}", username, action, amount);
    }
    /*
    Logs application errors.
    @param action       Operation during which the error occurred
    @param errorMessage Description of the error
    */
    public void logError(String action, String errorMessage) {
        // Record error details at ERROR level.
        logger.error("ERROR | Action: {} | Reason: {}", action, errorMessage);
    }
}
