package embed;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import org.kohsuke.github.GHRepository;

import discord4j.core.GatewayDiscordClient;
import discord4j.core.object.entity.channel.MessageChannel;
import discord4j.core.object.reaction.ReactionEmoji;
import discord4j.core.spec.EmbedCreateFields.Field;
import discord4j.core.spec.EmbedCreateFields.Footer;
import discord4j.core.spec.EmbedCreateSpec;
import discord4j.rest.util.Color;
import managers.App;
import managers.GitlabManager;
import pojo.GLIssue;
import pojo.GLList;

public class GitlabKanbanEmbed extends EmbedManager{
    
    private List<GLList> lists;
    private int selectedList;
    private List<GLIssue> selectedListIssues;
    private int selectedPage;
    private int maxPage;
    private GitlabIssueEmbed auxIssueEmbed;
    private GLIssue holdedIssue;
    private int holdedIssueList;
    private String boardId;
    private String projectPath;

    public GitlabKanbanEmbed(String userId, String serverId, MessageChannel canal, GatewayDiscordClient gateway,
            GHRepository repo, List<GLList> lists, String boardId, String projectPath) {
    super(userId, serverId, canal, gateway, repo, false);
        this.lists = new ArrayList<>(lists);
        this.boardId = boardId;
        this.projectPath = projectPath;
        this.selectedList = 0;
        this.selectedField = 0;
        this.selectedListIssues = lists.get(selectedList).getIssues();
        this.maxField = selectedListIssues.size() - 1;
        this.selectedPage = 0;
        this.maxPage = this.maxField/ 10;
        if (selectedListIssues.size() > 10)
            this.maxField = 9;
        if (!selectedListIssues.isEmpty())
            this.auxIssueEmbed = new GitlabIssueEmbed(userId, serverId, canal, gateway, repo, 
                true, null, "Issue data", selectedListIssues.get(0));
        else
            this.auxIssueEmbed = new GitlabIssueEmbed(userId, serverId, canal, gateway, repo,
                true, null, "Issue data", null);
        this.actions = new ArrayList<>();
        this.actions.add(ReactionEmoji.unicode("❓"));
        this.actions.add(ReactionEmoji.unicode("⬆️"));
        this.actions.add(ReactionEmoji.unicode("⬇️"));
        this.actions.add(ReactionEmoji.unicode("⬅️"));
        this.actions.add(ReactionEmoji.unicode("➡️"));
        this.actions.add(ReactionEmoji.unicode("❌"));
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

    private GLIssue getEmbedIssue() {
        return selectedListIssues.get(selectedField + selectedPage * 10);
    }

    public void inputSelect(String input) {
        log.info("Selecting new element: " + input);
        int lastSelected = (selectedPage + 1) * 10 > selectedListIssues.size() ? selectedListIssues.size() : (selectedPage + 1) * 10;
        List<GLIssue> sublist = selectedListIssues.subList(selectedPage * 10, lastSelected);
        GLIssue inputIssue = sublist.stream().filter(x -> x.getTitle().equals(input)).findAny().orElse(null);
        if (inputIssue != null) {
            selectedField = sublist.indexOf(inputIssue);
            auxIssueEmbed.selectNewIssue(getEmbedIssue());
            updateEmbed();
        }
    }

    private void updateEmbed() {
        int lastSelected = (selectedPage + 1) * 10 > selectedListIssues.size() ? selectedListIssues.size() : (selectedPage + 1) * 10;
        String desc = EMPTY_FIELD;
        if (holdedIssue != null)
            desc = "Holding issue: " + holdedIssue.getTitle();
        List<GLIssue> sublist = selectedListIssues.subList(selectedPage * 10, lastSelected);
        String footer = null;
        if (maxPage > 0) {
            Integer total = selectedListIssues.size();
            Integer inicio = selectedPage * 10;
            Integer fin = inicio + 10 > total ? total : inicio + 10;
            footer = "Showing issues " + inicio + "-" + fin + " of " + total;
        }
        emb = EmbedCreateSpec.builder()
            .color(Color.LIGHT_SEA_GREEN)
            .title(lists.get(selectedList).getName())
            .description(desc)
            .build();
        List<Field> fields = sublist.stream().map(x -> sublist.indexOf(x) == selectedField ?
            Field.of("-> " + x.getTitle() + " <-", "#-" + String.valueOf(x.getNumber()), false) : 
            Field.of(x.getTitle(), "#-" + String.valueOf(x.getNumber()), false)).collect(Collectors.toList());
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

            case "🔐":
                if (auxIssueEmbed.issue != null) {
                    GLIssue iss = auxIssueEmbed.issue;
                    if (iss.getState().equals("closed"))
                        GitlabManager.changeIssueState(projectPath, iss.getNumber(), "REOPEN");
                    else
                        GitlabManager.changeIssueState(projectPath, iss.getNumber(), "CLOSE");
                    auxIssueEmbed.selectNewIssue(getEmbedIssue());
                    //Al cerrar issues, estas se pueden mover de columnas
                    lists = GitlabManager.getLists(boardId, projectPath, repo);
                    int temp = selectedListIssues.size();
                    selectedListIssues = lists.get(selectedList).getIssues();
                    if (temp > selectedListIssues.size()) {
                        if (!selectedListIssues.isEmpty())
                            auxIssueEmbed.selectNewIssue(selectedListIssues
                                .get(selectedField == 0 ? 0 : --selectedField));
                        else
                            auxIssueEmbed.selectNewIssue(null);
                        updateEmbed();
                    }
                        
                    auxIssueEmbed.updateEmbed();
                }
                break;

            case "◀️":
                selectedList = selectedList == 0 ? lists.size() - 1 : selectedList - 1;
                selectedListIssues = lists.get(selectedList).getIssues();
                if (!selectedListIssues.isEmpty())
                    auxIssueEmbed.selectNewIssue(selectedListIssues.get(0));
                else
                    auxIssueEmbed.selectNewIssue(null);
                selectedField = 0;
                maxField = selectedListIssues.size() - 1;
                selectedPage = 0;
                maxPage = maxField/ 10;
                updateEmbed();
                
                break;

            case "💠":
                if (holdedIssue == null) {
                    if (!selectedListIssues.isEmpty()) {
                        holdedIssue = getEmbedIssue();
                        holdedIssueList = selectedList;
                        updateEmbed();
                        
                    }
                } else {
                    GLList selected = lists.get(selectedList);
                    if (!selected.getIssues().contains(holdedIssue)) {
                        GitlabManager.moveIssue(projectPath, boardId, holdedIssue.getNumber(), lists.get(holdedIssueList).getId(), selected.getId());
                        selected.addIssue(holdedIssue);
                        selectedListIssues = selected.getIssues();
                        lists.get(holdedIssueList).removeIssue(holdedIssue);
                        holdedIssue = null;
                        if (selectedListIssues.size() == 1)
                        //Si la lista estaba vacía, hay que seleccionar la issue para el embed de abajo
                            auxIssueEmbed.selectNewIssue(selectedListIssues.get(0));
                        updateEmbed();
                    }
                }
                break;

            case "▶️":
                selectedList = selectedList == lists.size() - 1 ? 0 : selectedList + 1;
                selectedListIssues = lists.get(selectedList).getIssues();
                if (!selectedListIssues.isEmpty())
                    auxIssueEmbed.selectNewIssue(selectedListIssues.get(0));
                else
                    auxIssueEmbed.selectNewIssue(null);
                selectedField = 0;
                maxField = selectedListIssues.size() - 1;
                selectedPage = 0;
                maxPage = maxField/ 10;
                updateEmbed();
                
                break;

            case "✏️":
                GLIssue issue = auxIssueEmbed.issue;
                GitlabIssueEmbed manager = new GitlabIssueEmbed(userId, serverId, canal, gateway, repo,
                    false, projectPath, "Edit issue", issue);
                end();
                manager.send();
                App.embedDict.put(userId + "-" + serverId, manager);
                App.sessions.put(userId + "-" + serverId, "InputGitlabIssue");
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
