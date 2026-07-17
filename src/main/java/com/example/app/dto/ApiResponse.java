package com.example.app.dto;

// Import the Client entity

import com.example.app.entity.Client;

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

// Automatically hides fields that are empty/null!
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ApiResponse {

    // Status of the API response (e.g., "success", "error")
    private Status status;

    // Message describing the result of the API call
    private String message;

    //hashmap to provide data
    private Map<String, Object> data;

    // Client data returned in the response
    private ClientResponse client;

    //constructor if no hashmap is needed
    public ApiResponse(Status status, String message, Client client) {
        this.status = status;
        this.message = message;
        this.client = client != null ? new ClientResponse(client) : null;
    }

    public ApiResponse(Status status, String message, Map<String, Object> data, Client client) {
        this.status = status;
        this.message = message;
        this.data = data;
        this.client = client != null ? new ClientResponse(client) : null;
    }
}
