package com.tphelps.backend.enums;

public enum SubscriptionLevel {

    BASIC(50),
    PRO(200),
    PREMIUM(600);

    private final int value;

    SubscriptionLevel(int value) {
        this.value = value;
    }

    public int getValue() {
        return value;
    }

}
