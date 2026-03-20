package com.tphelps.backend.enums;

public enum SubscriptionStatus {
    ACTIVE("active"),
    INACTIVE("inactive"),
    CANCELED("canceled");

    private String value;

    SubscriptionStatus(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }
}
