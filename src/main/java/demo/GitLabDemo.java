package demo;

import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;
import managers.Secrets;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.message.BasicNameValuePair;

//Demo para la API de Gitlab
public final class GitLabDemo {
    private static final Logger log = LogManager.getLogger();
    private static final String TOKEN = Secrets.GITLAB_TOKEN;
    private static final String ENDPOINT = "https://gitlab.com/api/graphql";
    private static final String QUERY = "{project(fullPath: \"test-group1183/mvg-blender\") {issue(iid: \"4\") {title description}}}";
    
    public static void main(final String[] args) throws Exception{
        log.debug("Launching Gitlab demo");
        HttpClient httpclient = HttpClients.createDefault();
        HttpPost httppost = new HttpPost(ENDPOINT);

        List<NameValuePair> params = new ArrayList<>();
        httppost.addHeader("Authorization", "Bearer " + TOKEN);
        params.add(new BasicNameValuePair("query", QUERY));
        httppost.setEntity(new UrlEncodedFormEntity(params, "UTF-8"));

        HttpResponse response = httpclient.execute(httppost);
        HttpEntity entity = response.getEntity();

        if (entity != null) {
            try (InputStream instream = entity.getContent()) {
                String json = new BufferedReader(
                    new InputStreamReader(instream, StandardCharsets.UTF_8))
                    .lines().collect(Collectors.joining("\n"));
                ObjectMapper objectMapper = new ObjectMapper();
                JsonNode jsonNode = objectMapper.readTree(json);
                log.info(jsonNode);
                JsonNode issue = jsonNode.get("data").get("project").get("issue");
                log.info("Issue title: " + issue.get("title").asText());
                log.info("Issue body: " + issue.get("description").asText().split("\n")[2]);
            }
        }
    }
}
