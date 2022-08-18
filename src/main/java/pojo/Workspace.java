package pojo;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import com.fasterxml.jackson.databind.JsonNode;

import embed.EmbedManager;

//POJO de un Workspace de Zenhub
public class Workspace {
    private String id;
    private String name;
    private String description;
    private List<Pipeline> pipelines;
    
    public Workspace(String id, String name, String description, List<Pipeline> pipelines) {
        this.id = id;
        this.name = name;
        this.description = description;
        this.pipelines = new ArrayList<>(pipelines);
    }

    public Workspace(JsonNode node) {
        this.id = node.get("id").asText();
        this.name = node.get("name").asText();
        this.description = node.get("description").asText();
        if (this.description.equals("null"))
            this.description = null;
        this.pipelines = new ArrayList<>();
        node.get("pipelinesConnection").get("nodes").forEach(x -> this.pipelines.add(new Pipeline(x, false, null)));
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
    public String getDescription() {
        return description;
    }
    public String getDescriptionOrEmpty() {
        return description == null ? EmbedManager.EMPTY_FIELD : description;
    }
    public void setDescription(String description) {
        this.description = description;
    }
    public List<Pipeline> getPipelines() {
        return pipelines;
    }
    public void setPipelines(List<Pipeline> pipelines) {
        this.pipelines = pipelines;
    }

    public List<String> getPipelinesNames() {
        return this.pipelines.stream().map(Pipeline::getName)
            .collect(Collectors.toList());
    }
}
