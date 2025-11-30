CREATE TABLE IF NOT EXISTS user_note_history (
    id SERIAL primary key,
    username VARCHAR(255) NOT NULL,
    link_to_note TEXT NOT NULL,
    saved_at TIME NOT NULL,
    CONSTRAINT fk_usernotehistory_username
        FOREIGN KEY (username)
        REFERENCES users(username)
        ON DELETE CASCADE
);