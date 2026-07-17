package com.example.app.service;

import com.example.app.dao.TransactionsRepository;
import com.example.app.dto.BalanceType;
import com.example.app.dto.Status;
import com.example.app.dto.TransactionType;
import com.example.app.entity.Client;
import com.example.app.dao.ClientRepository;
import com.example.app.entity.Transactions;
import com.example.app.exception.WalletException;
import com.example.app.logger.AppLogger;
import jakarta.transaction.Transactional;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
/*
Service class is responsible for handling all client-related operations
such as registration, authentication, deposits, and withdrawals.
*/

@Service
public class ClientService {
    public static final String ERR_USER_NOT_FOUND = "ERR_USER_NOT_FOUND";
    private final ClientRepository clientRepository;
    private final TransactionsRepository transactionsRepository;
    private final AppLogger logger;
    private final BCryptPasswordEncoder passwordEncoder;

    // Defines daily transaction limits for each balance type.
    private static final double MAIN_DEPOSIT_LIMIT = 100.0;
    private static final double MAIN_WITHDRAW_LIMIT = 500.0;
    private static final double VOUCHER_DEPOSIT_LIMIT = 10000.0;
    private static final double VOUCHER_WITHDRAW_LIMIT = 10000.0;

    ClientService(ClientRepository clientRepository, TransactionsRepository transactionsRepository, AppLogger logger, BCryptPasswordEncoder passwordEncoder) {
        this.clientRepository = clientRepository;
        this.transactionsRepository = transactionsRepository;
        this.logger = logger;
        this.passwordEncoder = passwordEncoder;
    }


    /*
    Registers a new client.
    return Saved client object
    throws WalletException if the username already exists
    */
    public Client registerClient(String username, String rawPassword) throws WalletException {
        // Check whether the username already exists.
        if (clientRepository.findByUsername(username).isPresent()) {
            logger.logError("REGISTER", "Username '" + username + "' is already taken.");
            throw new WalletException("ERR_USER_EXISTS", "Username is already taken!");
        }

        String hashedPassword = passwordEncoder.encode(rawPassword);

        // Create a new client with hashed credentials.
        final Client client = new Client(username, hashedPassword);

        // Log successful registration.
        logger.logTransaction(username, "REGISTER", 0.0);

        // Save the client in the database.
        return clientRepository.save(client);
    }

    //Validates client login credentials.
    public Client verifyLogin(String username, String password) throws WalletException {
        // Retrieve the client from the database.
        final Client client = clientRepository.findByUsername(username)
                .orElseThrow(() -> {
                    logger.logError("LOGIN", "Client '" + username + "' not found.");
                    return new WalletException(ERR_USER_NOT_FOUND, "Client not found!");
                });

        // Validate the password.
        if (!passwordEncoder.matches(password, client.getPassword())) {
            logger.logError("LOGIN", "Incorrect password for '" + username + "'.");
            throw new WalletException("ERR_WRONG_PASSWORD", "Incorrect password!");
        }
        return client;
    }

    // Helper method to look up a user whose username is securely tied to an active session
    public Client getClientByUsername(String username) throws WalletException {
        return clientRepository.findByUsername(username)
                .orElseThrow(() -> new WalletException(ERR_USER_NOT_FOUND, "Client session user no longer exists!"));
    }

