package managers;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BiPredicate;
import java.util.function.Consumer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;
import org.kohsuke.github.GHIssueState;
import org.kohsuke.github.GHMilestone;
import org.kohsuke.github.GHProject;
import org.kohsuke.github.GHProject.ProjectStateFilter;

import org.kohsuke.github.GHRepository;

import discord4j.core.DiscordClientBuilder;
import discord4j.core.GatewayDiscordClient;
import discord4j.core.event.domain.message.MessageCreateEvent;
import discord4j.core.object.entity.User;
import discord4j.core.object.entity.channel.MessageChannel;
import discord4j.core.object.presence.ClientActivity;
import discord4j.core.object.presence.ClientPresence;
import embed.EmbedManager;
import embed.IssueEmbed;
import embed.KanbanEmbed;
import embed.MilestoneEmbed;
import embed.MilestoneList;
import embed.ProjectEmbed;
import embed.ProjectList;
import embed.ServerRegistrationEmbed;
import embed.UserRegistrationEmbed;
import model.Server;

public final class App {

  private static final String TOKEN_DISCORD = Secrets.DISCORD_TOKEN;
  private static final String EMPTY_FIELD = "\u200B";
	public static Map<String, String> sessions = new HashMap<>();
  public static Map<String, EmbedManager> embedDict = new HashMap<>();
  private static GithubManager gitManager = new GithubManager();
  private static DialogflowManager dialogManager = new DialogflowManager();
  private static HibernateManager hibernateManager = new HibernateManager();
  private static final Logger log = LogManager.getLogger();

  public static void main(final String[] args) {
    log.debug("Starting Dialgit");
    GatewayDiscordClient gateway = DiscordClientBuilder.create(TOKEN_DISCORD)
      .build().login().block();
    ClientPresence presence = ClientPresence.online(ClientActivity.playing("dg!start | dg!help"));
    gateway.updatePresence(presence).block();
    log.debug("Dialgit is online!");
    gateway.getEventDispatcher().on(MessageCreateEvent.class)
    .subscribe(event -> {

		User usuario = event.getMessage().getAuthor().orElse(null);
    MessageChannel canal = event.getMessage().getChannel().block();

		if (!usuario.isBot()) {
      String message = event.getMessage().getContent();
      String userId = usuario.getId().asString();
      String serverId = event.getMessage().getGuild().block().getId().asString();
      
      log.info("Captured message at server [" + serverId + "]:\n" + message);
      String key = userId + "-" + serverId;
      if (message.equals("dg!help")) {
        canal.createMessage(EmbedManager.tutorialEmbed()).block();
      }
      if (message.equals("dg!start")) {
        if (sessions.containsKey(key)) {
          canal.createMessage("You already had established a conversation!").block();
        } else {
          Server sv = hibernateManager.getServer(Long.parseLong(serverId));
          if (sv == null) {
            canal.createMessage("There is no GitHub repository associated to this server. Please introduce the URL to a repository which has installed Dialgit.").block();
            BiPredicate<String, String> serverRegistrationCallback = (author, repoName) -> serverRegistrationCallback(author, repoName, serverId);
            BiPredicate<String, String> checkRepoCallback = (author, repoName) -> gitManager.checkRepositoryExists(author, repoName);
            ServerRegistrationEmbed serverEmbed = new ServerRegistrationEmbed(userId, serverId, canal,
              gateway, null, false, serverRegistrationCallback, checkRepoCallback);
            serverEmbed.send();
            embedDict.put(userId + "-" + serverId, serverEmbed);
            sessions.put(key, "InputServer");
          }
          else {
            model.User us = hibernateManager.getUser(Long.parseLong(userId));
            if (us == null) {
              canal.createMessage("You have no GitHub account associated to your Discord user. Please introduce your GitHub username.").block();
              Server server = hibernateManager.getServer(Long.parseLong(serverId));
              GHRepository repo = gitManager.getRepo(server);
              Consumer<String> callback = x -> hibernateManager.createUser(Long.valueOf(userId), x);
              UserRegistrationEmbed userEmbed = new UserRegistrationEmbed(userId, serverId, canal,
                gateway, repo, false, callback);
              userEmbed.send();
              embedDict.put(userId + "-" + serverId, userEmbed);
              sessions.put(key, "InputUsername");
            }
              else {
              canal.createMessage("What can I help you with?").block();
              sessions.put(key, "Start");
            }
          } 
        }
          
      } else if (sessions.containsKey(key)) {
        String contexto = sessions.get(key);
        log.info("Context associated with user [" + userId + "]: " + contexto);
        switch (contexto) {
          case "Start":
            sessions.put(key, queryManager(userId, serverId, message, canal, gateway));
            break;
          
          case "InputUsername":
            UserRegistrationEmbed userEmbed = (UserRegistrationEmbed) embedDict.get(key);
            userEmbed.addData(message);
            break;
          
          case "InputServer":
            ServerRegistrationEmbed serverEmbed = (ServerRegistrationEmbed) embedDict.get(key);
            serverEmbed.addData(message);
            break;

          case "InputProject":
            ProjectEmbed prjtEmbed = (ProjectEmbed) embedDict.get(key);
            prjtEmbed.addData(message);
            break;

          case "InputMilestone":
            MilestoneEmbed mlstEmbed = (MilestoneEmbed) embedDict.get(key);
            if (mlstEmbed.getSelectedField() == 2)
              mlstEmbed.addData(dialogManager.getDate(message, userId));
            else
              mlstEmbed.addData(message);
            break;

          case "InputIssue":
            IssueEmbed issueEmbed = (IssueEmbed) embedDict.get(key);
            if (issueEmbed.getSelectedField() == 2) {
              List<String> menciones = new ArrayList<>();
              //Recoge las menciones del mensaje para obtener las IDs
              Matcher m = Pattern.compile("@<\\d*>").matcher(message);
              while (m.find()) {
                String mencion = m.group();
                System.out.println(mencion);
                menciones.add(mencion.substring(2, mencion.length() - 1));
              }
              String strAssignees = "";
              for (String mnc : menciones) {
                model.User usr = hibernateManager.getUser(Long.valueOf(mnc));
                if (usr != null)
                  //Se usa el espacio como separador porque GitHub no permite nombres con espacio
                  strAssignees += usr.getName() + " ";
              }
              //Aquí solo se comprueba que los usuarios existan en la BD
              //Comprobar que pertenezcan o no al repositorio se hace dentro del EmbedManager
              issueEmbed.addData(strAssignees.substring(0, strAssignees.length() - 1));
            } else
              issueEmbed.addData(message);
            break;

          case "SelectProject":
            ProjectList prjtListEmbed = (ProjectList) embedDict.get(key);
            prjtListEmbed.inputSelect(message);
            break;

          case "SelectMilestone":
            MilestoneList mlstListEmbed = (MilestoneList) embedDict.get(key);
            mlstListEmbed.inputSelect(message);
            break;

          case "SelectIssue":
            KanbanEmbed kanbanEmbed = (KanbanEmbed) embedDict.get(key);
            kanbanEmbed.inputSelect(message);
            break;

          default:
            sessions.put(key, "Start");
            break;
        }
      }

		}});
    gateway.onDisconnect().block();
  }

