package embed;

import java.util.ArrayList;
import java.util.function.BiPredicate;

import org.kohsuke.github.GHRepository;

import discord4j.core.GatewayDiscordClient;
import discord4j.core.object.entity.channel.MessageChannel;
import discord4j.core.object.reaction.ReactionEmoji;
import discord4j.core.spec.EmbedCreateSpec;
import discord4j.rest.util.Color;
import managers.App;

public class ServerRegistrationEmbed extends EmbedManager{

    private String author;
    private String repoName;
    private BiPredicate<String, String> serverRegistrationCallback;
    private BiPredicate<String, String> checkRepoCallback;

    public ServerRegistrationEmbed(String userId, String serverId, MessageChannel canal,
            GatewayDiscordClient gateway, GHRepository repo, boolean readOnly,
            BiPredicate<String, String> serverRegistrationCallback,
            BiPredicate<String, String> checkRepoCallback) {
        super(userId, serverId, canal, gateway, repo, readOnly);
        this.serverRegistrationCallback = serverRegistrationCallback;
        this.checkRepoCallback = checkRepoCallback;
        this.actions = new ArrayList<>();
        this.actions.add(ReactionEmoji.unicode("❓"));
        this.actions.add(ReactionEmoji.unicode("❌"));
        this.actions.add(ReactionEmoji.unicode("✅"));
        updateEmbed();
    }

    public void addData(String data) {
        log.info("Adding data: " + data);
        this.error = null;
        String semiUrl = data.replace(".git","");
        semiUrl = semiUrl.replace("https://", "");
        semiUrl = semiUrl.replace("github.com/", "");
        String[] splits = semiUrl.split("/");
        if (checkRepoCallback.test(splits[0], splits[1])) {
            this.author = splits[0];
            this.repoName = splits[1];
        } else
            this.error = "No repository was found at " + data;
        updateEmbed();
        updateMessage();
    }

    private void updateEmbed() {
        if (author == null) {
            emb = EmbedCreateSpec.builder()
            .color(Color.RUBY)
            .title("Repository registration")
            .description("No GitHub repository with Dialgit installed was found")
            .build();
        } else {
            emb = EmbedCreateSpec.builder()
            .color(Color.RUBY)
            .title("Repository registration")
            .description("Would you like to register this GitHub repository?")
            .addField("Author", author, false)
            .addField("Repository", repoName, false)
            .build();
        }
        if (error != null)
            emb = emb.withDescription(error);
    }

    @Override
    protected void executeAction(String action) {
        log.info("Selected action: " + action);
        switch (action) {
            case "❓":
                iconHelp();
                break;

            case "❌":
                end();
                App.sessions.remove(userId + "-" + serverId);
                break;

            case "✅":
                if (this.author != null) {
                    Boolean repoValidated = serverRegistrationCallback.test(author, repoName);
                    if (repoValidated) {
                        end();
                        App.sessions.remove(userId + "-" + serverId);
                        canal.createMessage("Server registered successfully!").block();
                    } else {
                        this.error = "This repository does not have Dialgit installed!";
                        updateEmbed();
                        updateMessage();
                    }
                } else {
                    this.error = "No repository was selected!";
                    updateEmbed();
                    updateMessage();
                }
                break;

            default:
                break;
        }
    }
}