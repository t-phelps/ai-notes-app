package com.tphelps.backend.controller;

import com.tphelps.backend.controller.pojos.StudyGuide;
import com.tphelps.backend.dtos.notes.SaveNotesRequest;
import com.tphelps.backend.service.CustomUserDetailsService;
import com.tphelps.backend.service.NotesService;
import com.tphelps.backend.service.exceptions.UnauthorizedUserException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Map;

@RestController
@RequestMapping("/notes")
public class NotesController {

    private static final Logger logger = LoggerFactory.getLogger(NotesController.class);
    private final NotesService notesService;

    @Autowired
    public NotesController(NotesService notesService, CustomUserDetailsService customUserDetailsService) {
        this.notesService = notesService;
    }

    /**
     * Save notes to google drive and save path to database
     * @param notes - the dto containing the notes and the title
     * @return - response code indicating success or not
     */
    @PostMapping("/to-cloud")
    public ResponseEntity<?> saveToCloud(
            @RequestBody SaveNotesRequest notes,
            @AuthenticationPrincipal UserDetails userDetails) {

        if(validateNotes(notes)) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
        }

        try {
            logger.info("User {} initiated saving notes to cloud with title: {}", userDetails.getUsername(), notes.title());
            notesService.saveNotesToCloud(notes, userDetails.getUsername());
            return ResponseEntity.ok().build();
        }catch(IllegalStateException | EmptyResultDataAccessException | IOException e){
            logger.error("Exception caught while saving notes to cloud for user={}", userDetails.getUsername());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Sends the desired file to the front end as a stream. File comes from the remote via rclone http server
     * @param body - contains the path of the file
     * @return response containing the file stream on success
     */
    @PostMapping("/download-note")
    public ResponseEntity<StreamingResponseBody> getDownloadNote(@RequestBody Map<String, String> body, @AuthenticationPrincipal UserDetails userDetails) {
        String path = body.get("path");
        if(path == null || path.isEmpty()){
            logger.error("Error in download note endpoint with an invalid path for user={}", userDetails.getUsername());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
        }

        try{
            logger.info("Initiating fetch from google drive for user={}", userDetails.getUsername());
            File file = notesService.fetchNoteFromGoogleDrive(path, userDetails.getUsername());

            logger.trace("Streaming response to frontend for user={}", userDetails.getUsername());
            StreamingResponseBody stream = outputStream -> {
                try(InputStream inputStream = new FileInputStream(file)){
                    inputStream.transferTo(outputStream);
                }finally{
                    boolean deleted = file.delete();
                    if(!deleted){
                        logger.error("Failed to delete file at path={} for user={}",
                                file.getAbsolutePath(), userDetails.getUsername());
                    }
                }
            };

            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + file.getName() + "\"")
                    .contentType(MediaType.APPLICATION_OCTET_STREAM)
                    .contentLength(file.length())
                    .body(stream);
        }catch(IllegalStateException | EmptyResultDataAccessException e){
            logger.error("Exception caught streaming note to frontend, user={} exception={}",
                    userDetails.getUsername(), e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Utilize openai-api to generate a study guide from the notes passed in from the user within the notes request
     * @param notes - populated {@link SaveNotesRequest} from the user
     * @return response containing study guide on success
     */
    @PostMapping("/generate-study-guide")
    public ResponseEntity<StreamingResponseBody> generateStudyGuide(
            @RequestBody SaveNotesRequest notes,
            @AuthenticationPrincipal UserDetails userDetails) {

        if(validateNotes(notes)) {
            logger.error("Empty generate study guide request for user={}", userDetails.getUsername());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
        }
        try{
            logger.info("Initiating study guide generation for user={} with title={}",
                    userDetails.getUsername(), notes.title());

            notesService.validateUsersSubscription(userDetails.getUsername());

            StudyGuide studyGuide = notesService.generateStudyGuide(
                    notes.title(), notes.notes(), userDetails.getUsername());

            logger.trace("Streaming response to front end for user={}", userDetails.getUsername());
            StreamingResponseBody stream = outputStream -> {
                try{
                    for(int i = 0; i < studyGuide.questions().size(); i++){
                        String line = String.format(
                                "%d. %s\n    %c. %s\n",
                                i + 1,
                                studyGuide.questions().get(i).question(),
                                'a',
                                studyGuide.questions().get(i).answer());
                        outputStream.write(line.getBytes(StandardCharsets.UTF_8));
                        outputStream.flush();
                    }
                }catch(IOException e){
                    logger.error(
                            "IOException occurred while streaming study guide to front end for user={} with exception={}",
                            userDetails.getUsername(), e.getMessage());
                }
            };
            return ResponseEntity.ok()
                    .contentType(MediaType.TEXT_PLAIN)
                    .body(stream);
        }catch(UnauthorizedUserException e){
            logger.error("Unauthorized user tried to access study guide feature user={} at UTC time={} with exception={}",
                    userDetails.getUsername(), Instant.now(), e.getMessage());
            return ResponseEntity.status(403).build();
        }
        catch(Exception e){
            logger.error("Exception occurred while generating study guide for user={} with exception={}",
                    userDetails.getUsername(), e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }

    }

    /**
     * Bad name, validate notes fields within dto
     * @param notes - the {@link SaveNotesRequest}
     * @return true if not valid, false if valid
     */
    private boolean validateNotes(SaveNotesRequest notes) {
        return notes.title().isEmpty() && notes.notes().isEmpty();
    }
}
