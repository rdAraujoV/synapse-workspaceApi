package com.rodrigo.synapse.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import java.util.Date;
import javax.crypto.SecretKey;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;

// will create the JWT token, to be returner on the AuthService when the login is successful
@Service
public class JwtService {
    private final SecretKey secretKey;
    private final long jwtExpiration;

    public JwtService(
                    @Value("${jwt.secret}") String secret,
                    @Value("${jwt.expiration}") long jwtExpiration){
        this.secretKey = Keys.hmacShaKeyFor(Decoders.BASE64.decode(secret));
        this.jwtExpiration = jwtExpiration;
    }

    public String generateToken(String email) {
        return generateToken(email, jwtExpiration);
    }

    public String generateToken(String email, long expiration) {
        return Jwts.builder()
                .subject(email)
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + expiration))
                .signWith(secretKey)
                .compact();
    }

    public boolean isTokenValid(String token) {
        try {
            Jwts.parser()
                    .verifyWith(secretKey)
                    .build().
                    parseSignedClaims(token);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    public String extractEmail(String token) {
        return Jwts.parser()
                .verifyWith(secretKey)
                .build()
                .parseSignedClaims(token)
                .getPayload()
                .getSubject();
    }
}