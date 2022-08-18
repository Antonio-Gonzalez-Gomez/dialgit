package pojo;

import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.databind.JsonNode;

import org.kohsuke.github.GHRepository;

//POJO de un issue list de Gitlab
public class GLList {
    
    private String id;
    private String name;
    private List<GLIssue> issues;

    public GLList(JsonNode node, boolean withIssues, GHRepository repo) {
        this.id = node.get("id").asText();
        this.name = node.get("title").asText();
        this.issues = new ArrayList<>();
        if (withIssues)
            node.get("issues").get("nodes").forEach(x ->
                this.issues.add(new GLIssue(x, repo)));
    }

    public String getId() {
        return id;
    }
    public void setId(String id) {
        this.id = id;
    }
    public String getName() {
        return name;
    }
    public void setName(String name) {
        this.name = name;
    }
    public List<GLIssue> getIssues() {
        return issues;
    }
    public void setIssues(List<GLIssue> issues) {
        this.issues = issues;
    }

    public void addIssue(GLIssue issue) {
        this.issues.add(issue);
    }

    public void removeIssue(GLIssue issue) {
        this.issues.remove(issue);
    }

    
}
