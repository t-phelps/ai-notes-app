package com.tphelps.backend.service;

import org.apache.http.HttpEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.jooq.tools.json.JSONObject;
import org.springframework.beans.factory.annotation.Value;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;

public class HttpRequestService {

    @Value("rclone.user")
    private static String USERNAME;

    @Value("rclone.password")
    private static String PASSWORD;

    @Value("open.ai.key")
    private static String OPEN_AI_KEY;

    /**
     * Service method for sending an rclone http request to local remote running on machine.
     * Executes a
     * @param title
     * @param notes
     * @return
     */
    public static JSONObject rcloneHttpRequest(String title, String notes){
        try {
            // create a temp file with notes written to file
            String pathToTempFile = createFileWithNotes(title, notes);

            // pass a new file object to get the file to the tempFile
            JSONObject jsonObject = createJsonObject(new File(pathToTempFile));


            URI uri = new URIBuilder()
                    .setScheme("http")
                    .setHost("127.0.0.1")
                    .setPort(5572)
                    .setPath("/operations/copyfile")
                    .build();

            // build our post request with header and entity (jsonObject)
            HttpPost post = new HttpPost(uri);
            post.setHeader("Content-Type", "application/json");
            post.setEntity(new StringEntity(jsonObject.toString()));
//            post.setHeader("Authorization", "Basic " +
//                    Base64.getEncoder().encodeToString((USERNAME + ":" + PASSWORD).getBytes()));

            String response = executeRequest(post);

            if(response == null){
                throw new IOException("Response from http request is null, Rclone server could be down");
            }

            Files.deleteIfExists(Path.of(pathToTempFile));

            JSONObject object = new JSONObject();
            object.put("drive", jsonObject.get("dstFs"));
            object.put("path", jsonObject.get("dstRemote"));

            return object;
        }catch(IOException | URISyntaxException e){
            throw new IllegalStateException(e);
        }

    }

    /**
     *
     * @param title
     * @param notes
     */
    public static void openAiRequest(String title, String notes){

    }

    /**
     * Create a new file with the name of the file set to the title and the notes written to said file
     * @param title - name of file created
     * @param notes - text file contains
     * @return a String containing the path to the file
     * @throws IOException
     */
    private static String createFileWithNotes(String title, String notes) throws IOException {
        File tempFile = new File(System.getProperty("java.io.tmpdir"), title + ".txt");
        Files.writeString(tempFile.toPath(), notes);

        return tempFile.getAbsolutePath();
    }

    /**
     * Create a json object containing the information for the rclone request
     * @param tempFile - the file to get the attributes from to build to json object
     * @return - a json object containing the information for the rclone request
     * @throws IOException
     */
    private static JSONObject createJsonObject(File tempFile) throws IOException {

        JSONObject jsonObject  = new JSONObject();
        jsonObject.put("srcFs", tempFile.getParent());
        jsonObject.put("srcRemote", tempFile.getName());
        jsonObject.put("dstFs", "gdrive:");
        jsonObject.put("dstRemote", "/ai-notes/" + tempFile.getName());

        return jsonObject;
    }

    /**
     * Execute the http post request to the local rclone server
     * @param post an HttpPost object
     * @return - a string containing the entity response
     * @throws IOException
     */
    private static String executeRequest(HttpPost post) throws IOException {
        try(CloseableHttpClient httpClient = HttpClients.createDefault()) {
            CloseableHttpResponse response = httpClient.execute(post);
            HttpEntity entity = response.getEntity();

            if(entity != null) {
                return EntityUtils.toString(entity);

            }

            return null;
        }
    }


}
