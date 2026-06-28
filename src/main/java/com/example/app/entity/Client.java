package com.example.app.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import lombok.Getter;
import lombok.Setter;

@Entity
@Getter
@Setter
public class Client {

    @Id
    private String username;

    @JsonIgnore
    private String password;

    private Double balance;

    protected Client() {}

    public Client(String username, String password) {
        this.username = username;
        this.password = password;
        this.balance = 0.0;
    }

    public boolean checkPassword(String inputPassword) {
        return this.password.equals(inputPassword);
    }
}