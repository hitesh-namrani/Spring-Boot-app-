package com.example.app.util;

import com.example.app.exception.WalletException;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.util.Date;

/*
Utility class to generate and validate JSON Web Tokens (JWT).
*/
@Component
public class JwtUtil {

    private final SecretKey key;

    // Token validity (e.g., 10 minutes = 1000ms * 60s * 10m)
    private static final long EXPIRATION_TIME = 1000 * 60 * 10;

    public JwtUtil(@Value("${jwt.secret}") String secretString) {
        this.key = Keys.hmacShaKeyFor(secretString.getBytes());
    }

    // Generates a token containing the username
    public String generateToken(String username) {
        return Jwts.builder()
                .subject(username)
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + EXPIRATION_TIME))
                .signWith(key)
                .compact();
    }

    // Validates the math on the signature and extracts the username
    public String extractUsername(String token) {
        Claims claims = Jwts.parser()
                .verifyWith(key)
                .build()
                .parseSignedClaims(token)
                .getPayload();
        return claims.getSubject();
    }

    //Extracts the token from the header and validates it
    public String validateHeaderAndExtractUsername(String authHeader) throws WalletException {
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            throw new com.example.app.exception.WalletException("ERR_UNAUTHORIZED", "Missing or invalid Authorization header.");
        }

        try {
            // Remove "Bearer " prefix (7 characters)
            String token = authHeader.substring(7);
            return extractUsername(token);
        } catch (Exception e) {
            // If the math fails, or the token is expired, JJWT throws an exception
            throw new com.example.app.exception.WalletException("ERR_UNAUTHORIZED", "Token is expired or invalid. Please log in again.");
        }
    }
}