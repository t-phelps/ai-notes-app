package com.tphelps.backend.controller.exceptions;

public class IllegalRefreshTokenException extends Exception {
    public IllegalRefreshTokenException(String message) {
        super(message);
    }
}
