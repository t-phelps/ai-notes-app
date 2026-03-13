package com.tphelps.backend.controller.exceptions;

public class IllegalAccessTokenException extends Exception {
    public IllegalAccessTokenException(String message) {
        super(message);
    }
}
