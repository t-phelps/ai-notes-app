package com.tphelps.backend.repository;

import com.tphelps.backend.service.pojos.NoteEdges;
import com.tphelps.backend.service.pojos.NoteInformation;
import com.tphelps.backend.service.pojos.NoteRelationshipInformation;
import org.jooq.DSLContext;
import org.jooq.Query;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.stereotype.Repository;

import static test.generated.tables.UserNoteHistory.USER_NOTE_HISTORY;
import static test.generated.tables.NoteLinks.NOTE_LINKS;

import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

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

    /**
     * Fetches a singular note from databse
     * @param title - title of the note
     * @return
     */
    public String fetchNote(String title){
        return dslContext
                .select(USER_NOTE_HISTORY.LINK_TO_NOTE)
                .from(USER_NOTE_HISTORY)
                .where(USER_NOTE_HISTORY.TITLE.eq(title))
                .fetchOneInto(String.class);
    }

    /**
     * Fetch a map of user notes with (noteId, NoteInformation)
     * @param noteId - note id to get username to fetch all users notes
     * @return - Map.of() noteId -> NoteInformation
     */
    public Map<Integer, NoteInformation> fetchUsersNotes(int noteId){
        return dslContext
                .select(USER_NOTE_HISTORY.TITLE, USER_NOTE_HISTORY.ID)
                .from(USER_NOTE_HISTORY)
                .where(USER_NOTE_HISTORY.USERNAME.eq(
                        dslContext.select(USER_NOTE_HISTORY.USERNAME)
                                .from(USER_NOTE_HISTORY)
                                .where(USER_NOTE_HISTORY.ID.eq(noteId))
                ))
                .fetchMap(USER_NOTE_HISTORY.ID, NoteInformation.class);
    }


    /**
     * Query to set the adjacency list for edges that link notes together
     * @param noteId - note in a map to link to a list of notes
     * @param neighbors - list of related notes
     */
    public void linkNotes(int noteId, List<NoteRelationshipInformation> neighbors, String username){
        List<Query> queries = new ArrayList<>();
        for(NoteRelationshipInformation neighbor : neighbors){
            queries.add(dslContext.insertInto(NOTE_LINKS)
                    .set(NOTE_LINKS.FROM_NOTE_ID, noteId)
                    .set(NOTE_LINKS.TO_NOTE_ID, neighbor.noteId())
                    .set(NOTE_LINKS.SIMILARITY_SCORE, neighbor.similarityScore())
                    .set(NOTE_LINKS.USERNAME, username));
        }

        dslContext.batch(queries).execute();
    }


    /**
     * Fetch existing note_links AND any possibly orphaned notes if they have no similar notes
     *
     * this could produce null to_note_ids due to the leftJoin
     * @param username
     * @return
     */
    public List<NoteEdges> fetchNotesEdges(String username){
        return dslContext
                .select(USER_NOTE_HISTORY.ID,
                        NOTE_LINKS.TO_NOTE_ID,
                        NOTE_LINKS.SIMILARITY_SCORE,
                        USER_NOTE_HISTORY.TITLE)
                .from(USER_NOTE_HISTORY)
                .leftJoin(NOTE_LINKS)
                .on(USER_NOTE_HISTORY.ID.eq(NOTE_LINKS.FROM_NOTE_ID))
                .where(USER_NOTE_HISTORY.USERNAME.eq(username))
                .fetch(r -> new NoteEdges(
                        r.get(USER_NOTE_HISTORY.ID),
                        r.get(NOTE_LINKS.TO_NOTE_ID),
                        r.get(NOTE_LINKS.SIMILARITY_SCORE),
                        r.get(USER_NOTE_HISTORY.TITLE)
                ));
    }
}
