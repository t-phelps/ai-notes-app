package com.tphelps.backend.controller;

import com.tphelps.backend.controller.pojos.StudyGuide;
import com.tphelps.backend.dtos.notes.SaveNotesRequest;
import com.tphelps.backend.service.NotesService;
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
import java.nio.file.Files;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/notes")
public class NotesController {

    private static final Logger log = LoggerFactory.getLogger(NotesController.class);
    private final NotesService notesService;

    @Autowired
    public NotesController(NotesService notesService) {
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
            notesService.saveNotesToCloud(notes, userDetails.getUsername());
            return ResponseEntity.ok().build();
        }catch(IllegalStateException | EmptyResultDataAccessException e){
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
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
        }

        try{
            File file = notesService.fetchNoteFromGoogleDrive(path, userDetails.getUsername());

            // causing very long log within LOG not ERROR
            StreamingResponseBody stream = (StreamingResponseBody) outputStream -> {
                try(InputStream inputStream = new FileInputStream(file)){
                    inputStream.transferTo(outputStream);
                }finally{
                    boolean deleted = file.delete();
                    if(!deleted){
                        System.out.println("Could not delete file");
                    }
                }
            };

            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + file.getName() + "\"")
                    .header("Access-Control-Expose-Headers", "Content-Disposition")
                    .contentType(MediaType.APPLICATION_OCTET_STREAM)
                    .contentLength(file.length())
                    .body(stream);
        }catch(IllegalStateException | EmptyResultDataAccessException e){
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Utilize openai-api to generate a study guide from the notes passed in from the user within the notes request
     * @param notes - populated {@link SaveNotesRequest} from the user
     * @return response containing study guide on success
     */
    @PostMapping("/generate-study-guide")
    public ResponseEntity<StreamingResponseBody> generateStudyGuide(@RequestBody SaveNotesRequest notes) {

        if(validateNotes(notes)) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
        }
        try{
            // TODO finish implementation
            StudyGuide studyGuide = notesService.generateStudyGuide(notes.title(), notes.notes());
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
                    e.printStackTrace();
                }
            };
            return ResponseEntity.ok()
                    .contentType(MediaType.TEXT_PLAIN)
                    .body(stream);
        }catch(Exception e){
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
