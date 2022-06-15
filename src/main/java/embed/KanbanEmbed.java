package embed;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import org.kohsuke.github.GHIssue;
import org.kohsuke.github.GHIssueState;
import org.kohsuke.github.GHMilestone;
import org.kohsuke.github.GHProject;
import org.kohsuke.github.GHProjectCard;
import org.kohsuke.github.GHProjectColumn;
import org.kohsuke.github.GHRepository;

import discord4j.core.GatewayDiscordClient;
import discord4j.core.object.entity.channel.MessageChannel;
import discord4j.core.object.reaction.ReactionEmoji;
import discord4j.core.spec.EmbedCreateFields.Field;
import discord4j.core.spec.EmbedCreateFields.Footer;
import discord4j.core.spec.EmbedCreateSpec;
import discord4j.rest.util.Color;
import managers.App;

public class KanbanEmbed extends EmbedManager{
    
    private List<GHProjectColumn> columns;
    private List<GHIssue> selectedColumnIssues;
    private int selectedColumn;
    private int selectedPage;
    private int maxPage;
    private IssueEmbed selectedIssue;
    private GHProjectCard selectedCard;
    private boolean onlySelfAssigned;

    public KanbanEmbed(String userId, String serverId, MessageChannel canal, GatewayDiscordClient gateway,
            GHRepository repo, Boolean readOnly, GHProject project) {
        super(userId, serverId, canal, gateway, repo, readOnly);
        try {
            this.columns = project.listColumns().toList();
            this.selectedColumn = 0;
            this.selectedColumnIssues = columns.get(0).listCards().toList().stream()
                .map(x -> {
                    try {
                        System.out.println(x.getContent().getTitle());
                        return x.getContent();
                    } catch (IOException e) {
                        log.error(e.getMessage());
                        return null;
                    }
                }).collect(Collectors.toList());
            this.selectedField = 0;
            this.maxField = selectedColumnIssues.size() - 1;
            this.selectedPage = 0;
            this.maxPage = this.maxField/ 10;
            if (selectedColumnIssues.size() > 10)
                this.maxField = 9;
            if (!selectedColumnIssues.isEmpty())
                this.selectedIssue = new IssueEmbed(userId, serverId, canal, gateway, repo, true,
                    "Issue data", selectedColumnIssues.get(0).getTitle(), selectedColumnIssues.get(0).getBody(), selectedColumnIssues.get(0), null);
            else
                this.selectedIssue = new IssueEmbed(userId, serverId, canal, gateway, repo,
                    true, "Issue data", EMPTY_FIELD, EMPTY_FIELD, null, null);
            this.onlySelfAssigned = false;
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
        } catch (IOException e) {
            log.error(e.getMessage());
        }
    }

    public void sendSecondEmbed() {
        selectedIssue.send();
    }

    private GHIssue getSelectedIssue() {
        return selectedColumnIssues.get(selectedField + selectedPage * 10);
    }

    public void inputSelect(String input) {
        log.info("Selecting new element: " + input);
        int lastSelected = (selectedPage + 1) * 10 > selectedColumnIssues.size() ? selectedColumnIssues.size() : (selectedPage + 1) * 10;
        List<GHIssue> sublist = selectedColumnIssues.subList(selectedPage * 10, lastSelected);
        GHIssue inputIssue = sublist.stream().filter(x -> x.getTitle().equals(input)).findAny().orElse(null);
        if (inputIssue != null) {
            selectedField = sublist.indexOf(inputIssue);
            selectedIssue.selectNewIssue(getSelectedIssue());
            updateEmbed();
            updateMessage();
        }
    }

