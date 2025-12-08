package com.tphelps.backend.service;


import com.openai.client.OpenAIClient;
import com.openai.client.okhttp.OpenAIOkHttpClient;
import com.openai.models.ChatModel;
import com.openai.models.chat.completions.ChatCompletion;
import com.openai.models.chat.completions.ChatCompletionCreateParams;
import com.openai.models.chat.completions.StructuredChatCompletion;
import com.openai.models.chat.completions.StructuredChatCompletionCreateParams;
import com.openai.models.responses.Response;
import com.openai.models.responses.ResponseCreateParams;
import com.openai.models.responses.StructuredResponseCreateParams;
import com.tphelps.backend.dtos.notes.SaveNotesRequest;
import com.tphelps.backend.repository.NotesRepository;
import static com.tphelps.backend.service.HttpRequestService.rcloneHttpRequest;

import org.jooq.tools.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

import com.openai.*;

import java.util.List;
import java.util.Optional;


@Service
public class NotesService {

    @Value("open.ai.key")
    private static String OPEN_AI_KEY;


    private final NotesRepository notesRepository;
    private final OpenAIClient client;

    @Autowired
    public NotesService(NotesRepository notesRepository, OpenAIClient client) {
        this.notesRepository = notesRepository;
        this.client = OpenAIOkHttpClient.fromEnv();
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

    public void generateStudyGuide(String notes){
        // Build the request parameters
        StructuredChatCompletionCreateParams<String> params = StructuredChatCompletionCreateParams.<String>builder()
                .addUserMessage("Generate a study guide for the following notes: " + notes)
                .model(ChatModel.GPT_4)
                .responseFormat(String.class)
                .build();

        List<StructuredChatCompletion.Choice<String>> results = client.chat().completions().create(params).choices();

        // Extract the actual content from the Choice object
        String studyGuide = results.get(0).message().content();

        return studyGuide;
    }

}
