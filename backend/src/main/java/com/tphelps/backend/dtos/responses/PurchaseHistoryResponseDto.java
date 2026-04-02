package com.tphelps.backend.dtos.responses;

import java.time.OffsetDateTime;

public record PurchaseHistoryResponseDto(OffsetDateTime current_period_end, String status) {
}
