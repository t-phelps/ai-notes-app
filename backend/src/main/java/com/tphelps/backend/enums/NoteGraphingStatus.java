package com.tphelps.backend.enums;

public enum NoteGraphingStatus {

    PENDING("pending"),
    PROCESSING("processing"),
    FAILED("failed"),
    COMPLETED("completed");

    private final String value;

    NoteGraphingStatus(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }
}
