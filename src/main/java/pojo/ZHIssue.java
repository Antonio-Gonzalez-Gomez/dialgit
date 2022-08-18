package pojo;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.databind.JsonNode;

import org.kohsuke.github.GHIssue;
import org.kohsuke.github.GHRepository;

//POJO de una issue de Zenhub
public class ZHIssue {
    private String zenhubId;
    private GHIssue ghIssue;
    private List<String> parentEpics;
    private Boolean isEpic;
    private Integer number;
    private Integer estimate;
    private List<Sprint> sprints;

    public ZHIssue (JsonNode node, GHRepository repo, boolean withSprints) {
        this.zenhubId = node.get("id").asText();
        this.parentEpics = new ArrayList<>();
        node.get("parentEpics").get("nodes").forEach(x -> parentEpics.add(x.get("id").asText()));
        this.isEpic = !node.get("epic").isNull();
        this.number = node.get("number").asInt();
        JsonNode points = node.get("estimate");
        this.estimate = points.isNull() ? 0 : points.get("value").asInt();
        this.sprints = new ArrayList<>();
        if (withSprints)
            node.get("sprints").get("nodes").forEach(x -> this.sprints.add(new Sprint(x)));
        if (repo != null)
            try {
                this.ghIssue = repo.getIssue(number);
            } catch (IOException e) {
                this.ghIssue = null;
            }
    }

    public String getZenhubId() {
        return zenhubId;
    }

    public void setZenhubId(String zenhubId) {
        this.zenhubId = zenhubId;
    }

    public GHIssue getGhIssue() {
        return ghIssue;
    }

    public void setGhIssue(GHIssue ghIssue) {
        this.ghIssue = ghIssue;
    }

    public List<String> getParentEpics() {
        return parentEpics;
    }

    public void setParentEpics(List<String> parentEpics) {
        this.parentEpics = parentEpics;
    }

    public Boolean getIsEpic() {
        return isEpic;
    }

    public void setIsEpic(Boolean isEpic) {
        this.isEpic = isEpic;
    }

    public Integer getNumber() {
        return number;
    }

    public void setNumber(Integer number) {
        this.number = number;
    }

    public Integer getEstimate() {
        return estimate;
    }

    public void setEstimate(Integer estimate) {
        this.estimate = estimate;
    }

    public List<Sprint> getSprints() {
        return sprints;
    }

    public void setSprints(List<Sprint> sprints) {
        this.sprints = sprints;
    }

    @Override
    public String toString() {
        return "ZHIssue [estimate=" + estimate + ", ghIssue=" + ghIssue + ", isEpic=" + isEpic + ", number=" + number
                + ", parentEpics=" + parentEpics + ", sprints=" + sprints + ", zenhubId=" + zenhubId + "]";
    }
    
    
}