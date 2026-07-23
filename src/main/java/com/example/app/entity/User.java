package com.example.app.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
/*
Represents a user Entity in the application.
Stores the user's login credentials and account balance.
*/

@Entity
@Table(name="users")
@Getter
@Setter
public class User {

    //Auto-Incrementing Primary Key
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    //Unique username used for authentication.
    @Column(unique = true, nullable = false)
    private String username;

    // Stores the user's hashed password and excludes it from JSON responses.
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
    protected User() {
    }
    
    /*
    Creates a new user with the specified username and hashed password.
    The initial account balance is set to 0.0.
    */

    public User(String username, String password) {
        this.username = username;
        this.password = password;
        this.mainBalance = 0.0;
        this.voucherBalance = 0.0;
    }
}
