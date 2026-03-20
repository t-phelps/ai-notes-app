package com.tphelps.backend.service.exceptions;

public class UnauthorizedUserException extends Exception {
    public UnauthorizedUserException(String message) {
        super(message);
    }
}
