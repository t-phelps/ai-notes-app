package com.tphelps.backend.service;


import com.tphelps.backend.dtos.notes.SaveNotesRequest;
import com.tphelps.backend.repository.NotesRepository;
import static com.tphelps.backend.service.HttpRequestService.rcloneHttpRequest;

import org.jooq.tools.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;


@Service
public class NotesService {

    private final NotesRepository notesRepository;

    @Autowired
    public NotesService(NotesRepository notesRepository) {
        this.notesRepository = notesRepository;
    }

    /**
     * Service method for making rclone request and saving the location where the notes were stored in the database
     * @param notesRequest - notes request containing the title and text
     * @param authentication - the authentication object to get the username
     * @throws IllegalStateException
     * @throws EmptyResultDataAccessException
     */
    public void saveNotesToCloud(SaveNotesRequest notesRequest, Authentication authentication)
            throws IllegalStateException, EmptyResultDataAccessException {
        String title = notesRequest.title().replace(" ", "_");
        // TODO multi-thread this to do multiple things
        JSONObject pathObject = rcloneHttpRequest(title,  notesRequest.notes());
        String drive = pathObject.get("drive").toString();
        String path = pathObject.get("path").toString();

        UserDetails principal = (UserDetails) authentication.getPrincipal();
        String username = principal.getUsername();
        if(username == null) {
            throw new IllegalStateException("Username is null from security context");
        }
        notesRepository.saveNotePathToDatabase(drive + path, username);

    }
}
