package com.example.app.controller;

import com.example.app.entity.Client;
import com.example.app.exception.WalletException;
import com.example.app.service.ClientService;
import org.springframework.web.bind.annotation.*;
import com.example.app.dto.ApiResponse;
import jakarta.servlet.http.HttpSession;
/*
REST controller that provides API endpoints for client account operations
such as registration, deposit, withdrawal, and balance inquiry.
*/

@RestController
@RequestMapping("/api/clients")
public class ClientController {

    //Service layer responsible for handling client-related business logic.
    private final ClientService service;

    ClientController(ClientService service){
        this.service=service;
    }

    private static final String SUCCESS="Success";
    /*
    Registers a new client account.
    return an API response containing the newly created client
    @throws WalletException if the username already exists or registration fails
    */

    @PostMapping("/register")
    public ApiResponse register(@RequestParam String username, @RequestParam String password) throws WalletException {
        final Client newClient = service.registerClient(username, password);
        return new ApiResponse(SUCCESS, "Account created successfully", newClient);
    }

    @PostMapping("/login")
    public ApiResponse login(@RequestParam String username, @RequestParam String password, HttpSession session) throws WalletException {
        Client client = service.verifyLogin(username, password);

        // Save user identifying data securely in server memory
        session.setAttribute("user", client.getUsername());

        return new ApiResponse("Success", "Logged in successfully", client);
    }

    @PostMapping("/logout")
    public ApiResponse logout(HttpSession session) {
        // Instantly invalidates the active session cookie for explicit testing
        session.invalidate();
        return new ApiResponse("Success", "Logged out successfully", null);
    }

    /*
    Processes a financial transaction (deposit or withdrawal) for the authenticated client.
    @param amount  The amount of money to transact
    @param type    The kind of transaction to perform (must be "deposit" or "withdraw")
    @param session The active HTTP session verifying the user's identity
    return an API response containing the transaction status and updated client details
    @throws WalletException if the session is unauthorized, the transaction type is unrecognized, or the amount/balance is invalid
    */
    @PostMapping("/transaction")
    public ApiResponse processTransaction(
            @RequestParam Double amount,
            @RequestParam String type,
            HttpSession session) throws WalletException {

        // 1. Verify the active session
        String sessionUser = (String) session.getAttribute("user");
        if (sessionUser == null) {
            throw new WalletException("ERR_UNAUTHORIZED", "Active session not found. Please log in first.");
        }

        Client updatedClient;

        // 2. Route the request based on the 'type' parameter
        if ("deposit".equalsIgnoreCase(type)) {
            updatedClient = service.processDeposit(sessionUser, amount);
            return new ApiResponse("Success", "Deposit successful", updatedClient);

        } else if ("withdraw".equalsIgnoreCase(type)) {
            updatedClient = service.processWithdraw(sessionUser, amount);
            return new ApiResponse("Success", "Withdrawal successful", updatedClient);

        } else {
            // 3. Handle invalid transaction types gracefully
            throw new WalletException("ERR_INVALID_TYPE", "Transaction type must be either 'deposit' or 'withdraw'.");
        }
    }

    /*
    Retrieves the current wallet balance for the authenticated client.
    return an API response containing the client's current balance
    throws WalletException if authentication fails
    */
    @GetMapping("/balance")
    public ApiResponse getBalance(HttpSession session) throws WalletException {
        String sessionUser = (String) session.getAttribute("user");
        if (sessionUser == null) {
            throw new WalletException("ERR_UNAUTHORIZED", "Active session not found. Please log in first.");
        }
        final Client client = service.getClientByUsername(sessionUser);
        return new ApiResponse(SUCCESS, "Balance retrieved successfully", client);
    }
}