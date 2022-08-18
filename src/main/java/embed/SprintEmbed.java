package embed;

import java.time.format.DateTimeFormatter;

import org.kohsuke.github.GHRepository;

import discord4j.core.GatewayDiscordClient;
import discord4j.core.object.entity.channel.MessageChannel;
import discord4j.core.spec.EmbedCreateSpec;
import discord4j.rest.util.Color;
import pojo.Sprint;

public class SprintEmbed extends EmbedManager {

    //Embed solo de lectura para el listado de Sprints
    //La creacion de Sprints requiere de otra entidad (SprintConfig)
    protected Sprint sprint;

    public SprintEmbed(String userId, String serverId, MessageChannel canal,
            GatewayDiscordClient gateway, GHRepository repo, Sprint sprint) {
        super(userId, serverId, canal, gateway, repo, true);
        this.sprint = sprint;
        updateEmbed();
    }

    public void selectNewSprint(Sprint sprint) {
        log.info("Selecting sprint: " + sprint.getName());
        this.sprint = sprint;
        updateEmbed();
            
    }

    private void updateEmbed() {
        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("dd-MM-yyyy");
        Color col = Color.GREEN;
        if(sprint.getState().equals("CLOSED"))
            col = Color.DEEP_LILAC;
        emb = EmbedCreateSpec.builder()
            .color(col)
            .title("Sprint info")
            .description(sprint.getName())
            .addField("Start date", sprint.getStart().format(fmt), false)
            .addField("End date", sprint.getEnd().format(fmt), false)
            .addField("State", sprint.getState(), false)
            .build();
        updateMessage();
    }
}