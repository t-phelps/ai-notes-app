package com.tphelps.backend.service.pojos;

public record AdjacentNote(
        Integer to_note_id,
        Double similarity_score,
        String title) {
}
