package com.tphelps.backend.dtos;

public record ChangePasswordRequest(String newPassword, String oldPassword) {
}
