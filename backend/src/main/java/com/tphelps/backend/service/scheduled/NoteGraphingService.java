package com.tphelps.backend.service.scheduled;

import com.tphelps.backend.enums.NoteGraphingStatus;
import com.tphelps.backend.repository.JobsRepository;
import com.tphelps.backend.repository.NotesRepository;
import com.tphelps.backend.service.exceptions.EmptyNoteContentException;
import com.tphelps.backend.service.pojos.*;
import com.tphelps.backend.service.util.TextUtils;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
public class NoteGraphingService {

    private record Pair(Map<Integer, Set<String>> noteIdToTokenizedWordsMap, List<Integer> noteIds){}

    private final JobsRepository jobsRepository;
    private final NotesRepository notesRepository;

    private static final double THRESHOLD = 0.3;
    private static final short MAX_RETRIES = 3;

    private static final Logger logger = LoggerFactory.getLogger(NoteGraphingService.class);

    public NoteGraphingService(JobsRepository jobsRepository, NotesRepository notesRepository) {
        this.jobsRepository = jobsRepository;
        this.notesRepository = notesRepository;
    }

    /**
     * Scheduled job for creating relationships between notes
     */
    @Scheduled(initialDelay = 5_000, fixedDelay = 300_000)
    public void createJob(){

        List<NoteGraphingJob> noteGraphingJobs = jobsRepository.fetchPendingJobs();
        if(noteGraphingJobs == null || noteGraphingJobs.isEmpty()){
            logger.info("No pending note graphing jobs found");
            return;
        }

        for (NoteGraphingJob noteGraphingJob : noteGraphingJobs) {
            try {
                jobsRepository.updateJobStatus(noteGraphingJob.noteId());

                processJob(noteGraphingJob);

                jobsRepository.setJobCompleted(noteGraphingJob.noteId(), noteGraphingJob.attemptCount());

            }catch(EmptyResultDataAccessException e){

                handleFailedJob(noteGraphingJob, e.getMessage());

            }catch(Exception e){

                handleFailedJob(noteGraphingJob, e.getMessage());
            }
        }
    }

    /**
     * Creates an adjacency list for note edge links using Jaccard Similarity Forumla
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

        logger.info("Processing job for noteId {} with attempt count {}", noteGraphingJob.noteId(), noteGraphingJob.attemptCount());
        Map<Integer, NoteInformation> noteInformationMap = notesRepository.fetchUsersNotes(noteGraphingJob.noteId());

        List<NoteEdges> edgesList = notesRepository.fetchNotesEdges(noteGraphingJob.username());

        Map<Integer, Set<Integer>> existingAdjacencyMap = buildExistingAdjacencyMap(edgesList, noteInformationMap);

        // maps every note id to a set of tokenized words (excludes NTLK stop words)
        Pair pair = prepareNoteData(noteInformationMap);

        Map<Integer, List<NoteRelationshipInformation>> noteAdjacencyMap = createNoteAdjacencyMap(
                pair.noteIds,
                pair.noteIdToTokenizedWordsMap,
                existingAdjacencyMap,
                noteInformationMap);

        insertNoteLinks(noteAdjacencyMap, noteGraphingJob);
    }

    /**
     * Build the adjacency map from the edges stored in db
     * @param edgesList - edge list from db table note_link
     * @return - map of (noteId, Set of similar noteId)
     */
    private Map<Integer, Set<Integer>> buildExistingAdjacencyMap(
            List<NoteEdges> edgesList,
            Map<Integer, NoteInformation> noteInformationMap){
        Map<Integer, Set<Integer>> existingAdjacencyMap = new HashMap<>();

        // pre-compute all nodes so we don't lose any that don't have a relationship
        for(Map.Entry<Integer, NoteInformation> entry : noteInformationMap.entrySet()){
            existingAdjacencyMap.put(entry.getKey(), new HashSet<>());
        }

        for(NoteEdges edge : edgesList) {
            Integer from = edge.from_note_id();
            Integer to = edge.to_note_id();
            // check for orphaned notes with no adjacency links
            if(to != null) {
                existingAdjacencyMap.get(from)
                        .add(to);

                existingAdjacencyMap.get(to)
                        .add(from);
            }
        }
        return existingAdjacencyMap;
    }

    /**
     * Tokenize note content and assign it to a note Id
     * @param noteInformationMap
     */
    private Pair prepareNoteData(Map<Integer, NoteInformation> noteInformationMap){

        List<Integer> noteIds = new ArrayList<>();
        Map<Integer, Set<String>> noteIdToTokenizedWordsMap = new HashMap<>();
        for(Map.Entry<Integer, NoteInformation> entry : noteInformationMap.entrySet()){
            if(entry.getKey() == null || entry.getValue() == null){
                throw new EmptyNoteContentException("Either the note's title or text are null");
            }
            int noteId = entry.getKey();
            noteIdToTokenizedWordsMap.put(noteId, TextUtils.tokenizeAndFilter(entry.getValue().title()));
            noteIds.add(noteId);
        }
        return new Pair(noteIdToTokenizedWordsMap, noteIds);
    }

