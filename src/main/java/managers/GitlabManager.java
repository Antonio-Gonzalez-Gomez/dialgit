package managers;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
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
import org.apache.logging.log4j.Logger;
import org.kohsuke.github.GHRepository;
import org.kohsuke.github.GHUser;

import pojo.*;

//Controlador de la API Gitlab
public final class GitlabManager {
    private static final String TOKEN = Secrets.GITLAB_TOKEN;
    private static final String ENDPOINT = "https://gitlab.com/api/graphql";
    private static final Logger log = LogManager.getLogger();

    private static JsonNode graphqlRequest(String query) {
        log.info("GraphQL query: " + query);
        HttpClient httpclient = HttpClients.createDefault();
        HttpPost httppost = new HttpPost(ENDPOINT);

        try {
            List<NameValuePair> params = new ArrayList<>();
            httppost.addHeader("Authorization", "Bearer " + TOKEN);
            params.add(new BasicNameValuePair("query", query));
            httppost.setEntity(new UrlEncodedFormEntity(params, "UTF-8"));
    
            HttpResponse response = httpclient.execute(httppost);
            HttpEntity entity = response.getEntity();
            InputStream instream = entity.getContent();

            String json = new BufferedReader(
                new InputStreamReader(instream, StandardCharsets.UTF_8))
                .lines().collect(Collectors.joining("\n"));
            ObjectMapper objectMapper = new ObjectMapper();
            instream.close();
            JsonNode res = objectMapper.readTree(json);
            log.info("Json response: " + res);
            return res;

        } catch(IOException e) {
            log.error(e.getMessage());
            return null;
        }
    }

    private static <T> String listParse(List<T> list, Function<T, String> parser) {
        String res = "[";
        for (T t : list)
            res = res.concat("\"" + parser.apply(t) + "\",");
        return res.substring(0, res.length() - 1).concat("]");
    }

    // Groups

    public static Boolean checkGroupExists(String path) {
        String query = "query {group(fullPath: \"" + path + "\") {path}}";
        JsonNode data = graphqlRequest(query);
        return data.has("data") && !data.get("data").get("group").isNull();
    }
    
    public static Group getGroup(String path) {
        String query = "query {group(fullPath: \"" + path + "\") {name path webUrl projects(last: 100) {" + 
            "nodes {name}} groupMembers(last: 100) {nodes {user {username}}}}}";
        JsonNode data = graphqlRequest(query);
        return new Group(data.get("data").get("group"));
    }

    public static List<String> getGroupMembers(String path) {
        List<String> res = new ArrayList<>();
        String query = "query {group(fullPath: \"" + path + "\") {groupMembers(last: 100)" +
            " {nodes {user {username}}}}}";
        JsonNode data = graphqlRequest(query);
        data.get("data").get("group").get("groupMembers").get("nodes")
            .forEach(x -> res.add(x.get("user").get("username").asText()));
        return res;
    }

    public static List<String> getGroupRepos(String path) {
        List<String> res = new ArrayList<>();
        String query = "query {group(fullPath: \"" + path + "\") {projects(last: 100)" +
            " {nodes {name}}}}";
        JsonNode data = graphqlRequest(query);
        data.get("data").get("group").get("projects").get("nodes")
            .forEach(x -> res.add(x.get("name").asText()));
        return res;
    }

    //Boards

    public static Board getBoard(String projectPath, String boardId) {
        String query = "query {project(fullPath: \"" + projectPath + 
            "\") {board(id: \"" + boardId +"\") {id name lists {nodes {title id}}}}}";
        JsonNode res = graphqlRequest(query).get("data").get("project").get("board");
        return new Board(res);
    }

    public static List<Board> getBoards(String projectPath) {
        //Hay que hacerlo en dos queries diferentes por el limite de complejidad de queries
        List<Board> res = new ArrayList<>();
        String query = "query {project(fullPath: \"" + projectPath + "\") {boards {nodes {id}}}}";
        graphqlRequest(query).get("data").get("project").get("boards").get("nodes")
            .forEach(x -> res.add(getBoard(projectPath, x.get("id").asText())));
        return res;
    }

    public static String createBoard(String name, String projectPath) {
        String query = "mutation {createBoard(input: {clientMutationId: \"" + TOKEN + "\", projectPath: \"" + projectPath + 
            "\", name: \"" + name + "\"}) {clientMutationId board {id}}}";
        return graphqlRequest(query).get("data").get("createBoard").get("board").get("id").asText();
    }

    public static void updateBoard(String name, String boardId) {
        String query = "mutation {updateBoard(input: {clientMutationId: \"" + TOKEN + "\", id: \"" 
            + boardId + "\", name: \"" + name + "\"}) {clientMutationId board {id}}}";
        graphqlRequest(query);
    }

    // Labels
    
