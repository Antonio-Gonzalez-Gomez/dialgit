package embed;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.kohsuke.github.GHRepository;
import org.kohsuke.github.GHUser;
import discord4j.core.GatewayDiscordClient;
import discord4j.core.object.entity.channel.MessageChannel;
import discord4j.core.object.reaction.ReactionEmoji;
import discord4j.core.spec.EmbedCreateFields.Field;
import discord4j.core.spec.EmbedCreateFields.Footer;
import discord4j.core.spec.EmbedCreateSpec;
import discord4j.rest.util.Color;

import managers.App;
import managers.ZenhubManager;
import pojo.Pipeline;
import pojo.Sprint;
import pojo.ZHIssue;

public class KanbanEmbed extends EmbedManager{

    private List<Pipeline> pipelines;
    private int selectedPipeline;
    private List<ZHIssue> selectedPipelineIssues;
    private int selectedPage;
    private int maxPage;
    private IssueEmbed auxIssueEmbed;
    private ZHIssue holdedIssue;
    private int holdedIssuePipeline;
    private boolean onlySelfAssigned;
    private GHUser user;
    private String workspaceID;

    public KanbanEmbed(String userId, String serverId, MessageChannel canal, GatewayDiscordClient gateway,
            GHRepository repo, String workspaceID, GHUser user) {
    super(userId, serverId, canal, gateway, repo, false);
        this.workspaceID = workspaceID;
        this.pipelines =  ZenhubManager.getPipelines(workspaceID, repo);
        this.selectedPipeline = 0;
        this.selectedField = 0;
        this.selectedPipelineIssues = pipelines.get(selectedPipeline).getIssues();
        this.maxField = selectedPipelineIssues.size() - 1;
        this.selectedPage = 0;
        this.maxPage = this.maxField/ 10;
        Map<String, List<Sprint>> auxDict = ZenhubManager.getIssueToSprintMap(repo);
        List<Sprint> sprints = ZenhubManager.getSprints(repo);
        if (selectedPipelineIssues.size() > 10)
            this.maxField = 9;
        if (!selectedPipelineIssues.isEmpty())
            this.auxIssueEmbed = new IssueEmbed(userId, serverId, canal, gateway, repo, true,
                "Issue data", selectedPipelineIssues.get(0).getGhIssue().getTitle(), selectedPipelineIssues.get(0).getGhIssue().getBody(),
                selectedPipelineIssues.get(0), sprints, auxDict);
        else
            this.auxIssueEmbed = new IssueEmbed(userId, serverId, canal, gateway, repo,
                true, "Issue data", EMPTY_FIELD, EMPTY_FIELD, null, sprints, auxDict);
        this.onlySelfAssigned = false;
        this.user = user;        
        this.actions = new ArrayList<>();
        this.actions.add(ReactionEmoji.unicode("❓"));
        this.actions.add(ReactionEmoji.unicode("⬆️"));
        this.actions.add(ReactionEmoji.unicode("⬇️"));
        this.actions.add(ReactionEmoji.unicode("⬅️"));
        this.actions.add(ReactionEmoji.unicode("➡️"));
        this.actions.add(ReactionEmoji.unicode("❌"));
        this.actions.add(ReactionEmoji.unicode("👤"));
        this.actions.add(ReactionEmoji.unicode("🔐"));
        this.actions.add(ReactionEmoji.unicode("◀️"));
        this.actions.add(ReactionEmoji.unicode("💠"));
        this.actions.add(ReactionEmoji.unicode("▶️"));
        this.actions.add(ReactionEmoji.unicode("✏️"));
        updateEmbed();
    }

    public void sendSecondEmbed() {
        auxIssueEmbed.send();
    }

    private ZHIssue getEmbedIssue() {
        return selectedPipelineIssues.get(selectedField + selectedPage * 10);
    }

    public void inputSelect(String input) {
        log.info("Selecting new element: " + input);
        int lastSelected = (selectedPage + 1) * 10 > selectedPipelineIssues.size() ? selectedPipelineIssues.size() : (selectedPage + 1) * 10;
        List<ZHIssue> sublist = selectedPipelineIssues.subList(selectedPage * 10, lastSelected);
        ZHIssue inputIssue = sublist.stream().filter(x -> x.getGhIssue().getTitle().equals(input)).findAny().orElse(null);
        if (inputIssue != null) {
            selectedField = sublist.indexOf(inputIssue);
            auxIssueEmbed.selectNewIssue(getEmbedIssue());
            updateEmbed();
            
        }
    }

