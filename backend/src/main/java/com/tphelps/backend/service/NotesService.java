package com.tphelps.backend.service;

import com.openai.client.OpenAIClient;
import com.openai.client.okhttp.OpenAIOkHttpClient;
import com.openai.models.ChatModel;
import com.openai.models.chat.completions.StructuredChatCompletion;
import com.openai.models.chat.completions.StructuredChatCompletionCreateParams;
import com.tphelps.backend.dtos.notes.SaveNotesRequest;
import com.tphelps.backend.repository.NotesRepository;

import static com.tphelps.backend.service.HttpRequestService.rcloneHttpRequestGetFile;
import static com.tphelps.backend.service.HttpRequestService.rcloneHttpRequestPost;

import org.jooq.tools.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.stereotype.Service;

import java.io.File;
import java.util.List;


@Service
public class NotesService {

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
     * @param username - username for the db save
     * @throws IllegalStateException
     * @throws EmptyResultDataAccessException
     */
    public void saveNotesToCloud(SaveNotesRequest notesRequest, String username)
            throws IllegalStateException, EmptyResultDataAccessException {

        String title = notesRequest.title().replace(" ", "_");
        // TODO multi-thread this to do multiple things
        JSONObject pathObject = rcloneHttpRequestPost(title, notesRequest.notes(), username);
        String drive = pathObject.get("drive").toString();
        String path = pathObject.get("path").toString();

        if(username == null) {
            throw new IllegalStateException("Username is null from security context");
        }
        notesRepository.saveNotePathToDatabase(drive + path, username);

    }

    /**
     * Service method for pulling a file from the Google Drive and returning it to the user
     * @param path - the path where the file is stored in the cloud
     */
    public File fetchNoteFromGoogleDrive(String path){
        String fileName = path.substring(path.lastIndexOf("/") + 1);
        String pathToTempFile = rcloneHttpRequestGetFile(path) + "/" + fileName;
        return new File(pathToTempFile);
    }

    public String generateStudyGuide(String notes){
        // Build the request parameters
        StructuredChatCompletionCreateParams<String> params = StructuredChatCompletionCreateParams.<String>builder()
                .addUserMessage("Generate a study guide for the following notes: " + notes)
                .model(ChatModel.GPT_4)
                .responseFormat(String.class)
                .build();

        List<StructuredChatCompletion.Choice<String>> results = client.chat().completions().create(params).choices();

        // Extract the actual content from the Choice object
        String studyGuide = String.valueOf(results.get(0).message().content());

        return studyGuide;
    }

}
