package com.tphelps.backend.service;

import com.openai.client.OpenAIClient;
import com.openai.client.okhttp.OpenAIOkHttpClient;
import com.openai.models.chat.completions.StructuredChatCompletion;
import com.openai.models.chat.completions.StructuredChatCompletionCreateParams;
import com.tphelps.backend.controller.pojos.Steps;
import com.tphelps.backend.controller.pojos.StudyGuide;
import com.tphelps.backend.dtos.notes.SaveNotesRequest;
import com.tphelps.backend.repository.NotesRepository;

import static com.tphelps.backend.service.HttpRequestService.rcloneHttpRequestGetFile;
import static com.tphelps.backend.service.HttpRequestService.rcloneHttpRequestPost;

import org.jooq.tools.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.List;
import java.util.Optional;


@Service
public class NotesService {

    private final NotesRepository notesRepository;
    private final OpenAIClient client;
    private final CustomUserDetailsService customUserDetailsService;
    private static final String AI_NOTES_FOLDER = "ai-notes/";

    @Autowired
    public NotesService(
            NotesRepository notesRepository,
            OpenAIClient client,
            CustomUserDetailsService customUserDetailsService) {
        this.notesRepository = notesRepository;
        this.client = OpenAIOkHttpClient.fromEnv();
        this.customUserDetailsService = customUserDetailsService;
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
    public File fetchNoteFromGoogleDrive(String path, String username){
        String fileName = path.substring(path.lastIndexOf("/") + 1);
        String updatedPath = AI_NOTES_FOLDER + username + "/" + fileName;
        String pathToTempFile = rcloneHttpRequestGetFile(updatedPath);
        return new File(pathToTempFile);
    }

    public StudyGuide generateStudyGuide(String title, String notes) throws IllegalStateException{

        // Build the request parameters
        StructuredChatCompletionCreateParams<StudyGuide> params = StructuredChatCompletionCreateParams.<StudyGuide>builder()
                .addUserMessage("Generate a study guide for the following notes: " + notes)
                .model("gpt-4o-mini")
                .responseFormat(StudyGuide.class)
                .build();

        List<StructuredChatCompletion.Choice<StudyGuide>> results = client.chat().completions().create(params).choices();

        // Extract the actual content from the Choice object
        Optional<StudyGuide> res = results.get(0).message().content();
        if(res.isEmpty()) {
            throw new IllegalStateException("Result from OpenAI API is empty");
        }
        return res.get();
//        return writeStudyGuideToFile(res.get(), title);
    }

//    public void validateUsersSubscription(String username){
//        customUserDetailsService.loadSubscriptionData(username);
//    }

    /**
     * Write study guide to file with specific formatting
     * @param studyGuide - the study guide returned from openai api response
     * @param title - title of their notes
     * @return - file object containing their study guide
     * @throws IllegalStateException
     */
    private File writeStudyGuideToFile(StudyGuide studyGuide, String title)throws IllegalStateException {
        String tempDir = System.getProperty("java.io.tmpdir");
        String fullPath = tempDir + File.separator + title + "StudyGuide.txt";
        File file = new File(fullPath);

        if(!file.exists() || !file.isFile()){
            throw new  IllegalStateException("File does not exist or is not a file");
        }

        try(FileWriter fileWriter = new FileWriter(file)){
            List<Steps> responses = studyGuide.questions();
            for(int i = 0; i < studyGuide.questions().size(); i++){
                fileWriter.write(
                        String.format(
                                "%d. %s\n    %c. %s\n",
                                i + 1,
                                responses.get(i).question(),
                                'a',
                                responses.get(i).answer()
                        ));
            }
        }catch(IOException e){
            throw new IllegalStateException("Error writing study guide to file");
        }
        return file;
    }

}
