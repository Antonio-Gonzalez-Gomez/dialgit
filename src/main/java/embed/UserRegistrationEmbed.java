package embed;

import java.io.IOException;
import java.util.ArrayList;
import java.util.function.Consumer;

import org.kohsuke.github.GHRepository;
import org.kohsuke.github.GHUser;

import discord4j.core.GatewayDiscordClient;
import discord4j.core.object.entity.channel.MessageChannel;
import discord4j.core.object.reaction.ReactionEmoji;
import discord4j.core.spec.EmbedCreateSpec;
import discord4j.rest.util.Color;
import managers.App;

public class UserRegistrationEmbed extends EmbedManager{

    private GHUser user;
    private Consumer<String> userCallback;

    public UserRegistrationEmbed(String userId, String serverId, MessageChannel canal,
            GatewayDiscordClient gateway, GHRepository repo, boolean readOnly, Consumer<String> userCallback) {
        super(userId, serverId, canal, gateway, repo, readOnly);
        this.user = null;
        this.userCallback = userCallback;
        this.actions = new ArrayList<>();
        this.actions.add(ReactionEmoji.unicode("❓"));
        this.actions.add(ReactionEmoji.unicode("❌"));
        this.actions.add(ReactionEmoji.unicode("✅"));
        updateEmbed();
    }

    public void addData(String data) {
        log.info("Adding data: " + data);
        try {
            this.user = repo.listCollaborators().toList().stream().filter(x -> 
                x.getLogin().equals(data)).findAny().orElse(null);
            updateEmbed();
            updateMessage();
        } catch (IOException e) {
            log.error(e.getMessage());
        }
    }

    private void updateEmbed() {
        if (user == null) {
            emb = EmbedCreateSpec.builder()
            .color(Color.RUBY)
            .title("User registration")
            .description("No GitHub user was found inside the repository " + repo.getName())
            .build();
        } else {
            emb = EmbedCreateSpec.builder()
            .color(Color.RUBY)
            .title("User registration")
            .image(user.getAvatarUrl())
            .addField("Name", user.getLogin(), false)
            .description("Would you like to register with this GitHub user?")
            .build();
        }
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
                if (this.user != null) {
                    userCallback.accept(user.getLogin());
                    end();
                    App.sessions.put(userId + "-" + serverId, "Start");
                    canal.createMessage("User registered successfully! Can I help you with something more?").block();
                } else
                    canal.createMessage("No user was selected!").block();
                break;

            default:
                break;
        }
    }
}