package com.tphelps.backend.service.exceptions;

public class EmptyNoteContentException extends RuntimeException {
    public EmptyNoteContentException(String message) {
        super(message);
    }
}