    //Processes deposits and withdrawals while enforcing wallet rules and limits.
    @Transactional
    public Client processTransaction(String username, Double amount, TransactionType transactionType, BalanceType balanceType) throws WalletException {

        // Locks the client row to prevent concurrent balance updates.
        final Client client = clientRepository.findByUsernameForUpdate(username)
                .orElseThrow(() -> new WalletException(ERR_USER_NOT_FOUND, "Client session user no longer exists!"));
        //amount must be positive.
        if (amount <= 0) {
            logger.logError(String.valueOf(transactionType), "Invalid amount: " + amount);
            throw new WalletException("ERR_INVALID_AMOUNT", "Amount must be greater than zero.");
        }
        Instant startOfDay = LocalDate.now(ZoneOffset.UTC).atStartOfDay().toInstant(ZoneOffset.UTC);

        // Deposit processing logic.
        if (transactionType == TransactionType.DEPOSIT) {
            if (balanceType == null) {
                throw new WalletException("ERR_BALANCE_TYPE_REQUIRED", "Balance type is required for deposits.");
            }
            double depositedToday = transactionsRepository.getDailyTransactionSum(client.getId(), transactionType, balanceType, startOfDay);
            double limit = (balanceType == BalanceType.MAIN) ? 100.0 : 10000.0;
            if (depositedToday + amount > limit) {
                throw new WalletException("ERR_LIMIT_EXCEEDED", "Daily deposit limit exceeded for " + balanceType + ". Remaining limit: " + (limit - depositedToday));
            }
            // Update selected wallet balance.
            else {
                if (balanceType == BalanceType.MAIN) {
                    client.setMainBalance(client.getMainBalance() + amount);
                } else {
                    client.setVoucherBalance(client.getVoucherBalance() + amount);
                }
                transactionsRepository.save(new Transactions(client.getId(), amount, transactionType, balanceType, Status.SUCCESS));
                logger.logTransaction(username, String.valueOf(transactionType), amount);
            }
        }

        // Withdrawal processing logic.
        else if (transactionType == TransactionType.WITHDRAW) {
            if (client.getMainBalance() + client.getVoucherBalance() < amount) {
                logger.logError(String.valueOf(transactionType), "Insufficient funds for '" + username + "'.");
                transactionsRepository.save(new Transactions(client.getId(), amount, transactionType, BalanceType.BOTH, Status.FAILED));
                throw new WalletException("ERR_INSUFFICIENT_FUNDS", "Insufficient funds!");
            }
            double voucherLimitLeft = VOUCHER_WITHDRAW_LIMIT - transactionsRepository.getDailyTransactionSum(client.getId(), transactionType, BalanceType.VOUCHER, startOfDay);
            double mainLimitLeft = MAIN_WITHDRAW_LIMIT - transactionsRepository.getDailyTransactionSum(client.getId(), transactionType, BalanceType.MAIN, startOfDay);
            if (voucherLimitLeft + mainLimitLeft < amount) {
                logger.logError(String.valueOf(transactionType), "Limit reached for " + username + ".");
                transactionsRepository.save(new Transactions(client.getId(), amount, transactionType, BalanceType.BOTH, Status.FAILED));
                throw new WalletException("ERR_LIMIT_REACHED", "Limit reached!");
            }
            double remainingAmount = amount;
            double voucherDeduction = 0;
            double mainDeduction = 0;
            if (client.getVoucherBalance() > 0 && remainingAmount > 0) {
                double maxVoucherWithdrawal = Math.min(client.getVoucherBalance(), voucherLimitLeft);
                voucherDeduction = Math.min(remainingAmount, maxVoucherWithdrawal);
                remainingAmount -= voucherDeduction;
            }
            if (remainingAmount > 0) {
                if (client.getMainBalance() < remainingAmount || mainLimitLeft < remainingAmount) {
                    logger.logError(String.valueOf(transactionType), "Deduction validation failed for " + username + ". Main balance or limit breached.");
                    transactionsRepository.save(new Transactions(client.getId(), amount, transactionType, BalanceType.BOTH, Status.FAILED));
                    throw new WalletException("ERR_LIMIT_OR_FUNDS_BREACH", "Transaction violates independent balance limitations.");
                }
                mainDeduction = remainingAmount;
            }
            if (voucherDeduction > 0) {
                client.setVoucherBalance(client.getVoucherBalance() - voucherDeduction);
                transactionsRepository.save(new Transactions(client.getId(), voucherDeduction, transactionType, BalanceType.VOUCHER, Status.SUCCESS));
            }
            if (mainDeduction > 0) {
                client.setMainBalance(client.getMainBalance() - mainDeduction);
                transactionsRepository.save(new Transactions(client.getId(), mainDeduction, transactionType, BalanceType.MAIN, Status.SUCCESS));
            }
        }
        logger.logTransaction(username, String.valueOf(transactionType), amount);
        // Save the updated balance.
        return clientRepository.save(client);
    }

    //Returns remaining daily transaction limits for a client.
    public Map<String, Object> limits(String username) throws WalletException {
        Client client = getClientByUsername(username);
        Instant startOfDay = LocalDate.now(ZoneOffset.UTC).atStartOfDay().toInstant(ZoneOffset.UTC);
        double mainDepositLimitLeft = MAIN_DEPOSIT_LIMIT - transactionsRepository.getDailyTransactionSum(client.getId(), TransactionType.DEPOSIT, BalanceType.MAIN, startOfDay);
        double voucherDepositLimitLeft = VOUCHER_DEPOSIT_LIMIT - transactionsRepository.getDailyTransactionSum(client.getId(), TransactionType.DEPOSIT, BalanceType.VOUCHER, startOfDay);
        double mainWithdrawLimitLeft = MAIN_WITHDRAW_LIMIT - transactionsRepository.getDailyTransactionSum(client.getId(), TransactionType.WITHDRAW, BalanceType.MAIN, startOfDay);
        double voucherWithdrawLimitLeft = VOUCHER_WITHDRAW_LIMIT - transactionsRepository.getDailyTransactionSum(client.getId(), TransactionType.WITHDRAW, BalanceType.VOUCHER, startOfDay);
        Map<String, Object> data = new HashMap<>();
        data.put("MainDepositLimitLeft", mainDepositLimitLeft);
        data.put("MainWithdrawLimitLeft", mainWithdrawLimitLeft);
        data.put("VoucherDepositLimitLeft", voucherDepositLimitLeft);
        data.put("VoucherWithdrawLimitLeft", voucherWithdrawLimitLeft);
        return data;
    }

    // Retrieves transaction history ordered by latest transaction first.
    public List<Transactions> getTransactionHistory(String username) throws WalletException {
        long clientId = getClientByUsername(username).getId();
        return transactionsRepository.findByClientIdOrderByTimestampDesc(clientId);
    }

}
