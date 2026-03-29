package com.tphelps.backend.repository;

import com.tphelps.backend.service.pojos.NoteInformation;
import org.jooq.DSLContext;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.stereotype.Repository;

import static test.generated.tables.UserNoteHistory.USER_NOTE_HISTORY;

import java.time.LocalTime;
import java.util.List;

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
    public int saveNoteToDatabase(String pathToNote, String username, String title, String notes){
        Integer noteId = dslContext
                .insertInto(USER_NOTE_HISTORY)
                .set(USER_NOTE_HISTORY.USERNAME, username)
                .set(USER_NOTE_HISTORY.LINK_TO_NOTE, pathToNote)
                .set(USER_NOTE_HISTORY.SAVED_AT, LocalTime.now())
                .set(USER_NOTE_HISTORY.TITLE, title)
                .set(USER_NOTE_HISTORY.TEXT_CONTENT, notes)
                .returningResult(USER_NOTE_HISTORY.ID)
                .fetchOneInto(Integer.class);
        if(noteId == null){
            throw new EmptyResultDataAccessException(1);
        }
        return noteId;
    }

    public List<NoteInformation> fetchUsersNotes(int noteId){
        return dslContext
                .select(USER_NOTE_HISTORY.TITLE, USER_NOTE_HISTORY.ID)
                .from(USER_NOTE_HISTORY)
                .where(USER_NOTE_HISTORY.USERNAME.eq(
                        dslContext.select(USER_NOTE_HISTORY.USERNAME)
                                .from(USER_NOTE_HISTORY)
                                .where(USER_NOTE_HISTORY.ID.eq(noteId))
                ))
                .fetchInto(NoteInformation.class);
    }
}
