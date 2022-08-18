package pojo;

import java.io.IOException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

import com.fasterxml.jackson.databind.JsonNode;

import org.kohsuke.github.GHRepository;
import org.kohsuke.github.GHUser;

//POJO de una issue de Gitlab
public class GLIssue {
    private String title;
    private String id;
    private Integer number;
    private String state;
    private List<GHUser> assignees;
    private LocalDate dueDate;

    public GLIssue(JsonNode node, GHRepository repo) {
        this.title = node.get("title").asText();
        this.id = node.get("id").asText();
        this.number = node.get("iid").asInt();
        this.state = node.get("state").asText();
        this.assignees = new ArrayList<>();
        try {
            Stream<GHUser> collaborators = repo.listCollaborators().toList().stream();
            node.get("assignees").get("nodes").forEach(x -> assignees.add(
                collaborators.filter(y -> y.getLogin().equals(
                x.get("username").asText())).findAny().orElse(null)));    
        } catch (IOException e) {
            e.printStackTrace();
        }
        String date = node.get("dueDate").asText();
        if (!date.equals("null"))
            this.dueDate = LocalDate.parse(date, DateTimeFormatter.ofPattern("yyyy-MM-dd"));
    }

    public String getTitle() {
        return title;
    }
    public void setTitle(String title) {
        this.title = title;
    }
    public String getId() {
        return id;
    }
    public void setId(String id) {
        this.id = id;
    }
    public Integer getNumber() {
        return number;
    }
    public void setNumber(Integer number) {
        this.number = number;
    }
    public String getState() {
        return state;
    }
    public void setState(String state) {
        this.state = state;
    }
    public List<GHUser> getAssignees() {
        return assignees;
    }
    public void setAssignees(List<GHUser> assignees) {
        this.assignees = assignees;
    }
    public LocalDate getDueDate() {
        return dueDate;
    }
    public void setDueDate(LocalDate dueDate) {
        this.dueDate = dueDate;
    }

    
}
