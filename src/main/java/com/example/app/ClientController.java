package com.example.app;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/clients")
public class ClientController {

    @Autowired
    private ClientService service;

    @PostMapping("/register")
    public Client register(@RequestParam String username, @RequestParam String password) {
        return service.registerClient(username, password);
    }

    @PostMapping("/transaction")
    public Client transact(
            @RequestParam String username,
            @RequestParam String password,
            @RequestParam Double amount) {
        return service.processTransaction(username, password, amount);
    }

    @GetMapping("/balance")
    public Double getBalance(@RequestParam String username, @RequestParam String password) {
        return service.verifyLogin(username, password).getBalance();
    }
}