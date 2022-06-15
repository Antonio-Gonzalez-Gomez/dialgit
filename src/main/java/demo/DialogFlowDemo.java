package demo;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.cloud.dialogflow.v2.DetectIntentResponse;
import com.google.cloud.dialogflow.v2.QueryInput;
import com.google.cloud.dialogflow.v2.QueryResult;
import com.google.cloud.dialogflow.v2.SessionName;
import com.google.cloud.dialogflow.v2.SessionsClient;
import com.google.cloud.dialogflow.v2.TextInput;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import managers.Secrets;

public final class DialogFlowDemo {
    private static final String CREDENTIALS = Secrets.GOOGLE_CREDENTIALS;
    private static final String PROJECT = Secrets.GOOGLE_PROJECT_ID;
    private static final Logger log = LogManager.getLogger();
    public static void main(final String[] args) {
        log.debug("Launching DialogFlow demo");
        try {
            GoogleCredentials credentials = GoogleCredentials.fromStream(new FileInputStream(CREDENTIALS));
            List<String> scopes = new ArrayList<>();
            scopes.add("https://www.googleapis.com/auth/dialogflow");
            credentials = credentials.createScoped(scopes);
            credentials.refreshIfExpired();
            SessionsClient sessionsClient = SessionsClient.create();
            SessionName session = SessionName.of(PROJECT, "123456789");
            TextInput.Builder textInput =
                TextInput.newBuilder().setText("Hello!").setLanguageCode("en-US");
            QueryInput queryInput = QueryInput.newBuilder().setText(textInput).build();

            DetectIntentResponse response = sessionsClient.detectIntent(session, queryInput);
            QueryResult queryResult = response.getQueryResult();
            
            log.info("Expected message Intent: Default Welcome Intent");
            log.info("Received message Intent: " + queryResult.getIntent().getDisplayName());
        } catch (IOException ex) {
            log.error(ex.getMessage());
        }
    }
    
}
