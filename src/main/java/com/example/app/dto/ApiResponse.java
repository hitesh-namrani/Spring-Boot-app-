package com.example.app.dto;

// Import the Client entity
import com.example.app.entity.Client;

// Lombok annotations to reduce boilerplate code
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/*
Data Transfer Object (DTO) used to send API responses.

This class contains:
- status  : Indicates whether the request was successful or failed.
- message : Provides additional information about the response.
- client  : Contains the Client object returned by the API (if applicable).
*/

// Generates getter methods for all fields
@Getter

// Generates setter methods for all fields
@Setter

// Generates a no-argument constructor
@NoArgsConstructor

// Generates a constructor with all fields as parameters
@AllArgsConstructor
public class ApiResponse{

    // Status of the API response (e.g., "success", "error")
    private String status;

    // Message describing the result of the API call
    private String message;

    // Client data returned in the response
    private Client client;

}
