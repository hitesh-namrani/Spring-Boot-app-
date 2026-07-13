package com.example.app.dto;

import lombok.Getter;

@Getter
public enum Status {
    Success("Success"),
    Failed("Failed");
    private String status;
    Status(String status) {
        this.status = status;
    }
}
