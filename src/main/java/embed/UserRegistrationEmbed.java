package embed;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
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
    private List<String> groupMembers;
    private Consumer<String> userCallback;

    public UserRegistrationEmbed(String userId, String serverId, MessageChannel canal, GatewayDiscordClient gateway,
            GHRepository repo, boolean readOnly, List<String> groupMembers, Consumer<String> userCallback) {
        super(userId, serverId, canal, gateway, repo, readOnly);
        this.user = null;
        this.error = EMPTY_FIELD;
        this.groupMembers = groupMembers;
        this.userCallback = userCallback;
        this.actions = new ArrayList<>();
        this.actions.add(ReactionEmoji.unicode("❓"));
        this.actions.add(ReactionEmoji.unicode("❌"));
        this.actions.add(ReactionEmoji.unicode("✅"));
        updateEmbed();
    }

    public void addData(String data) {
        log.info("Adding data: " + data);
        error = null;
        try {
            user = repo.listCollaborators().toList().stream().filter(x -> 
                x.getLogin().equals(data)).findAny().orElse(null);
            if (user == null)
                error = "No GitHub user with name " + data + " was found inside the repository " + repo.getName();
            else if (!groupMembers.contains(user.getLogin()))
                error = "This user is not a part of the server GitLab group";
            updateEmbed();
            
        } catch (IOException e) {
            log.error(e.getMessage());
        }
    }

    private void updateEmbed() {
        if (error != null) {
            emb = EmbedCreateSpec.builder()
            .color(Color.RUBY)
            .title("User registration")
            .build();
            emb = error.equals(EMPTY_FIELD) ? emb : emb.withDescription(error);
        } else {
            emb = EmbedCreateSpec.builder()
            .color(Color.RUBY)
            .title("User registration")
            .image(user.getAvatarUrl())
            .addField("Name", user.getLogin(), false)
            .description("Would you like to register with this GitHub user?")
            .build();
        }
        updateMessage();
    }

    @Override
    protected void executeAction(String action) {
        switch (action) {
            case "❓":
                iconHelp();
                break;

            case "❌":
                end();
                App.sessions.remove(userId + "-" + serverId);
                break;

            case "✅":
                if (user != null) {
                    userCallback.accept(user.getLogin());
                    canal.createMessage("User registered successfully!").block();
                    end();
                } else
                    canal.createMessage("No user was selected!").block();
                break;

            default:
                break;
        }
    }
}