package com.example.app.controller;

import com.example.app.entity.Client;
import com.example.app.exception.WalletException;
import com.example.app.service.ClientService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import com.example.app.dto.ApiResponse;

@RestController
@RequestMapping("/api/clients")
public class ClientController {

    @Autowired
    private ClientService service;

    @PostMapping("/register")
    public ApiResponse register(@RequestParam String username, @RequestParam String password) throws WalletException {
        final Client newClient = service.registerClient(username, password);
        return new ApiResponse("Success", "Account created successfully", newClient);
    }

    @PostMapping("/deposit")
    public ApiResponse deposit(
            @RequestParam String username,
            @RequestParam String password,
            @RequestParam Double amount) throws WalletException {
        final Client client = service.processDeposit(username, password, amount);
        return new ApiResponse("Success", "Deposit successful", client);
    }

    @PostMapping("/withdraw")
    public ApiResponse withdraw(
            @RequestParam String username,
            @RequestParam String password,
            @RequestParam Double amount) throws WalletException {
        final Client client = service.processWithdraw(username, password, amount);
        return new ApiResponse("Success", "Withdraw successful", client);
    }

    @GetMapping("/balance")
    public ApiResponse getBalance(@RequestParam String username, @RequestParam String password) throws WalletException {
        final Client client = service.verifyLogin(username, password);
        return new ApiResponse("Success", "Balance retrieved successfully", client);
    }
}