package com.example.app.service;

import com.example.app.entity.Client;
import com.example.app.dao.ClientRepository;
import com.example.app.exception.WalletException;
import com.example.app.logger.AppLogger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class ClientService {

  @Autowired
  private ClientRepository repository;

  @Autowired
  private AppLogger logger;

  public Client registerClient(String username, String rawPassword) throws WalletException {
    if (repository.findById(username).isPresent()) {
      logger.logError("REGISTER", "Username '" + username + "' is already taken.");
      throw new WalletException("ERR_USER_EXISTS","Username is already taken!");
    }
    Client client = new Client(username, rawPassword);
    logger.logTransaction(username, "REGISTER", 0.0);
    return repository.save(client);
  }

  public Client verifyLogin(String username, String password) throws WalletException {
    Client client = repository.findById(username)
            .orElseThrow(() -> {
              logger.logError("LOGIN", "Client '" + username + "' not found.");
              return new WalletException("ERR_USER_NOT_FOUND","Client not found!");
            });

    if (!client.checkPassword(password)) {
      logger.logError("LOGIN", "Incorrect password for '" + username + "'.");
      throw new WalletException("ERR_WRONG_PASSWORD","Incorrect password!");
    }
    return client;
  }

  public Client processDeposit(String username, String password, Double amount) throws WalletException {
    if (amount <= 0) {
      logger.logError("DEPOSIT", "Attempted invalid deposit amount: " + amount);
      throw new WalletException("ERR_INVALID_AMOUNT","Amount must be greater than zero.");
    }
    Client client = verifyLogin(username, password);
    client.setBalance(client.getBalance() + amount);

    logger.logTransaction(username, "DEPOSIT", amount);
    return repository.save(client);
  }

  public Client processWithdraw(String username, String password, Double amount) throws WalletException {
    if (amount <= 0) {
      logger.logError("WITHDRAWAL", "Attempted invalid withdrawal amount: " + amount);
      throw new WalletException("ERR_INVALID_AMOUNT","Amount must be greater than zero.");
    }

    Client client = verifyLogin(username, password);

    if (client.getBalance() < amount) {
      logger.logError("WITHDRAWAL", "Insufficient funds for '" + username + "'.");
      throw new WalletException("ERR_INSUFFICIENT_FUNDS","Insufficient funds!");
    }

    client.setBalance(client.getBalance() - amount);
    logger.logTransaction(username, "WITHDRAWAL", amount);
    return repository.save(client);
  }
}