    private void updateEmbed() {
        int lastSelected = (selectedPage + 1) * 10 > selectedPipelineIssues.size() ? selectedPipelineIssues.size() : (selectedPage + 1) * 10;
        String desc = EMPTY_FIELD;
        if (holdedIssue != null)
            desc = "Holding issue: " + holdedIssue.getGhIssue().getTitle();
        List<ZHIssue> sublist = selectedPipelineIssues.subList(selectedPage * 10, lastSelected);
        String footer = null;
        if (maxPage > 0) {
            Integer total = selectedPipelineIssues.size();
            Integer inicio = selectedPage * 10;
            Integer fin = inicio + 10 > total ? total : inicio + 10;
            footer = "Showing issues " + inicio + "-" + fin + " of " + total;
        }
        emb = EmbedCreateSpec.builder()
            .color(Color.LIGHT_SEA_GREEN)
            .title(pipelines.get(selectedPipeline).getName())
            .description(desc)
            .build();
        List<Field> fields = sublist.stream().map(x -> sublist.indexOf(x) == selectedField ?
            Field.of("-> " + x.getGhIssue().getTitle() + " <-", "#-" + String.valueOf(x.getNumber()), false) : 
            Field.of(x.getGhIssue().getTitle(), "#-" + String.valueOf(x.getNumber()), false)).collect(Collectors.toList());
        emb = emb.withFields(fields);
        if (footer != null)
            emb = emb.withFooter(Footer.of(footer, "https://64.media.tumblr.com/aa65057a2ba418757cee5ae25c07d790/tumblr_pb2ky0qi6A1w6xh18o8_250.png"));
        updateMessage();
    }

