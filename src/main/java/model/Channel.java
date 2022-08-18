package model;
import java.io.Serializable;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;

@Entity
@Table(name = "channel")
public class Channel implements Serializable {

    @Id
    @Column(name = "id", unique = true)
    private long id;

    @Column(name = "url", nullable = false)
    private String url;

    @Column(name = "installation", nullable = false)
    private int installation;

    @ManyToOne()
    @JoinColumn(name = "server_id")
    private Server server;

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public int getInstallation() {
        return installation;
    }

    public void setInstallation(int installation) {
        this.installation = installation;
    }

    public Server getServer() {
        return server;
    }

    public void setServer(Server server) {
        this.server = server;
    }

    @Override
    public String toString() {
        return "Channel [id=" + id + ", installation=" + installation + ", server=" + server + ", url=" + url + "]";
    }
    
}
