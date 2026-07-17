package com.example.app.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
/*
Represents a client Entity in the application. 
Stores the client's login credentials and account balance.
*/

@Entity
@Getter
@Setter
public class Client {

    //Auto-Incrementing Primary Key
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    //Unique username used for authentication.
    @Column(unique = true, nullable = false)
    private String username;

    // Stores the client's hashed password and excludes it from JSON responses.
    @JsonIgnore
    private String password;

    // Current main wallet balance.
    private Double mainBalance;

    // Current main wallet balance.
    private Double voucherBalance;

    /*
    Default constructor required by JPA.
    Should not be used directly.
    */
    protected Client() {
    }
    
    /*
    Creates a new client with the specified username and hashed password.
    The initial account balance is set to 0.0.
    */

    public Client(String username, String password) {
        this.username = username;
        this.password = password;
        this.mainBalance = 0.0;
        this.voucherBalance = 0.0;
    }
}
