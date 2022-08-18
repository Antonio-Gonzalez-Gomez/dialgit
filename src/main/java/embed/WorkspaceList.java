package embed;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import org.kohsuke.github.GHRepository;
import org.kohsuke.github.GHUser;

import discord4j.core.GatewayDiscordClient;
import discord4j.core.object.entity.channel.MessageChannel;
import discord4j.core.object.reaction.ReactionEmoji;
import discord4j.core.spec.EmbedCreateFields.Field;
import discord4j.core.spec.EmbedCreateFields.Footer;
import discord4j.core.spec.EmbedCreateSpec;
import discord4j.rest.util.Color;

import managers.App;
import pojo.Workspace;

public class WorkspaceList extends EmbedManager{

    private List<Workspace> workspaces;
    private int selectedPage;
    private int maxPage;
    private WorkspaceEmbed selectedWorkspace;
    private Consumer<Workspace> callback;
    private GHUser user;

    public WorkspaceList(String userId, String serverId, MessageChannel canal, GatewayDiscordClient gateway,
            GHRepository repo, Boolean isAux, List<Workspace> workspaces, GHUser user, Consumer<Workspace> callback) {
        super(userId, serverId, canal, gateway, repo, false);
        this.workspaces = workspaces;
        this.callback = callback;
        this.actions = new ArrayList<>();
        this.actions.add(ReactionEmoji.unicode("❓"));
        this.actions.add(ReactionEmoji.unicode("⬆️"));
        this.actions.add(ReactionEmoji.unicode("⬇️"));
        this.maxField = workspaces.size() - 1;
        this.selectedPage = 0;
        this.maxPage = this.maxField/ 10;
        if (workspaces.size() > 10) {
            this.maxField = 9;
            this.actions.add(ReactionEmoji.unicode("⬅️"));
            this.actions.add(ReactionEmoji.unicode("➡️"));
        }
        this.actions.add(ReactionEmoji.unicode("❌"));
        if (!isAux) {
            Workspace wrkpInicial = workspaces.get(0);
            this.selectedWorkspace = new WorkspaceEmbed(userId, serverId, canal, gateway, repo,
                true, "Workspace info", wrkpInicial.getName(), wrkpInicial.getDescription(), null, wrkpInicial);    
            this.user = user;
            this.actions.add(ReactionEmoji.unicode("🔍"));
            this.actions.add(ReactionEmoji.unicode("✏️"));
        }
        else
            this.actions.add(ReactionEmoji.unicode("✅"));
        updateEmbed();
    }

    public String getWorkspaceName(String id) {
        Workspace res = workspaces.stream().filter(x -> 
            x.getId().equals(id)).findAny().orElse(null);
        if (res != null)
            return res.getId();
        else
            return EMPTY_FIELD;
    }

    public void sendSecondEmbed() {
        selectedWorkspace.send();
    }

    private Workspace getSelectedWorkspace() {
        return workspaces.get(selectedField + selectedPage * 10);
    }

    public void inputSelect(String input) {
        log.info("Selecting new element: " + input);
        int lastSelected = (selectedPage + 1) * 10 > workspaces.size() ? workspaces.size() : (selectedPage + 1) * 10;
        List<Workspace> sublist = workspaces.subList(selectedPage * 10, lastSelected);
        Workspace inputWorkspace = sublist.stream().filter(x -> x.getName().equals(input)).findAny().orElse(null);
        if (inputWorkspace != null) {
            selectedField = sublist.indexOf(inputWorkspace);
            if (selectedWorkspace != null)
                selectedWorkspace.selectNewWorkspace(getSelectedWorkspace());
            updateEmbed();
        }
    }

    private void updateEmbed() {
        int lastSelected = (selectedPage + 1) * 10 > workspaces.size() ? workspaces.size() : (selectedPage + 1) * 10;
        List<Workspace> sublist = workspaces.subList(selectedPage * 10, lastSelected);
        String footer = null;
        if (maxPage > 0) {
            Integer total = workspaces.size();
            Integer inicio = selectedPage * 10;
            Integer fin = inicio + 10 > total ? total : inicio + 10;
            footer = "Showing workspaces " + inicio + "-" + fin + " of " + total;
        }
        emb = EmbedCreateSpec.builder()
            .color(Color.LIGHT_SEA_GREEN)
            .title("Workspace list")
            .build();
        List<Field> fields = sublist.stream().map(x -> sublist.indexOf(x) == selectedField ?
        Field.of("-> " + x.getName() + " <-", x.getDescriptionOrEmpty(), false) : 
        Field.of(x.getName(), x.getDescriptionOrEmpty(), false)).collect(Collectors.toList());
        emb = emb.withFields(fields);
        if (footer != null)
            emb = emb.withFooter(Footer.of(footer, "https://64.media.tumblr.com/aa65057a2ba418757cee5ae25c07d790/tumblr_pb2ky0qi6A1w6xh18o8_250.png"));
        if (selectedWorkspace == null)
            emb = emb.withDescription("Select the workspace which sprint configuration you want to modify");
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
                if (selectedWorkspace != null)
                    selectedWorkspace.selectNewWorkspace(getSelectedWorkspace());
                updateEmbed();
                
                break;
            
            case "⬇️":
                selectedField = selectedField == maxField ? 0 : selectedField + 1;
                if (selectedWorkspace != null)
                    selectedWorkspace.selectNewWorkspace(getSelectedWorkspace());
                updateEmbed();
                
                break;

            case "⬅️":
                selectedPage = selectedPage == 0 ? maxPage : selectedPage - 1;
                selectedField = 0;
                if (selectedWorkspace != null)
                    selectedWorkspace.selectNewWorkspace(getSelectedWorkspace());
                updateEmbed();
                break;

            case "➡️":
                selectedPage = selectedPage == maxPage ? 0 : selectedPage + 1;
                selectedField = 0;
                if (selectedWorkspace != null)
                    selectedWorkspace.selectNewWorkspace(getSelectedWorkspace());
                updateEmbed();
                
                break;

            case "❌":
                end();
                App.sessions.put(userId + "-" + serverId, "Start");
                canal.createMessage("Can I help you with something more?").block();
                break;

            case "🔍":
                end();
                KanbanEmbed mng = new KanbanEmbed(userId, serverId, canal, gateway,
                    repo, selectedWorkspace.workspace.getId(), user);
                mng.send();
                mng.sendSecondEmbed();
                App.embedDict.put(userId + "-" + serverId, mng);
                App.sessions.put(userId + "-" + serverId, "SelectIssue");
                break;

            case "✅":
                end();
                callback.accept(workspaces.get(selectedPage * 10 + selectedField));
                break;

            case "✏️":
                end();
                Workspace workspace = selectedWorkspace.workspace;
                List<String> pipelines = workspace.getPipelinesNames();
                WorkspaceEmbed manager = new WorkspaceEmbed(userId, serverId, canal, gateway,
                    repo, false, "Edit workspace", workspace.getName(), workspace.getDescription(),
                    pipelines, workspace);
                manager.send();
                App.embedDict.put(userId + "-" + serverId, manager);
                App.sessions.put(userId + "-" + serverId, "InputWorkspace");
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
        if (selectedWorkspace != null) {
            selectedWorkspace.msg.delete().block();
        }
    }
}
