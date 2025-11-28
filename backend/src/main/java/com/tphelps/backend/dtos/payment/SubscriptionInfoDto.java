package com.tphelps.backend.dtos.payment;

public record SubscriptionInfoDto(String customerId, String subscriptionId,  String status, Long startDate,
                                  Long created, Long currentPeriodStart, Long currentPeriodEnd,
                                  String priceId) {
}
