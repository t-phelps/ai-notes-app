package com.tphelps.backend.controller;

import com.tphelps.backend.dtos.notes.SaveNotesRequest;
import com.tphelps.backend.service.NotesService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import static com.tphelps.backend.controller.authentication.AuthenticationValidator.validateUserAuthentication;

@RestController
@RequestMapping("/notes")
public class NotesController {


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
    public ResponseEntity<?> saveToCloud(@RequestBody SaveNotesRequest notes) {
        Authentication authentication = validateUserAuthentication();
        if(authentication == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        if(validateNotes(notes)) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
        }

        try {
            notesService.saveNotesToCloud(notes, authentication);
            return ResponseEntity.ok().build();
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
    public ResponseEntity<?> generateStudyGuide(SaveNotesRequest notes) {
        Authentication authentication = validateUserAuthentication();
        if(authentication == null) {
            return  ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        if(validateNotes(notes)) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
        }
        try{
            // TODO finish implementation

            return ResponseEntity.ok().build();
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
