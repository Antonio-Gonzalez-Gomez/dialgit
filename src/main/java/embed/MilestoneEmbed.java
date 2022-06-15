package embed;

import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Locale;

import org.kohsuke.github.GHIssueState;
import org.kohsuke.github.GHMilestone;
import org.kohsuke.github.GHMilestoneState;
import org.kohsuke.github.GHRepository;

import discord4j.core.GatewayDiscordClient;
import discord4j.core.object.entity.channel.MessageChannel;
import discord4j.core.object.reaction.ReactionEmoji;
import discord4j.core.spec.EmbedCreateSpec;
import discord4j.rest.util.Color;

public class MilestoneEmbed extends EmbedManager{

    private String title;
    private String name;
    private String description;
    private String deadline;
    protected GHMilestone milestone;

    public MilestoneEmbed(String userId, String serverId, MessageChannel canal,
            GatewayDiscordClient gateway, GHRepository repo, boolean readOnly,
            String title, String name, String description, String deadline, GHMilestone milestone) {
        super(userId, serverId, canal, gateway, repo, readOnly);
        this.title = title;
        this.name = name;
        this.description = description;
        this.deadline = deadline;
        this.actions = new ArrayList<>();
        if (!readOnly) {
            this.maxField = 2;
            this.actions.add(ReactionEmoji.unicode("❓"));
            this.actions.add(ReactionEmoji.unicode("⬆️"));
            this.actions.add(ReactionEmoji.unicode("⬇️"));
            this.actions.add(ReactionEmoji.unicode("❌"));
            this.actions.add(ReactionEmoji.unicode("✅"));
        }
        
        this.milestone = milestone;
        updateEmbed();
    }

    public void addData(String data) {
        log.info("Adding data: " + data);
        int length = data.length();
        this.error = null;
        switch (selectedField) {
            case 0:
                if (length <= 1024)
                    name = data;
                else
                    this.error = "Milestone name can't be longer than 1024 characters";
                break;

            case 1:
                if (length <= 1024)
                    description = data;
                else
                    this.error = "Milestone description can't be longer than 1015 characters";
                break;

            case 2:
                if (!data.equals("Date not found!")) {
                    String[] reversed = data.split("T")[0].split("-");
                    deadline = reversed[2] + "-" + reversed[1] + "-" + reversed[0];
                }
                else
                    this.error = "Cannot identify a valid date from the previous message";
                break;

            default:
                break;
        }
        if (error == null)
            selectedField = selectedField == maxField ? selectedField : selectedField + 1;
        updateEmbed();
        updateMessage();
    }

    public void selectNewMilestone(GHMilestone milestone) {
        log.info("Selecting milestone: " + milestone.getTitle());
        this.name = milestone.getTitle();
        this.description = milestone.getDescription();
        this.deadline = dateParse(milestone.getDueOn());
        this.milestone = milestone;
        updateEmbed();
        if (this.msg != null)
            updateMessage();
    }

    private void updateEmbed() {
        String[] fields = {"Name", "Description", "Deadline"};
        Color col = Color.GREEN;
        String desc = EMPTY_FIELD;
        if (!readOnly) {
            col = Color.RUST;
            fields[selectedField] = "-> " + fields[selectedField] + " <-";
        }
        else {
            try {
                if (this.error == null) {
                    int open = repo.getIssues(GHIssueState.OPEN, milestone).size();
                    int closed = repo.getIssues(GHIssueState.CLOSED, milestone).size();
                    desc = "Milestone used in " + String.valueOf(open) + " open issues, "
                        + String.valueOf(closed) + " closed issues.";
                } else
                    desc = this.error;
            } catch (IOException e) {
                log.error(e.getMessage());
            }
            if(milestone.getState().equals(GHMilestoneState.CLOSED))
                col = Color.DEEP_LILAC;
        } 
        emb = EmbedCreateSpec.builder()
            .color(col)
            .title(title)
            .description(desc)
            .addField(fields[0], name, false)
            .addField(fields[1], description, false)
            .addField(fields[2], deadline, false)
            .build();
    }

    @Override
    protected void executeAction(String action) {
        log.info("Selected action: " + action);
        switch (action) {
            case "❓":
                iconHelp();
                break;

            case "⬆️":
                selectedField = selectedField == 0 ? maxField : selectedField - 1;
                updateEmbed();
                updateMessage();
                break;
            
            case "⬇️":
                selectedField = selectedField == maxField ? 0 : selectedField + 1;
                updateEmbed();
                updateMessage();
                break;

            case "❌":
                end();
                break;

            case "✅":
                try {
                    if (this.milestone == null) {
                        GHMilestone newMile = repo.createMilestone(name, description);
                        newMile.setDueOn(new SimpleDateFormat("dd-MM-yyyy", Locale.GERMAN).parse(deadline));
                    } else {
                        milestone.setTitle(name);
                        milestone.setDescription(description);
                        milestone.setDueOn(new SimpleDateFormat("dd-MM-yyyy", Locale.GERMAN).parse(deadline));
                    }
                    end();
                } catch (IOException | ParseException e) {
                    log.error(e.getMessage());
                }
                break;

            default:
                break;
        }
    }
}