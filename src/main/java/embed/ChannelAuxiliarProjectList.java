package embed;

import java.util.ArrayList;
import java.util.List;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import org.kohsuke.github.GHRepository;

import discord4j.common.util.Snowflake;
import discord4j.core.GatewayDiscordClient;
import discord4j.core.object.entity.channel.MessageChannel;
import discord4j.core.object.reaction.ReactionEmoji;
import discord4j.core.spec.EmbedCreateFields.Field;
import discord4j.core.spec.EmbedCreateFields.Footer;
import discord4j.core.spec.EmbedCreateSpec;
import discord4j.rest.util.Color;
import model.Channel;

public class ChannelAuxiliarProjectList extends EmbedManager{
    
    private SortedMap<String, String> projects;
    private String selectedProject;
    private Consumer<String> channelCallback;
    private int selectedPage;
    private int maxPage;

    public ChannelAuxiliarProjectList(String userId, String serverId, MessageChannel canal, GatewayDiscordClient gateway,
            GHRepository repo, List<Channel> channels, List<String> groupRepos, Consumer<String> channelCallback) {
        super(userId, serverId, canal, gateway, repo, false);
        this.projects = new TreeMap<>();
        for (String name : groupRepos) {
            List<String> connectedChannels = channels.stream().filter(x -> x.getUrl().equals(name))
                .map(x -> gateway.getGuildById(Snowflake.of(serverId)).block().getChannelById(
                    Snowflake.of(x.getId())).block().getName()).collect(Collectors.toList());
            projects.put(name, listParse(connectedChannels, 99));
        }
        this.selectedProject = projects.firstKey();
        this.channelCallback = channelCallback;
        this.selectedField = 0;
        this.maxField = projects.size() - 1;
        this.selectedPage = 0;
        this.maxPage = this.maxField/ 10;
        this.actions = new ArrayList<>();
        this.actions.add(ReactionEmoji.unicode("❓"));
        this.actions.add(ReactionEmoji.unicode("⬆️"));
        this.actions.add(ReactionEmoji.unicode("⬇️"));
        this.actions.add(ReactionEmoji.unicode("✅"));
        if (projects.size() > 10) {
            this.maxField = 9;
            this.actions.add(ReactionEmoji.unicode("⬅️"));
            this.actions.add(ReactionEmoji.unicode("➡️"));
        }
        updateEmbed();
    }

    public boolean repoExists(String repoName) {
        return projects.keySet().contains(repoName);
    }

    private void updateEmbed() {
        List<String> keys = new ArrayList<>(projects.keySet());
        selectedProject = keys.get(selectedPage * 10 + selectedField);
        int lastSelected = (selectedPage + 1) * 10 > keys.size() ? keys.size() : (selectedPage + 1) * 10;
        List<String> sublist = keys.subList(selectedPage * 10, lastSelected);
        String footer = null;
        if (maxPage > 0) {
            Integer total = keys.size();
            Integer inicio = selectedPage * 10;
            Integer fin = inicio + 10 > total ? total : inicio + 10;
            footer = "Showing registered channels in projects " + inicio + "-" + fin + " of " + total;
        }
        emb = EmbedCreateSpec.builder()
            .color(Color.LIGHT_SEA_GREEN)
            .title("Available projects")
            .build();
        List<Field> fields = sublist.stream().map(x -> keys.indexOf(x) == selectedField ?
        Field.of("-> " + x + " <-", projects.get(x), false) : 
        Field.of(x, projects.get(x), false)).collect(Collectors.toList());
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

            case "✅":
                channelCallback.accept(selectedProject);
                break;

            default:
                break;
        }
    }
}
