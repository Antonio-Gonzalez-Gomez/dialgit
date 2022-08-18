package embed;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Date;

import org.kohsuke.github.GHRepository;

import discord4j.core.GatewayDiscordClient;
import discord4j.core.object.entity.channel.MessageChannel;
import discord4j.core.object.reaction.ReactionEmoji;
import discord4j.core.spec.EmbedCreateSpec;
import discord4j.rest.util.Color;
import managers.ZenhubManager;
import pojo.Sprint;

public class SprintConfigEmbed extends EmbedManager{
    
    private String name;
    private Date start;
    private Date end;
    private boolean isNew;
    private String workspaceID;
    
    public SprintConfigEmbed(String userId, String serverId, MessageChannel canal, GatewayDiscordClient gateway,
            GHRepository repo, Sprint nextSprint, String workspaceID) {
        super(userId, serverId, canal, gateway, repo, false);
        if (nextSprint != null) {
            this.name = nextSprint.getName();
            this.start = Date.from(nextSprint.getStart().atZone(ZoneId.systemDefault()).toInstant());
            this.end = Date.from(nextSprint.getEnd().atZone(ZoneId.systemDefault()).toInstant());
            isNew = false;
        } else {
            this.name = "New sprint config";
            this.start = Date.from(LocalDate.now().atStartOfDay(ZoneId.systemDefault()).toInstant());
            this.end = Date.from(LocalDate.now().plusDays(5).atStartOfDay(ZoneId.systemDefault()).toInstant());
            isNew = true;
        }
        this.workspaceID = workspaceID;
        this.actions = new ArrayList<>();
        this.maxField = 2;
        this.actions.add(ReactionEmoji.unicode("❓"));
        this.actions.add(ReactionEmoji.unicode("⬆️"));
        this.actions.add(ReactionEmoji.unicode("⬇️"));
        this.actions.add(ReactionEmoji.unicode("❌"));
        this.actions.add(ReactionEmoji.unicode("✅"));
        updateEmbed();
        
    }

    public void addData(String data) {
        log.info("Adding data: " + data);
        int length = data.length();
        error = null;
        switch (selectedField) {
            case 0:
                if (length <= 1024)
                    name = data;
                else
                    error = "Sprint config name can't be longer than 1024 characters";
                break;

            case 1:
                if (!data.equals("Date not found!")) {
                    String startStr = data.split("T")[0];
                    try {
                        start = new SimpleDateFormat("yyyy-MM-dd").parse(startStr);
                    } catch (ParseException e) {
                        log.error(e.getMessage());
                    }
                }
                else
                    error = "Cannot identify a valid date from the previous message";
                break;

            case 2:
                if (!data.equals("Date not found!")) {
                    String endStr = data.split("T")[0];
                    try {
                        end = new SimpleDateFormat("yyyy-MM-dd").parse(endStr);
                    } catch (ParseException e) {
                        log.error(e.getMessage());
                    }
                }
                else
                    error = "Cannot identify a valid date from the previous message";
                break;
            
            default:
                break;
        }
        if (error == null)
            selectedField = selectedField == maxField ? selectedField : selectedField + 1;
        updateEmbed();
    }

    protected void updateEmbed() {
        String[] fields = {"Name", "Start date", "End date"};
        fields[selectedField] = "-> " + fields[selectedField] + " <-";
        Color col = Color.RUST;
        String title = isNew ? "Create sprint config" : "Update sprint config";
        emb = EmbedCreateSpec.builder()
            .color(col)
            .title(title)
            .addField(fields[0], name, false)
            .addField(fields[1], dateParse(start), false)
            .addField(fields[2], dateParse(end), false)
            .build();
        if (error != null)
            emb = emb.withDescription(error);
        updateMessage();
    }

    @Override
    protected void executeAction(String action) {
        switch (action) {
            case "❓":
                iconHelp();
                break;

            case "⬆️":
                selectedField = selectedField == 0 ? maxField : selectedField - 1;
                updateEmbed();
                break;
            
            case "⬇️":
                selectedField = selectedField == maxField ? 0 : selectedField + 1;
                updateEmbed();
                break;

            case "❌":
                end();
                break;

            case "✅":
                String startStr = new SimpleDateFormat("yyyy-MM-dd").format(start) + "T00:00:00Z";
                String endStr = new SimpleDateFormat("yyyy-MM-dd").format(end) + "T23:59:59Z";
                ZenhubManager.modifySprintConfig(isNew, workspaceID, name, startStr, endStr);
                end();
                break;

            default:
                break;
        }
    }
}