    /**
     * Build the new adjacency map .... skips over all existing edges
     * @param noteIds
     * @param noteIdToTokenizedWordsMap
     * @param existingAdjacencyMap
     * @param noteInformationMap
     * @return
     */
    private Map<Integer, List<NoteRelationshipInformation>> createNoteAdjacencyMap(
            List<Integer> noteIds,
            Map<Integer, Set<String>> noteIdToTokenizedWordsMap,
            Map<Integer, Set<Integer>> existingAdjacencyMap,
            Map<Integer, NoteInformation> noteInformationMap){

        HashMap<Integer, List<NoteRelationshipInformation>> noteAdjacencyMap = new HashMap<>();
        // pre-compute all nodes so we don't lose any that don't have a relationship
        for(Integer noteId : noteIds){
            noteAdjacencyMap.put(noteId, new ArrayList<>());
        }

        for(int i = 0; i < noteIds.size(); i++) {
            int noteIdA = noteIds.get(i);
            Set<String> tokensA =  noteIdToTokenizedWordsMap.get(noteIdA);

            for(int j = i + 1; j < noteIds.size(); j++) {
                int noteIdB = noteIds.get(j);
                if(!hasAdjacencyLink(existingAdjacencyMap, noteIdA, noteIdB)){

                    Set<String> tokensB = noteIdToTokenizedWordsMap.getOrDefault(noteIdB, Collections.emptySet());

                    double similarityCoefficient = calculateJaccardSimilarityCoefficient(tokensA, tokensB);

                    if (similarityCoefficient > THRESHOLD) {

                        NoteInformation noteA = noteInformationMap.get(noteIdA);
                        NoteInformation noteB = noteInformationMap.get(noteIdB);

                        noteAdjacencyMap.get(noteIdA)
                                .add(new NoteRelationshipInformation(
                                        noteA.title(),
                                        noteB.noteId(),
                                        similarityCoefficient));

                        noteAdjacencyMap.get(noteIdB)
                                .add(new NoteRelationshipInformation(
                                        noteB.title(),
                                        noteA.noteId(),
                                        similarityCoefficient));
                    }
                }
            }
        }

        return noteAdjacencyMap;
    }

    /**
     * Insert compiled note links into the database
     * @param noteAdjacencyMap
     * @param noteGraphingJob
     */
    private void insertNoteLinks(
            Map<Integer, List<NoteRelationshipInformation>> noteAdjacencyMap,
            NoteGraphingJob noteGraphingJob){
        for(Map.Entry<Integer, List<NoteRelationshipInformation>> entry : noteAdjacencyMap.entrySet()){
            logger.trace("Inserting note relationships for noteId {}", entry.getKey());
            notesRepository.linkNotes(entry.getKey(), entry.getValue(), noteGraphingJob.username());
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
    private void handleFailedJob(NoteGraphingJob noteGraphingJob, String errorMessage) {
        logger.error("Job failed for noteId {} with {} attempts left",
                noteGraphingJob.noteId(),
                MAX_RETRIES - noteGraphingJob.attemptCount());

        short attempts = (short) (noteGraphingJob.attemptCount() + 1);
        if(attempts > MAX_RETRIES){ // max retry attempts allowed
            traceLogRetryMessage(NoteGraphingStatus.FAILED.getValue(), attempts);
            jobsRepository.updateJobStatus(
                    noteGraphingJob.noteId(),
                    NoteGraphingStatus.FAILED.getValue(),
                    (short) (attempts - 1),
                    errorMessage);
        }else{ // retry
            traceLogRetryMessage(NoteGraphingStatus.PENDING.getValue(), attempts);
            jobsRepository.updateJobStatus(
                    noteGraphingJob.noteId(),
                    NoteGraphingStatus.PENDING.getValue(),
                    attempts,
                    errorMessage
            );
        }
    }

    /**
     * Checks if an edge already exists between notes in the existing notes_link adjacency list from the databse for the user
     * @param existingAdjacencyList - existing adjacency list
     * @param noteIdA - from_noteId
     * @param noteIdB - the to_note_id link which makes the edge
     * @return - true if a link exists
     */
    private boolean hasAdjacencyLink(
            Map<Integer, Set<Integer>> existingAdjacencyList,
            int noteIdA,
            int noteIdB){

        return existingAdjacencyList.getOrDefault(noteIdA, Collections.emptySet()).contains(noteIdB);
    }

    /**
     * Extracted logging message
     * @param status -
     * @param attempts
     */
    private void traceLogRetryMessage(String status, short attempts){
        logger.trace("Inserting failed job into database with status {} and {} attempts remaining",
                status, attempts);
    }
}
