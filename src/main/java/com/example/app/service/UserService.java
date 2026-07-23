package com.example.app.service;

import com.example.app.dao.TransactionsRepository;
import com.example.app.dto.BalanceType;
import com.example.app.dto.Status;
import com.example.app.dto.TransactionType;
import com.example.app.entity.User;
import com.example.app.dao.UserRepository;
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
Service class is responsible for handling all user-related operations
such as registration, authentication, deposits, and withdrawals.
*/

@Service
public class UserService {
    public static final String ERR_USER_NOT_FOUND = "ERR_USER_NOT_FOUND";
    private final UserRepository userRepository;
    private final TransactionsRepository transactionsRepository;
    private final AppLogger logger;
    private final BCryptPasswordEncoder passwordEncoder;

    // Defines daily transaction limits for each balance type.
    private static final double MAIN_DEPOSIT_LIMIT = 100.0;
    private static final double MAIN_WITHDRAW_LIMIT = 500.0;
    private static final double VOUCHER_DEPOSIT_LIMIT = 10000.0;
    private static final double VOUCHER_WITHDRAW_LIMIT = 10000.0;
    private static final double MAIN_TRANSFER_LIMIT = 500.0;
    private static final double VOUCHER_TRANSFER_LIMIT = 5000.0;

    UserService(UserRepository userRepository, TransactionsRepository transactionsRepository, AppLogger logger, BCryptPasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.transactionsRepository = transactionsRepository;
        this.logger = logger;
        this.passwordEncoder = passwordEncoder;
    }


    /*
    Registers a new user.
    return Saved user object
    throws WalletException if the username already exists
    */
    public User registerUser(String username, String rawPassword) throws WalletException {
        // Check whether the username already exists.
        if (userRepository.findByUsername(username).isPresent()) {
            logger.logError("REGISTER", "Username '" + username + "' is already taken.");
            throw new WalletException("ERR_USER_EXISTS", "Username is already taken!");
        }

        String hashedPassword = passwordEncoder.encode(rawPassword);

        // Create a new user with hashed credentials.
        final User user = new User(username, hashedPassword);

        // Log successful registration.
        logger.logTransaction(username, "REGISTER", 0.0);

        // Save the user in the database.
        return userRepository.save(user);
    }

    //Validates user login credentials.
    public User verifyLogin(String username, String password) throws WalletException {
        // Retrieve the user from the database.
        final User user = userRepository.findByUsername(username)
                .orElseThrow(() -> {
                    logger.logError("LOGIN", "User '" + username + "' not found.");
                    return new WalletException(ERR_USER_NOT_FOUND, "User not found!");
                });

        // Validate the password.
        if (!passwordEncoder.matches(password, user.getPassword())) {
            logger.logError("LOGIN", "Incorrect password for '" + username + "'.");
            throw new WalletException("ERR_WRONG_PASSWORD", "Incorrect password!");
        }
        return user;
    }

