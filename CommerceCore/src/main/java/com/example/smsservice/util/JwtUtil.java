package com.example.smsservice.util;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.spec.SecretKeySpec;
import java.security.Key;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

@Component
public class JwtUtil {

    @Value("${jwt.secret}")
    private String secretKey;

    @Value("${jwt.expiration}")
    private long jwtExpirationInMs;

    private Key getSigningKey() {
        return new SecretKeySpec(secretKey.getBytes(), SignatureAlgorithm.HS256.getJcaName());
    }

    public String generateToken(String username, String role) {
        System.out.println("Generating token for username: " + username + ", role: " + role);
        Map<String, Object> claims = new HashMap<>();
        claims.put("role", role); // Добавляем роль в payload
        String token = createToken(claims, username);
        System.out.println("Generated token: " + token);
        return token;
    }

    public String createToken(Map<String, Object> claims, String subject) {
        return Jwts.builder()
                .setClaims(claims)
                .setSubject(subject)
                .setIssuedAt(new Date(System.currentTimeMillis()))
                .setExpiration(new Date(System.currentTimeMillis() + jwtExpirationInMs))
                .signWith(getSigningKey(), SignatureAlgorithm.HS256)
                .compact();
    }

    public Boolean validateToken(String token, String username) {
        try {
            final String extractedUsername = extractUsername(token);
            boolean isValid = (extractedUsername.equals(username) && !isTokenExpired(token));
            System.out.println("Validating token for username: " + username + ", isValid: " + isValid);
            return isValid;
        } catch (Exception e) {
            System.err.println("Error during token validation: " + e.getMessage());
            return false;
        }
    }

    public String extractUsername(String token) {
        try {
            String username = extractClaim(token, Claims::getSubject);
            System.out.println("Extracted username from token: " + username);
            return username;
        } catch (Exception e) {
            System.err.println("Error extracting username: " + e.getMessage());
            throw e;
        }
    }

    public String extractRole(String token) {
        try {
            String role = extractClaim(token, claims -> claims.get("role", String.class));
            System.out.println("Extracted role from token: " + role);
            return role;
        } catch (Exception e) {
            System.err.println("Error extracting role: " + e.getMessage());
            throw e;
        }
    }

    public <T> T extractClaim(String token, Function<Claims, T> claimsResolver) {
        final Claims claims = extractAllClaims(token);
        return claimsResolver.apply(claims);
    }

    private Claims extractAllClaims(String token) {
        try {
            return Jwts.parserBuilder()
                    .setSigningKey(getSigningKey())
                    .build()
                    .parseClaimsJws(token)
                    .getBody();
        } catch (Exception e) {
            System.err.println("Error extracting claims from token: " + e.getMessage());
            throw e;
        }
    }

    private Boolean isTokenExpired(String token) {
        try {
            boolean isExpired = extractExpiration(token).before(new Date());
            System.out.println("Is token expired: " + isExpired);
            return isExpired;
        } catch (Exception e) {
            System.err.println("Error checking token expiration: " + e.getMessage());
            throw e;
        }
    }

    public Date extractExpiration(String token) {
        return extractClaim(token, Claims::getExpiration);
    }
}