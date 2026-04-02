package com.tphelps.backend.service;

import org.apache.commons.logging.impl.Log4JLogger;
import org.apache.http.HttpEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.jooq.tools.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

@Service
public class HttpRequestService {

    @Value("rclone.user")
    private static String USERNAME;

    @Value("rclone.password")
    private static String PASSWORD;

    private static final Logger logger = LoggerFactory.getLogger(HttpRequestService.class);

    /**
     * Service method for sending an rclone http request to local remote running on machine.
     * Executes a
     * @param title
     * @param notes
     * @return
     */
    public static JSONObject rcloneHttpRequestPost(String title, String notes, String username){
        try {
            logger.trace("Creating temporary file with title={} for user={}", title, username);
            // create a temp file with notes written to file
            String pathToTempFile = createFileWithNotes(title, notes);

            // pass a new file object to get the file to the tempFile
            JSONObject jsonObject = createJsonObjectForPost(new File(pathToTempFile), username);

            logger.trace("Initiating rclone post request for user={}", username);
            buildHttpPostRequest(jsonObject);

            // TODO get rid of this and move to a scheduled job
            logger.trace("Deleting temporary file with path={}", pathToTempFile);
            Files.deleteIfExists(Path.of(pathToTempFile));

            JSONObject object = new JSONObject();
            object.put("drive", jsonObject.get("dstFs"));
            object.put("path", jsonObject.get("dstRemote"));

            return object;
        }catch(IOException | URISyntaxException e){
            logger.atError().log("Exception caught while making rclone request for saving notes, ex={}, user={}",
                    e.getMessage(), username);
            throw new IllegalStateException(e);
        }
    }

    /**
     * Calls rclone remote server for fetching the file for the user
     * @param path - the path in the remote to the file
     * @return - a path to the temp file created
     */
    public static String rcloneHttpRequestGetFile(String path, String username){
        try {
            String tempDir = System.getProperty("java.io.tmpdir");
            String fileName = path.substring(path.lastIndexOf("/") + 1);
            String fullPath = tempDir + File.separator + fileName;

            JSONObject object = createJsonObjectForFetch(path, fileName);

            logger.trace("Initiating rclone get request for user={}", username);
            buildHttpPostRequest(object);

            File f = new File(tempDir + "/" + fileName);
            if(!f.exists() || !f.isFile()) {
                throw new IllegalStateException("File didn't transfer from the remote into local disk storage");
            }

            return fullPath;

        }catch(URISyntaxException | IOException e){
            logger.error("Exception caught fetching file from path={} for user={} with message={}",
                    path, username, e.getMessage());
            throw new IllegalStateException(e);
        }
    }

    /**
     * Build and executes an HTTP post request
     * @param jsonObject - the JSON object for the post request (rclone remote setup)
     * @throws URISyntaxException
     * @throws IOException
     */
    private static void buildHttpPostRequest(JSONObject jsonObject) throws URISyntaxException, IOException {

        HttpPost post = new HttpPost("http://127.0.0.1:5572/operations/copyfile");
        post.setHeader("Content-Type", "application/json; charset=UTF-8");

        // Make sure entity is UTF-8 encoded and has correct content length
        StringEntity entity = new StringEntity(jsonObject.toString(), StandardCharsets.UTF_8);
        entity.setContentType("application/json; charset=UTF-8");
        post.setEntity(entity);

        String response = executePostRequest(post);
        logger.trace("Rclone response={}", response);
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
        logger.trace("Creating JSON object for rclone post request for user={}", username);

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
     * @param fileName - name of file to be stored in /tmp
     * @return - a JSONObject containing the necessary info for a rclone remote http server call
     * @throws IOException
     */
    private static JSONObject createJsonObjectForFetch(String path, String fileName) throws IOException {
        JSONObject jsonObject = new JSONObject();
        jsonObject.put("srcFs", "gdrive:");
        jsonObject.put("srcRemote", path);
        jsonObject.put("dstFs", "tmp:");
        jsonObject.put("dstRemote", fileName);

        return jsonObject;
    }

    /**
     * Execute the http post request
     * @param post an HttpPost object
     * @return - a string containing the entity response
     * @throws IOException
     */
    private static String executePostRequest(HttpPost post) throws IOException {
        try (CloseableHttpClient httpClient = HttpClients.createDefault();
             CloseableHttpResponse response = httpClient.execute(post)) {

            HttpEntity entity = response.getEntity();

            if (entity == null) {
                throw new IOException("No response from rclone RC server");
            }

            // consume fully as UTF-8
            String result = EntityUtils.toString(entity, StandardCharsets.UTF_8);
            EntityUtils.consume(entity); // make sure entity is fully consumed
            return result;
        }
    }
}