    @Override
    protected void executeAction(String action) {
        switch (action) {
            case "❓":
                iconHelp();
                break;
                
            case "⬆️":
                if (maxField > 0) {
                    selectedField = selectedField == 0 ? maxField : selectedField - 1;
                    auxIssueEmbed.selectNewIssue(getEmbedIssue());
                    updateEmbed();
                    
                }
                break;
            
            case "⬇️":
                if (maxField > 0) {
                    selectedField = selectedField == maxField ? 0 : selectedField + 1;
                    auxIssueEmbed.selectNewIssue(getEmbedIssue());
                    updateEmbed();
                    
                }
                break;

            case "⬅️":
                if (maxPage > 0) {
                    selectedPage = selectedPage == 0 ? maxPage : selectedPage - 1;
                    auxIssueEmbed.selectNewIssue(getEmbedIssue());
                    updateEmbed();
                    
                }
                break;

            case "➡️":
                if (maxPage > 0) {
                    selectedPage = selectedPage == maxPage ? 0 : selectedPage + 1;
                    auxIssueEmbed.selectNewIssue(getEmbedIssue());
                    updateEmbed();
                    
                }
                break;

            case "❌":
                end();
                App.sessions.put(userId + "-" + serverId, "Start");
                canal.createMessage("Can I help you with something more?").block();
                break;

            case "👤":
                onlySelfAssigned = !onlySelfAssigned;
                if (onlySelfAssigned)
                    selectedPipelineIssues = pipelines.get(selectedPipeline).getIssues()
                        .stream().filter(x -> x.getGhIssue().getAssignees().contains(user))
                        .collect(Collectors.toList());
                else
                    selectedPipelineIssues = pipelines.get(selectedPipeline).getIssues();
                updateEmbed();
                
                break;

            case "🔐":
                if (auxIssueEmbed.issue != null) {
                    ZHIssue iss = auxIssueEmbed.issue;
                    try {
                        if (iss.getGhIssue().getState().toString().equals("CLOSED"))
                            iss.getGhIssue().reopen();
                        else
                            iss.getGhIssue().close();
                        //Al cerrar issues, estas se pueden mover de pipeline
                        pipelines = ZenhubManager.getPipelines(workspaceID, repo);
                        int temp = selectedPipelineIssues.size();
                        selectedPipelineIssues = pipelines.get(selectedPipeline).getIssues();
                        if (temp > selectedPipelineIssues.size()) {
                            if (!selectedPipelineIssues.isEmpty())
                                auxIssueEmbed.selectNewIssue(selectedPipelineIssues
                                    .get(selectedField == 0 ? 0 : --selectedField));
                            else
                                auxIssueEmbed.selectNewIssue(null);
                            updateEmbed();
                        }
                        auxIssueEmbed.selectNewIssue(iss);
                    } catch(IOException ex) {
                        log.error(ex.getMessage());
                    }
                    auxIssueEmbed.updateEmbed();
                }
                break;

            case "◀️":
                selectedPipeline = selectedPipeline == 0 ? pipelines.size() - 1 : selectedPipeline - 1;
                selectedPipelineIssues = pipelines.get(selectedPipeline).getIssues();
                if (onlySelfAssigned)
                    selectedPipelineIssues = selectedPipelineIssues.stream()
                        .filter(x -> x.getGhIssue().getAssignees().contains(user))
                        .collect(Collectors.toList());
                if (!selectedPipelineIssues.isEmpty())
                    auxIssueEmbed.selectNewIssue(selectedPipelineIssues.get(0));
                else
                    auxIssueEmbed.selectNewIssue(null);
                selectedField = 0;
                maxField = selectedPipelineIssues.size() - 1;
                selectedPage = 0;
                maxPage = maxField/ 10;
                updateEmbed();
                
                break;

            case "💠":
                if (holdedIssue == null) {
                    if (!selectedPipelineIssues.isEmpty()) {
                        holdedIssue = getEmbedIssue();
                        holdedIssuePipeline = selectedPipeline;
                        updateEmbed();
                        
                    }
                } else {
                    Pipeline selected = pipelines.get(selectedPipeline);
                    if (!selected.getIssues().contains(holdedIssue)) {
                        
                        String pipeId = selected.getId();
                        ZenhubManager.moveIssue(holdedIssue.getZenhubId(), pipeId);
                        selected.addIssue(holdedIssue);
                        selectedPipelineIssues = selected.getIssues();
                        pipelines.get(holdedIssuePipeline).removeIssue(holdedIssue);
                        holdedIssue = null;
                        if (selectedPipelineIssues.size() == 1)
                        //Si la pipeline estaba vacía, hay que seleccionar la issue para el embed de abajo
                            auxIssueEmbed.selectNewIssue(selectedPipelineIssues.get(0));
                        updateEmbed();
                        
                    }
                }
                break;

            case "▶️":
                selectedPipeline = selectedPipeline == pipelines.size() - 1 ? 0 : selectedPipeline + 1;
                selectedPipelineIssues = pipelines.get(selectedPipeline).getIssues();
                if (onlySelfAssigned)
                    selectedPipelineIssues = selectedPipelineIssues.stream()
                        .filter(x -> x.getGhIssue().getAssignees().contains(user))
                        .collect(Collectors.toList());
                if (!selectedPipelineIssues.isEmpty())
                    auxIssueEmbed.selectNewIssue(selectedPipelineIssues.get(0));
                else
                    auxIssueEmbed.selectNewIssue(null);
                selectedField = 0;
                maxField = selectedPipelineIssues.size() - 1;
                selectedPage = 0;
                maxPage = maxField/ 10;
                updateEmbed();
                
                break;

            case "✏️":
                ZHIssue issue = auxIssueEmbed.issue;
                try {
                    end();
                    issue.setGhIssue(repo.getIssue(issue.getNumber()));
                    List<Sprint> sprints = ZenhubManager.getSprints(repo);
                    Map<String, List<Sprint>> auxDict = ZenhubManager.getIssueToSprintMap(repo);
                    IssueEmbed manager = new IssueEmbed(userId, serverId, canal, gateway, repo,
                        false, "Edit issue", issue.getGhIssue().getTitle(), issue.getGhIssue().getBody(),
                        issue, sprints, auxDict);
                    manager.send();
                    App.embedDict.put(userId + "-" + serverId, manager);
                    App.sessions.put(userId + "-" + serverId, "InputIssue");
                } catch (IOException e) {
                    log.error(e);
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
        auxIssueEmbed.msg.delete().block();
    }
}
