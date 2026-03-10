package com.tphelps.backend.dtos;

import java.util.UUID;

public record PasswordResetRequest(String password, UUID uuid) {
}
