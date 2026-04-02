CREATE TABLE note_links (
    id SERIAL PRIMARY KEY,
    from_note_id INTEGER REFERENCES user_note_history(id) ON DELETE CASCADE,
    to_note_id INTEGER REFERENCES user_note_history(id) ON DELETE CASCADE,
    similarity_score DOUBLE PRECISION,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    UNIQUE (from_note_id, to_note_id) -- doesn't allow duplicate relationships
);

CREATE INDEX idx_from_note ON note_links(from_note_id);
CREATE INDEX idx_to_note ON note_links(to_note_id);
CREATE INDEX idx_from_similarity ON note_links(from_note_id, similarity_score DESC);