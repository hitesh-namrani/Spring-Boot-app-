package com.example.app.exception;

import com.example.app.dto.Status;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

import java.util.HashMap;
import java.util.Map;
/*
 Global exception handler for the application.
 Intercepts exceptions thrown by controllers and returns
 standardized error responses to the client.
*/

@ControllerAdvice
public class GlobalExceptionHandler {
    /*
    Handles WalletException and returns a structured error response.
    param ex the WalletException that was thrown
    return a ResponseEntity containing the error details and HTTP 400 status
    */

    @ExceptionHandler(WalletException.class)
    public ResponseEntity<Map<String, String>> handleWalletException(WalletException ex) {
        /*
        Creates a response body containing the error status,
        application-specific error code, and error message.
        */

        Map<String, String> errorResponse = new HashMap<>();

        // Injecting your custom error code and the readable message
        errorResponse.put("status", String.valueOf(Status.Failed));
        errorResponse.put("errorCode", ex.getErrorCode());
        errorResponse.put("errorMessage", ex.getMessage());

        return new ResponseEntity<>(errorResponse, HttpStatus.BAD_REQUEST);
    }
}
