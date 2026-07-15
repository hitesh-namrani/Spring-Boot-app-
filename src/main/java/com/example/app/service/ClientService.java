package com.example.app.service;

import com.example.app.dto.TransactionType;
import com.example.app.entity.Client;
import com.example.app.dao.ClientRepository;
import com.example.app.exception.WalletException;
import com.example.app.logger.AppLogger;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
/*
Service class is responsible for handling all client-related operations
such as registration, authentication, deposits, and withdrawals.
*/

@Service
public class ClientService {
    private final ClientRepository repository;
    private final AppLogger logger;
    private final BCryptPasswordEncoder passwordEncoder;
    ClientService(ClientRepository repository,AppLogger logger,BCryptPasswordEncoder passwordEncoder) {
        this.repository = repository;
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
        if (repository.findByUsername(username).isPresent()) {
            logger.logError("REGISTER", "Username '" + username + "' is already taken.");
            throw new WalletException("ERR_USER_EXISTS", "Username is already taken!");
        }

        String hashedPassword = passwordEncoder.encode(rawPassword);

        // Create a new client with hashed credentials.
        final Client client = new Client(username, hashedPassword);

        // Log successful registration.
        logger.logTransaction(username, "REGISTER", 0.0);

        // Save the client in the database.
        return repository.save(client);
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
        final Client client = repository.findByUsername(username)
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
        return repository.findByUsername(username)
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

    public Client processTransaction(String username, Double amount, TransactionType type) throws WalletException {
        //amount must be positive.
        if (amount <= 0) {
            logger.logError(String.valueOf(type), "Invalid amount: " + amount);
            throw new WalletException("ERR_INVALID_AMOUNT", "Amount must be greater than zero.");
        }

        // get client via username.
        final Client client = getClientByUsername(username);

        if(type==TransactionType.Deposit) {
            client.setBalance(client.getBalance() + amount);
        }
        else if(type==TransactionType.Withdraw) {
            if (client.getBalance() < amount) {
                logger.logError(String.valueOf(type), "Insufficient funds for '" + username + "'.");
                throw new WalletException("ERR_INSUFFICIENT_FUNDS", "Insufficient funds!");
            }

            // Deduct the withdrawal amount.
            client.setBalance(client.getBalance() - amount);
        }
        // Record the transaction.
        logger.logTransaction(username, String.valueOf(type), amount);
        // Save the updated balance.
        return repository.save(client);
    }
}
