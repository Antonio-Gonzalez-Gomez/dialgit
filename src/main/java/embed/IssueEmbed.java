package embed;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import org.kohsuke.github.GHIssue;
import org.kohsuke.github.GHIssueBuilder;
import org.kohsuke.github.GHMilestone;
import org.kohsuke.github.GHRepository;
import org.kohsuke.github.GHUser;

import discord4j.core.GatewayDiscordClient;
import discord4j.core.object.entity.channel.MessageChannel;
import discord4j.core.object.reaction.ReactionEmoji;
import discord4j.core.spec.EmbedCreateSpec;
import discord4j.rest.util.Color;

public class IssueEmbed extends EmbedManager{

    private String title;
    private String name;
    private String body;
    private List<GHUser> assignees;
    private GHMilestone milestone;
    private MilestoneList auxMilestoneList;
    protected GHIssue issue;

    public IssueEmbed(String userId, String serverId, MessageChannel canal,
            GatewayDiscordClient gateway, GHRepository repo, boolean readOnly,
            String title, String name, String body, GHIssue issue, List<GHMilestone> milestones) {
        super(userId, serverId, canal, gateway, repo, readOnly);
        this.title = title;
        this.name = name;
        this.body = body;
        this.assignees = new ArrayList<>();
        this.actions = new ArrayList<>();
        if (!readOnly) {
            this.maxField = 3;
            this.actions.add(ReactionEmoji.unicode("❓"));
            this.actions.add(ReactionEmoji.unicode("⬆️"));
            this.actions.add(ReactionEmoji.unicode("⬇️"));
            this.actions.add(ReactionEmoji.unicode("❌"));
            this.actions.add(ReactionEmoji.unicode("✅"));
            this.auxMilestoneList = new MilestoneList(userId, serverId, canal, gateway,
                repo, false, true, milestones);
            this.auxMilestoneList.setIssueCallback(this::selectNewMilestone);
        }
        if (issue != null) {
            this.assignees = issue.getAssignees();
            this.milestone = issue.getMilestone();
            this.issue = issue;
        }
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
                    this.error = "Issue name can't be longer than 1024 characters";
                break;

            case 1:
                if (length <= 1024)
                    body = data;
                else
                    this.error = "Issue body can't be longer than 1024 characters";
                break;

            case 2:
                List<String> nombres = Arrays.asList(data.split(" "));
                List<GHUser> collaborators = new ArrayList<>();
                try {
                    collaborators = repo.listCollaborators().toList();
                    this.assignees = collaborators.stream().filter(x 
                        -> nombres.contains(x.getLogin())).collect(Collectors.toList());
                } catch (IOException e) {
                    log.error(e.getMessage());
                }
                break;
            
            default:
                break;
        }
        if (this.error == null) {
            int oldSelected = selectedField;
            selectedField = selectedField == maxField ? selectedField : selectedField + 1;
            updateAuxiliarList(oldSelected);
        }
        updateEmbed();
        updateMessage();
    }

    public void selectNewIssue(GHIssue issue) {
        if (issue == null) {
            log.info("Deleting selected issue");
            this.issue = null;
            this.name = EMPTY_FIELD;
            this.body = EMPTY_FIELD;
            this.assignees = new ArrayList<>();
            this.milestone = null;
        } else {
            log.info("Selecting issue: " + issue.getTitle());
            this.issue = issue;
            this.name = issue.getTitle();
            this.body = issue.getBody();
            this.assignees = issue.getAssignees();
            this.milestone = issue.getMilestone();
        }
        updateEmbed();
        if (this.msg != null)
            updateMessage();
    }

    public void selectNewMilestone(GHMilestone milestone) {
        log.info("Selecting milestone: " + milestone.getTitle());
        this.milestone = milestone;
        updateEmbed();
        updateMessage();
    }

    private void updateAuxiliarList(int oldSelectedField) {
        if (oldSelectedField != 3 && selectedField == 3)
            auxMilestoneList.send();
        else if (oldSelectedField == 3 && selectedField != 3) {
            auxMilestoneList.fluxDisposer.dispose();
            auxMilestoneList.msg.delete().block();
        }
    }

    protected void updateEmbed() {
        String[] fields = {"Name", "Body", "Assignees", "Milestone"};
        List<String> nombresAssignees = assignees.stream()
            .map(GHUser::getLogin).collect(Collectors.toList());
        String strAssignees = listParse(nombresAssignees, 99);
        String strMilestone = milestone == null ? EMPTY_FIELD : milestone.getTitle();
        Color col = Color.GREEN;
        if (!readOnly) {
            col = Color.RUST;
            fields[selectedField] = "-> " + fields[selectedField] + " <-";
        }
        else if(issue != null && issue.getState().toString().equals("CLOSED"))
            col = Color.DEEP_LILAC;
        emb = EmbedCreateSpec.builder()
            .color(col)
            .title(title)
            .addField(fields[0], name, false)
            .addField(fields[1], body == null ? EMPTY_FIELD : body, false)
            .addField(fields[2], strAssignees, false)
            .addField(fields[3], strMilestone, false)
            .build();
        if (this.error != null)
            emb = emb.withDescription(this.error);
    }

    @Override
    protected void executeAction(String action) {
        int oldSelected = selectedField;
        log.info("Selected action: " + action);
        switch (action) {
            case "❓":
                iconHelp();
                break;

            case "⬆️":
                selectedField = selectedField == 0 ? maxField : selectedField - 1;
                updateAuxiliarList(oldSelected);
                updateEmbed();
                updateMessage();
                break;
            
            case "⬇️":
                selectedField = selectedField == maxField ? 0 : selectedField + 1;
                updateAuxiliarList(oldSelected);
                updateEmbed();
                updateMessage();
                break;

            case "❌":
                if (selectedField == 3) {
                    auxMilestoneList.fluxDisposer.dispose();
                    auxMilestoneList.msg.delete().block();
                }
                end();
                break;

            case "✅":
                if (selectedField == 3) {
                    auxMilestoneList.fluxDisposer.dispose();
                    auxMilestoneList.msg.delete().block();
                }
                try {
                    
                    if (this.issue == null) {
                        GHIssueBuilder builder = repo.createIssue(name).body(body).milestone(milestone);
                        assignees.forEach(builder::assignee);
                        builder.create();
                    } else {
                        issue.setTitle(name);
                        issue.setBody(body);
                        issue.setMilestone(milestone);
                        issue.setAssignees(assignees);
                    }
                    end();
                } catch (IOException e) {
                    log.error(e.getMessage());
                }
                break;

            default:
                break;
        }
    }
}