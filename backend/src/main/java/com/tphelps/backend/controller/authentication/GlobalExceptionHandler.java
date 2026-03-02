package com.tphelps.backend.controller.authentication;


import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import javax.naming.AuthenticationException;

/**
 * Catches all exceptions in one place and translates into consistent HTTP responses
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    /**
     * Authentication exception handler for controllers
     * @param e the Authentication exception
     * @return a 404 status
     */
    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<?> handleAuthenticationException(AuthenticationException e) {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
    }
}
