package com.tphelps.backend.service.pojos;

public record NoteEdges(
        int from_note_id,
        Integer to_note_id,
        Double similarity_score,
        String title){
}
