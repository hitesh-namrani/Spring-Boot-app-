package com.example.app.dto;

// Import the User entity

import com.example.app.entity.User;

//import JsonInclude
import com.fasterxml.jackson.annotation.JsonInclude;

// Lombok annotations to reduce boilerplate code
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.Map;

/*
Data Transfer Object (DTO) used to send API responses.

This class contains:
- status  : Indicates whether the request was successful or failed.
- message : Provides additional information about the response.
- user  : Contains the User object returned by the API (if applicable).
*/

// Generates getter methods for all fields
@Getter

// Generates setter methods for all fields
@Setter

// Generates a no-argument constructor
@NoArgsConstructor

// Generates a constructor with all fields as parameters
@AllArgsConstructor

// Automatically hides fields that are empty/null!
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ApiResponse {

    // Status of the API response (e.g., "success", "error")
    private Status status;

    // Message describing the result of the API call
    private String message;

    //hashmap to provide data
    private Map<String, Object> data;

    // User data returned in the response
    private UserResponse user;

    //constructor if no hashmap is needed
    public ApiResponse(Status status, String message, User user) {
        this.status = status;
        this.message = message;
        this.user = user != null ? new UserResponse(user) : null;
    }

    public ApiResponse(Status status, String message, Map<String, Object> data, User user) {
        this.status = status;
        this.message = message;
        this.data = data;
        this.user = user != null ? new UserResponse(user) : null;
    }
}
