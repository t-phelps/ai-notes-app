package com.tphelps.backend.service.payment.enums;

public enum StripeEventEnum {
    DELETED("customer.subscription.deleted"),
    TRIAL_WILL_END("customer.subscription.trial_will_end"),
    CREATED("customer.subscription.created"),
    UPDATED("customer.subscription.updated"),
    ENTITLEMENT_SUMMARY_UPDATED("entitlements.active_entitlement_summary.updated");

    private final String value;

    StripeEventEnum(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }
}
