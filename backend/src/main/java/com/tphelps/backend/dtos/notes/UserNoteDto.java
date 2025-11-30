package com.tphelps.backend.dtos.notes;

import java.time.LocalTime;

public record UserNoteDto(String pathToNote, LocalTime savedAt) {
}
