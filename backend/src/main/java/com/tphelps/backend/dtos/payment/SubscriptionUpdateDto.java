package com.tphelps.backend.dtos.payment;

public record SubscriptionUpdateDto(String customerId, String subscriptionId, String status) {
}
