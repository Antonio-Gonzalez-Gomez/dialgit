package embed;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import org.kohsuke.github.GHRepository;

import discord4j.core.GatewayDiscordClient;
import discord4j.core.object.entity.channel.MessageChannel;
import discord4j.core.object.reaction.ReactionEmoji;
import discord4j.core.spec.EmbedCreateFields.Field;
import discord4j.core.spec.EmbedCreateSpec;
import discord4j.rest.util.Color;
import managers.ZenhubManager;
import pojo.Pipeline;
import pojo.Workspace;

public class WorkspaceEmbed extends EmbedManager{
    
    private String title;
    private String name;
    private String description;
    private List<String> pipeNames;
    protected Workspace workspace;
    private String inputStatus;

    public WorkspaceEmbed(String userId, String serverId, MessageChannel canal,
            GatewayDiscordClient gateway, GHRepository repo, boolean readOnly,
            String title, String name, String description, List<String> pipeNames,
            Workspace workspace) {
        super(userId, serverId, canal, gateway, repo, readOnly);
        this.title = title;
        this.name = name;
        this.description = description;
        this.actions = new ArrayList<>();
        if (!readOnly) {
            this.maxField = 1 + pipeNames.size();
            this.inputStatus = "default";
            this.pipeNames = new ArrayList<>(pipeNames);
            this.actions.add(ReactionEmoji.unicode("❓"));
            this.actions.add(ReactionEmoji.unicode("⬆️"));
            this.actions.add(ReactionEmoji.unicode("⬇️"));
            this.actions.add(ReactionEmoji.unicode("➖"));
            this.actions.add(ReactionEmoji.unicode("➕"));
            this.actions.add(ReactionEmoji.unicode("❌"));
            this.actions.add(ReactionEmoji.unicode("✅"));
        }
        this.workspace = workspace;
        updateEmbed();
    }

    public void addData(String data) {
        log.info("Adding data: " + data);
        int length = data.length();
        this.error = null;
        switch (inputStatus) {
            case "inputPosition":
                try {
                    Integer position = Integer.valueOf(data);
                    if (position > 0 && position < pipeNames.size() + 1)
                        pipeNames.add(position - 1, EMPTY_FIELD);
                    else
                        pipeNames.add(EMPTY_FIELD);
                    inputStatus = "default";
                    maxField++;
                    
                } catch(NumberFormatException e) {
                    error = EMPTY_FIELD;
                }
                break;

            case "inputName":
                if (maxField > 1 && pipeNames.remove(data)) {
                    if (selectedField == maxField)
                        selectedField--;
                    maxField--;
                    inputStatus = "default";
                }
                else 
                    error = EMPTY_FIELD;
                break;

            default:
                switch (selectedField) {
                    case 0:
                        if (length <= 140)
                            name = data;
                        else
                            error = "Workspace name can't be longer than 140 characters";
                        break;
        
                    case 1:
                        if (length <= 1024)
                            description = data;
                        else
                            error = "Workspace description can't be longer than 1024 characters";
                        break;
        
                    default:
                        if (length > 140)
                            error = "Pipeline names can't be longer than 140 characters";
                        else if(pipeNames.contains(data))
                            error = "Pipeline names must be different";
                        else
                            pipeNames.set(selectedField - 2, data);
                        break;
            }
        }
        if (error == null)
            selectedField = selectedField == maxField ? selectedField : selectedField + 1;
        updateEmbed();
        
    }

    public void selectNewWorkspace(Workspace workspace) {
        log.info("Selecting workspace: " + workspace.getName());
        this.workspace = workspace;
        this.name = workspace.getName();
        this.description = workspace.getDescription();
        this.pipeNames = workspace.getPipelinesNames();
        updateEmbed();
    }

    private void updateEmbed() {
        List<Field> fields = new ArrayList<>();
        fields.add(Field.of("Name", name == null || name.equals("") ? EMPTY_FIELD : name, false));
        fields.add(Field.of("Description", description == null || description.equals("") ? EMPTY_FIELD : description, false));
        Color col = Color.GREEN;
        String desc = EMPTY_FIELD;
        if (!readOnly) {
            col = Color.RUST;
            fields.addAll(pipeNames.stream().map(x -> 
                Field.of("Pipeline " + String.valueOf(pipeNames.indexOf(x) + 1), x, false)).collect(Collectors.toList()));
            Field selected = fields.get(selectedField);
            fields.set(selectedField, Field.of("->" + selected.name() + " <-", selected.value(), false));
            if (error != null)
                desc = error;
            else {
                //Si el inputStatus es default, la descripcion quedará en blanco
                desc = inputStatus.equals("inputName") ? "Input the pipeline name you want to delete" : desc;
                desc = inputStatus.equals("inputPosition") ? "Input a position in the range [1-" +
                    String.valueOf(maxField) + "] you want to add the new pipeline, or input " +
                    "any number outside that range to place it in last position" : desc;
            }
        }
        else if(workspace != null) {
            String strpipeNames = EMPTY_FIELD;
            List<String> pipeNamestrings = workspace.getPipelinesNames();
            strpipeNames = listParse(pipeNamestrings, 99);
            fields.add(Field.of("pipeNames", strpipeNames, false));
        }
        emb = EmbedCreateSpec.builder()
            .color(col)
            .title(title)
            .build();
        emb = emb.withFields(fields);
        if (!desc.equals(EMPTY_FIELD))
            emb = emb.withDescription(desc);
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

            case "➖":
                inputStatus = inputStatus.equals("inputName") ? "default" : "inputName";
                updateEmbed();
                break;

            case "➕": 
                inputStatus = inputStatus.equals("inputPosition") ? "default" : "inputPosition";
                updateEmbed();
                break;

            case "❌":
                end();
                break;

            case "✅":
                String workspaceID = "";
                if (description.equals(EMPTY_FIELD))
                    description = "";
                if (workspace == null) {
                    workspaceID = ZenhubManager.createWorkspace(name, description, repo.getId());
                }
                else {
                    workspaceID = workspace.getId();
                    ZenhubManager.updateWorkspace(name, description, workspaceID);
                }
                List<Pipeline> oldPipelines = ZenhubManager.getPipelines(workspaceID, repo);
                List<String> oldNames = oldPipelines.stream().map(Pipeline::getName)
                    .collect(Collectors.toList());
                int position = 0;
                for (String nm : pipeNames) {
                    if (!oldNames.contains(nm))
                        ZenhubManager.createPipeline(nm, position, workspaceID);
                    else {
                        int oldIndex = oldNames.indexOf(nm);
                        //Existe la pipeline pero en otra posición -> Actualizar pipeline
                        if (oldIndex != position)
                            ZenhubManager.updatePipeline(nm, position, oldPipelines.get(oldIndex).getId());
                        //else -> Posicion y nombres correctos,no hay que hacer nada
                        oldPipelines.remove(oldIndex);
                        oldNames.remove(oldIndex);
                    }
                    position++;
                }
                for (Pipeline pln : oldPipelines)
                    //Las pipelines viejas que no se hayan encontrado en la nueva lista se borran
                    ZenhubManager.deletePipeline(pln.getId());
                end();
                break;

            default:
                break;
        }
    }
}
