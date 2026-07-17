package com.example.app.dto;

import lombok.Getter;

@Getter
public enum Status {
    SUCCESS("Success"),
    FAILED("Failed");
    private final String status;

    Status(String status) {
        this.status = status;
    }
}
