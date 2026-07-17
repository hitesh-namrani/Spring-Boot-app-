package com.example.app.controller;

import com.example.app.dto.*;
import com.example.app.entity.Client;
import com.example.app.entity.Transactions;
import com.example.app.exception.WalletException;
import com.example.app.service.ClientService;
import com.example.app.util.JwtUtil;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
/*
REST controller that provides API endpoints for client account operations
such as registration, deposit, withdrawal, and balance inquiry.
*/

@RestController
@RequestMapping("/api/clients")
public class ClientController {

    //Service layer responsible for handling client-related business logic.
    private final ClientService service;
    //Utility class for generating and validating JWT tokens.
    private final JwtUtil jwtUtil;

    ClientController(ClientService service, JwtUtil jwtUtil) {
        this.service = service;
        this.jwtUtil = jwtUtil;
    }

    /*
    Registers a new client account.
    @param request contains the username and password for the new client
    @return an API response containing the newly created client
    @throws WalletException if the username already exists or registration fails
    */

    @PostMapping("/register")
    public ApiResponse register(@RequestBody AuthRequest request) throws WalletException {
        final Client newClient = service.registerClient(request.getUsername(), request.getPassword());
        return new ApiResponse(Status.SUCCESS, "Account created successfully", newClient);
    }

    /*
    Authenticates a client and initializes a secure session.

    @param request contains the client's login credentials
    @return API response containing the JWT token and authenticated client details
    @throws WalletException If the credentials do not match or user is not found
    */
    @PostMapping("/login")
    public ApiResponse login(@RequestBody AuthRequest request) throws WalletException {
        Client client = service.verifyLogin(request.getUsername(), request.getPassword());

        String token = jwtUtil.generateToken(client.getUsername());
        Map<String, Object> data = new HashMap<>();
        data.put("token", token);

        return new ApiResponse(Status.SUCCESS, "Logged in successfully", data, client);
    }

    /*
    Since authentication is stateless using JWT, the server does not invalidate the token.
    The client is responsible for deleting the stored token.
    @return An API response indicating successful logout status
    */
    @PostMapping("/logout")
    public ApiResponse logout() {
        return new ApiResponse(Status.SUCCESS, "Logged out successfully (Delete token on client side)", null);
    }

    /*
    Processes a financial transaction (deposit or withdrawal) for the authenticated client.
    @param amount The amount of money to transact
    @param transactionType type of transaction (Deposit or Withdraw)
    @param balanceType optional wallet balance type (if applicable)
    @param authHeader JWT authorization header in the format "Bearer &lt;token&gt;"
    @return an API response containing the transaction status and updated client details
    @throws WalletException if the session is unauthorized, the transaction type is unrecognized, or the amount/balance is invalid
    */
    @PostMapping("/transaction")
    public ApiResponse processTransaction(
            @RequestParam Double amount,
            @RequestParam TransactionType transactionType,
            @RequestParam(required = false) BalanceType balanceType,
            @RequestHeader(value = "Authorization", required = false) String authHeader) throws WalletException {

        // Verify the active session via JWT
        String sessionUser = jwtUtil.validateHeaderAndExtractUsername(authHeader);

        Client updatedClient;

        updatedClient = service.processTransaction(sessionUser, amount, transactionType, balanceType);
        return new ApiResponse(Status.SUCCESS, transactionType + " successful", updatedClient);
    }

    /*
    Retrieves the current wallet balances and transaction limits for the authenticated client.
    @param authHeader JWT authorization header
    @return an API response containing the client's current balance
    @throws WalletException if authentication fails
    */
    @GetMapping("/balance")
    public ApiResponse getBalance(@RequestHeader(value = "Authorization", required = false) String authHeader) throws WalletException {
        String sessionUser = jwtUtil.validateHeaderAndExtractUsername(authHeader);
        final Client client = service.getClientByUsername(sessionUser);
        Map<String, Object> data = service.limits(sessionUser);
        return new ApiResponse(Status.SUCCESS, "Balance retrieved successfully", data, client);
    }

    /*
    Retrieves the transaction history of the authenticated client.

    @param authHeader JWT authorization header
    @return API response containing the client's transaction history
    @throws WalletException if authentication fails
    */
    @GetMapping("/history")
    public ApiResponse getHistory(@RequestHeader(value = "Authorization", required = false) String authHeader) throws WalletException {
        String sessionUser = jwtUtil.validateHeaderAndExtractUsername(authHeader);
        Client client = service.getClientByUsername(sessionUser);
        List<Transactions> history = service.getTransactionHistory(sessionUser);
        List<TransactionResponse> historyDto = history.stream()
                .map(TransactionResponse::new)
                .toList();
        Map<String, Object> responseData = new HashMap<>();
        responseData.put("history", historyDto);
        return new ApiResponse(Status.SUCCESS, "Transaction history retrieved successfully", responseData, client);
    }
}