    private void updateEmbed() {
        int lastSelected = (selectedPage + 1) * 10 > selectedColumnIssues.size() ? selectedColumnIssues.size() : (selectedPage + 1) * 10;
        String desc = EMPTY_FIELD;
        if (selectedCard != null)
            try {
                desc = "Holding issue: " + selectedCard.getContent().getTitle();
            } catch (IOException e) {
                log.error(e.getMessage());
            }
        List<GHIssue> sublist = selectedColumnIssues.subList(selectedPage * 10, lastSelected);
        String footer = null;
        if (maxPage > 0) {
            Integer total = selectedColumnIssues.size();
            Integer inicio = selectedPage * 10;
            Integer fin = inicio + 10 > total ? total : inicio + 10;
            footer = "Showing issues " + inicio + "-" + fin + " of " + total;
        }
        emb = EmbedCreateSpec.builder()
            .color(Color.LIGHT_SEA_GREEN)
            .title(columns.get(selectedColumn).getName())
            .description(desc)
            .build();
        List<Field> fields = sublist.stream().map(x -> sublist.indexOf(x) == selectedField ?
            Field.of("-> " + x.getTitle() + " <-", String.valueOf(x.getId()), false) : 
            Field.of(x.getTitle(), String.valueOf(x.getId()), false)).collect(Collectors.toList());
        emb = emb.withFields(fields);
        if (footer != null)
            emb = emb.withFooter(Footer.of(footer, "https://64.media.tumblr.com/aa65057a2ba418757cee5ae25c07d790/tumblr_pb2ky0qi6A1w6xh18o8_250.png"));
        
    }

