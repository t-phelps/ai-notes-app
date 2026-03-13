package com.tphelps.backend.service.payment.enums;

public enum StripeEventEnum {
    CUSTOMER_SUBSCRIPTION_DELETED("customer.subscription.deleted"),
    CUSTOMER_SUBSCRIPTION_CREATED("customer.subscription.created"),
    CUSTOMER_SUBSCRIPTION_UPDATED("customer.subscription.updated"),
    INVOICE_PAYMENT_SUCCEEDED("invoice.payment_succeeded"),
    INVOICE_PAYMENT_FAILURE("invoice.payment_failed"),
    REFUND_CREATED("refund.created"),
    CHARGE_REFUNDED("charge.refunded"),
    ISSUING_DISPUTE_FUNDS_RESCINDED("issuing_dispute.funds_rescinded");

    private final String value;

    StripeEventEnum(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }
}
