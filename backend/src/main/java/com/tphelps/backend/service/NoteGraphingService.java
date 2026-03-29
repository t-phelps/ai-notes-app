package com.tphelps.backend.service;

import com.tphelps.backend.enums.NoteGraphingStatus;
import com.tphelps.backend.repository.JobsRepository;
import com.tphelps.backend.repository.NotesRepository;
import com.tphelps.backend.service.pojos.NoteGraphingJob;
import com.tphelps.backend.service.pojos.NoteInformation;
import com.tphelps.backend.service.util.TextUtils;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class NoteGraphingService {

    private final JobsRepository jobsRepository;
    private final NotesRepository notesRepository;

    public NoteGraphingService(JobsRepository jobsRepository, NotesRepository notesRepository) {
        this.jobsRepository = jobsRepository;
        this.notesRepository = notesRepository;
    }

    @Scheduled(fixedDelay = 300_000) // fixedDelay prevents overlap of multiple schedulers running at once
    public void createJob(){

        List<NoteGraphingJob> noteGraphingJobs = jobsRepository.fetchPendingJobs();

        for (NoteGraphingJob noteGraphingJob : noteGraphingJobs) {
            try {
                jobsRepository.updateJobStatus(noteGraphingJob.noteId());

                processJob(noteGraphingJob);

                jobsRepository.setJobCompleted(noteGraphingJob.noteId());

            }catch(EmptyResultDataAccessException e){

                handleFailedJob(noteGraphingJob, e.getMessage());

            }
        }
    }

    /**
     * Handle the graphing of a note through Jaccard Similarity Forumla
     *
     * Retrieve a note from the db, utilize its title for the formula,
     * create edges for related notes, store related edges in the note_links table
     *
     * Could also save a notes set (the words contained in the title or text) in the db
     * to prevent having to parse everytime we pull it out
     *
     * @param noteGraphingJob - the job to process
     */
    private void processJob(NoteGraphingJob noteGraphingJob) {
        List<NoteInformation> noteInformationList = notesRepository.fetchUsersNotes(noteGraphingJob.noteId());

        // maps every note id to a set of tokenized words (excludes NTLK stop words)
        Map<Integer, Set<String>> noteTokenMap = noteInformationList
                .stream()
                .collect(Collectors.toMap(
                        NoteInformation::noteId, // note id (key)
                        note -> TextUtils.tokenizeAndFilter(note.title()) // set of tokenized and filtered title words
                ));

        for(Map.Entry<Integer, Set<String>> entryA : noteTokenMap.entrySet()){

            int noteIdA =  entryA.getKey();
            Set<String> tokensA = entryA.getValue();

            for(Map.Entry<Integer, Set<String>> entryB : noteTokenMap.entrySet()){

                int noteIdB =  entryB.getKey();
                if(noteIdA == noteIdB) {
                    continue;
                }
                Set<String> tokensB = entryB.getValue();

                double similarityCoefficient = calculateJaccardSimilarityCoefficient(tokensA, tokensB);
                // 0.3 is the score deemed to be enough for now (can change later)
                // coefficient > 0.3 deems a relationship
                if(similarityCoefficient > 0.3){

                }

            }
        }
    }

    /**
     * Calculate jaccard similarity coefficient of 2 sets
     * @param set1 - set1
     * @param set2 - set2
     * @return - the coefficient score
     */
    private double calculateJaccardSimilarityCoefficient(Set<String> set1, Set<String> set2) {
        Set<String> intersection = new HashSet<>(set1);
        intersection.retainAll(set2);

        Set<String> union = new HashSet<>(set1);
        union.addAll(set2);

        if (union.isEmpty()) return 0.0;
        return (double) intersection.size() / union.size();
    }

    /**
     * Handle a failed job
     * @param noteGraphingJob - failed job
     * @param errorMessage - error to update db column for errors
     */
    private  void handleFailedJob(NoteGraphingJob noteGraphingJob, String errorMessage) {

        short attempts = (short) (noteGraphingJob.attemptCount() + 1);
        if(attempts > 3){ // max retry attempts allowed
            jobsRepository.updateJobStatus(
                    noteGraphingJob.noteId(),
                    NoteGraphingStatus.FAILED.getValue(),
                    attempts,
                    errorMessage);
        }else{ // retry
            jobsRepository.updateJobStatus(
                    noteGraphingJob.noteId(),
                    NoteGraphingStatus.PENDING.getValue(),
                    attempts,
                    errorMessage
            );
        }

    }
}
