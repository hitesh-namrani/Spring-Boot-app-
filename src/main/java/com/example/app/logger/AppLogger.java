package com.example.app.logger;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class AppLogger {

    private static final Logger logger = LoggerFactory.getLogger(AppLogger.class);

    public void logTransaction(String username, String action, double amount) {
        logger.info("TRANSACTION | User: {} | Action: {} | Amount: {}", username, action, amount);
    }

    public void logError(String action, String errorMessage) {
        logger.error("ERROR | Action: {} | Reason: {}", action, errorMessage);
    }
}