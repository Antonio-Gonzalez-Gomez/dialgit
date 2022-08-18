package embed;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.kohsuke.github.GHIssue;
import org.kohsuke.github.GHIssueBuilder;
import org.kohsuke.github.GHRepository;
import org.kohsuke.github.GHUser;

import discord4j.core.GatewayDiscordClient;
import discord4j.core.object.entity.channel.MessageChannel;
import discord4j.core.object.reaction.ReactionEmoji;
import discord4j.core.spec.EmbedCreateSpec;
import discord4j.rest.util.Color;
import managers.App;
import managers.ZenhubManager;
import pojo.Sprint;
import pojo.ZHIssue;

public class IssueEmbed extends EmbedManager{

    private String title;
    private String name;
    private String body;
    private Integer estimate;
    private List<GHUser> assignees;
    private Boolean isEpic;
    private List<String> epics;
    private EpicList auxEpicList;
    private List<Sprint> sprints;
    private SprintList auxSprintList;
    private Map<String, List<Sprint>> auxDict;
    protected ZHIssue issue;

    public IssueEmbed(String userId, String serverId, MessageChannel canal,
            GatewayDiscordClient gateway, GHRepository repo, boolean readOnly,
            String title, String name, String body, ZHIssue issue,
            List<Sprint> sprints, Map<String, List<Sprint>> auxDict) {
        super(userId, serverId, canal, gateway, repo, readOnly);
        this.title = title;
        this.name = name;
        this.body = body;
        this.estimate = 0;
        this.assignees = new ArrayList<>();
        this.isEpic = false;
        this.epics = new ArrayList<>();
        this.sprints = new ArrayList<>();
        this.actions = new ArrayList<>();
        this.auxEpicList = new EpicList(userId, serverId, canal, gateway, repo,
            this::selectNewEpic, this::deselectEpic);
        if (!readOnly) {
            this.maxField = 5;
            this.actions.add(ReactionEmoji.unicode("❓"));
            this.actions.add(ReactionEmoji.unicode("⬆️"));
            this.actions.add(ReactionEmoji.unicode("⬇️"));
            this.actions.add(ReactionEmoji.unicode("🇪"));
            this.actions.add(ReactionEmoji.unicode("❌"));
            this.actions.add(ReactionEmoji.unicode("✅"));
            this.auxSprintList = new SprintList(userId, serverId, canal, gateway,
                repo, true, sprints, this::selectNewSprint, this::deselectSprint);
        }
        if (issue != null) {
            this.estimate = issue.getEstimate();
            this.assignees = issue.getGhIssue().getAssignees();
            this.isEpic = issue.getIsEpic();
            this.epics = new ArrayList<>(issue.getParentEpics());
            this.sprints = new ArrayList<>(issue.getSprints());
            this.auxDict = new HashMap<>(auxDict);
            this.sprints = auxDict.get(issue.getZenhubId());
            this.issue = issue;
        }
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
                if (length <= 1024)
                    body = data;
                else
                    error = "Issue body can't be longer than 1024 characters";
                break;

            case 2:
                try {
                    Matcher m = Pattern.compile("\\d*").matcher(data);
                    if (m.find())
                        estimate = Integer.parseInt(m.group());
                    else
                        error = "Please enter a valid integer number";
                } catch (NumberFormatException e) {
                    error = "Please enter a valid integer number";
                }
                break;
            
            case 3:
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

            case 4:
                auxEpicList.inputSelect(data);
                error = EMPTY_FIELD;
                break;

            case 5:
                auxSprintList.inputSelect(data);
                error = EMPTY_FIELD;
                break;

            default:
                break;
        }
        if (error == null ) {
            int oldSelected = selectedField;
            selectedField = selectedField == maxField ? selectedField : selectedField + 1;
            updateAuxiliarList(oldSelected);
        }
        updateEmbed();
    }

    public void selectNewIssue(ZHIssue issue) {
        if (issue == null) {
            log.info("Deleting selected issue");
            this.issue = null;
            this.name = EMPTY_FIELD;
            this.body = EMPTY_FIELD;
            this.estimate = 0;
            this.assignees = new ArrayList<>();
            this.isEpic = false;
            this.epics = new ArrayList<>();
            this.sprints = new ArrayList<>();
        } else {
            log.info("Selecting issue: " + issue.getGhIssue().getTitle());
            this.issue = issue;
            this.name = issue.getGhIssue().getTitle();
            this.body = issue.getGhIssue().getBody();
            this.estimate = issue.getEstimate();
            this.assignees = issue.getGhIssue().getAssignees();
            this.isEpic = issue.getIsEpic();
            this.epics = issue.getParentEpics();
            this.sprints = auxDict.get(issue.getZenhubId());
        }
        updateEmbed();
    }

    public void selectNewEpic(String epicId) {
        if (!epics.contains(epicId)) {
            log.info("Selecting epic: " + epicId);
            epics.add(epicId);
            updateEmbed();
        }
    }

    public void deselectEpic(String epicId) {
        if (epics.contains(epicId)) {
            log.info("Removing epic: " + epicId);
            epics.remove(epicId);
            updateEmbed();
        }
    }

    public void selectNewSprint(Sprint sprint) {
        if (!sprints.contains(sprint)) {
            log.info("Selecting sprint: " + sprint.getName());
            sprints.add(sprint);
            updateEmbed();
            
        }
    }

    public void deselectSprint(Sprint sprint) {
        if (sprints.contains(sprint)) {
            log.info("Removing sprint: " + sprint.getName());
            sprints.remove(sprint);
            updateEmbed();
            
        }
    }

    private void updateAuxiliarList(int oldSelectedField) {
        if (oldSelectedField != selectedField) {
            switch (oldSelectedField) {
                case 4:
                    auxEpicList.fluxDisposer.dispose();
                    auxEpicList.msg.delete().block();
                    break;

                case 5:
                    auxSprintList.fluxDisposer.dispose();
                    auxSprintList.msg.delete().block();
                    break;

                default:
                    break;
            }

            switch (selectedField) {
                case 4:
                    auxEpicList.send();
                    break;

                case 5:
                    auxSprintList.send();
                    break;

                default:
                    break;
            }
        }
    }

    protected void updateEmbed() {
        String[] fields = {"Name", "Body", "Estimate", "Assignees", "Assigned epic issue", "Sprints"};
        List<String> nombresAssignees = assignees.stream()
            .map(GHUser::getLogin).collect(Collectors.toList());
        String strAssignees = listParse(nombresAssignees, 99);
        String strEpics = listParse(epics.stream().map(auxEpicList::getEpicName)
            .collect(Collectors.toList()), 99);
        String strSprints = listParse(sprints.stream().map(Sprint::getName) 
            .collect(Collectors.toList()), 99);
        Color col = Color.GREEN;
        if (!readOnly) {
            col = Color.RUST;
            fields[selectedField] = "-> " + fields[selectedField] + " <-";
        }
        else if(issue != null && issue.getGhIssue().getState().toString().equals("CLOSED"))
            col = Color.DEEP_LILAC;
        emb = EmbedCreateSpec.builder()
            .color(col)
            .title(title)
            .addField(fields[0], name, false)
            .addField(fields[1], body == null ? EMPTY_FIELD : body, false)
            .addField(fields[2], String.valueOf(estimate), false)
            .addField(fields[3], strAssignees, false)
            .addField(fields[4], strEpics, false)
            .addField(fields[5], strSprints, false)
            .build();
        if (error != null && !error.equals(EMPTY_FIELD))
            emb = emb.withDescription(error);
        else if (isEpic)
            emb = emb.withDescription("Epic issue");
        updateMessage();
    }

    @Override
    protected void executeAction(String action) {
        int oldSelected = selectedField;
        switch (action) {
            case "❓":
                iconHelp();
                break;

            case "⬆️":
                selectedField = selectedField == 0 ? maxField : selectedField - 1;
                updateAuxiliarList(oldSelected);
                updateEmbed();
                
                break;
            
            case "⬇️":
                selectedField = selectedField == maxField ? 0 : selectedField + 1;
                updateAuxiliarList(oldSelected);
                updateEmbed();
                break;

            case "🇪":
                isEpic = !isEpic;
                updateEmbed();
                break;

            case "❌":
                if (selectedField == 3) {
                    auxSprintList.fluxDisposer.dispose();
                    auxSprintList.msg.delete().block();
                }
                end();
                break;

            case "✅":
                try {
                    if (issue == null) {
                        GHIssueBuilder builder = repo.createIssue(name).body(body);
                        assignees.forEach(builder::assignee);
                        Integer issueNumber = builder.create().getNumber();
                        ZenhubManager.createIssue(repo.getId(), issueNumber, estimate, isEpic, epics, sprints);
                    } else {
                        GHIssue ghiss = issue.getGhIssue();
                        ghiss.setTitle(name);
                        ghiss.setBody(body);
                        ghiss.setAssignees(assignees);
                        ZenhubManager.updateIssue(issue, repo, estimate, isEpic, epics, sprints);
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

    @Override
    protected void end() {
        log.debug("Deleting embed");
        fluxDisposer.dispose();
        msg.delete().block();
        switch (selectedField) {
            case 4:
                auxEpicList.fluxDisposer.dispose();
                auxEpicList.msg.delete().block();
                break;

            case 5:
                auxSprintList.fluxDisposer.dispose();
                auxSprintList.msg.delete().block();
                break;

            default:
                break;
        }
        App.sessions.put(userId + "-" + serverId, "Start");
        canal.createMessage("Can I help you with something more?").block();
    }
}