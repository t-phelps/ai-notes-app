package com.tphelps.backend.repository;

import com.tphelps.backend.enums.NoteGraphingStatus;
import com.tphelps.backend.service.pojos.NoteGraphingJob;
import org.jooq.DSLContext;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.stereotype.Repository;

import java.util.List;

import static test.generated.tables.Jobs.JOBS;

@Repository
public class JobsRepository {

    private final DSLContext dslContext;

    public JobsRepository(DSLContext dslContext) {
        this.dslContext = dslContext;
    }

    /**
     * Create a job record in the db to be used in the NotesGraphingService
     * @param noteId - note_id fk from user_note_history
     * @param status - status of job, default PENDING
     */
    public void createJob(int noteId, String status){
        int rowsAffected = dslContext
                .insertInto(JOBS)
                .set(JOBS.NOTE_ID, noteId)
                .set(JOBS.STATUS, status)
                .execute();

        if(rowsAffected == 0) {
            throw new EmptyResultDataAccessException(1);
        }
    }

    /**
     * Fetch the 10 oldest jobs with the status PENDING
     * @return - list of jobs
     */
    public List<NoteGraphingJob> fetchPendingJobs(){
        return dslContext
                .select(JOBS.NOTE_ID, JOBS.STATUS, JOBS.ATTEMPT_COUNT)
                .from(JOBS)
                .where(JOBS.STATUS.eq(NoteGraphingStatus.PENDING.getValue()))
                .orderBy(JOBS.CREATED_AT.asc())
                .limit(10)
                .fetchInto(NoteGraphingJob.class);
    }

    /**
     * Update a job status when its being processed
     * @param noteId - noteId to match on
     */
    public void updateJobStatus(int noteId){
        int rowsAffected = dslContext
                .update(JOBS)
                .set(JOBS.STATUS, NoteGraphingStatus.PROCESSING.getValue())
                .where(JOBS.NOTE_ID.eq(noteId))
                .execute();

        if(rowsAffected == 0) {
            throw new EmptyResultDataAccessException(1);
        }
    }

    /**
     * Update a job when it failed
     * @param noteId - noteId to match on
     * @param status - updated status
     * @param attemptCount - updated attempt count
     * @param error - error message from failure exception
     */
    public void updateJobStatus(int noteId, String status, short attemptCount, String error){
        int rowsAffected = dslContext
                .update(JOBS)
                .set(JOBS.STATUS, status)
                .set(JOBS.ATTEMPT_COUNT, attemptCount)
                .set(JOBS.LAST_ERROR, error)
                .where(JOBS.NOTE_ID.eq(noteId))
                .execute();

        if(rowsAffected == 0) {
            throw new EmptyResultDataAccessException(1);
        }
    }


    public void setJobCompleted(int noteId){
        int rowsAffected = dslContext
                .update(JOBS)
                .set(JOBS.STATUS, NoteGraphingStatus.COMPLETED.getValue())
                .where(JOBS.NOTE_ID.eq(noteId))
                .execute();

        if(rowsAffected == 0) {
            throw new EmptyResultDataAccessException(1);
        }
    }
}
