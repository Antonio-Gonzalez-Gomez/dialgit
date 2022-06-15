package embed;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import org.kohsuke.github.GHMilestone;
import org.kohsuke.github.GHMilestoneState;
import org.kohsuke.github.GHRepository;

import discord4j.core.GatewayDiscordClient;
import discord4j.core.object.entity.channel.MessageChannel;
import discord4j.core.object.reaction.ReactionEmoji;
import discord4j.core.spec.EmbedCreateFields.Field;
import discord4j.core.spec.EmbedCreateFields.Footer;
import discord4j.core.spec.EmbedCreateSpec;
import discord4j.rest.util.Color;
import managers.App;

public class MilestoneList extends EmbedManager{
    
    private List<GHMilestone> milestones;
    private int selectedPage;
    private int maxPage;
    private MilestoneEmbed selectedMilestone;
    private Consumer<GHMilestone> issueCallback;

    public MilestoneList(String userId, String serverId, MessageChannel canal, GatewayDiscordClient gateway,
            GHRepository repo, Boolean readOnly, Boolean issueAux, List<GHMilestone> milestones) {
        super(userId, serverId, canal, gateway, repo, readOnly);
        this.milestones = milestones;
        this.selectedMilestone = new MilestoneEmbed(userId, serverId, canal, gateway, repo,
            true, "Milestone info", EMPTY_FIELD, EMPTY_FIELD, EMPTY_FIELD, milestones.get(0));
        this.selectedField = 0;
        this.maxField = milestones.size() - 1;
        this.selectedPage = 0;
        this.maxPage = this.maxField/ 10;
        this.readOnly = readOnly;
        this.actions = new ArrayList<>();
        this.actions.add(ReactionEmoji.unicode("❓"));
        this.actions.add(ReactionEmoji.unicode("⬆️"));
        this.actions.add(ReactionEmoji.unicode("⬇️"));
        if (milestones.size() > 10) {
            this.maxField = 9;
            this.actions.add(ReactionEmoji.unicode("⬅️"));
            this.actions.add(ReactionEmoji.unicode("➡️"));
        }
        if (issueAux)
            this.actions.add(ReactionEmoji.unicode("✅"));
        else {
            this.actions.add(ReactionEmoji.unicode("❌"));
            this.actions.add(ReactionEmoji.unicode("🔐"));
            this.actions.add(ReactionEmoji.unicode("✏️"));
        }
        updateEmbed();
    }

    public void setIssueCallback(Consumer<GHMilestone> lambda) {
        this.issueCallback = lambda;
    }

    public void sendSecondEmbed() {
        selectedMilestone.send();
    }

    private GHMilestone getSelectedMilestone() {
        return milestones.get(selectedField + selectedPage * 10);
    }

    public void inputSelect(String input) {
        log.info("Selecting new element: " + input);
        int lastSelected = (selectedPage + 1) * 10 > milestones.size() ? milestones.size() : (selectedPage + 1) * 10;
        List<GHMilestone> sublist = milestones.subList(selectedPage * 10, lastSelected);
        GHMilestone inputMilestone = sublist.stream().filter(x -> x.getTitle().equals(input)).findAny().orElse(null);
        if (inputMilestone != null) {
            selectedField = sublist.indexOf(inputMilestone);
            selectedMilestone.selectNewMilestone(getSelectedMilestone());
            updateEmbed();
            updateMessage();
        }
    }

    private void updateEmbed() {
        int lastSelected = (selectedPage + 1) * 10 > milestones.size() ? milestones.size() : (selectedPage + 1) * 10;
        List<GHMilestone> sublist = milestones.subList(selectedPage * 10, lastSelected);
        String footer = null;
        if (maxPage > 0) {
            Integer total = milestones.size();
            Integer inicio = selectedPage * 10;
            Integer fin = inicio + 10 > total ? total : inicio + 10;
            footer = "Showing milestones " + inicio + "-" + fin + " of " + total;
        }
        emb = EmbedCreateSpec.builder()
            .color(Color.LIGHT_SEA_GREEN)
            .title("Milestone list")
            .build();
        List<Field> fields = sublist.stream().map(x -> sublist.indexOf(x) == selectedField ?
        Field.of("-> " + x.getTitle() + " <-", "Due on " + dateParse(x.getDueOn()), false) : 
        Field.of(x.getTitle(), "Due on " + dateParse(x.getDueOn()), false)).collect(Collectors.toList());
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
                selectedField = selectedField == 0 ? maxField : selectedField - 1;
                selectedMilestone.selectNewMilestone(getSelectedMilestone());
                updateEmbed();
                updateMessage();
                break;
            
            case "⬇️":
                selectedField = selectedField == maxField ? 0 : selectedField + 1;
                selectedMilestone.selectNewMilestone(getSelectedMilestone());
                updateEmbed();
                updateMessage();
                break;

            case "⬅️":
                selectedPage = selectedPage == 0 ? maxPage : selectedPage - 1;
                selectedMilestone.selectNewMilestone(getSelectedMilestone());
                updateEmbed();
                updateMessage();
                break;

            case "➡️":
                selectedPage = selectedPage == maxPage ? 0 : selectedPage + 1;
                selectedMilestone.selectNewMilestone(getSelectedMilestone());
                updateEmbed();
                updateMessage();
                break;

            case "❌":
                end();
                App.sessions.put(userId + "-" + serverId, "Start");
                canal.createMessage("Can I help you with something more?").block();
                break;

            case "✅":
                issueCallback.accept(selectedMilestone.milestone);
                break;

            case "🔐":
                GHMilestone mlst = selectedMilestone.milestone;
                try {
                    if (mlst.getState().equals(GHMilestoneState.CLOSED))
                        mlst.reopen();
                    else
                        mlst.close();
                    selectedMilestone.selectNewMilestone(getSelectedMilestone());
                } catch(IOException ex) {
                    log.error(ex.getMessage());
                }
                
                break;

            case "✏️":
                GHMilestone mlstn = selectedMilestone.milestone;
                MilestoneEmbed manager = new MilestoneEmbed(userId, serverId, canal, gateway,
                    repo, false, "Edit milestone", mlstn.getTitle(), mlstn.getDescription(),
                    dateParse(mlstn.getDueOn()), mlstn);
                manager.send();
                App.embedDict.put(userId + "-" + serverId, manager);
                App.sessions.put(userId + "-" + serverId, "InputMilestone");
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
        selectedMilestone.msg.delete().block();
    }
}
