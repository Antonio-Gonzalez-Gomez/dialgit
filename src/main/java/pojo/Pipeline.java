package pojo;

import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.databind.JsonNode;

import org.kohsuke.github.GHRepository;

//POJO de un pipeline de Zenhub
public class Pipeline {
    private String name;
    private String id;
    private List<ZHIssue> issues;

    public Pipeline(String name, String id, List<ZHIssue> issues) {
        this.name = name;
        this.id = id;
        this.issues = new ArrayList<>(issues);
    }

    public Pipeline(JsonNode node, boolean withIssues, GHRepository repo) {
        this.name = node.get("name").asText();
        this.id = node.get("id").asText();
        this.issues = new ArrayList<>();
        if (withIssues)
            node.get("issues").get("nodes").forEach(x -> this.issues.add(new ZHIssue(x, repo, true)));
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public List<ZHIssue> getIssues() {
        return new ArrayList<>(this.issues);
    }

    public void addIssue(ZHIssue issue) {
        this.issues.add(issue);
    }

    public ZHIssue getIssue(int index) {
        return this.issues.get(index);
    }

    public void removeIssue(ZHIssue issue) {
        this.issues.remove(issue);
    }
}
