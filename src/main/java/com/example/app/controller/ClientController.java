package com.example.app.controller;

import com.example.app.entity.Client;
import com.example.app.exception.WalletException;
import com.example.app.service.ClientService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import com.example.app.dto.ApiResponse;
/*
REST controller that provides API endpoints for client account operations
such as registration, deposit, withdrawal, and balance inquiry.
*/

@RestController
@RequestMapping("/api/clients")
public class ClientController {

    //Service layer responsible for handling client-related business logic.
    @Autowired
    private ClientService service;
    
    /*
    Registers a new client account.
    return an API response containing the newly created client
    @throws WalletException if the username already exists or registration fails
    */

    @PostMapping("/register")
    public ApiResponse register(@RequestParam String username, @RequestParam String password) throws WalletException {
        final Client newClient = service.registerClient(username, password);
        return new ApiResponse("Success", "Account created successfully", newClient);
    }

    /*
    Deposits the specified amount into the client's wallet.
    return an API response containing the updated client details
    @throws WalletException if authentication fails or the amount is invalid
    */
    @PostMapping("/deposit")
    public ApiResponse deposit(
            @RequestParam String username,
            @RequestParam String password,
            @RequestParam Double amount) throws WalletException {
        final Client client = service.processDeposit(username, password, amount);
        return new ApiResponse("Success", "Deposit successful", client);
    }

    /*
    Withdraws the specified amount from the client's wallet.
    @param username the client's username
    @param password the client's password
    @param amount the amount to withdraw
    @return an API response containing the updated client details
    throws WalletException if authentication fails, the amount is invalid,or the balance is insufficient
    */
    @PostMapping("/withdraw")
    public ApiResponse withdraw(
            @RequestParam String username,
            @RequestParam String password,
            @RequestParam Double amount) throws WalletException {
        final Client client = service.processWithdraw(username, password, amount);
        return new ApiResponse("Success", "Withdraw successful", client);
    }

    /*
    Retrieves the current wallet balance for the authenticated client.
    return an API response containing the client's current balance
    throws WalletException if authentication fails
    */
    @GetMapping("/balance")
    public ApiResponse getBalance(@RequestParam String username, @RequestParam String password) throws WalletException {
        final Client client = service.verifyLogin(username, password);
        return new ApiResponse("Success", "Balance retrieved successfully", client);
    }
}
