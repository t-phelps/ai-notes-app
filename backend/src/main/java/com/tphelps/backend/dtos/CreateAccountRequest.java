package com.tphelps.backend.dtos;

public record CreateAccountRequest(String email, String username, String password) {
}
