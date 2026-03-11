package com.tphelps.backend.dtos.payment;

import java.time.OffsetDateTime;

public record SubscriptionCreationDto(String customerId, String subscriptionId, OffsetDateTime startDate,
                                      OffsetDateTime created, OffsetDateTime currentPeriodStart, OffsetDateTime currentPeriodEnd,
                                      String priceId, String latestInvoice) {
}
