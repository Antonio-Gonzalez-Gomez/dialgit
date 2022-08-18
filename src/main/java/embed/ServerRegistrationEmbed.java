package embed;

import java.util.ArrayList;
import java.util.function.Predicate;

import org.kohsuke.github.GHRepository;

import discord4j.core.GatewayDiscordClient;
import discord4j.core.object.entity.channel.MessageChannel;
import discord4j.core.object.reaction.ReactionEmoji;
import discord4j.core.spec.EmbedCreateSpec;
import discord4j.rest.util.Color;
import managers.App;
import managers.GitlabManager;
import pojo.Group;

public class ServerRegistrationEmbed extends EmbedManager{
    
    private String path;
    private Predicate<String> checkGroupPredicate;
    private Predicate<String> groupRegistrationCallback;

    public ServerRegistrationEmbed(String userId, String serverId, MessageChannel canal, GatewayDiscordClient gateway,
            GHRepository repo, Boolean readOnly, Predicate<String> checkGroupPredicate,
            Predicate<String> groupRegistrationCallback) {
        super(userId, serverId, canal, gateway, repo, readOnly);
        this.path = null;
        this.checkGroupPredicate = checkGroupPredicate;
        this.groupRegistrationCallback = groupRegistrationCallback;
        this.actions = new ArrayList<>();
        this.actions.add(ReactionEmoji.unicode("❓"));
        this.actions.add(ReactionEmoji.unicode("❌"));
        this.actions.add(ReactionEmoji.unicode("✅"));
        updateEmbed();
    }
    
    public void addData(String data) {
        log.info("Adding group data: " + data);
        error = null;
        String semiUrl = data.replace(".git","");
        semiUrl = semiUrl.replace("https://", "");
        semiUrl = semiUrl.replace("gitlab.com/", "");
        String[] splits = semiUrl.split("/");
        if (checkGroupPredicate.test(splits[0])) {
            path = splits[0];
        } else
            error = "No GitLab group was found at " + data;
        updateEmbed();
        
    }

    private void updateEmbed() {
        if (path == null) {
            emb = EmbedCreateSpec.builder()
            .color(Color.RUBY)
            .title("Group registration")
            .description("No GitLab group was found")
            .build();
        } else {
            Group group = GitlabManager.getGroup(path);
            String strProjects = listParse(group.getProjects(), 99);
            String strMembers = listParse(group.getMembers(), 99);
            emb = EmbedCreateSpec.builder()
            .color(Color.RUBY)
            .title("Group registration")
            .description("Would you like to register this GitLab group?")
            .addField("Name", group.getName(), false)
            .addField("URL", group.getUrl(), false)
            .addField("Projects", strProjects, false)
            .addField("Members", strMembers, false)
            .build();
        }
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

            case "❌":
                end();
                App.sessions.remove(userId + "-" + serverId);
                break;

            case "✅":
                if (path != null) {
                    Boolean groupValidated = groupRegistrationCallback.test(path);
                    if (groupValidated) {
                        end();
                    } else {
                        error = "This group is not accesible!";
                        updateEmbed();
                    }
                } else {
                    error = "No group was selected!";
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
        App.sessions.remove(userId + "-" + serverId);
        canal.createMessage("Server registered successfully! Type \"dg!start\" again to continue").block();
    }
}
