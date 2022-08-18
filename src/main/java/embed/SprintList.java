package embed;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
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
import pojo.Sprint;

public class SprintList extends EmbedManager{
    
    private List<Sprint> sprints;
    private int selectedPage;
    private int maxPage;
    private SprintEmbed selectedSprint;
    private Consumer<Sprint> selectionCallback;
    private Consumer<Sprint> removalCallback;

    public SprintList(String userId, String serverId, MessageChannel canal, GatewayDiscordClient gateway,
            GHRepository repo, Boolean isAux, List<Sprint> sprints, Consumer<Sprint> selectionCallback,
            Consumer<Sprint> removalCallback) {
        super(userId, serverId, canal, gateway, repo, false);
        this.sprints = sprints;
        this.selectedField = 0;
        this.maxField = sprints.size() - 1;
        this.selectedPage = 0;
        this.maxPage = this.maxField/ 10;
        this.actions = new ArrayList<>();
        this.actions.add(ReactionEmoji.unicode("❓"));
        this.actions.add(ReactionEmoji.unicode("⬆️"));
        this.actions.add(ReactionEmoji.unicode("⬇️"));
        this.actions.add(ReactionEmoji.unicode("❌"));
        if (sprints.size() > 10) {
            this.maxField = 9;
            this.actions.add(ReactionEmoji.unicode("⬅️"));
            this.actions.add(ReactionEmoji.unicode("➡️"));
        }
        if (isAux) {
            this.selectionCallback = selectionCallback;
            this.removalCallback = removalCallback;
            this.actions.add(ReactionEmoji.unicode("✅"));
        }
        else
            this.selectedSprint = new SprintEmbed(userId, serverId, canal, gateway, repo, sprints.get(0));
        updateEmbed();
    }

    public void sendSecondEmbed() {
        selectedSprint.send();
    }

    private Sprint getSelectedSprint() {
        return sprints.get(selectedField + selectedPage * 10);
    }

    public void inputSelect(String input) {
        log.info("Selecting new element: " + input);
        int lastSelected = (selectedPage + 1) * 10 > sprints.size() ? sprints.size() : (selectedPage + 1) * 10;
        List<Sprint> sublist = sprints.subList(selectedPage * 10, lastSelected);
        Sprint inputSprint = sublist.stream().filter(x -> x.getName().equals(input)).findAny().orElse(null);
        if (inputSprint != null) {
            selectedField = sublist.indexOf(inputSprint);
            if (selectedSprint != null)
                selectedSprint.selectNewSprint(getSelectedSprint());
            updateEmbed();
            
        }
    }

    private void updateEmbed() {
        int lastSelected = (selectedPage + 1) * 10 > sprints.size() ? sprints.size() : (selectedPage + 1) * 10;
        List<Sprint> sublist = sprints.subList(selectedPage * 10, lastSelected);
        String footer = null;
        if (maxPage > 0) {
            Integer total = sprints.size();
            Integer inicio = selectedPage * 10;
            Integer fin = inicio + 10 > total ? total : inicio + 10;
            footer = "Showing sprints " + inicio + "-" + fin + " of " + total;
        }
        emb = EmbedCreateSpec.builder()
            .color(Color.LIGHT_SEA_GREEN)
            .title("Sprint list")
            .build();
        List<Field> fields = sublist.stream().map(x -> sublist.indexOf(x) == selectedField ?
        Field.of("-> " + x.getName() + " <-", x.getState(), false) : 
        Field.of(x.getName(), x.getState(), false)).collect(Collectors.toList());
        emb = emb.withFields(fields);
        if (footer != null)
            emb = emb.withFooter(Footer.of(footer, "https://64.media.tumblr.com/aa65057a2ba418757cee5ae25c07d790/tumblr_pb2ky0qi6A1w6xh18o8_250.png"));
        if (selectionCallback != null && removalCallback == null) //Embed auxiliar para análisis de sprint
            emb = emb.withDescription("Select the sprint you want to analyse");
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
                if (selectedSprint != null)
                    selectedSprint.selectNewSprint(getSelectedSprint());
                updateEmbed();
                
                break;
            
            case "⬇️":
                selectedField = selectedField == maxField ? 0 : selectedField + 1;
                if (selectedSprint != null)
                    selectedSprint.selectNewSprint(getSelectedSprint());
                updateEmbed();
                
                break;

            case "⬅️":
                selectedPage = selectedPage == 0 ? maxPage : selectedPage - 1;
                selectedField = 0;
                if (selectedSprint != null)
                    selectedSprint.selectNewSprint(getSelectedSprint());
                updateEmbed();
                
                break;

            case "➡️":
                selectedPage = selectedPage == maxPage ? 0 : selectedPage + 1;
                selectedField = 0;
                if (selectedSprint != null)
                    selectedSprint.selectNewSprint(getSelectedSprint());
                updateEmbed();
                
                break;

            case "❌":
                if (removalCallback != null) {
                    removalCallback.accept(sprints.get(selectedPage * 10 + selectedField));
                }
                else {
                    end();
                    App.sessions.put(userId + "-" + serverId, "Start");
                    canal.createMessage("Can I help you with something more?").block();
                }
                break;

            case "✅":
                if (removalCallback == null)
                    end();
                selectionCallback.accept(sprints.get(selectedPage * 10 + selectedField));
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
        if (selectedSprint != null)
            selectedSprint.msg.delete().block();
    }
}
