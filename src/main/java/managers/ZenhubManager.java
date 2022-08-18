package managers;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.AbstractMap;
import java.util.AbstractMap.SimpleEntry;
import java.util.function.Function;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;
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

import pojo.Pipeline;
import pojo.Sprint;
import pojo.Workspace;
import pojo.ZHIssue;

//Controlador de la API de Zenhub
public final class ZenhubManager {
    private static final String TOKEN = Secrets.ZENHUB_TOKEN;
    private static final String ENDPOINT = "https://api.zenhub.com/public/graphql";
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
            JsonNode res = objectMapper.readTree(json).get("data");
            log.debug("Json response: " + res);
            return res;

        } catch(IOException e) {
            log.error(e.getMessage());
            return null;
        }
    }

    //Sprints

    private static List<Sprint> parseSprints(JsonNode node) {
        List<Sprint> res = new ArrayList<>();
        node.get("sprints").get("nodes").forEach(x -> res.add(new Sprint(x)));
        return res;

    } 

    public static List<Sprint> getSprints(GHRepository repo) {
        Set<Sprint> res = new HashSet<>();
        String ghId = String.valueOf(repo.getId());
        String query = "query {repositoriesByGhId(ghIds: " + ghId + ") {workspacesConnection" +
            "{nodes {sprints {nodes {name id startAt endAt state}}}}}}";
        graphqlRequest(query).get("repositoriesByGhId").get(0)
            .get("workspacesConnection").get("nodes").forEach(x -> res.addAll(parseSprints(x)));
        return new ArrayList<>(res);
    }


    public static Map<String, List<Sprint>> getIssueToSprintMap(GHRepository repo) {
        Map<String, List<Sprint>> res = new HashMap<>();
        String ghId = String.valueOf(repo.getId());
        String query = "query {repositoriesByGhId(ghIds: " + ghId + ") {issues" +
            "{nodes {id sprints {nodes {name id startAt endAt state}}}}}}";
        graphqlRequest(query).get("repositoriesByGhId").get(0).get("issues").get("nodes")
            .forEach(x -> res.put(x.get("id").asText(), parseSprints(x)));
        return res;
    }

    public static Sprint getNextSprint(String workspaceID) {
        String query = "query {workspace(id: \"" + workspaceID + "\") {upcomingSprint {id name startAt endAt state}}}";
        JsonNode node = graphqlRequest(query).get("workspace").get("upcomingSprint");
        if (node.isNull())
            return null;
        else
            return new Sprint(node);
    }

    public static void modifySprintConfig(boolean isNew, String workspaceID, String name, String start, String end) {
        String type = isNew ? "createSprintConfig" : "updateSprintConfig";
        String query = "mutation {" + type + "(input: {sprintConfig: {workspaceId: \"" + workspaceID + 
            "\", name: \"" + name + "\", tzIdentifier: \"Europe/Madrid\", startOn: \"" + start + "\", endOn: \"" + 
            end + "\", settings: {moveUnfinishedIssues: false, issuesFromPipeline: {enabled: false}}}, clientMutationId: \"" + 
            TOKEN + "\"}) {clientMutationId}}";
        graphqlRequest(query);
    }

    //Issues

    public static List<ZHIssue> getIssues(GHRepository repo) {
        List<ZHIssue> res = new ArrayList<>();
        String ghId = String.valueOf(repo.getId());
        String query = "{repositoriesByGhId(ghIds: " + ghId + ") {issues {nodes {id ghId epic {id} parentEpics {nodes {id}}" + 
            " number estimate {value} sprints {nodes {name id startAt endAt state}}}}}}";
        graphqlRequest(query).get("repositoriesByGhId").get(0).get("issues").get("nodes")
            .forEach(x -> res.add(new ZHIssue(x, repo, true)));
        return res;
    }

    public static SortedMap<String, String> getEpicMap(GHRepository repo) {
        SortedMap<String, String> res = new TreeMap<>();
        String ghId = String.valueOf(repo.getId());
        String query = "{repositoriesByGhId(ghIds: " + ghId + ") {workspacesConnection " + 
            "{nodes {epics {nodes {id issue {title}}}}}}}";
        graphqlRequest(query).get("repositoriesByGhId").get(0).get("workspacesConnection").get("nodes")
            .forEach(x -> x.get("epics").get("nodes").forEach(y -> res
            .put(y.get("id").asText(), y.get("issue").get("title").asText())));
        return res;
    }

    public static void moveIssue(String issueId, String pipelineId) {
        String query = "mutation {moveIssue(input: {pipelineId: \"" + pipelineId + "\",issueId:\"" +
            issueId + "\",clientMutationId: \"" + TOKEN + "\"}) {clientMutationId}}";
        graphqlRequest(query);
    }

    private static void setEstimate(String issueId, Integer estimate) {
        String query = "mutation {setEstimate(input: {value: " + estimate + ", issueId: \"" + 
            issueId + "\", clientMutationId: \"" + TOKEN + "\"}) {clientMutationId}}";
        graphqlRequest(query);
    }

    private static void createEpic(String issueId) {
        String query = "mutation {createEpicFromIssue(input: {issueId: \"" + issueId + 
            "\", epicChildIds: [], clientMutationId: \"" + TOKEN + "\"}) {clientMutationId}}";
        graphqlRequest(query);
    }

    private static <T> String listParse(List<T> list, Function<T, String> parser) {
        String res = "[";
        for (T t : list)
            res = res.concat("\"" + parser.apply(t) + "\",");
        return res.substring(0, res.length() - 1).concat("]");
    }

    private static void addIssueToEpics(String issueId, List<String> epics) {
        String epicsIds = listParse(epics, (x -> x));
        if (!epicsIds.equals("]")) {
            String query = "mutation {addIssuesToEpics(input: {issueIds: \"" + issueId + "\",epicIds: " + 
                epicsIds + ", clientMutationId: \"" + TOKEN + "\"}) {clientMutationId}}";
            graphqlRequest(query);
        }
    }

    private static void removeIssueToEpics(String issueId, List<String> epics) {
        String epicsIds = listParse(epics, (x -> x));
        if (!epicsIds.equals("]")) {
            String query = "mutation {removeIssuesFromEpics(input: {issueIds: \"" + issueId + 
                "\",epicIds: \"" + epicsIds + "\",clientMutationId: \"" + TOKEN + "\"}) {clientMutationId}}";
            graphqlRequest(query);
        }
    }

    private static void addIssueToSprints(String issueId, List<Sprint> sprints) {
        String sprintIds = listParse(sprints, (Sprint::getId));
        if (!sprintIds.equals("]")) {
            String query = "mutation {addIssuesToSprints(input: {issueIds: " + issueId + ",sprintIds: \"" + 
                sprintIds + "\", clientMutationId: \"" + TOKEN + "\"}) {clientMutationId}}";
            graphqlRequest(query);
        }
    }

    private static void removeIssueToSprints(String issueId, List<Sprint> sprints) {
        String sprintIds = listParse(sprints, (Sprint::getId));
        if (!sprintIds.equals("]")) {
            String query = "mutation {removeIssuesFromEpics(input: {issueIds: \"" + issueId + "\",sprintIds: \"" + 
                sprintIds + "\", clientMutationId: \"" + TOKEN + "\"}) {clientMutationId}}";
            graphqlRequest(query);
        }
    }

    public static void createIssue(Long repoId, Integer issueNumber, Integer estimate, boolean isEpic, List<String> epics, List<Sprint> sprints) {
        String idQuery = "query {issueByInfo(repositoryGhId: " + repoId + ", issueNumber: " + issueNumber + ") {id}}";
        String issueId = graphqlRequest(idQuery).get("issueByInfo").get("id").asText();
        setEstimate(issueId, estimate);
        if (isEpic)
            createEpic(issueId);
        addIssueToEpics(issueId, epics);
        addIssueToSprints(issueId, sprints);
    }

    public static void updateIssue(ZHIssue issue, GHRepository repo, Integer estimate, boolean isEpic, List<String> epics, List<Sprint> sprints) {
        String id = issue.getZenhubId();
        setEstimate(id, estimate);
        if (isEpic && !issue.getIsEpic())
            createEpic(id);
        if (!isEpic && issue.getIsEpic()) {
            String convertToIssueQuery = "mutation {deleteEpicByIssueInfo(input: {issue: {repositoryGhId: " + String.valueOf(repo.getId()) + 
                ", issueNumber: " + String.valueOf(issue.getGhIssue().getNumber()) + "}, clientMutationId: \"" + TOKEN + "\"}) {clientMutationId}}";
            graphqlRequest(convertToIssueQuery);
        }
        List<String> deleteEpics = new ArrayList<>(issue.getParentEpics()).stream().filter(x -> !epics.contains(x)).collect(Collectors.toList());
        List<String> addEpics = epics.stream().filter(x -> !issue.getParentEpics().contains(x)).collect(Collectors.toList());
        removeIssueToEpics(id, deleteEpics);
        addIssueToEpics(id, addEpics);
        List<Sprint> deleteSprints = new ArrayList<>(issue.getSprints()).stream().filter(x -> !sprints.contains(x)).collect(Collectors.toList());
        List<Sprint> addSprints = sprints.stream().filter(x -> !issue.getSprints().contains(x)).collect(Collectors.toList());
        removeIssueToSprints(id, deleteSprints);
        addIssueToSprints(id, addSprints);
    }

    //Pipelines

    public static List<Pipeline> getPipelines(String workspaceID, GHRepository repo) {
        List<Pipeline> res = new ArrayList<>();
        String query = "query {workspace(id: \"" + workspaceID + "\") {pipelinesConnection {nodes " + 
            "{name id issues {nodes {id epic {id} parentEpics {nodes {id}} number estimate {value} " +
            "sprints {nodes {name id startAt endAt state}}}}}}}}";
        graphqlRequest(query).get("workspace").get("pipelinesConnection").get("nodes")
            .forEach(x -> res.add(new Pipeline(x, true, repo)));
        return res;
    }

    public static void createPipeline(String name, Integer position, String workspaceID) {
        String query = "mutation {createPipeline(input: {workspaceId: \"" + workspaceID + "\", name: \"" + 
            name + "\", position: " + position + ", clientMutationId: \"" + TOKEN + "\"}) {clientMutationId}}";
        graphqlRequest(query);
    }

    public static void updatePipeline(String name, Integer position, String pipelineID) {
        String query = "mutation {updatePipeline(input: {pipelineId: \"" + pipelineID + "\", name: \"" + 
            name + "\", position: " + position + ", clientMutationId: \"" + TOKEN + "\"}) {clientMutationId}}";
        graphqlRequest(query);
    }

    public static void deletePipeline(String pipelineID) {
        String query = "mutation {deletePipeline(input: {pipelineId: \"" + pipelineID + "\", destinationPipelineId: \"" + 
            pipelineID + "\", clientMutationId: \"" + TOKEN + "\"}) {clientMutationId}}";
        graphqlRequest(query);
    }

    //Workspaces

    public static List<Workspace> getWorkspaces(GHRepository repo) {
        List<Workspace> res = new ArrayList<>();
        String ghId = String.valueOf(repo.getId());
        String query = "query {repositoriesByGhId(ghIds: " + ghId + ") {workspacesConnection {nodes " + 
            "{id name description pipelinesConnection {nodes {id name}} closedPipeline {id name}}}}}";
        graphqlRequest(query).get("repositoriesByGhId").get(0).get("workspacesConnection")
            .get("nodes").forEach(x -> res.add(new Workspace(x)));
        return res;
    }

    public static String createWorkspace(String name, String description, Long repoID) {
        String query = "mutation {createWorkspace(input: {name: \"" + name + "\", description: \"" + 
            description + "\", repositoryGhIds: ["+ repoID +"], defaultRepositoryGhId: "+ repoID + 
            ", clientMutationId: \"" + TOKEN + "\"}) {workspace {id}}}";
        return graphqlRequest(query).get("createWorkspace")
            .get("workspace").get("id").asText();
    }

    public static void updateWorkspace(String name, String description, String workspaceID) {
        String query = "mutation {updateWorkspace(input: {workspaceId: \"" + workspaceID + "\", name: \"" + name + 
            "\", description: \"" + description + "\", clientMutationId: \"" + TOKEN + "\"}) {clientMutationId}}";
        graphqlRequest(query);
    }

    //Analysis

    public static SimpleEntry<List<Integer>, List<ZHIssue>> getSprintAnalysisInfo(String sprintId, GHRepository repo) {
        List<Integer> sprintMetrics = new ArrayList<>();
        List<ZHIssue> issues = new ArrayList<>();
        String query = "query {node(id: \"" + sprintId + "\") {__typename ... on Sprint {issues {nodes " + 
            "{id epic {id} parentEpics {nodes {id}} number estimate {value}}} closedIssuesCount completedPoints totalPoints}}}";
        JsonNode data = graphqlRequest(query).get("node");
        sprintMetrics.add(data.get("closedIssuesCount").asInt());
        sprintMetrics.add(data.get("totalPoints").asInt());
        sprintMetrics.add(data.get("completedPoints").asInt());
        data.get("issues").get("nodes").forEach(x -> issues.add(new ZHIssue(x, repo, false)));
        return new AbstractMap.SimpleEntry<>(sprintMetrics, issues);
    }
}