  protected static Boolean serverRegistrationCallback(String author, String repoName, String serverId) {
    int installationId = gitManager.getInstallationId(author, repoName);
    if (installationId == 0) {
      return false;
    }
    hibernateManager.createServer(Long.valueOf(serverId), author + "/" + repoName, installationId);
    return true;      
  }

  private static String queryManager(String userId, String serverId, String query, MessageChannel canal, GatewayDiscordClient gateway) {
    String intent = dialogManager.simpleDetect(query, userId);
    String result = "Start";    
    log.info("Detected intent: " + intent);
    Server server = hibernateManager.getServer(Long.parseLong(serverId));
    GHRepository repo = gitManager.getRepo(server);
    switch (intent) {

        case "Help":
          canal.createMessage(EmbedManager.tutorialEmbed()).block();
          break;

        case "AddProject":
          List<String> columns = Arrays.asList("To Do", "In Progress", "Done");
          ProjectEmbed prjtEmbed = new ProjectEmbed(userId, serverId, canal, gateway,
            repo, false, "Add project", EMPTY_FIELD, EMPTY_FIELD, columns, null);
          prjtEmbed.send();
          embedDict.put(userId + "-" + serverId, prjtEmbed);
          result = "InputProject";
          break;

        case "ListProject":
          try {
            List<GHProject> lista = repo.listProjects(ProjectStateFilter.ALL).toList();
            ProjectList prjtList = new ProjectList(userId, serverId, canal, gateway,
              repo, false, lista);
            prjtList.send();
            prjtList.sendSecondEmbed();
            embedDict.put(userId + "-" + serverId, prjtList);
            result = "SelectProject";
          } catch(IOException ex) {
            log.error(ex.getMessage());
          }
          break;
        
        case "AddMilestone":
          MilestoneEmbed mlstEmbed = new MilestoneEmbed(userId, serverId, canal, gateway,
            repo, false, "Add milestone", EMPTY_FIELD, EMPTY_FIELD, EMPTY_FIELD, null);
          mlstEmbed.send();
          embedDict.put(userId + "-" + serverId, mlstEmbed);
          result = "InputMilestone";
          break;

        case "ListMilestone":
          try {
            List<GHMilestone> lista = repo.listMilestones(GHIssueState.valueOf("ALL")).toList();
            MilestoneList mlstList = new MilestoneList(userId, serverId, canal, gateway,
              repo, false, false, lista);
            mlstList.send();
            mlstList.sendSecondEmbed();
            embedDict.put(userId + "-" + serverId, mlstList);
            result = "SelectMilestone";
          } catch(IOException ex) {
            log.error(ex.getMessage());
          }
          break;
          
        case "AddIssue":
          try {
            List<GHMilestone> lista = repo.listMilestones(GHIssueState.valueOf("OPEN")).toList();
            IssueEmbed issuEmbed = new IssueEmbed(userId, serverId, canal, gateway, repo,
              false, "Add issue", EMPTY_FIELD, EMPTY_FIELD, null, lista);
            issuEmbed.send();
            embedDict.put(userId + "-" + serverId, issuEmbed);
            result = "InputIssue";
          } catch (IOException e) {
            log.error(e.getMessage());
          }
        break;

        default:
          canal.createMessage("Sorry, I didn't understand you. What can I help you with?").block();
          break;
    }
    return result;
  }
}