    // Helper method to look up a user whose username is securely tied to an active session
    public User getUserByUsername(String username) throws WalletException {
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new WalletException(ERR_USER_NOT_FOUND, "User session user no longer exists!"));
    }

    //Processes deposits and withdrawals while enforcing wallet rules and limits.
    @Transactional
    public User processTransaction(String username, Double amount, TransactionType transactionType, BalanceType balanceType, String receiverUsername) throws WalletException {

        // Locks the user row to prevent concurrent balance updates.
        final User user = userRepository.findByUsernameForUpdate(username)
                .orElseThrow(() -> new WalletException(ERR_USER_NOT_FOUND, "User session user no longer exists!"));
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
            double depositedToday = transactionsRepository.getDailyTransactionSum(user.getId(), transactionType, balanceType, startOfDay);
            double limit = (balanceType == BalanceType.MAIN) ? 100.0 : 10000.0;
            if (depositedToday + amount > limit) {
                throw new WalletException("ERR_LIMIT_EXCEEDED", "Daily deposit limit exceeded for " + balanceType + ". Remaining limit: " + (limit - depositedToday));
            }
            // Update selected wallet balance.
            else {
                if (balanceType == BalanceType.MAIN) {
                    user.setMainBalance(user.getMainBalance() + amount);
                } else {
                    user.setVoucherBalance(user.getVoucherBalance() + amount);
                }
                transactionsRepository.save(new Transactions(user.getId(), amount, transactionType, balanceType, Status.SUCCESS));
                logger.logTransaction(username, String.valueOf(transactionType), amount);
            }
        }

        // Withdrawal processing logic.
        else if (transactionType == TransactionType.WITHDRAW) {
            if (user.getMainBalance() + user.getVoucherBalance() < amount) {
                logger.logError(String.valueOf(transactionType), "Insufficient funds for '" + username + "'.");
                transactionsRepository.save(new Transactions(user.getId(), amount, transactionType, BalanceType.BOTH, Status.FAILED));
                throw new WalletException("ERR_INSUFFICIENT_FUNDS", "Insufficient funds!");
            }
            double voucherLimitLeft = VOUCHER_WITHDRAW_LIMIT - transactionsRepository.getDailyTransactionSum(user.getId(), transactionType, BalanceType.VOUCHER, startOfDay);
            double mainLimitLeft = MAIN_WITHDRAW_LIMIT - transactionsRepository.getDailyTransactionSum(user.getId(), transactionType, BalanceType.MAIN, startOfDay);
            if (voucherLimitLeft + mainLimitLeft < amount) {
                logger.logError(String.valueOf(transactionType), "Limit reached for " + username + ".");
                transactionsRepository.save(new Transactions(user.getId(), amount, transactionType, BalanceType.BOTH, Status.FAILED));
                throw new WalletException("ERR_LIMIT_REACHED", "Limit reached!");
            }
            double remainingAmount = amount;
            double voucherDeduction = 0;
            double mainDeduction = 0;
            if (user.getVoucherBalance() > 0 && remainingAmount > 0) {
                double maxVoucherWithdrawal = Math.min(user.getVoucherBalance(), voucherLimitLeft);
                voucherDeduction = Math.min(remainingAmount, maxVoucherWithdrawal);
                remainingAmount -= voucherDeduction;
            }
            if (remainingAmount > 0) {
                if (user.getMainBalance() < remainingAmount || mainLimitLeft < remainingAmount) {
                    logger.logError(String.valueOf(transactionType), "Deduction validation failed for " + username + ". Main balance or limit breached.");
                    transactionsRepository.save(new Transactions(user.getId(), amount, transactionType, BalanceType.BOTH, Status.FAILED));
                    throw new WalletException("ERR_LIMIT_OR_FUNDS_BREACH", "Transaction violates independent balance limitations.");
                }
                mainDeduction = remainingAmount;
            }
            if (voucherDeduction > 0) {
                user.setVoucherBalance(user.getVoucherBalance() - voucherDeduction);
                transactionsRepository.save(new Transactions(user.getId(), voucherDeduction, transactionType, BalanceType.VOUCHER, Status.SUCCESS));
            }
            if (mainDeduction > 0) {
                user.setMainBalance(user.getMainBalance() - mainDeduction);
                transactionsRepository.save(new Transactions(user.getId(), mainDeduction, transactionType, BalanceType.MAIN, Status.SUCCESS));
            }
        }
        else if(transactionType==TransactionType.TRANSFER){
            if (username.equals(receiverUsername)) {
                throw new WalletException("ERR_INVALID_RECEIVER", "Cannot transfer funds to yourself.");
            }
            User receiver = userRepository.findByUsernameForUpdate(receiverUsername)
                    .orElseThrow(()-> new WalletException("ERR_RECEIVER_NOT_FOUND","receiver username is incorrect"));
            if(balanceType==null){
                throw new WalletException("ERR_BALANCE_TYPE_REQUIRED", "Balance type is required for transfers.");
            }
            transfer(user,receiver,amount,balanceType);
        }
        logger.logTransaction(username, String.valueOf(transactionType), amount);
        // Save the updated balance.
        return userRepository.save(user);
    }
    void transfer(User user,User receiver, double amount,BalanceType balanceType) throws WalletException{
        //empty method for implementing transfer functionality
        //Max main transfer limit per day=500
        //Max voucher transfer limit per day=5000
    }

    //Returns remaining daily transaction limits for a user.
    public Map<String, Object> limits(String username) throws WalletException {
        User user = getUserByUsername(username);
        Instant startOfDay = LocalDate.now(ZoneOffset.UTC).atStartOfDay().toInstant(ZoneOffset.UTC);
        double mainDepositLimitLeft = MAIN_DEPOSIT_LIMIT - transactionsRepository.getDailyTransactionSum(user.getId(), TransactionType.DEPOSIT, BalanceType.MAIN, startOfDay);
        double voucherDepositLimitLeft = VOUCHER_DEPOSIT_LIMIT - transactionsRepository.getDailyTransactionSum(user.getId(), TransactionType.DEPOSIT, BalanceType.VOUCHER, startOfDay);
        double mainWithdrawLimitLeft = MAIN_WITHDRAW_LIMIT - transactionsRepository.getDailyTransactionSum(user.getId(), TransactionType.WITHDRAW, BalanceType.MAIN, startOfDay);
        double voucherWithdrawLimitLeft = VOUCHER_WITHDRAW_LIMIT - transactionsRepository.getDailyTransactionSum(user.getId(), TransactionType.WITHDRAW, BalanceType.VOUCHER, startOfDay);
        double mainTransferLimitLeft= MAIN_TRANSFER_LIMIT - transactionsRepository.getDailyTransactionSum(user.getId(), TransactionType.TRANSFER,BalanceType.MAIN,startOfDay);
        double voucherTransferLimitLeft= VOUCHER_TRANSFER_LIMIT - transactionsRepository.getDailyTransactionSum(user.getId(), TransactionType.TRANSFER,BalanceType.VOUCHER,startOfDay);
        Map<String, Object> data = new HashMap<>();
        data.put("MainDepositLimitLeft", mainDepositLimitLeft);
        data.put("MainWithdrawLimitLeft", mainWithdrawLimitLeft);
        data.put("MainTransferLimitLeft", mainTransferLimitLeft);
        data.put("VoucherDepositLimitLeft", voucherDepositLimitLeft);
        data.put("VoucherWithdrawLimitLeft", voucherWithdrawLimitLeft);
        data.put("VoucherTransferLimitLeft", voucherTransferLimitLeft);
        return data;
    }

    // Retrieves transaction history ordered by latest transaction first.
    public List<Transactions> getTransactionHistory(String username) throws WalletException {
        long userId = getUserByUsername(username).getId();
        return transactionsRepository.findByUserIdOrderByTimestampDesc(userId);
    }

}