    @Override
    protected void executeAction(String action) {
        log.info("Selected action: " + action);
        switch (action) {
            case "❓":
                iconHelp();
                break;
                
            case "⬆️":
                if (maxField > 0) {
                    selectedField = selectedField == 0 ? maxField : selectedField - 1;
                    selectedIssue.selectNewIssue(getSelectedIssue());
                    updateEmbed();
                    updateMessage();
                }
                break;
            
            case "⬇️":
                if (maxField > 0) {
                    selectedField = selectedField == maxField ? 0 : selectedField + 1;
                    selectedIssue.selectNewIssue(getSelectedIssue());
                    updateEmbed();
                    updateMessage();
                }
                break;

            case "⬅️":
                if (maxPage > 0) {
                    selectedPage = selectedPage == 0 ? maxPage : selectedPage - 1;
                    selectedIssue.selectNewIssue(getSelectedIssue());
                    updateEmbed();
                    updateMessage();
                }
                break;

            case "➡️":
                if (maxPage > 0) {
                    selectedPage = selectedPage == maxPage ? 0 : selectedPage + 1;
                    selectedIssue.selectNewIssue(getSelectedIssue());
                    updateEmbed();
                    updateMessage();
                }
                break;

            case "❌":
                end();
                App.sessions.put(userId + "-" + serverId, "Start");
                canal.createMessage("Can I help you with something more?").block();
                break;

            case "👤":
                onlySelfAssigned = !onlySelfAssigned;
                updateEmbed();
                updateMessage();
                break;

            case "🔐":
                if (selectedIssue.issue != null) {
                    GHIssue mlst = selectedIssue.issue;
                    try {
                        if (mlst.getState().toString().equals("CLOSED"))
                            mlst.reopen();
                        else
                            mlst.close();
                        selectedIssue.selectNewIssue(getSelectedIssue());
                        int temp = selectedColumnIssues.size();
                        selectedColumnIssues = columns.get(selectedColumn).listCards().toList().stream()
                            .map(x -> {
                                try {
                                    return x.getContent();
                                } catch (IOException e) {
                                    log.error(e.getMessage());
                                    return null;
                                }
                            }).collect(Collectors.toList());
                        if (temp > selectedColumnIssues.size()) {
                            if (!selectedColumnIssues.isEmpty())
                                selectedIssue.selectNewIssue(selectedColumnIssues
                                    .get(selectedField == 0 ? 0 : --selectedField));
                            else
                                selectedIssue.selectNewIssue(null);
                            updateEmbed();
                            updateMessage();
                        }
                    } catch(IOException ex) {
                        log.error(ex.getMessage());
                    }
                    selectedIssue.updateEmbed();
                    selectedIssue.updateMessage();
                }
                break;

            case "◀️":
                try {
                    selectedColumn = selectedColumn == 0 ? columns.size() - 1 : selectedColumn - 1;
                    selectedColumnIssues = columns.get(selectedColumn).listCards().toList().stream()
                    .map(x -> {
                        try {
                            return x.getContent();
                        } catch (IOException e) {
                            log.error(e.getMessage());
                            return null;
                        }
                    }).collect(Collectors.toList());
                    if (!selectedColumnIssues.isEmpty())
                        selectedIssue.selectNewIssue(selectedColumnIssues.get(0));
                    else
                        selectedIssue.selectNewIssue(null);
                    selectedField = 0;
                    maxField = selectedColumnIssues.size() - 1;
                    selectedPage = 0;
                    maxPage = this.maxField/ 10;
                    updateEmbed();
                    updateMessage();
                } catch (IOException e) {
                    log.error(e.getMessage());
                }
                break;

            case "💠":
                if (selectedCard == null) {
                    if (!selectedColumnIssues.isEmpty()) {
                        try {
                            selectedCard = columns.get(selectedColumn).listCards().toList().stream()
                                .filter(x -> {
                                    try {
                                        return x.getContent().getId() == getSelectedIssue().getId();
                                    } catch (IOException e) {
                                        log.error(e.getMessage());
                                        return false;
                                    }
                                }).findFirst().orElse(null);
                            updateEmbed();
                            updateMessage();
                        } catch (IOException e) {
                            log.error(e.getMessage());
                        }
                    }
                } else {
                    try {
                        if (!selectedCard.getColumn().equals(columns.get(0))) {
                            GHIssue issueToMove = selectedCard.getContent();
                            selectedCard.delete();
                            selectedCard = null;
                            columns.get(selectedColumn).createCard(issueToMove);
                            selectedColumnIssues = columns.get(selectedColumn).listCards().toList().stream()
                                .map(x -> {
                                    try {
                                        return x.getContent();
                                    } catch (IOException e) {
                                        log.error(e.getMessage());
                                        return null;
                                    }
                                }).collect(Collectors.toList());
                            if (selectedColumnIssues.size() == 1)
                                selectedIssue.selectNewIssue(selectedColumnIssues.get(0));
                            updateEmbed();
                            updateMessage();
                        }
                    } catch (IOException e) {
                        log.error(e.getMessage());
                    }
                    
                }
                break;

            case "▶️":
                try {
                    selectedColumn = selectedColumn == columns.size() - 1 ? 0 : selectedColumn + 1;
                    selectedColumnIssues = columns.get(selectedColumn).listCards().toList().stream()
                    .map(x -> {
                        try {
                            return x.getContent();
                        } catch (IOException e) {
                            log.error(e.getMessage());
                            return null;
                        }
                    }).collect(Collectors.toList());

                    if (!selectedColumnIssues.isEmpty())
                        selectedIssue.selectNewIssue(selectedColumnIssues.get(0));
                    else
                        selectedIssue.selectNewIssue(null);
                    selectedField = 0;
                    maxField = selectedColumnIssues.size() - 1;
                    selectedPage = 0;
                    maxPage = this.maxField/ 10;
                    updateEmbed();
                    updateMessage();
                } catch (IOException e) {
                    log.error(e.getMessage());
                }
                break;

            case "✏️":
                try {
                    GHIssue issue = selectedIssue.issue;
                    List<GHMilestone> lista = repo.listMilestones(GHIssueState.valueOf("OPEN")).toList();
                    IssueEmbed manager = new IssueEmbed(userId, serverId, canal, gateway, repo,
                        false, "Edit issue", issue.getTitle(), issue.getBody(), issue, lista);
                    manager.send();
                    App.embedDict.put(userId + "-" + serverId, manager);
                    App.sessions.put(userId + "-" + serverId, "InputIssue");
                } catch (IOException e) {
                    log.error(e.getMessage());
                }
                end();
                break;

            default:
                break;
        }
    }

    @Override
    protected void end() {
        log.debug("Deleting embed");
        this.fluxDisposer.dispose();
        msg.delete().block();
        selectedIssue.msg.delete().block();
    }
}
