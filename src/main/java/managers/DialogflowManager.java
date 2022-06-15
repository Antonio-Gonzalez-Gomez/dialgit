package managers;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;

import com.google.api.gax.rpc.ApiException;
import com.google.auth.oauth2.GoogleCredentials;
import com.google.cloud.dialogflow.v2.DetectIntentResponse;
import com.google.cloud.dialogflow.v2.QueryInput;
import com.google.cloud.dialogflow.v2.QueryResult;
import com.google.cloud.dialogflow.v2.SessionName;
import com.google.cloud.dialogflow.v2.SessionsClient;
import com.google.cloud.dialogflow.v2.TextInput;

public class DialogflowManager {
    private GoogleCredentials googleCredentials;
    private static final String CREDENTIALS = Secrets.GOOGLE_CREDENTIALS;
    private static final String PROJECT = Secrets.GOOGLE_PROJECT_ID;
    private static final Logger log = LogManager.getLogger();

    public DialogflowManager() {
        log.debug("Creating DialogFlow manager");
        try {
            this.googleCredentials = GoogleCredentials.fromStream(new FileInputStream(CREDENTIALS));
            List<String> scopes = new ArrayList<>();
            scopes.add("https://www.googleapis.com/auth/dialogflow");
            googleCredentials = googleCredentials.createScoped(scopes);
            this.googleCredentials.refreshIfExpired();
        } catch (IOException | ApiException e) {
            log.error(e.getMessage());
        }
    }

    public String simpleDetect(String message, String sessionId) {
        log.info("Sending query with session [" + sessionId + "]");
        try {
            this.googleCredentials.refreshIfExpired();
            SessionsClient sessionsClient = SessionsClient.create();
            SessionName session = SessionName.of(PROJECT, sessionId);
            TextInput.Builder textInput =
                TextInput.newBuilder().setText(message).setLanguageCode("en-US");
            QueryInput queryInput = QueryInput.newBuilder().setText(textInput).build();
            DetectIntentResponse response = sessionsClient.detectIntent(session, queryInput);
            QueryResult queryResult = response.getQueryResult();
            sessionsClient.shutdown();
            return queryResult.getIntent().getDisplayName();
        } catch (IOException e) {
            log.error(e.getMessage());
            return null;
        }
    }

    public String getDate(String message, String sessionId) {
        log.info("Sending query with session [" + sessionId + "]");
        try {
            this.googleCredentials.refreshIfExpired();
            SessionsClient sessionsClient = SessionsClient.create();
            SessionName session = SessionName.of(PROJECT, sessionId);
            TextInput.Builder textInput =
                TextInput.newBuilder().setText(message).setLanguageCode("en-US");
            QueryInput queryInput = QueryInput.newBuilder().setText(textInput).build();
            DetectIntentResponse response = sessionsClient.detectIntent(session, queryInput);
            QueryResult queryResult = response.getQueryResult();
            sessionsClient.shutdown();
            if (queryResult.getIntent().getDisplayName().equals("DateIntent")) {
                return queryResult.getParameters().getFieldsMap().get("date-time").getStringValue();
            }
            return "Date not found!";
        } catch (IOException e) {
            log.error(e.getMessage());
            return "Date not found!";
        }
    }
}