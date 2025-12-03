package com.tphelps.backend.jwt;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseCookie;
import org.springframework.security.authentication.AuthenticationCredentialsNotFoundException;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.security.Key;
import java.util.Date;


@Component
public class JwtTokenGenerator {

    /**
     * Jwt secret from application.properties
     */
    @Value("${jwt.secret}")
    private String jwtSecret;

    /**
     * Get a signing key from the jwt secret
     * @return - the generated key
     */
    private Key getSigningKey() {
        return Keys.hmacShaKeyFor(jwtSecret.getBytes(StandardCharsets.UTF_8));
    }

    /**
     * Get a jwt key for specified user
     * @param username - username for key
     * @return - a string containing JWT
     */
    public String getJwt(String username){
        return Jwts.builder()
                .setSubject(username)
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + 300_000)) // 5 minutes
                .signWith(getSigningKey())
                .compact();
    }

    /**
     * Extract a username from a JWT
     * @param token - JWT to get the username from
     * @return - the subject
     */
    public String getUsernameFromJwt(String token){
        Claims claims = Jwts.parserBuilder()
                .setSigningKey(getSigningKey())
                .build()
                .parseClaimsJws(token)
                .getBody();
        return claims.getSubject();
    }

    /**
     * Validate a jwt token
     * @param token - token to be validated
     * @return - true on validation, false else
     */
    public boolean validateJwt(String token){
        try{
            Jwts.parserBuilder().setSigningKey(getSigningKey()).build().parseClaimsJws(token);
            return true;
        }catch (JwtException e){
            return false;
        }
    }

}