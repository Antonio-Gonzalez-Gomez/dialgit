package embed;

import java.util.AbstractMap.SimpleEntry;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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

public class SprintIndividualAnalysis extends EmbedManager{

    private User dcUser;
    private GHUser ghUser;
    private Sprint sprint;

    public SprintIndividualAnalysis(String userId, String serverId, MessageChannel canal, GatewayDiscordClient gateway,
            GHRepository repo, Sprint sprint, User dcUser, GHUser ghUser) {
        super(userId, serverId, canal, gateway, repo, true);
        this.dcUser = dcUser;
        this.ghUser = ghUser;
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
        Integer totalClosed = metrics.get(0);
        Integer ghUserIssues = 0;
        Integer ghUserClosed = 0;
        Integer totalAssigned = metrics.get(1);
        Integer totalBurnt = metrics.get(2);
        Integer ghUserAssigned = 0;
        Integer ghUserBurnt = 0;
        Map<GHUser, Double> assignedPonderated = new HashMap<>();
        Map<GHUser, Double> burntPonderated = new HashMap<>();

        for (ZHIssue zhiss : issues) {
            GHIssue ghiss = zhiss.getGhIssue();
            if (zhiss.getIsEpic()) {
                totalIssues--;
                if (ghiss.getState().toString().equals("CLOSED")) {
                    totalClosed--;
                }
            }
            else {
                Double ponderated = Double.valueOf(zhiss.getEstimate() / (double) ghiss.getAssignees().size());
                for (GHUser user : ghiss.getAssignees()) {
                    boolean isAssignee = user.equals(ghUser);
                    assignedPonderated.computeIfAbsent(user, x -> 0.0);
                    burntPonderated.computeIfAbsent(user, x -> 0.0);
                    assignedPonderated.put(user, assignedPonderated.get(user) + ponderated);
                    if (isAssignee) {
                        ghUserIssues++;
                        ghUserAssigned += zhiss.getEstimate();
                    }
                    if (ghiss.getState().toString().equals("CLOSED")) {
                        burntPonderated.put(user, burntPonderated.get(user) + ponderated);
                        if (isAssignee) {
                            ghUserClosed++;
                            ghUserBurnt += zhiss.getEstimate();
                        }
                    }
                }
            }
        }
        String avatar = dcUser.getAvatarUrl();
        String ghUserPondAssigned = String.format("%.2f", assignedPonderated.get(ghUser));
        String ghUserPondBurnt = String.format("%.2f", burntPonderated.get(ghUser));
        String meanAssigned = String.format("%.2f", assignedPonderated.values().stream().map(x -> x / assignedPonderated.size()).reduce(0.0, Double::sum));
        String meanBurnt = String.format("%.2f", burntPonderated.values().stream().map(x -> x / burntPonderated.size()).reduce(0.0, Double::sum));

        emb = EmbedCreateSpec.builder()
            .color(Color.PINK)
            .title("Sprint analysis")
            .description(sprint.getName())
            .addField("Assigned story points (vs total)", ghUserAssigned + "/" + totalAssigned + parsePercentage(ghUserAssigned, totalAssigned), false)
            .addField("Burnt story points (vs total)", ghUserBurnt + "/" + totalBurnt + parsePercentage(ghUserBurnt, totalBurnt), false)
            .addField("Assigned issues (vs total)", ghUserIssues + "/" + totalIssues + parsePercentage(ghUserIssues, totalIssues), false)
            .addField("Closed issues (vs total)", ghUserClosed + "/" + totalClosed + parsePercentage(ghUserClosed, totalClosed), false)
            .addField("Ponderated assigned story points (vs mean value)", ghUserPondAssigned + "/" + meanAssigned, false)
            .addField("Ponderated burnt story points (vs mean value)", ghUserPondBurnt + "/" + meanBurnt, false)
            
            .thumbnail(avatar)
            .build();
        //No hace falta updatear el mensaje
    }
    
}
