package com.example.app.controller;

import com.example.app.dto.AuthRequest;
import com.example.app.dto.Status;
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

    /*
    Registers a new client account.
    return an API response containing the newly created client
    @throws WalletException if the username already exists or registration fails
    */

    @PostMapping("/register")
    public ApiResponse register(@RequestBody AuthRequest request) throws WalletException {
        final Client newClient = service.registerClient(request.getUsername(), request.getPassword());
        return new ApiResponse(Status.Success, "Account created successfully", newClient);
    }

    /*
    Authenticates a client and initializes a secure session.

    @param username The client's unique username credential
    @param password The client's account password credential
    @param session  The current HTTP session context to persist state
    @return An API response containing the logged-in client details
    @throws WalletException If the credentials do not match or user is not found
    */
    @PostMapping("/login")
    public ApiResponse login(@RequestBody AuthRequest request, HttpSession session) throws WalletException {
        Client client = service.verifyLogin(request.getUsername(), request.getPassword());

        // Save user identifying data securely in server memory
        session.setAttribute("user", client.getUsername());

        return new ApiResponse(Status.Success, "Logged in successfully", client);
    }

    /*
    Terminate the client session and clear tracking details.

    @param session The active HTTP session to be invalidated
    @return An API response indicating successful logout status
    */
    @PostMapping("/logout")
    public ApiResponse logout(HttpSession session) {
        // Instantly invalidates the active session cookie for explicit testing
        session.invalidate();
        return new ApiResponse(Status.Success, "Logged out successfully", null);
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
            return new ApiResponse(Status.Success, "Deposit successful", updatedClient);

        } else if ("withdraw".equalsIgnoreCase(type)) {
            updatedClient = service.processWithdraw(sessionUser, amount);
            return new ApiResponse(Status.Success, "Withdrawal successful", updatedClient);

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
        return new ApiResponse(Status.Success, "Balance retrieved successfully", client);
    }
}