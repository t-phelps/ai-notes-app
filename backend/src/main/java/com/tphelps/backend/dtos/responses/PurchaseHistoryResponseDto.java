package com.tphelps.backend.dtos.responses;

public record PurchaseHistoryResponseDto(long current_period_start, long current_period_end, String status) {
}
