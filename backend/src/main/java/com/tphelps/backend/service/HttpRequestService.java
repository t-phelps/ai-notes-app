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
import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;

public class HttpRequestService {

    @Value("rclone.user")
    private static String USERNAME;

    @Value("rclone.password")
    private static String PASSWORD;

    /**
     * Service method for sending an rclone http request to local remote running on machine.
     * Executes a
     * @param title
     * @param notes
     * @return
     */
    public static JSONObject rcloneHttpRequestPost(String title, String notes, String username){
        try {
            // create a temp file with notes written to file
            String pathToTempFile = createFileWithNotes(title, notes);

            // pass a new file object to get the file to the tempFile
            JSONObject jsonObject = createJsonObjectForPost(new File(pathToTempFile), username);

            buildHttpPostRequest(jsonObject);

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
     * Calls rclone remote server for fetching the file for the user
     * @param path - the path in the remote to the file
     * @return - a path to the temp file created
     */
    public static String rcloneHttpRequestGetFile(String path){
        try {
            String pathToTempFile = new File(System.getProperty("java.io.tmpdir")).getAbsolutePath();

            JSONObject object = createJsonObjectForFetch(path, pathToTempFile);

            buildHttpPostRequest(object);

            return pathToTempFile;

        }catch(URISyntaxException | IOException e){
            throw new IllegalStateException(e);
        }
    }

    /**
     *
     * @param notes
     */
    public static void openAiRequest(String notes){

    }

    /**
     * Build and executes an HTTP post request
     * @param jsonObject - the JSON object for the post request (rclone remote setup)
     * @throws URISyntaxException
     * @throws IOException
     */
    private static void buildHttpPostRequest(JSONObject jsonObject) throws URISyntaxException, IOException {
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

        String response = executePostRequest(post);

        if(response == null){
            throw new IOException("Response from http request is null, Rclone server could be down");
        }
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
    private static JSONObject createJsonObjectForPost(File tempFile, String username) throws IOException {

        JSONObject jsonObject = new JSONObject();
        jsonObject.put("srcFs", tempFile.getParent());
        jsonObject.put("srcRemote", tempFile.getName());
        jsonObject.put("dstFs", "gdrive:");
        jsonObject.put("dstRemote", "/ai-notes/" + username + "/" + tempFile.getName());

        return jsonObject;
    }

    /**
     * Copy file from remote into local /tmp dir for temporary storage
     * @param path - the path of the file in the remote
     * @param pathToTempFile - the path to the temp file created in /tmp
     * @return - a JSONObject containing the necessary info for a rclone remote http server call
     * @throws IOException
     */
    private static JSONObject createJsonObjectForFetch(String path, String pathToTempFile) throws IOException {

        JSONObject jsonObject = new JSONObject();
        jsonObject.put("srcFs", "gdrive:");
        jsonObject.put("srcRemote", path);
        jsonObject.put("dstFs", pathToTempFile);
        jsonObject.put("dstRemote", "");

        return jsonObject;
    }

    /**
     * Execute the http post request
     * @param post an HttpPost object
     * @return - a string containing the entity response
     * @throws IOException
     */
    private static String executePostRequest(HttpPost post) throws IOException {
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
