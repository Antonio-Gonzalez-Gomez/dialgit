package pojo;

import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.databind.JsonNode;

//POJO de un grupo de repositorios de Gitlab
public class Group {
    String path;
    String name;
    String url;
    List<String> projects;
    List<String> members;

    public Group(JsonNode node) {
        this.path = node.get("path").asText();
        this.name = node.get("name").asText();
        this.url = node.get("webUrl").asText();
        this.projects = new ArrayList<>();
        node.get("projects").get("nodes").forEach(x -> this.projects.add(x.get("name").asText()));
        this.members = new ArrayList<>();
        node.get("groupMembers").get("nodes").forEach(x -> this.members.add(x.get("user").get("username").asText()));
    }

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public List<String> getProjects() {
        return projects;
    }

    public void setProjects(List<String> projects) {
        this.projects = projects;
    }

    public List<String> getMembers() {
        return members;
    }

    public void setMembers(List<String> members) {
        this.members = members;
    }

    
}
