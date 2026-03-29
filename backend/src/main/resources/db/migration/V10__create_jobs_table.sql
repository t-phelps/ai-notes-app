CREATE TABLE jobs(
    id SERIAL PRIMARY KEY,
    note_id INTEGER NOT NULL,
    status varchar(15) NOT NULL, -- (PENDING, PROCESSING, FAILED, COMPLETED)
    attempt_count SMALLINT NOT NULL DEFAULT 0,
    last_error TEXT, -- optional error messages
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_usernotehistory_noteid
                 FOREIGN KEY (note_id)
                 REFERENCES user_note_history(id)
                 ON DELETE CASCADE
);