package demo;

import org.apache.logging.log4j.Logger;

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
import org.apache.logging.log4j.LogManager;

//Demo para la API de Zenhub
public final class ZenHubDemo {
  private static final String TOKEN = Secrets.ZENHUB_TOKEN;
  private static final String ENDPOINT = "https://api.zenhub.com/public/graphql";
  private static final String QUERY = "{issueByInfo(repositoryGhId:366781499, issueNumber:4){title body}}";
  private static final Logger log = LogManager.getLogger();
  
  public static void main(final String[] args) throws Exception{
    log.debug("Launching Zenhub demo");
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
        JsonNode issue = jsonNode.get("data").get("issueByInfo");
        log.info("Issue title: " + issue.get("title").asText());
        log.info("Issue body: " + issue.get("body").asText());
      }
    }
  }
}