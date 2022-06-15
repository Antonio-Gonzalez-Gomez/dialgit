package embed;

import java.text.Format;
import java.text.SimpleDateFormat;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.concurrent.TimeoutException;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.kohsuke.github.GHRepository;

import discord4j.core.GatewayDiscordClient;
import discord4j.core.event.domain.message.ReactionAddEvent;
import discord4j.core.object.entity.Message;
import discord4j.core.object.entity.channel.MessageChannel;
import discord4j.core.object.reaction.ReactionEmoji;
import discord4j.core.spec.EmbedCreateFields.Field;
import discord4j.core.spec.EmbedCreateSpec;
import discord4j.core.spec.MessageEditSpec;
import discord4j.rest.util.Color;
import reactor.core.Disposable;
import reactor.core.Exceptions;
import reactor.core.publisher.Flux;
import managers.App;

public class EmbedManager {
    public static final String EMPTY_FIELD = "\u200B";
    protected static final Logger log = LogManager.getLogger();
    protected String userId;
    protected String serverId;
    protected MessageChannel canal;
    protected GatewayDiscordClient gateway;
    protected GHRepository repo;
    protected String error;

    protected EmbedCreateSpec emb;
    protected Message msg;
    protected int selectedField;
    protected int maxField;
    protected boolean readOnly;
    protected List<ReactionEmoji> actions;
    protected Disposable fluxDisposer;

    public EmbedManager(String userId, String serverId, MessageChannel canal,
        GatewayDiscordClient gateway, GHRepository repo, Boolean readOnly) {
        this.userId = userId;
        this.serverId = serverId;
        this.canal = canal;
        this.gateway = gateway;
        this.repo = repo;
        this.readOnly = readOnly;
        this.error = null;
        if (!readOnly)
            this.selectedField = 0;
    }

    public int getSelectedField() {
        return selectedField;
    }

    protected void executeAction(String action) {
        //Método abstracto, debe ser declarado para definir el método send()
    }

    public void send() {
        log.debug("Sending embed");
        this.msg = canal.createMessage(emb).block();
        if (!this.readOnly) {
            actions.forEach(x -> msg.addReaction(x).block());
            //Flujo de reacciones, si se queda inactivo por más de 3 minutos
            //lanza una TimeoutException y finaliza el flujo
            Flux<ReactionAddEvent> flux = gateway.getEventDispatcher().on(ReactionAddEvent.class)
                .timeout(Duration.ofMinutes(3)).doFinally(x -> endFlux());
            
            this.fluxDisposer = flux.subscribe(ev -> {
                if (ev.getMessage().block().equals(msg) && 
                ev.getUser().block().getId().asString().equals(userId)) {
                    ReactionEmoji emj = ev.getEmoji();
                    if (actions.contains(emj)) {
                        msg.removeReaction(emj, ev.getUserId()).block();
                        executeAction(emj.asUnicodeEmoji().get().getRaw());
                    }
                }
            },
            er -> {
                if (Exceptions.unwrap(er) instanceof TimeoutException) {
                    log.debug("Embed reactions timed out: " + er.getMessage());
                } else {
                    log.warn(er.getMessage());
                }
            });
        }
    }

    private void endFlux() {
        App.sessions.remove(userId + "-" + serverId);
        App.embedDict.remove(userId + "-" + serverId);
    }

    protected void updateMessage() {
        msg.edit(MessageEditSpec.builder().addEmbed(emb).build()).block();
    }

    protected void end() {
        log.debug("Deleting embed");
        if (!this.readOnly) {
            this.fluxDisposer.dispose();
        }
        msg.delete().block();
        App.sessions.put(userId + "-" + serverId, "Start");
        canal.createMessage("Can I help you with something more?").block();
    }

