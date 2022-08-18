package embed;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import org.kohsuke.github.GHRepository;

import discord4j.core.GatewayDiscordClient;
import discord4j.core.object.entity.channel.MessageChannel;
import discord4j.core.object.reaction.ReactionEmoji;
import discord4j.core.spec.EmbedCreateFields.Field;
import discord4j.core.spec.EmbedCreateFields.Footer;
import discord4j.core.spec.EmbedCreateSpec;
import discord4j.rest.util.Color;

import managers.App;
import managers.GitlabManager;
import pojo.GLList;
import pojo.Board;

public class BoardList extends EmbedManager{
    private List<Board> boards;
    private int selectedPage;
    private int maxPage;
    private Consumer<Board> callback;
    private String projectPath;

    public BoardList(String userId, String serverId, MessageChannel canal, GatewayDiscordClient gateway,
            GHRepository repo, Boolean isAux, List<Board> boards, Consumer<Board> callback, String projectPath) {
        super(userId, serverId, canal, gateway, repo, false);
        this.boards = boards;
        this.callback = callback;
        this.projectPath = projectPath;
        this.actions = new ArrayList<>();
        this.actions.add(ReactionEmoji.unicode("❓"));
        this.actions.add(ReactionEmoji.unicode("⬆️"));
        this.actions.add(ReactionEmoji.unicode("⬇️"));
        this.maxField = boards.size() - 1;
        this.selectedPage = 0;
        this.maxPage = this.maxField/ 10;
        if (boards.size() > 10) {
            this.maxField = 9;
            this.actions.add(ReactionEmoji.unicode("⬅️"));
            this.actions.add(ReactionEmoji.unicode("➡️"));
        }
        this.actions.add(ReactionEmoji.unicode("❌"));
        if (!isAux) {
            this.actions.add(ReactionEmoji.unicode("🔍"));
            this.actions.add(ReactionEmoji.unicode("✏️"));
        }
        else
            this.actions.add(ReactionEmoji.unicode("✅"));
        updateEmbed();
    }

    public String getBoardName(String id) {
        Board res = boards.stream().filter(x -> 
            x.getId().equals(id)).findAny().orElse(null);
        if (res != null)
            return res.getName();
        else
            return EMPTY_FIELD;
    }

    public void inputSelect(String input) {
        log.info("Selecting new element: " + input);
        int lastSelected = (selectedPage + 1) * 10 > boards.size() ? boards.size() : (selectedPage + 1) * 10;
        List<Board> sublist = boards.subList(selectedPage * 10, lastSelected);
        Board inputBoard = sublist.stream().filter(x -> x.getName().equals(input)).findAny().orElse(null);
        if (inputBoard != null) {
            selectedField = sublist.indexOf(inputBoard);
            updateEmbed();
        }
    }

    private void updateEmbed() {
        int lastSelected = (selectedPage + 1) * 10 > boards.size() ? boards.size() : (selectedPage + 1) * 10;
        List<Board> sublist = boards.subList(selectedPage * 10, lastSelected);
        String footer = null;
        if (maxPage > 0) {
            Integer total = boards.size();
            Integer inicio = selectedPage * 10;
            Integer fin = inicio + 10 > total ? total : inicio + 10;
            footer = "Showing boards " + inicio + "-" + fin + " of " + total;
        }
        emb = EmbedCreateSpec.builder()
            .color(Color.LIGHT_SEA_GREEN)
            .title("Board list")
            .build();
        List<Field> fields = sublist.stream().map(x -> sublist.indexOf(x) == selectedField ?
        Field.of("-> " + x.getName() + " <-", listParse(x.getListNames(), 99), false) : 
        Field.of(x.getName(), listParse(x.getListNames(), 99), false)).collect(Collectors.toList());
        emb = emb.withFields(fields);
        if (footer != null)
            emb = emb.withFooter(Footer.of(footer, "https://64.media.tumblr.com/aa65057a2ba418757cee5ae25c07d790/tumblr_pb2ky0qi6A1w6xh18o8_250.png"));
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

            case "⬅️":
                selectedPage = selectedPage == 0 ? maxPage : selectedPage - 1;
                updateEmbed();
                
                break;

            case "➡️":
                selectedPage = selectedPage == maxPage ? 0 : selectedPage + 1;
                updateEmbed();
                
                break;

            case "❌":
                end();
                App.sessions.put(userId + "-" + serverId, "Start");
                canal.createMessage("Can I help you with something more?").block();
                break;

            case "🔍":
                end();
                Board board = boards.get(selectedPage * 10 + selectedField);
                List<GLList> lists = GitlabManager.getLists(board.getId(), projectPath, repo);
                GitlabKanbanEmbed mng = new GitlabKanbanEmbed(userId, serverId, canal, gateway, repo,
                    lists, board.getId(), projectPath);
                mng.send();
                mng.sendSecondEmbed();
                App.embedDict.put(userId + "-" + serverId, mng);
                App.sessions.put(userId + "-" + serverId, "SelectGitlabIssue");
                break;

            case "✅":
                end();
                callback.accept(boards.get(selectedPage * 10 + selectedField));
                break;

            case "✏️":
                BoardEmbed manager = new BoardEmbed(userId, serverId, canal, gateway, repo, false, 
                    "Edit board", boards.get(selectedPage * 10 + selectedField), projectPath);
                end();
                manager.send();
                App.embedDict.put(userId + "-" + serverId, manager);
                App.sessions.put(userId + "-" + serverId, "InputBoard");
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
    }
}
