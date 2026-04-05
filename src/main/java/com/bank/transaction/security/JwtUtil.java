package com.bank.transaction.security;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import javax.crypto.SecretKey;
import java.util.Date;

public class JwtUtil {

    private static final SecretKey KEY = Keys.hmacShaKeyFor(
            "this-is-a-very-secure-32-character-long-key-!!!!".getBytes());
    private static final long EXPIRATION_MS = 3600000; // 1 hour

    public static String generateToken(String username, String role) {
        return Jwts.builder()
                .setSubject(username) // Use setSubject for version 0.11.5
                .claim("role", role)
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + EXPIRATION_MS))
                .signWith(KEY) // JJWT 0.11.5 handles algorithm automatically with the key
                .compact();
    }

    public static SecretKey getKey() { return KEY; }
}