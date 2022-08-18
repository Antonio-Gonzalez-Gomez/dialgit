package pojo;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

import com.fasterxml.jackson.databind.JsonNode;

//POJO de un Sprint de Zenhub
public class Sprint {
    public static final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss'Z'");
    private String name;
    private String id;
    private LocalDateTime startAt;
    private LocalDateTime endAt;
    private String state;
    
    public Sprint(String id, String name, LocalDateTime startAt, LocalDateTime endAt, String state) {
        this.id = id;
        this.name = name;
        this.startAt = startAt;
        this.endAt = endAt;
        this.state = state;
    }

    public Sprint(JsonNode node) {
        this.id = node.get("id").asText();
        this.name = node.get("name").asText();
        this.startAt = LocalDateTime.parse(node.get("startAt").asText(), formatter);
        this.endAt = LocalDateTime.parse(node.get("endAt").asText(), formatter);
        this.state = node.get("state").asText();
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

    public LocalDateTime getStart() {
        return startAt;
    }

    public void setStart(LocalDateTime startAt) {
        this.startAt = startAt;
    }

    public LocalDateTime getEnd() {
        return endAt;
    }

    public void setEnd(LocalDateTime endAt) {
        this.endAt = endAt;
    }

    public String getState() {
        return state;
    }

    public void setState(String state) {
        this.state = state;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((id == null) ? 0 : id.hashCode());
        result = prime * result + ((name == null) ? 0 : name.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        Sprint other = (Sprint) obj;
        if (id == null) {
            if (other.id != null)
                return false;
        } else if (!id.equals(other.id))
            return false;
        if (name == null) {
            if (other.name != null)
                return false;
        } else if (!name.equals(other.name))
            return false;
        return true;
    }

    @Override
    public String toString() {
        return "Sprint [endAt=" + endAt + ", id=" + id + ", name=" + name + ", startAt=" + startAt + ", state=" + state + "]";
    }

}
