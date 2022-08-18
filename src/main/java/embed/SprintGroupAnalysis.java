package embed;

import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.AbstractMap.SimpleEntry;
import java.util.function.Function;

import org.kohsuke.github.GHIssue;
import org.kohsuke.github.GHRepository;
import org.kohsuke.github.GHUser;

import discord4j.core.GatewayDiscordClient;
import discord4j.core.object.entity.User;
import discord4j.core.object.entity.channel.MessageChannel;
import discord4j.core.spec.EmbedCreateSpec;
import discord4j.rest.util.Color;
import managers.ZenhubManager;
import pojo.Sprint;
import pojo.ZHIssue;

public class SprintGroupAnalysis extends EmbedManager{

    private Function<GHUser, User> userCallback;
    private Sprint sprint;

    public SprintGroupAnalysis(String userId, String serverId, MessageChannel canal, GatewayDiscordClient gateway,
            GHRepository repo, Sprint sprint, Function<GHUser, User> userCallback) {
        super(userId, serverId, canal, gateway, repo, true);
        this.userCallback = userCallback;
        this.sprint = sprint;
        SimpleEntry<List<Integer>, List<ZHIssue>> queryResults = 
            ZenhubManager.getSprintAnalysisInfo(sprint.getId(), repo);
        updateEmbed(queryResults.getKey(), queryResults.getValue());
    }

    private String parsePercentage(Integer numerator, Integer denominator) {
        return " (" + Math.round(numerator * 100 / (double) denominator) + "%) ";
    }
    
    private void updateEmbed(List<Integer> metrics, List<ZHIssue> issues) {
        Integer totalIssues = issues.size();
        Integer closedIssues = metrics.get(0);
        Integer totalEpics = 0;
        Integer closedEpics = 0;
        Integer totalPoints = metrics.get(1);
        Integer burntPoints = metrics.get(2);
        Map<GHUser, Integer> assignedPoints = new HashMap<>();
        Map<GHUser, Integer> burnedPoints = new HashMap<>();
        for (ZHIssue zhiss : issues) {
            GHIssue ghiss = zhiss.getGhIssue();
            if (zhiss.getIsEpic()) {
                totalEpics++;
                totalIssues--;
                if (ghiss.getState().toString().equals("CLOSED")) {
                    closedEpics++;
                    closedIssues--;
                }
            }
            else {
                for (GHUser user : ghiss.getAssignees()) {
                    assignedPoints.computeIfAbsent(user, x -> 0);
                    burnedPoints.computeIfAbsent(user, x -> 0);
                    assignedPoints.put(user, assignedPoints.get(user) + zhiss.getEstimate());
                    if (ghiss.getState().toString().equals("CLOSED"))
                        burnedPoints.put(user, burnedPoints.get(user) + zhiss.getEstimate());
                }
            }
        }
        GHUser ghMaxAssigned = Collections.max(assignedPoints.entrySet(), Comparator.comparingInt(Map.Entry::getValue)).getKey();
        GHUser ghMaxBurned = Collections.max(burnedPoints.entrySet(), Comparator.comparingInt(Map.Entry::getValue)).getKey();
        User dcMaxAssigned = userCallback.apply(ghMaxAssigned);
        User dcMaxBurned = userCallback.apply(ghMaxBurned);
        String nameMaxAssigned = dcMaxAssigned == null ? ghMaxAssigned.getLogin() : dcMaxAssigned.getMention();
        String nameMaxBurned = dcMaxBurned == null ? ghMaxBurned.getLogin() : dcMaxBurned.getMention();
        String avatar = dcMaxBurned == null ? ghMaxBurned.getAvatarUrl() : dcMaxBurned.getAvatarUrl();
        emb = EmbedCreateSpec.builder()
            .color(Color.PINK)
            .title("Sprint analysis")
            .description(sprint.getName())
            .addField("Burnt story points", burntPoints + "/" + totalPoints + parsePercentage(burntPoints, totalPoints), false)
            .addField("Completed epic issues", closedEpics + "/" + totalEpics + parsePercentage(closedEpics, totalEpics), false)
            .addField("Closed normal issues", closedIssues + "/" + totalIssues + parsePercentage(closedIssues, totalIssues), false)
            .addField("Collaborator with the most story points assigned", nameMaxAssigned + ": " + assignedPoints.get(ghMaxAssigned) 
                + parsePercentage(assignedPoints.get(ghMaxAssigned), totalPoints), false)
            .addField("Collaborator with the most story points burnt", nameMaxBurned + ": " + burnedPoints.get(ghMaxBurned) 
                + parsePercentage(burnedPoints.get(ghMaxBurned), burntPoints), false)
            .thumbnail(avatar)
            .build();
        //No hace falta updatear el mensaje
    }
    
}
