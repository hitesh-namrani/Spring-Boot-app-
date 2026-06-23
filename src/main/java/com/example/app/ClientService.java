package com.example.app;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class ClientService {

  @Autowired
  private ClientRepository repository;

  public Client registerClient(String username, String rawPassword) {
    if (repository.findById(username).isPresent()) {
      throw new RuntimeException("Username is already taken!");
    }
    Client client = new Client(username, rawPassword);
    return repository.save(client);
  }

  public Client verifyLogin(String username, String password) {
    Client client = repository.findById(username)
        .orElseThrow(() -> new RuntimeException("Client not found!"));
    if (!client.checkPassword(password)) {
      throw new RuntimeException("Incorrect password!");
    }
    return client;
  }

  public Client processTransaction(String username, String password, Double amount) {
    Client client = verifyLogin(username, password);
    double newBalance = client.getBalance() + amount;
    if (newBalance < 0) {
      throw new RuntimeException("Insufficient funds!");
    }
    client.setBalance(newBalance);
    return repository.save(client);
  }
}
