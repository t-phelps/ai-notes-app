package com.tphelps.backend.service;

import com.openai.client.OpenAIClient;
import com.openai.client.okhttp.OpenAIOkHttpClient;
import com.openai.models.chat.completions.StructuredChatCompletion;
import com.openai.models.chat.completions.StructuredChatCompletionCreateParams;
import com.tphelps.backend.controller.pojos.StudyGuide;
import com.tphelps.backend.dtos.notes.SaveNotesRequest;
import com.tphelps.backend.enums.NoteGraphingStatus;
import com.tphelps.backend.enums.SubscriptionStatus;
import com.tphelps.backend.repository.JobsRepository;
import com.tphelps.backend.repository.NotesRepository;
import com.tphelps.backend.service.exceptions.UnauthorizedUserException;
import static com.tphelps.backend.service.HttpRequestService.rcloneHttpRequestGetFile;
import static com.tphelps.backend.service.HttpRequestService.rcloneHttpRequestPost;

import com.tphelps.backend.service.pojos.NoteEdges;
import org.jooq.tools.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.IOException;
import java.util.*;

@Service
public class NotesService {

    private final NotesRepository notesRepository;
    private final OpenAIClient client;
    private final CustomUserDetailsService customUserDetailsService;
    private final JobsRepository jobsRepository;

    private static final String AI_NOTES_FOLDER = "ai-notes/";
    private static final Logger logger = LoggerFactory.getLogger(NotesService.class);

    @Autowired
    public NotesService(
            NotesRepository notesRepository,
            CustomUserDetailsService customUserDetailsService,
            JobsRepository jobsRepository) {
        this.notesRepository = notesRepository;
        this.jobsRepository = jobsRepository;
        this.client = OpenAIOkHttpClient.fromEnv();
        this.customUserDetailsService = customUserDetailsService;
    }

    /**
     * Fetch clustered notes from the existing notes for the user
     * @param username - users username
     * @return - map of cluster name -> titles
     */
    public Map<String, Set<String>> fetchClusteredNotes(String username){
        // fetch all adjacency lists for user in db
        List<NoteEdges> edgesList = notesRepository.fetchNotesEdges(username);

        return NoteClusterer.clusterEdges(edgesList);
    }

    /**
     * Service method for making rclone request and saving the location where the notes were stored in the database
     * @param notesRequest - notes request containing the title and text
     * @param username - username for the db save
     * @throws IllegalStateException
     * @throws EmptyResultDataAccessException
     */
    public void saveNotesToCloud(SaveNotesRequest notesRequest, String username)
            throws IllegalStateException, EmptyResultDataAccessException, IOException {

        String title = notesRequest.title().replace(" ", "_");

        JSONObject pathObject = rcloneHttpRequestPost(title, notesRequest.notes(), username);
        String drive = pathObject.get("drive").toString();
        String path = pathObject.get("path").toString();

        if(username == null) {
            throw new IllegalStateException("Username is null from security context");
        }
        logger.trace("Saving user note path to database for user={} with path={}",
                username, path);

        int noteId = notesRepository.saveNoteToDatabase(drive + path,
                username,
                notesRequest.title(),
                notesRequest.notes());
        jobsRepository.createJob(noteId, NoteGraphingStatus.PENDING.getValue(), username);
    }

    /**
     * Service method for pulling a file from the Google Drive and returning it to the user
     * @param name - name of the file
     */
    public File fetchNoteFromGoogleDrive(String name, String username){
        String path = notesRepository.fetchNote(name);
        String fileName = path.substring(path.lastIndexOf("/") + 1);
        String updatedPath = AI_NOTES_FOLDER + username + "/" + fileName;
        String pathToTempFile = rcloneHttpRequestGetFile(updatedPath, username);
        return new File(pathToTempFile);
    }

    /**
     * Generate a study guide from OpenAI API based on users notes
     * @param title - title of notes
     * @param notes - content of notes
     * @param username - user to generate
     * @return - a StudyGuide object {@link StudyGuide}
     * @throws IllegalStateException
     */
    public StudyGuide generateStudyGuide(String title, String notes, String username) throws IllegalStateException{
        // Build the request parameters
        logger.trace("Initiating OpenAI api request for user={}", username);
        StructuredChatCompletionCreateParams<StudyGuide> params = StructuredChatCompletionCreateParams.<StudyGuide>builder()
                .addUserMessage("Generate a study guide for the following notes: " + notes)
                .model("gpt-4o-mini")
                .responseFormat(StudyGuide.class)
                .maxCompletionTokens(5000) // set a max on output tokens to prevent runaway responses / malicious input
                .build();

        List<StructuredChatCompletion.Choice<StudyGuide>> results = client.chat().completions().create(params).choices();

        // Extract the actual content from the Choice object
        Optional<StudyGuide> res = results.get(0).message().content();
        if(res.isEmpty()) {
            throw new IllegalStateException("Result from OpenAI API is empty");
        }
        logger.trace("Finished extracting content for study guide for user={}", username);
        return res.get();
    }

    /**
     * Validate a user has an existing subscription
     * @param username - user to validate
     * @throws UnauthorizedUserException
     */
    public void validateUsersSubscription(String username) throws UnauthorizedUserException {
        com.tphelps.backend.controller.pojos.SubscriptionData data = customUserDetailsService.getUserSubscriptionData(username);

        if(data == null || !SubscriptionStatus.ACTIVE.getValue().equals(data.status()) || data.generations_left() <= 0) {
            throw new UnauthorizedUserException("User subscription status is inactive");
        }
    }
}
