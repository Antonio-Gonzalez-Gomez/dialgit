package embed;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BiPredicate;

import discord4j.core.GatewayDiscordClient;
import discord4j.core.object.entity.channel.MessageChannel;
import discord4j.core.object.reaction.ReactionEmoji;
import discord4j.core.spec.EmbedCreateSpec;
import discord4j.rest.util.Color;
import managers.App;
import model.Channel;

public class ChannelRegistrationEmbed extends EmbedManager{

    private String project;
    private String author;
    private String repoName;
    private ChannelAuxiliarProjectList auxProjectList;
    private BiPredicate<String, String> channelRegistrationCallback;
    private BiPredicate<String, String> checkRepoCallback;

    public ChannelRegistrationEmbed(String userId, String serverId, MessageChannel canal,
            GatewayDiscordClient gateway,
            BiPredicate<String, String> channelRegistrationCallback,
            BiPredicate<String, String> checkRepoCallback,
            List<Channel> channels, List<String> groupRepos) {
        super(userId, serverId, canal, gateway, null, false);
        this.project = EMPTY_FIELD;
        this.author = null;
        this.repoName = null;
        this.auxProjectList = new ChannelAuxiliarProjectList(userId, serverId, canal, gateway, null,
            channels, groupRepos, this::setProject);
        this.channelRegistrationCallback = channelRegistrationCallback;
        this.checkRepoCallback = checkRepoCallback;
        this.selectedField = 0;
        this.maxField = 1;
        this.actions = new ArrayList<>();
        this.actions.add(ReactionEmoji.unicode("❓"));
        this.actions.add(ReactionEmoji.unicode("⬆️"));
        this.actions.add(ReactionEmoji.unicode("⬇️"));
        this.actions.add(ReactionEmoji.unicode("❌"));
        this.actions.add(ReactionEmoji.unicode("✅"));
        updateEmbed();
    }

    public void setProject(String project) {
        this.project = project;
        updateEmbed();
    }

    public void sendSecondEmbed() {
        auxProjectList.send();
    }

    public void addData(String data) {
        log.info("Adding data: " + data);
        error = null;
        if (selectedField == 0) {
            if (auxProjectList.repoExists(data))
                project = data;
            else
                error = "The GitLab group doesn't contains a project with name \"" + data + "\"";
        }
        else {
            String semiUrl = data.replace(".git","");
            semiUrl = semiUrl.replace("https://", "");
            semiUrl = semiUrl.replace("github.com/", "");
            String[] splits = semiUrl.split("/");
            if (splits.length >= 2 && checkRepoCallback.test(splits[0], splits[1])) {
                author = splits[0];
                repoName = splits[1];
            } else
                error = "No repository was found at " + data;
        }
        updateEmbed();
        
    }

    private void updateEmbed() {
        String[] fields = selectedField == 0 ? new String[]{"-> Project <-", "Author", "Repository"} 
            : new String[]{"Project", "-> Author <-", "-> Repository <-"};
        String description = author == null ? "No GitHub repository with Dialgit installed was found"
            : "Would you like to register this GitHub repository?";
        emb = EmbedCreateSpec.builder()
            .color(Color.RUBY)
            .title("Repository registration")
            .description(description)
            .addField(fields[0], project, false)
            .addField(fields[1], author != null ? author : EMPTY_FIELD, false)
            .addField(fields[2], repoName != null ? repoName : EMPTY_FIELD, false)
            .build();
        if (error != null)
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
                if (author != null) {
                    Boolean repoValidated = channelRegistrationCallback.test(author, repoName);
                    if (repoValidated) {
                        end();
                    } else {
                        error = "This repository does not have Dialgit installed!";
                        updateEmbed();
                        
                    }
                } else {
                    error = "No repository was selected!";
                    updateEmbed();
                    
                }
                break;

            default:
                break;
        }
    }

    @Override
    protected void end() {
        log.debug("Deleting embed");
        if (!readOnly) {
            fluxDisposer.dispose();
        }
        msg.delete().block();
        auxProjectList.fluxDisposer.dispose();
        auxProjectList.msg.delete().block();
        App.sessions.remove(userId + "-" + serverId);
        canal.createMessage("Channel registered successfully! Type \"dg!start\" again to continue").block();
    }
}