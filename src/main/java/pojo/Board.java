package pojo;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import com.fasterxml.jackson.databind.JsonNode;

//POJO de un tablero de Gitlab
public class Board {
    
    private String id;
    private String name;
    private List<GLList> lists;
    
    public Board(JsonNode node) {
        this.id = node.get("id").asText();
        this.name = node.get("name").asText();
        this.lists = new ArrayList<>();
        node.get("lists").get("nodes").forEach(x ->
            this.lists.add(new GLList(x, false, null)));
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
    public List<GLList> getLists() {
        return lists;
    }
    public void setLists(List<GLList> lists) {
        this.lists = lists;
    }
    
    public List<String> getListNames() {
        return lists.stream().map(GLList::getName).collect(Collectors.toList());
    }
    
}
