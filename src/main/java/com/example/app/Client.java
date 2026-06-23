package com.example.app;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;

@Entity
public class Client {

    @Id
    private String username;

    private String password;

    private Double balance;

    protected Client() {}

    public Client(String username, String password) {
        this.username = username;
        this.password = password;
        this.balance = 0.0;
    }

    public String getUsername() { return username; }
    public Double getBalance() { return balance; }
    public void setBalance(Double balance) { this.balance = balance; }

    public boolean checkPassword(String inputPassword) {
        return this.password.equals(inputPassword);
    }
}