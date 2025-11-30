package com.tphelps.backend.repository;

import org.jooq.DSLContext;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.stereotype.Repository;

import static test.generated.tables.UserNoteHistory.USER_NOTE_HISTORY;
import test.generated.tables.pojos.UserNoteHistory;

import java.time.LocalTime;

@Repository
public class NotesRepository {

    private final DSLContext dslContext;

    public NotesRepository(DSLContext dslContext) {
        this.dslContext = dslContext;
    }

    /**
     * Save user note info to have a path to retrieve later from the drive
     * @param pathToNote - a path that exists in the drive AFTER it was already put there
     * @param username - the user for which we want to save their path to note
     * @throws EmptyResultDataAccessException if row not set
     */
    public void saveNotePathToDatabase(String pathToNote, String username){
        int rowsAffected = dslContext
                .insertInto(USER_NOTE_HISTORY)
                .set(USER_NOTE_HISTORY.USERNAME, username)
                .set(USER_NOTE_HISTORY.LINK_TO_NOTE, pathToNote)
                .set(USER_NOTE_HISTORY.SAVED_AT, LocalTime.now())
                .execute();

        if (rowsAffected == 0) {
            throw new EmptyResultDataAccessException(1);
        }
    }
}