    public static String createLabel(String projectPath, String title) {
        String query = "mutation {labelCreate(input: {clientMutationId: \"" + TOKEN + "\", projectPath: \"" + 
            projectPath + "\", title: \"" + title + "\"}) {label {id}}}";
        return graphqlRequest(query).get("data").get("labelCreate").get("label").get("id").asText();
    }

    public static Map<String, String> getLabels(String projectPath)  {
        Map<String, String> res = new HashMap<>();
        String query = "query {project(fullPath: \"" + projectPath + "\") {labels {nodes {title id}}}}";
        graphqlRequest(query).get("data").get("project").get("labels").get("nodes")
            .forEach(x -> res.put(x.get("title").asText(), x.get("id").asText()));
        return res;
    }

    // Lists

    public static List<GLList> getLists(String id, String projectPath, GHRepository repo) {
        List<GLList> res = new ArrayList<>();
        String query = "{project(fullPath: \""+ projectPath +"\") {board(id: \""+ id +"\") {lists " + 
            "{nodes {id title issues {nodes {id iid title assignees {nodes {username}} dueDate state}}}}}}}";
        graphqlRequest(query).get("data").get("project").get("board").get("lists").get("nodes")
        .forEach(x -> res.add(new GLList(x, true, repo)));
        return res;
    }

    public static String createList(String boardId, String labelId) {
        String query = "mutation {boardListCreate(input: {boardId: \"" + boardId + "\", labelId: \"" + 
            labelId + "\", clientMutationId: \"" + TOKEN + "\"}) {list {id}}}";
        return graphqlRequest(query).get("data").get("boardListCreate")
            .get("list").get("id").asText();
    }

    public static void updateList(String listId, Integer position) {
        String query = "mutation {updateBoardList(input: {listId: \"" + listId + "\", position: " + 
            position + ", clientMutationId: \"" + TOKEN + "\"}) {clientMutationId}}";
        graphqlRequest(query);
    }

    public static void deleteList(String id) {
        String query = "mutation {destroyBoardList(input: {listId: \"" + id + 
            "\", clientMutationId: \"" + TOKEN + "\"}) {clientMutationId}}";
        graphqlRequest(query);
    }

    // Issues

    public static void createIssue(String projectPath, String title, String date, List<GHUser> assignees) {
        List<String> ids = new ArrayList<>();
        String assigneesId = "[]";
        if (!assignees.isEmpty()) {
            String assigneesNames = listParse(assignees, (x -> x.getLogin()));
            String auxQuery = "query {users(usernames: " + assigneesNames + ") {nodes {id}}}";
            graphqlRequest(auxQuery).get("data").get("users").get("nodes").forEach(
                x -> ids.add(x.get("id").asText()));
            assigneesId = listParse(ids, (x -> x));
        }
        String query = "mutation {createIssue(input: {clientMutationId: \"" + TOKEN + "\", projectPath: \"" + 
            projectPath + "\", title: \"" + title + "\", dueDate: \"" + date + "\", assigneeIds: " + 
            assigneesId +"}) {clientMutationId}}";
        graphqlRequest(query);
    }

    public static void updateIssue(String projectPath, String title, String date, List<GHUser> assignees, GLIssue issue) {
        String query = "mutation {updateIssue(input: {clientMutationId: \"" + TOKEN + "\", projectPath: \"" + 
            projectPath + "\", title: \"" + title + "\", dueDate: \"" + date + "\", iid: \"" + 
            issue.getNumber() +"\"}) {clientMutationId}}";
        graphqlRequest(query);
        if (!assignees.isEmpty()) {
            //No se pueden actualizar los asignados directamente, es necesario otra query
            String assigneesStr = listParse(assignees, (x -> x.getLogin()));
            String auxQuery = "mutation {issueSetAssignees(input: {clientMutationId: \"" + TOKEN + 
            "\", projectPath: \"" + projectPath + "\", iid: \"" + issue.getNumber() + 
            "\", assigneeUsernames: " + assigneesStr +"}) {clientMutationId}}";
            graphqlRequest(auxQuery);
        }
    }

    public static void changeIssueState(String projectPath, Integer number, String newState) {
        String query = "mutation {updateIssue(input: {clientMutationId: \"" + TOKEN + "\", projectPath: \"" + 
            projectPath + "\", iid: \"" + number + "\", stateEvent: " + newState + "}) {clientMutationId}}";
        graphqlRequest(query);
    }

    public static void moveIssue(String projectPath, String boardId, Integer number, String origin, String destination) {
        String query = "mutation {issueMoveList(input: {clientMutationId: \"" + TOKEN + "\", projectPath: \"" +
            projectPath + "\", boardId: \"" + boardId + "\", iid: \"" + number + "\", fromListId: \"" + origin + 
            "\", toListId: \"" + destination + "\"}) {clientMutationId}}";
        graphqlRequest(query);
    }
}