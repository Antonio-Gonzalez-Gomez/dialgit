package embed;

import java.io.IOException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

import org.kohsuke.github.GHRepository;
import org.kohsuke.github.GHUser;

import discord4j.core.GatewayDiscordClient;
import discord4j.core.object.entity.channel.MessageChannel;
import discord4j.core.object.reaction.ReactionEmoji;
import discord4j.core.spec.EmbedCreateSpec;
import discord4j.rest.util.Color;
import managers.GitlabManager;
import pojo.GLIssue;

public class GitlabIssueEmbed extends EmbedManager{

    private String title;
    private String projectPath;
    private String name;
    private List<GHUser> assignees;
    private LocalDate dueDate;
    protected GLIssue issue;

    public GitlabIssueEmbed(String userId, String serverId, MessageChannel canal,
            GatewayDiscordClient gateway, GHRepository repo, boolean readOnly,
            String projectPath, String title, GLIssue issue) {
        super(userId, serverId, canal, gateway, repo, readOnly);
        this.projectPath = projectPath;
        this.title = title;
        this.name = "New issue";
        this.assignees = new ArrayList<>();
        this.dueDate = LocalDate.now();
        this.actions = new ArrayList<>();
        if (!readOnly) {
            this.maxField = 2;
            this.actions.add(ReactionEmoji.unicode("❓"));
            this.actions.add(ReactionEmoji.unicode("⬆️"));
            this.actions.add(ReactionEmoji.unicode("⬇️"));
            this.actions.add(ReactionEmoji.unicode("❌"));
            this.actions.add(ReactionEmoji.unicode("✅"));
        }
        if (issue != null) {
            this.name = issue.getTitle();
            this.assignees = new ArrayList<>(issue.getAssignees());
            this.dueDate = issue.getDueDate();
            this.issue = issue;
        } else if (readOnly)
            this.name = EMPTY_FIELD;
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
                    error = "Issue name can't be longer than 1024 characters";
                break;
            
            case 1:
                List<String> nombres = Arrays.asList(data.split(" "));
                List<GHUser> collaborators = new ArrayList<>();
                try {
                    collaborators = repo.listCollaborators().toList();
                    assignees = collaborators.stream().filter(x 
                        -> nombres.contains(x.getLogin())).collect(Collectors.toList());
                } catch (IOException e) {
                    log.error(e.getMessage());
                }
                break;

            case 2:
                if (!data.equals("Date not found!")) {
                    String dateStr = data.split("T")[0];
                    DateTimeFormatter fmt = DateTimeFormatter.ofPattern("yyyy-MM-dd").withLocale(Locale.GERMANY);
                    dueDate = LocalDate.parse(dateStr, fmt);
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

    public void selectNewIssue(GLIssue issue) {
        if (issue == null) {
            log.info("Deleting selected issue");
            this.issue = null;
            this.name = EMPTY_FIELD;
            this.assignees = new ArrayList<>();
            this.dueDate = null;
        } else {
            log.info("Selecting issue: " + issue.getTitle());
            this.issue = issue;
            this.name = issue.getTitle();
            this.assignees = new ArrayList<>(issue.getAssignees());
            this.dueDate = issue.getDueDate();
        }
        updateEmbed();
    }

    protected void updateEmbed() {
        String[] fields = {"Name", "Assignees", "Due date"};
        List<String> nombresAssignees = assignees.stream()
            .map(GHUser::getLogin).collect(Collectors.toList());
        String strAssignees = listParse(nombresAssignees, 99);
        String strDate = EMPTY_FIELD;
        if (dueDate != null)
            strDate = dueDate.format(DateTimeFormatter.ofPattern("dd-MM-yyyy"));
        Color col = Color.GREEN;
        if (!readOnly) {
            col = Color.RUST;
            fields[selectedField] = "-> " + fields[selectedField] + " <-";
        }
        else if(issue != null && issue.getState().equals("closed"))
            col = Color.DEEP_LILAC;
        emb = EmbedCreateSpec.builder()
            .color(col)
            .title(title)
            .addField(fields[0], name, false)
            .addField(fields[1], strAssignees, false)
            .addField(fields[2], strDate, false)
            .build();
        if (error != null && !error.equals(EMPTY_FIELD))
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
                String dateStr = DateTimeFormatter.ofPattern("yyyy-MM-dd").format(dueDate) + "T00:00:00Z";
                if (issue == null)
                    GitlabManager.createIssue(projectPath, name, dateStr, assignees);
                else
                    GitlabManager.updateIssue(projectPath, name, dateStr, assignees, issue);
                end();
                break;

            default:
                break;
        }
    }
}