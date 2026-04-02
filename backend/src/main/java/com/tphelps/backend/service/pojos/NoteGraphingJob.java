package com.tphelps.backend.service.pojos;

public record NoteGraphingJob(int noteId, String status, short attemptCount, String username) {
}
