package embed;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import org.kohsuke.github.GHProject;
import org.kohsuke.github.GHProject.ProjectState;
import org.kohsuke.github.GHProject.ProjectStateFilter;
import org.kohsuke.github.GHProjectColumn;
import org.kohsuke.github.GHRepository;

import discord4j.core.GatewayDiscordClient;
import discord4j.core.object.entity.channel.MessageChannel;
import discord4j.core.object.reaction.ReactionEmoji;
import discord4j.core.spec.EmbedCreateFields.Field;
import discord4j.core.spec.EmbedCreateFields.Footer;
import discord4j.core.spec.EmbedCreateSpec;
import discord4j.rest.util.Color;
import managers.App;

public class ProjectList extends EmbedManager{
    private List<GHProject> projects;
    private int selectedPage;
    private int maxPage;
    private ProjectEmbed selectedProject;

    public ProjectList(String userId, String serverId, MessageChannel canal, GatewayDiscordClient gateway,
            GHRepository repo, Boolean readOnly, List<GHProject> projects) {
        super(userId, serverId, canal, gateway, repo, readOnly);
        this.projects = projects;
        GHProject prjtInicial = projects.get(0);
        List<String> columns = new ArrayList<>();
        try {
            columns = prjtInicial.listColumns().toList().stream()
                .map(GHProjectColumn::getName).collect(Collectors.toList());
        } catch (IOException e) {
            log.error(e.getMessage());
        }
        this.selectedProject = new ProjectEmbed(userId, serverId, canal, gateway, repo,
            true, "Project info", prjtInicial.getName(), prjtInicial.getBody(), columns, prjtInicial);
        this.selectedField = 0;
        this.maxField = projects.size() - 1;
        this.selectedPage = 0;
        this.maxPage = this.maxField/ 10;
        this.actions = new ArrayList<>();
        this.actions.add(ReactionEmoji.unicode("❓"));
        this.actions.add(ReactionEmoji.unicode("⬆️"));
        this.actions.add(ReactionEmoji.unicode("⬇️"));
        if (projects.size() > 10) {
            this.maxField = 9;
            this.actions.add(ReactionEmoji.unicode("⬅️"));
            this.actions.add(ReactionEmoji.unicode("➡️"));
        }
        this.actions.add(ReactionEmoji.unicode("❌"));
        this.actions.add(ReactionEmoji.unicode("🔐"));
        this.actions.add(ReactionEmoji.unicode("🔍"));
        this.actions.add(ReactionEmoji.unicode("✏️"));
        updateEmbed();
    }

    public void sendSecondEmbed() {
        selectedProject.send();
    }

    private GHProject getSelectedProject() {
        return projects.get(selectedField + selectedPage * 10);
    }

    public void inputSelect(String input) {
        log.info("Selecting new element: " + input);
        int lastSelected = (selectedPage + 1) * 10 > projects.size() ? projects.size() : (selectedPage + 1) * 10;
        List<GHProject> sublist = projects.subList(selectedPage * 10, lastSelected);
        GHProject inputProject = sublist.stream().filter(x -> x.getName().equals(input)).findAny().orElse(null);
        if (inputProject != null) {
            selectedField = sublist.indexOf(inputProject);
            selectedProject.selectNewProject(getSelectedProject());
            updateEmbed();
            updateMessage();
        }
    }

    private void updateEmbed() {
        int lastSelected = (selectedPage + 1) * 10 > projects.size() ? projects.size() : (selectedPage + 1) * 10;
        List<GHProject> sublist = projects.subList(selectedPage * 10, lastSelected);
        String footer = null;
        if (maxPage > 0) {
            Integer total = projects.size();
            Integer inicio = selectedPage * 10;
            Integer fin = inicio + 10 > total ? total : inicio + 10;
            footer = "Showing projects " + inicio + "-" + fin + " of " + total;
        }
        emb = EmbedCreateSpec.builder()
            .color(Color.LIGHT_SEA_GREEN)
            .title("Project list")
            .build();
        List<Field> fields = sublist.stream().map(x -> sublist.indexOf(x) == selectedField ?
        Field.of("-> " + x.getName() + " <-", "#" + String.valueOf(x.getId()), false) : 
        Field.of(x.getName(), "#" + String.valueOf(x.getId()), false)).collect(Collectors.toList());
        emb = emb.withFields(fields);
        if (footer != null)
            emb = emb.withFooter(Footer.of(footer, "https://64.media.tumblr.com/aa65057a2ba418757cee5ae25c07d790/tumblr_pb2ky0qi6A1w6xh18o8_250.png"));
    }

    @Override
    protected void executeAction(String action) {
        log.info("Selected action: " + action);
        switch (action) {
            case "❓":
                iconHelp();
                break;

            case "⬆️":
                selectedField = selectedField == 0 ? maxField : selectedField - 1;
                selectedProject.selectNewProject(getSelectedProject());
                updateEmbed();
                updateMessage();
                break;
            
            case "⬇️":
                selectedField = selectedField == maxField ? 0 : selectedField + 1;
                selectedProject.selectNewProject(getSelectedProject());
                updateEmbed();
                updateMessage();
                break;

            case "⬅️":
                selectedPage = selectedPage == 0 ? maxPage : selectedPage - 1;
                selectedProject.selectNewProject(getSelectedProject());
                updateEmbed();
                updateMessage();
                break;

            case "➡️":
                selectedPage = selectedPage == maxPage ? 0 : selectedPage + 1;
                selectedProject.selectNewProject(getSelectedProject());
                updateEmbed();
                updateMessage();
                break;

            case "❌":
                end();
                App.sessions.put(userId + "-" + serverId, "Start");
                canal.createMessage("Can I help you with something more?").block();
                break;

            case "🔐":
                GHProject prjt = selectedProject.project;
                try {
                    if (prjt.getState().toString().equals("CLOSED"))
                        prjt.setState(ProjectState.OPEN);
                    else
                        prjt.setState(ProjectState.CLOSED);
                    projects = repo.listProjects(ProjectStateFilter.ALL).toList();
                    selectedProject.selectNewProject(getSelectedProject());
                } catch(IOException ex) {
                    log.error(ex.getMessage());
                }
                
                break;

            case "🔍":
                end();
                GHProject prjct = selectedProject.project;
                KanbanEmbed mng = new KanbanEmbed(userId, serverId,
                    canal, gateway, repo, false, prjct);
                mng.send();
                mng.sendSecondEmbed();
                App.embedDict.put(userId + "-" + serverId, mng);
                App.sessions.put(userId + "-" + serverId, "SelectIssue");
                break;

            case "✏️":
                end();
                GHProject project = selectedProject.project;
                List<String> columns = new ArrayList<>();
                try {
                    columns = project.listColumns().toList().stream()
                        .map(x -> x.getName()).collect(Collectors.toList());
                } catch (IOException e) {
                    log.error(e.getMessage());
                }
                ProjectEmbed manager = new ProjectEmbed(userId, serverId, canal, gateway,
                    repo, false, "Edit project", project.getName(), project.getBody(), columns, project);
                manager.send();
                App.embedDict.put(userId + "-" + serverId, manager);
                App.sessions.put(userId + "-" + serverId, "InputProject");
                break;

            default:
                break;
        }
    }

    @Override
    protected void end() {
        log.debug("Deleting embed");
        this.fluxDisposer.dispose();
        selectedProject.msg.delete().block();
        this.msg.delete().block();
    }
}
