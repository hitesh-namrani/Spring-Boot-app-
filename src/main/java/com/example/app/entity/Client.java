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

    //New Auto-Incrementing Primary Key
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    //Username is no longer @Id, but it MUST be unique and not null
    @Column(unique = true, nullable = false)
    private String username;

    //Password excluded from JSON responses for security.
    @JsonIgnore
    private String password;

    //Current account balance of the client.
    private Double balance;

    /*
    Default constructor required by JPA.
    Should not be used directly.
    */
    protected Client() {
    }
    
    /*
    Creates a new client with the specified username and password.
    The initial account balance is set to 0.0.
    */

    public Client(String username, String password) {
        this.username = username;
        this.password = password;
        this.balance = 0.0;
    }
    /*
    Verifies whether the provided password matches the stored password.
    return true if the password matches, else return false.
    */

    public boolean checkPassword(String inputPassword) {
        return this.password.equals(inputPassword);
    }
}