    protected void iconHelp() {
        log.info("Sending icon help embed");
        EmbedCreateSpec helpEmbed = EmbedCreateSpec.builder()
            .color(Color.PINK)
            .title("Icons legend")
            .build();
        List<Field> fields = new ArrayList<>();
        for (ReactionEmoji emj : this.actions) {
            String unicode = emj.asUnicodeEmoji().orElse(ReactionEmoji.unicode("🤓")).getRaw();
            switch (unicode) {
                case "⬆️":
                    fields.add(Field.of("⬆️⬇️", "Navigate through the embed fields.", false));
                    break;

                case "❌":
                    fields.add(Field.of("❌", "Cancel the current operation.", false));
                    break;

                case "✅":
                    fields.add(Field.of("✅", "Confirm the current operation.", false));
                    break;

                case "⬅️":
                    fields.add(Field.of("⬅️➡️", "Navigate through the embed pages.", false));
                    break;

                case "🔐":
                    fields.add(Field.of("🔐", "Close or open the selected element.", false));
                    break;

                case "✏️":
                    fields.add(Field.of("✏️", "Edit the selected element.", false));
                    break;

                case "👤":
                    fields.add(Field.of("👤", "Toggle visibility for issues in which the user is not assigned.", false));
                    break;

                case "◀️":
                    fields.add(Field.of("◀️▶️", "Navigate through the project columns.", false));
                    break;

                case "💠":
                    fields.add(Field.of("💠", "Store the selected issue. Press this button again in a different column to place the issue there.", false));
                    break;
                
                case "🔍":
                    fields.add(Field.of("🔍", "Open the kanban view for the selected project.", false));
                    break;

                case "➖":
                    fields.add(Field.of("➖", "Remove the last column.", false));
                    break;

                case "➕":
                    fields.add(Field.of("➕", "Add a new column.", false));
                    break;
                default:
                    break;
            }
        }
        helpEmbed = helpEmbed.withFields(fields);
        canal.createMessage(helpEmbed).block();
    }

    public static EmbedCreateSpec tutorialEmbed() { 
        log.info("Sending tutorial help embed");
        return EmbedCreateSpec.builder()
            .color(Color.PINK)
            .title("Install Dialgit here")
            .url("https://github.com/apps/dialgit")
            .addField("Dialgit Tutorial", "DialGit is a work group management bot that relies on natural " + 
                "speech processing to perform its duties. It will link your Discord server to your Github " + 
                "repository to translate your planning into Github projects, issues and milestones.", false)

            .addField("How do I link it to my Github repository?", "You must first install Dialgit in your " + 
                "Github repository by clicking on the link above. Then, simply type \"dg!start\" to start a " + 
                "conversation with the bot. It will request your repository url and your Github username to " + 
                "link it, and after that you are good to go!", false)

            .addField("How does it work?", "After typing \"dg!start\", you can request to create a project " + 
                "or list them, to create a milestone or list them, to create an issue and to show this help " + 
                "message. For example, if you wanna see your projects, you can type \"show me the projects\" " + 
                "or \"I need to see the Github projects\" and DialGit will know what to do. ", false)
                
            .addField("How do I interact with the bot?", "DialGit embeds use reaction emojis as buttons, so " + 
                "you can simply click on them. If you have doubts, click the :question: button to see all " + 
                "buttons functionality. If you want to add information (like creating an issue), you can " + 
                "directly type the data field by field. You can also type the name of an element in a list " + 
                "of elements to select it.", false)
            .build();
    }

    protected static String listParse(List<String> data, int charLimit) {
        if (data.isEmpty())
            return EMPTY_FIELD;
        String result = "";
        for (String e : data) {
            if (result.length() + e.length() + 3 > charLimit) {
                result = result.substring(0, result.length() - 2);
                return result + "...";
            }
            result += e + ", ";
        }
        return result.substring(0, result.length() - 2);
    }

    protected static String dateParse(Date date) {
        Format formatter = new SimpleDateFormat("dd-MM-yyyy");
        return formatter.format(date);
    }
}
