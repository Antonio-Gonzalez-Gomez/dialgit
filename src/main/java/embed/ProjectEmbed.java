package embed;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import org.kohsuke.github.GHIssueState;
import org.kohsuke.github.GHProject;
import org.kohsuke.github.GHProjectColumn;
import org.kohsuke.github.GHRepository;

import discord4j.core.GatewayDiscordClient;
import discord4j.core.object.entity.channel.MessageChannel;
import discord4j.core.object.reaction.ReactionEmoji;
import discord4j.core.spec.EmbedCreateFields.Field;
import discord4j.core.spec.EmbedCreateSpec;
import discord4j.rest.util.Color;

public class ProjectEmbed extends EmbedManager{
    
    private String title;
    private String name;
    private String body;
    private List<String> columns;
    protected GHProject project;

    public ProjectEmbed(String userId, String serverId, MessageChannel canal,
            GatewayDiscordClient gateway, GHRepository repo, boolean readOnly,
            String title, String name, String body, List<String> columns, GHProject project) {
        super(userId, serverId, canal, gateway, repo, readOnly);
        this.title = title;
        this.name = name;
        this.body = body;
        this.columns = new ArrayList<>(columns);
        this.actions = new ArrayList<>();
        if (!readOnly) {
            this.maxField = 1 + columns.size();
            this.actions.add(ReactionEmoji.unicode("❓"));
            this.actions.add(ReactionEmoji.unicode("⬆️"));
            this.actions.add(ReactionEmoji.unicode("⬇️"));
            this.actions.add(ReactionEmoji.unicode("➖"));
            this.actions.add(ReactionEmoji.unicode("➕"));
            this.actions.add(ReactionEmoji.unicode("❌"));
            this.actions.add(ReactionEmoji.unicode("✅"));
        }
        this.project = project;
        updateEmbed();
    }

    public void addData(String data) {
        log.info("Adding data: " + data);
        int length = data.length();
        this.error = null;
        switch (selectedField) {
            case 0:
                if (length <= 140)
                    name = data;
                else
                    this.error = "Project name can't be longer than 140 characters";
                break;

            case 1:
                if (length <= 1024)
                    body = data;
                else
                    this.error = "Project body can't be longer than 1024 characters";
                break;

            default:
                if (length > 140)
                    this.error = "Column names can't be longer than 140 characters";
                else if(columns.contains(data))
                    this.error = "Column names must be different";
                else
                    columns.set(selectedField - 2, data);
                break;
        }
        if (error == null)
            selectedField = selectedField == maxField ? selectedField : selectedField + 1;
        updateEmbed();
        updateMessage();
    }

    public void selectNewProject(GHProject project) {
        log.info("Selecting project: " + project.getName());
        this.project = project;
        this.name = project.getName();
        this.body = project.getBody();
        try {
            this.columns = project.listColumns().toList().stream()
                .map(GHProjectColumn::getName).collect(Collectors.toList());
        } catch (IOException ex) {
            log.error(ex.getMessage());
        }
        
        updateEmbed();
        if (this.msg != null)
            updateMessage();
    }

    private void updateEmbed() {
        List<Field> fields = new ArrayList<>();
        fields.add(Field.of("Name", name == null || name.equals("") ? EMPTY_FIELD : name, false));
        fields.add(Field.of("Body", body == null || body.equals("") ? EMPTY_FIELD : body, false));
        Color col = Color.GREEN;
        String desc = EMPTY_FIELD;
        if (!readOnly) {
            col = Color.RUST;
            Field selected = fields.get(selectedField);
            fields.addAll(columns.stream().map(x -> 
                Field.of("Column " + String.valueOf(columns.indexOf(x) + 1), x, false)).collect(Collectors.toList()));
            fields.set(selectedField, Field.of("->" + selected.name() + " <-", selected.value(), false));
        }
        else if(project != null) {
            String strColumns = EMPTY_FIELD;
            try {
                List<GHProjectColumn> clmList = project.listColumns().toList();
                List<String> columnStrings = clmList.stream()
                    .map(GHProjectColumn::getName).collect(Collectors.toList());
                strColumns = listParse(columnStrings, 99);
                if (this.error == null) {
                    long open = clmList.stream().map(x -> 
                        {
                            try {
                                return x.listCards().toList().stream()
                                    .filter(y -> {
                                        try {
                                            return y.getContent().getState().equals(GHIssueState.OPEN);
                                        } catch (IOException e) {
                                            log.error(e.getMessage());
                                            return false;
                                        }
                                    }).count();
                            } catch (IOException e) {
                                log.error(e.getMessage());
                                return 0l;
                            }
                        }).collect(Collectors.summingLong(Long::longValue));
                    long closed = clmList.stream().map(x -> 
                        {
                            try {
                                return x.listCards().toList().stream()
                                    .filter(y -> {
                                        try {
                                            return y.getContent().getState().equals(GHIssueState.CLOSED);
                                        } catch (IOException e) {
                                            log.error(e.getMessage());
                                            return false;
                                        }
                                    }).count();
                            } catch (IOException e) {
                                log.error(e.getMessage());
                                return 0l;
                            }
                        }).collect(Collectors.summingLong(Long::longValue));
                    desc = "Contains " + String.valueOf(open) + " open issues and "
                        + String.valueOf(closed) + " closed issues.";
                } else
                    desc = this.error;
            } catch (IOException e) {
                log.error(e.getMessage());
            }
            fields.add(Field.of("Columns", strColumns, false));
            if(project.getState().toString().equals("CLOSED"))
                col = Color.DEEP_LILAC;
        }
        emb = EmbedCreateSpec.builder()
            .color(col)
            .description(desc)
            .title(title)
            .build();
        emb = emb.withFields(fields);
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
                updateEmbed();
                updateMessage();
                break;
            
            case "⬇️":
                selectedField = selectedField == maxField ? 0 : selectedField + 1;
                updateEmbed();
                updateMessage();
                break;

            case "➖":
                if (maxField > 1) {
                    if (selectedField == maxField)
                        selectedField--;
                    columns.remove(maxField - 2);
                    maxField--;
                    updateEmbed();
                    updateMessage();
                }
                break;

            case "➕":
                maxField++;
                columns.add(EMPTY_FIELD);
                updateEmbed();
                updateMessage();
                break;

            case "❌":
                end();
                break;

            case "✅":
                try {
                    if (this.project == null) {
                        GHProject proj = repo.createProject(name, body);
                        columns.forEach(x -> {
                            try {
                                proj.createColumn(x);
                            } catch (IOException e) {
                                log.error(e.getMessage());
                            }
                        });
                        
                    } else {
                        project.setName(name);
                        project.setBody(body);
                        int index = 0;
                        for (GHProjectColumn col : project.listColumns()) {
                            if (index >= columns.size())
                                col.delete();
                            else
                                col.setName(columns.get(index));
                            index++;
                        }
                    }
                    end();
                } catch (IOException ex) {
                    log.error(ex.getMessage());
                }
                break;

            default:
                break;
        }
    }
}
