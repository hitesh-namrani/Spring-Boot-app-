package com.example.app.dto;

import com.example.app.entity.User;
import lombok.Getter;

@Getter
public class UserResponse {
    private final String username;
    private final double mainBalance;
    private final double voucherBalance;

    UserResponse(User c) {
        this.username = c.getUsername();
        this.mainBalance = c.getMainBalance();
        this.voucherBalance = c.getVoucherBalance();
    }
}
