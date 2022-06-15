package model;
import java.io.Serializable;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;

@Entity
@Table(name = "server")
public class Server implements Serializable {
    @Id
    @Column(name = "server_id", unique = true)
    private long id;

    @Column(name = "server_url", nullable = false)
    private String url;

    @Column(name = "server_installation", nullable = false)
    private int installation;

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

    @Override
    public String toString() {
        return id + "\t" + url + "\t" + installation;
    }
}