package embed;

import java.util.ArrayList;
import java.util.List;
import java.util.SortedMap;
import java.util.Map.Entry;
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
import managers.ZenhubManager;

public class EpicList extends EmbedManager{

    private SortedMap<String, String> epics;
    private String selectedEpic;
    private Consumer<String> selectionCallback;
    private Consumer<String> removalCallback;
    private int selectedPage;
    private int maxPage;

    public EpicList(String userId, String serverId, MessageChannel canal, GatewayDiscordClient gateway,
            GHRepository repo, Consumer<String> selectionCallback, Consumer<String> removalCallback) {
        super(userId, serverId, canal, gateway, repo, false);
        this.epics = ZenhubManager.getEpicMap(repo);
        this.selectionCallback = selectionCallback;
        this.removalCallback = removalCallback;
        this.selectedField = 0;
        this.maxField = epics.size() - 1;
        this.selectedPage = 0;
        this.maxPage = this.maxField/ 10;
        this.actions = new ArrayList<>();
        this.actions.add(ReactionEmoji.unicode("❓"));
        this.actions.add(ReactionEmoji.unicode("⬆️"));
        this.actions.add(ReactionEmoji.unicode("⬇️"));
        if (epics.size() > 10) {
            this.maxField = 9;
            this.actions.add(ReactionEmoji.unicode("⬅️"));
            this.actions.add(ReactionEmoji.unicode("➡️"));
        }
        this.actions.add(ReactionEmoji.unicode("❌"));
        this.actions.add(ReactionEmoji.unicode("✅"));
        updateEmbed();
    }

    public String getEpicName(String id) {
        return epics.get(id);
    }

    public String getEpicId(String name) {
        return epics.entrySet().stream().filter(x -> x.getValue().equals(name))
            .map(Entry::getValue).findFirst().orElse(null);
    }

    public void inputSelect(String input) {
        log.info("Selecting new element: " + input);
        int lastSelected = (selectedPage + 1) * 10 > epics.size() ? epics.size() : (selectedPage + 1) * 10;
        List<Entry<String, String>> sublist = new ArrayList<>(epics.entrySet()).subList(selectedPage * 10, lastSelected);
        Entry<String, String> inputEntry = sublist.stream().filter(x -> x.getValue().equals(input)).findAny().orElse(null);
        if (inputEntry != null) {
            selectedField = sublist.indexOf(inputEntry);
            updateEmbed();
        }
    }

    private void updateEmbed() {
        List<String> keys = new ArrayList<>(epics.keySet());
        selectedEpic = keys.get(selectedPage * 10 + selectedField);
        int lastSelected = (selectedPage + 1) * 10 > keys.size() ? keys.size() : (selectedPage + 1) * 10;
        List<String> sublist = keys.subList(selectedPage * 10, lastSelected);
        String footer = null;
        if (maxPage > 0) {
            Integer total = keys.size();
            Integer inicio = selectedPage * 10;
            Integer fin = inicio + 10 > total ? total : inicio + 10;
            footer = "Showing epics " + inicio + "-" + fin + " of " + total;
        }
        emb = EmbedCreateSpec.builder()
            .color(Color.LIGHT_SEA_GREEN)
            .title("Epic issues")
            .build();
        List<Field> fields = sublist.stream().map(x -> keys.indexOf(x) == selectedField ?
        Field.of("-> " + epics.get(x) + " <-", x, false) : 
        Field.of(epics.get(x), x, false)).collect(Collectors.toList());
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
                selectedField = selectedField == 0 ? maxField : selectedField - 1;
                updateEmbed();
                break;
            
            case "⬇️":
                selectedField = selectedField == maxField ? 0 : selectedField + 1;
                updateEmbed();
                break;

            case "⬅️":
                selectedPage = selectedPage == 0 ? maxPage : selectedPage - 1;
                updateEmbed();
                break;

            case "➡️":
                selectedPage = selectedPage == maxPage ? 0 : selectedPage + 1;
                updateEmbed();
                break;

            case "❌":
                removalCallback.accept(selectedEpic);
                break;

            case "✅":
                selectionCallback.accept(selectedEpic);
                break;

            default:
                break;
        }
    }
}
