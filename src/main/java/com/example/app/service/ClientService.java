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
    private final ClientRepository clientRepository;
    private final TransactionsRepository transactionsRepository;
    private final AppLogger logger;
    private final BCryptPasswordEncoder passwordEncoder;
    private final static double mainDepositLimit=100.0;
    private final static double mainWithdrawLimit=500.0;
    private final static double voucherDepositLimit=10000.0;
    private final static double voucherWithdrawLimit=10000.0;
    ClientService(ClientRepository clientRepository,TransactionsRepository transactionsRepository,AppLogger logger,BCryptPasswordEncoder passwordEncoder) {
        this.clientRepository = clientRepository;
        this.transactionsRepository = transactionsRepository;
        this.logger=logger;
        this.passwordEncoder=passwordEncoder;
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
  
    /*
    Authenticates a client using username and password.
    @param username Client username
    @param password Client password
    return Authenticated client
    throws WalletException if the client is not found or password is incorrect
    */

    public Client verifyLogin(String username, String password) throws WalletException {
        // Retrieve the client from the database.
        final Client client = clientRepository.findByUsername(username)
                .orElseThrow(() -> {
                    logger.logError("LOGIN", "Client '" + username + "' not found.");
                    return new WalletException("ERR_USER_NOT_FOUND", "Client not found!");
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
                .orElseThrow(() -> new WalletException("ERR_USER_NOT_FOUND", "Client session user no longer exists!"));
    }

    /*
    Deposits money into the client's wallet.
    @param username Client username
    @param amount   Amount to deposit
    @param type     Transaction type
    return Updated client object
    throws WalletException if the amount is invalid or authentication fails
    */

    @Transactional
    public Client processTransaction(String username, Double amount, TransactionType transactionType, BalanceType balanceType) throws WalletException {

        // get client via username.
        final Client client = clientRepository.findByUsernameForUpdate(username)
                .orElseThrow(() -> new WalletException("ERR_USER_NOT_FOUND", "Client session user no longer exists!"));

        //amount must be positive.
        if (amount <= 0) {
            logger.logError(String.valueOf(transactionType), "Invalid amount: " + amount);
            transactionsRepository.save(new Transactions(client.getId(), amount, transactionType,balanceType, Status.Failed));
            throw new WalletException("ERR_INVALID_AMOUNT", "Amount must be greater than zero.");
        }
        Instant startOfDay= LocalDate.now(ZoneOffset.UTC).atStartOfDay().toInstant(ZoneOffset.UTC);
        if(transactionType==TransactionType.Deposit) {
            double depositedToday = transactionsRepository.getDailyTransactionSum(client.getId(), transactionType, balanceType, startOfDay);
            double limit=(balanceType==BalanceType.Main)?100.0:10000.0;
            if(depositedToday+amount>limit){
                throw new WalletException("ERR_LIMIT_EXCEEDED", "Daily deposit limit exceeded for " + balanceType + ". Remaining limit: " +(limit - depositedToday));
            }
            else {
                if (balanceType == BalanceType.Main) {
                    client.setMainBalance(client.getMainBalance() + amount);
                }
                else{
                    client.setVoucherBalance(client.getVoucherBalance() + amount);
                }
                transactionsRepository.save(new Transactions(client.getId(), amount, transactionType, balanceType, Status.Success));
                logger.logTransaction(username, String.valueOf(transactionType), amount);
            }
        }
        else if(transactionType==TransactionType.Withdraw) {
            if(client.getMainBalance()+ client.getVoucherBalance()<amount){
                logger.logError(String.valueOf(transactionType), "Insufficient funds for '" + username + "'.");
                transactionsRepository.save(new Transactions(client.getId(), amount, transactionType, balanceType, Status.Failed));
                throw new WalletException("ERR_INSUFFICIENT_FUNDS","Insufficient funds!");
            }
            double voucherLimitLeft=10000.0-transactionsRepository.getDailyTransactionSum(client.getId(), transactionType,BalanceType.Voucher,startOfDay);
            double mainLimitLeft=500.0-transactionsRepository.getDailyTransactionSum(client.getId(), transactionType,BalanceType.Main,startOfDay);
            if(voucherLimitLeft+mainLimitLeft<amount){
                logger.logError(String.valueOf(transactionType),"Limit reached for " + username + ".");
                transactionsRepository.save(new Transactions(client.getId(), amount, transactionType, balanceType, Status.Failed));
                throw new WalletException("ERR_LIMIT_REACHED","Limit reached!");
            }
            double remainingAmount=amount;
            double voucherDeduction = 0;
            double mainDeduction = 0;
            if(client.getVoucherBalance()>0&&remainingAmount>0){
                double maxVoucherWithdrawal = Math.min(client.getVoucherBalance(), voucherLimitLeft);
                voucherDeduction = Math.min(remainingAmount, maxVoucherWithdrawal);
                remainingAmount -= voucherDeduction;
            }
            if(remainingAmount>0){
                if(client.getMainBalance()<remainingAmount||mainLimitLeft<remainingAmount){
                    logger.logError(String.valueOf(transactionType), "Deduction validation failed for " + username + ". Main balance or limit breached.");
                    transactionsRepository.save(new Transactions(client.getId(), amount, transactionType, BalanceType.Main, Status.Failed));
                    throw new WalletException("ERR_LIMIT_OR_FUNDS_BREACH", "Transaction violates independent balance limitations.");
                }
                mainDeduction=remainingAmount;
            }
            if (voucherDeduction > 0) {
                client.setVoucherBalance(client.getVoucherBalance() - voucherDeduction);
                transactionsRepository.save(new Transactions(client.getId(), voucherDeduction, transactionType, BalanceType.Voucher, Status.Success));
            }
            if (mainDeduction > 0) {
                client.setMainBalance(client.getMainBalance() - mainDeduction);
                transactionsRepository.save(new Transactions(client.getId(), mainDeduction, transactionType, BalanceType.Main, Status.Success));
            }
        }
        logger.logTransaction(username, String.valueOf(transactionType), amount);
        // Save the updated balance.
        return clientRepository.save(client);
    }
    public Map<String,Object> limits(String username) throws WalletException{
        Client client=getClientByUsername(username);
        Instant startOfDay= LocalDate.now(ZoneOffset.UTC).atStartOfDay().toInstant(ZoneOffset.UTC);
        double mainDepositLimitLeft=mainDepositLimit-transactionsRepository.getDailyTransactionSum(client.getId(), TransactionType.Deposit, BalanceType.Main, startOfDay);
        double voucherDepositLimitLeft=voucherDepositLimit-transactionsRepository.getDailyTransactionSum(client.getId(), TransactionType.Deposit, BalanceType.Voucher, startOfDay);
        double mainWithdrawLimitLeft=mainWithdrawLimit-transactionsRepository.getDailyTransactionSum(client.getId(), TransactionType.Withdraw, BalanceType.Main, startOfDay);
        double voucherWithdrawLimitLeft=voucherWithdrawLimit-transactionsRepository.getDailyTransactionSum(client.getId(), TransactionType.Withdraw, BalanceType.Voucher, startOfDay);
        Map<String,Object> data=new HashMap<>();
        data.put("MainDepositLimitLeft",mainDepositLimitLeft);
        data.put("MainWithdrawLimitLeft",mainWithdrawLimitLeft);
        data.put("VoucherDepositLimitLeft",voucherDepositLimitLeft);
        data.put("VoucherWithdrawLimitLeft",voucherWithdrawLimitLeft);
        return data;
    }
    public List<Transactions> getTransactionHistory(String username) throws WalletException {
        long clientId = getClientByUsername(username).getId();
        return transactionsRepository.findByClientIdOrderByTimestampDesc(clientId);
    }

}
