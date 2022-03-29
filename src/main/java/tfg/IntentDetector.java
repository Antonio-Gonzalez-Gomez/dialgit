package tfg;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import com.google.api.gax.rpc.ApiException;
import com.google.auth.oauth2.GoogleCredentials;
import com.google.cloud.dialogflow.v2.DetectIntentResponse;
import com.google.cloud.dialogflow.v2.QueryInput;
import com.google.cloud.dialogflow.v2.QueryResult;
import com.google.cloud.dialogflow.v2.SessionName;
import com.google.cloud.dialogflow.v2.SessionsClient;
import com.google.cloud.dialogflow.v2.TextInput;

public class IntentDetector {

    private static String CREDENTIALS = Secrets.GOOGLE_CREDENTIALS;
    private static String PROJECT = Secrets.GOOGLE_PROJECT_ID;

    public static String simpleDetect(String message) throws IOException, ApiException {
        GoogleCredentials credentials = GoogleCredentials.fromStream(new FileInputStream(CREDENTIALS));
        List<String> scopes = new ArrayList<>();
        scopes.add("https://www.googleapis.com/auth/dialogflow");
        credentials = credentials.createScoped(scopes);
        credentials.refreshIfExpired();
      
        try (SessionsClient sessionsClient = SessionsClient.create()) {
            SessionName session = SessionName.of(PROJECT, "123456789");
            TextInput.Builder textInput =
                TextInput.newBuilder().setText(message).setLanguageCode("en-US");
            QueryInput queryInput = QueryInput.newBuilder().setText(textInput).build();
            DetectIntentResponse response = sessionsClient.detectIntent(session, queryInput);
            QueryResult queryResult = response.getQueryResult();

            return queryResult.getIntent().getDisplayName();
        }
    }
}