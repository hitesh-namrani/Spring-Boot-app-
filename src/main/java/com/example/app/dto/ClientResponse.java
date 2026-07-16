package com.example.app.dto;

import com.example.app.entity.Client;
import lombok.Getter;

@Getter
public class ClientResponse {
    private final String username;
    private final double mainBalance;
    private final double voucherBalance;
    ClientResponse(Client c){
        this.username=c.getUsername();
        this.mainBalance=c.getMainBalance();
        this.voucherBalance=c.getVoucherBalance();
    }
}
