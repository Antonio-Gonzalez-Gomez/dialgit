package embed;

import java.util.Map;
import java.util.SortedMap;
import java.util.stream.Collectors;

import org.kohsuke.github.GHRepository;
import org.kohsuke.github.GHUser;

import discord4j.core.GatewayDiscordClient;
import discord4j.core.object.entity.channel.MessageChannel;
import discord4j.core.object.reaction.ReactionEmoji;
import discord4j.core.spec.EmbedCreateSpec;
import discord4j.core.spec.EmbedCreateFields.Field;
import discord4j.rest.util.Color;
import managers.ZenhubManager;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;

import pojo.ZHIssue;
import pojo.Sprint;

public class StatusEmbed extends EmbedManager{
    
    private String avatar;
    private List<Sprint> availableSprints;
    private Map<Sprint, List<ZHIssue>> issueMap;
    private Map<ZHIssue, Double> issueScore;
    private SortedMap<String, String> epicNames;

    public StatusEmbed(String userId, String serverId, MessageChannel canal, GatewayDiscordClient gateway,
            GHRepository repo, GHUser user) {
        super(userId, serverId, canal, gateway, repo, false);
        List<ZHIssue> issues = ZenhubManager.getIssues(repo);
        this.avatar = user.getAvatarUrl();
        this.availableSprints = new ArrayList<>();
        this.issueMap = new HashMap<>();
        this.issueScore = new HashMap<>();
        this.epicNames = ZenhubManager.getEpicMap(repo);
        issues.stream().filter(x -> !x.getIsEpic())
            .filter(x -> x.getGhIssue().getState().toString().equals("OPEN"))
            .filter(x -> x.getGhIssue().getAssignees().contains(user))
            .forEach(this::issueLoop);
        this.selectedField = 0;
        this.maxField = availableSprints.size() - 1;
        this.actions = new ArrayList<>();
        this.actions.add(ReactionEmoji.unicode("❓"));
        this.actions.add(ReactionEmoji.unicode("⬅️"));
        this.actions.add(ReactionEmoji.unicode("➡️"));
        this.actions.add(ReactionEmoji.unicode("❌"));
        updateEmbed();
        
    }

    private void issueLoop(ZHIssue iss) {
        Sprint next = null;
        for (Sprint sp : iss.getSprints()) {
            if (sp.getStart().isBefore(LocalDateTime.of(2022, 7, 27, 12, 30)) && sp.getEnd().isAfter(LocalDateTime.of(2022, 7, 27, 12, 30))) {
                next = sp;
                break;
            }
        }
        if (next != null) {
            log.debug("Next sprint: " + next.getName());
            if (availableSprints.contains(next))
                issueMap.get(next).add(iss);
            else {
                List<ZHIssue> value = new ArrayList<>();
                value.add(iss);
                issueMap.put(next, value);
                availableSprints.add(next);
            }
            Double score = Double.valueOf(iss.getEstimate() / (double) iss.getGhIssue().getAssignees().size());
            log.debug("Score calculated for issue [" + iss.getZenhubId() + "] : " + score);
            issueScore.put(iss, score);
        }
    }

    private Integer compare(ZHIssue iss) {
        return iss.getEstimate() / iss.getGhIssue().getAssignees().size();
    }

    private void updateEmbed() {
        Sprint sp = availableSprints.get(selectedField);
        List<ZHIssue> issues = issueMap.get(sp);
        issues.sort(Comparator.comparing(this::compare).reversed());
        Integer phRestantes = issues.stream().map(ZHIssue::getEstimate).reduce(0, Integer::sum);
        Integer isRestantes = issues.size();
        if (issues.size() > 10)
            issues = issues.subList(0, 10);
        emb = EmbedCreateSpec.builder()
            .color(Color.PINK)
            .title("Top priority issues for " + sp.getName())
            .description(phRestantes + " story points accross " + isRestantes +
                " issues remain to be burned.")
            .thumbnail(avatar)
            .build();
        List<Field> fields = issues.stream().map(x -> 
            Field.of("#" + x.getNumber() + "-" + x.getGhIssue().getTitle(),
            "Epics: " + listParse(x.getParentEpics().stream().map(y -> epicNames.get(y))
            .collect(Collectors.toList()), 99), false)).collect(Collectors.toList());
        emb = emb.withFields(fields);
        updateMessage();
    }

    @Override
    protected void executeAction(String action) {
        switch (action) {
            case "❓":
                iconHelp();
                break;
                
            case "⬅️":
                selectedField = selectedField == 0 ? maxField : selectedField - 1;
                updateEmbed();
                
                break;
            
            case "➡️":
                selectedField = selectedField == maxField ? 0 : selectedField + 1;
                updateEmbed();
                
                break;

            case "❌":
                end();
                break;

            default:
                break;
        }
    }
    
}
