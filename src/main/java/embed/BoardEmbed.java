package embed;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import org.kohsuke.github.GHRepository;
import discord4j.core.GatewayDiscordClient;
import discord4j.core.object.entity.channel.MessageChannel;
import discord4j.core.object.reaction.ReactionEmoji;
import discord4j.core.spec.EmbedCreateFields.Field;
import discord4j.core.spec.EmbedCreateSpec;
import discord4j.rest.util.Color;
import managers.GitlabManager;
import pojo.GLList;
import pojo.Board;

public class BoardEmbed extends EmbedManager{
    
    private String title;
    private String name;
    private List<String> listNames;
    protected Board board;
    private String inputStatus;
    private String projectPath;
    private Map<String, String> labels;

    public BoardEmbed(String userId, String serverId, MessageChannel canal,
            GatewayDiscordClient gateway, GHRepository repo, boolean readOnly,
            String title, Board board, String projectPath) {
        super(userId, serverId, canal, gateway, repo, readOnly);
        this.title = title;
        this.name = "New board";
        this.listNames = new ArrayList<>();
        this.projectPath = projectPath;
        this.labels = GitlabManager.getLabels(projectPath);
        this.actions = new ArrayList<>();
        if (!readOnly) {
            this.maxField = 0;
            this.inputStatus = "default";
            this.actions.add(ReactionEmoji.unicode("❓"));
            this.actions.add(ReactionEmoji.unicode("⬆️"));
            this.actions.add(ReactionEmoji.unicode("⬇️"));
            this.actions.add(ReactionEmoji.unicode("➖"));
            this.actions.add(ReactionEmoji.unicode("➕"));
            this.actions.add(ReactionEmoji.unicode("❌"));
            this.actions.add(ReactionEmoji.unicode("✅"));
            canal.createMessage("Available labels: " + 
                listParse(new ArrayList<>(labels.keySet()), 199)).block();
        }
        if (board != null) {
            this.board = board;
            this.name = board.getName();
            this.listNames = board.getListNames();
            this.listNames.remove("Open");
            this.listNames.remove("Closed");
            this.maxField = this.listNames.size();
        }
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
                    if (position > 0 && position <= listNames.size() + 1)
                        listNames.add(position - 1, EMPTY_FIELD);
                    else
                        listNames.add(EMPTY_FIELD);
                    inputStatus = "default";
                    maxField++;
                    
                } catch(NumberFormatException e) {
                    error = EMPTY_FIELD;
                }
                break;

            case "inputName":
                if (maxField > 0 && listNames.remove(data)) {
                    if (selectedField == maxField)
                        selectedField--;
                    maxField--;
                    inputStatus = "default";
                }
                else 
                    error = EMPTY_FIELD;
                break;

            default:
                if (selectedField == 0) {
                    if (length <= 140)
                        name = data;
                    else
                        error = "Board name can't be longer than 140 characters";
                
                }
    
                else {
                    if (length > 140)
                        error = "Issue list names can't be longer than 140 characters";
                    else if(listNames.contains(data))
                        error = "Issue list names must be different";
                    else
                        listNames.set(selectedField - 1, data);  
                }
            }
        if (error == null)
            selectedField = selectedField == maxField ? selectedField : selectedField + 1;
        updateEmbed();
        
    }

    private void updateEmbed() {
        List<Field> fields = new ArrayList<>();
        fields.add(Field.of("Name", name == null || name.equals("") ? EMPTY_FIELD : name, false));
        Color col = Color.GREEN;
        String desc = EMPTY_FIELD;
        if (!readOnly) {
            col = Color.RUST;
            fields.addAll(IntStream.range(0, listNames.size()).mapToObj(
                x -> Field.of("Issue List " + String.valueOf(x + 1), listNames.get(x), false))
                .collect(Collectors.toList()));
            Field selected = fields.get(selectedField);
            fields.set(selectedField, Field.of("->" + selected.name() + " <-", selected.value(), false));
            if (error != null && !error.equals(EMPTY_FIELD))
                desc = error;
            else {
                //Si el inputStatus es default, la descripcion quedará en blanco
                desc = inputStatus.equals("inputName") ? "Input the issue list name you want to delete" : desc;
                desc = inputStatus.equals("inputPosition") ? "Input a position in the range [1-" +
                    String.valueOf(maxField + 1) + "] you want to add the new issue list, or input " +
                    "any number outside that range to place it in last position" : desc;
            }
        }
        emb = EmbedCreateSpec.builder()
            .color(col)
            .description(desc)
            .title(title)
            .build();
        emb = emb.withFields(fields);
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
                String boardID = "";
                if (board == null) {
                    boardID = GitlabManager.createBoard(name, projectPath);
                }
                else {
                    boardID = board.getId();
                    GitlabManager.updateBoard(name, boardID);
                }
                List<GLList> oldLists = GitlabManager.getLists(boardID, projectPath, repo);
                List<String> oldNames = oldLists.stream().map(GLList::getName)
                    .collect(Collectors.toList());
                int position = 0;
                for (String nm : listNames) {
                    if (!oldNames.contains(nm)) {
                        String labelId = labels.get(nm);
                        if (labelId == null)
                            labelId = GitlabManager.createLabel(projectPath, nm);
                        String listID = GitlabManager.createList(boardID, labelId);
                        //Es necesario hacer un update porque Gitlab añade las listas nuevas al final
                        GitlabManager.updateList(listID, position);
                    }
                    else {
                        int oldIndex = oldNames.indexOf(nm);
                        //Existe la lista pero en otra posición -> Actualizar lista
                        if (oldIndex != position)
                            GitlabManager.updateList(oldLists.get(oldIndex).getId(), position);
                        //else -> Posicion y nombres correctos,no hay que hacer nada
                        oldLists.remove(oldIndex);
                        oldNames.remove(oldIndex);
                    }
                    position++;
                }
                for (GLList lst : oldLists)
                    //Las listas viejas que no se hayan encontrado se borran
                    GitlabManager.deleteList(lst.getId());
                end();
                break;

            default:
                break;
        }
    }
}