package com.tphelps.backend.dtos.payment;

public record SubscriptionCreationDto(String customerId, String subscriptionId, String status, Long startDate,
                                      Long created, Long currentPeriodStart, Long currentPeriodEnd,
                                      String priceId) {
}
