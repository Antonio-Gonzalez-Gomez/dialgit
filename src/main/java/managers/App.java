package managers;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BiPredicate;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.kohsuke.github.GHRepository;
import org.kohsuke.github.GHUser;

import discord4j.common.util.Snowflake;
import discord4j.core.DiscordClientBuilder;
import discord4j.core.GatewayDiscordClient;
import discord4j.core.event.domain.message.MessageCreateEvent;
import discord4j.core.object.entity.User;
import discord4j.core.object.entity.channel.MessageChannel;
import discord4j.core.object.presence.ClientActivity;
import discord4j.core.object.presence.ClientPresence;
import embed.*;
import model.*;
import pojo.*;

//Clase main de la aplicación
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
      long longServerId = event.getMessage().getGuild().block().getId().asLong();
      
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
            canal.createMessage("There is no GitLab group associated to this server. Please introduce the URL to one.").block();
            ServerRegistrationEmbed serverEmbed = new ServerRegistrationEmbed(userId, serverId, canal,
              gateway, null, false, (GitlabManager::checkGroupExists), (x -> hibernateManager.createServer(longServerId, x))); 
            serverEmbed.send();
            embedDict.put(userId + "-" + serverId, serverEmbed);
            sessions.put(key, "InputServer");
          }
          else {
            long channelId = canal.getId().asLong();
            Channel ch = hibernateManager.getChannel(channelId);
            if (ch == null) {
              canal.createMessage("There is no GitHub repository associated to this channel. Please introduce an URL to a repository connected to the GitLab group.").block();
              BiPredicate<String, String> channelRegistrationCallback = (author, repoName) -> 
                channelRegistrationCallback(author, repoName, longServerId, canal.getId().asLong());
              BiPredicate<String, String> checkRepoCallback = (author, repoName) -> gitManager.getInstallationId(author, repoName) != 0;
              List<Channel> channels = hibernateManager.getChannelsFromServer(longServerId);
              List<String> groupRepos = GitlabManager.getGroupRepos(sv.getUrl());
              ChannelRegistrationEmbed channelEmbed = new ChannelRegistrationEmbed(userId, serverId, canal,
                gateway, channelRegistrationCallback, checkRepoCallback, channels, groupRepos);
              channelEmbed.send();
              channelEmbed.sendSecondEmbed();
              embedDict.put(userId + "-" + serverId, channelEmbed);
              sessions.put(key, "InputChannel");
            }
            else {
              Member mb = hibernateManager.getMember(Long.parseLong(userId));
              GHRepository repo = gitManager.getRepo(ch);
              List<String> groupMembers = GitlabManager.getGroupMembers(sv.getUrl());
              if (mb == null) {
                canal.createMessage("You have no GitHub account associated to your Discord user. Please introduce your GitHub username.").block();
                UserRegistrationEmbed userEmbed = new UserRegistrationEmbed(userId, serverId, canal,
                  gateway, repo, false, groupMembers, (x -> hibernateManager.createMember(Long.valueOf(userId), x)));
                userEmbed.send();
                embedDict.put(userId + "-" + serverId, userEmbed);
                sessions.put(key, "InputUsername");
              }
              else if (!groupMembers.contains(mb.getName())) {
                canal.createMessage("You are registered in DialGit, but you aren't a member of this group in Gitlab").block();
              } 
              else
                try {
                  if (repo.getCollaborators().byLogin(mb.getName()) == null) {
                    canal.createMessage("You are registered in DialGit, but you aren't a collaborator of this repository in Github").block();
                  }
                  else {
                    canal.createMessage("What can I help you with?").block();
                    sessions.put(key, "Start");
                  }
                } catch (IOException e) {
                  log.warn(e.getMessage());
                }
            }
          } 
        }
          
      } else if (sessions.containsKey(key)) {
        boolean input = true;
        String contexto = sessions.get(key);
        log.info("Context associated with user [" + userId + "]: " + contexto);
        switch (contexto) {
          case "Start":
            sessions.put(key, queryManager(userId, serverId, message, canal, gateway));
            input = false;
            break;
          
          case "InputServer":
            ServerRegistrationEmbed serverEmbed = (ServerRegistrationEmbed) embedDict.get(key);
            serverEmbed.addData(message);
            break;

          case "InputUsername":
            UserRegistrationEmbed userEmbed = (UserRegistrationEmbed) embedDict.get(key);
            userEmbed.addData(message);
            break;

          case "InputChannel":
            ChannelRegistrationEmbed channelEmbed = (ChannelRegistrationEmbed) embedDict.get(key);
            channelEmbed.addData(message);
            break;

          case "InputWorkspace":
            WorkspaceEmbed wrkpEmbed = (WorkspaceEmbed) embedDict.get(key);
            wrkpEmbed.addData(message);
            break;

          case "InputSprint":
            SprintConfigEmbed sprtEmbed = (SprintConfigEmbed) embedDict.get(key);
            if (sprtEmbed.getSelectedField() != 0)
              sprtEmbed.addData(dialogManager.getDate(message, userId));
            else
              sprtEmbed.addData(message);
            break;

          case "InputIssue":
            log.debug("Chale banda atinamos al input");
            IssueEmbed issueEmbed = (IssueEmbed) embedDict.get(key);
            if (issueEmbed.getSelectedField() == 3) {
              List<String> menciones = new ArrayList<>();
              //Recoge las menciones del mensaje para obtener las IDs
              Matcher m = Pattern.compile("<@\\d*>").matcher(message);
              while (m.find()) {
                String mencion = m.group();
                log.info(mencion);
                menciones.add(mencion.substring(2, mencion.length() - 1));
              }
              String strAssignees = "";
              for (String mnc : menciones) {
                Member mb = hibernateManager.getMember(Long.valueOf(mnc));
                if (mb != null)
                  //Se usa el espacio como separador porque GitHub no permite nombres con espacio
                  strAssignees += mb.getName() + " ";
              }
              //Aquí solo se comprueba que los usuarios existan en la BD
              //Comprobar que pertenezcan o no al repositorio se hace dentro del EmbedManager
              issueEmbed.addData(strAssignees);
            } else
              issueEmbed.addData(message);
            break;
          

          case "InputBoard":
            BoardEmbed boardEmbed = (BoardEmbed) embedDict.get(key);
            boardEmbed.addData(message);
            break;

          case "InputGitlabIssue":
            GitlabIssueEmbed gitlabIssue = (GitlabIssueEmbed) embedDict.get(key);
            switch (gitlabIssue.getSelectedField()) {
              case 0:
                gitlabIssue.addData(message);
                break;

              case 1:
                List<String> menciones = new ArrayList<>();
                //Recoge las menciones del mensaje para obtener las IDs
                Matcher m = Pattern.compile("<@\\d*>").matcher(message);
                while (m.find()) {
                  String mencion = m.group();
                  log.info(mencion);
                  menciones.add(mencion.substring(2, mencion.length() - 1));
                }
                String strAssignees = "";
                for (String mnc : menciones) {
                  Member mb = hibernateManager.getMember(Long.valueOf(mnc));
                  if (mb != null)
                    //Se usa el espacio como separador porque GitHub no permite nombres con espacio
                    strAssignees += mb.getName() + " ";
                  //Aquí solo se comprueba que los usuarios existan en la BD
                  //Comprobar que pertenezcan o no al repositorio se hace dentro del EmbedManager
                }
                gitlabIssue.addData(strAssignees);
                break;

              case 2:
                gitlabIssue.addData(dialogManager.getDate(message, userId));
                break;

              default:
                break;
            }
            break;

          case "SelectWorkspace":
            WorkspaceList wrkpListEmbed = (WorkspaceList) embedDict.get(key);
            wrkpListEmbed.inputSelect(message);
            break;

          case "SelectBoard":
            BoardList boardListEmbed = (BoardList) embedDict.get(key);
            boardListEmbed.inputSelect(message);
            break;

          case "SelectSprint":
            SprintList sprtListEmbed = (SprintList) embedDict.get(key);
            sprtListEmbed.inputSelect(message);
            break;

          case "SelectIssue":
            KanbanEmbed kanbanEmbed = (KanbanEmbed) embedDict.get(key);
            kanbanEmbed.inputSelect(message);
            break;

          case "SelectGitlabIssue":
            GitlabKanbanEmbed gitlabKanban = (GitlabKanbanEmbed) embedDict.get(key);
            gitlabKanban.inputSelect(message);
            break;

          case "NoInput":
            input = false;
            break;

          default:
            input = false;
            sessions.put(key, "Start");
            break;
        }
        if (input && event.getMessage() != null)
          event.getMessage().delete().block();
      }

		}});
    gateway.onDisconnect().block();
  }

  private static Boolean channelRegistrationCallback(String author, String repoName, long serverId, long channelId) {
    int installationId = gitManager.getInstallationId(author, repoName);
    log.info(author);
    log.info(repoName);
    if (installationId == 0) {
      return false;
    }
    Server sv = hibernateManager.getServer(serverId);
    hibernateManager.createChannel(channelId, author + "/" + repoName, installationId, sv);
    return true;      
  }

  private static void workspaceSelectionCallback(Workspace wk, String userId, String serverId, MessageChannel canal,
    GatewayDiscordClient gateway, GHRepository repo) {
    
    SprintConfigEmbed sprintConfig = new SprintConfigEmbed(userId, serverId, canal, gateway,
      repo, ZenhubManager.getNextSprint(wk.getId()), wk.getId());
    sprintConfig.send();
    embedDict.put(userId + "-" + serverId, sprintConfig);
    sessions.put(userId + "-" + serverId, "InputSprint");
  }

  private static void sprintGroupAnalysisCallback(String userId, String serverId, MessageChannel canal,
    GatewayDiscordClient gateway, GHRepository repo, Sprint sprint) {
      Function<GHUser, User> userCallback = (ghus -> gateway.getUserById(Snowflake.of(
        hibernateManager.getUserFromName(ghus.getLogin()))).block());
      SprintGroupAnalysis groupAnalysis = new SprintGroupAnalysis(userId,
        serverId, canal, gateway, repo, sprint, userCallback);
      groupAnalysis.send();
      sessions.put(userId + "-" + serverId, "Start");
      canal.createMessage("Can I help you with something more?").block();
  }

  private static void sprintIndividualAnalysisCallback(String userId, String serverId, MessageChannel canal,
    GatewayDiscordClient gateway, GHRepository repo, Sprint sprint, User dcUser, GHUser ghUser) {
    SprintIndividualAnalysis indivAnalysis = new SprintIndividualAnalysis(userId,
      serverId, canal, gateway, repo, sprint, dcUser, ghUser);
    indivAnalysis.send();
    sessions.put(userId + "-" + serverId, "Start");
    canal.createMessage("Can I help you with something more?").block();
  }

  //Método que conecta con DialogFlow para obtener la intención de un mensaje
  private static String queryManager(String userId, String serverId, String query, MessageChannel canal, GatewayDiscordClient gateway) {
    if (query.length() >= 256) {
      canal.createMessage("Please introduce a message shorter than 256 characters").block();
      return "Start";
    }
    String intent = dialogManager.simpleDetect(query, userId);
    String result = "Start";    
    log.info("Detected intent: " + intent);
    Channel ch = hibernateManager.getChannel(canal.getId().asLong());
    Member mb = hibernateManager.getMember(Long.valueOf(userId));
    GHRepository repo = gitManager.getRepo(ch);
    String projectPath = hibernateManager.getServer(Long.parseLong(serverId)).getUrl() +
      "/" + ch.getUrl().split("/")[1];
    List<Sprint> sprints = ZenhubManager.getSprints(repo);
    GHUser ghUser = gitManager.getUser(mb, ch);

    switch (intent) {

        case "Help":
          canal.createMessage(EmbedManager.tutorialEmbed()).block();
          break;

        case "AddWorkspace":
          List<String> columns = Arrays.asList("New Issues", "Epics", "Icebox", "Product Backlog",
            "Sprint Backlog", "In Progress", "Review/QA", "Done");
          WorkspaceEmbed wrkpEmbed = new WorkspaceEmbed(userId, serverId, canal, gateway,
            repo, false, "Add workspace", EMPTY_FIELD, EMPTY_FIELD, columns, null);
          wrkpEmbed.send();
          embedDict.put(userId + "-" + serverId, wrkpEmbed);
          result = "InputWorkspace";
          break;

        case "ListWorkspace":
          WorkspaceList wrkpList = new WorkspaceList(userId, serverId, canal, gateway, repo,
            false, ZenhubManager.getWorkspaces(repo), ghUser, null);
          wrkpList.send();
          wrkpList.sendSecondEmbed();
          embedDict.put(userId + "-" + serverId, wrkpList);
          result = "SelectWorkspace";
          break;
        
        case "ConfigSprint":
          Consumer<Workspace> callback = (x -> workspaceSelectionCallback(x, 
            userId, serverId, canal, gateway, repo));
          WorkspaceList auxWrkpList = new WorkspaceList(userId, serverId, canal, gateway, repo,
            true, ZenhubManager.getWorkspaces(repo), null, callback);
          auxWrkpList.send();
          embedDict.put(userId + "-" + serverId, auxWrkpList);
          result = "SelectWorkspace";
          break;

        case "ListSprint":
          SprintList sprintList = new SprintList(userId, serverId, canal, gateway, repo, 
            false, sprints, null, null);
          sprintList.send();
          sprintList.sendSecondEmbed();
          embedDict.put(userId + "-" + serverId, sprintList);
          result = "SelectSprint";
          break;
          
        case "AddIssue":
          Map<String, List<Sprint>> auxDict = ZenhubManager.getIssueToSprintMap(repo);
          IssueEmbed issuEmbed = new IssueEmbed(userId, serverId, canal, gateway, repo, false, 
            "Add issue", EMPTY_FIELD, EMPTY_FIELD, null, sprints, auxDict);
          issuEmbed.send();
          embedDict.put(userId + "-" + serverId, issuEmbed);
          result = "InputIssue";
          break;

        case "AddBoard":
          BoardEmbed boardEmbed = new BoardEmbed(userId, serverId, canal, gateway, repo, 
            false, "Add board", null, projectPath);
          boardEmbed.send();
          embedDict.put(userId + "-" + serverId, boardEmbed);
          result = "InputBoard";
          break;

        case "ListBoard":
          BoardList boardList = new BoardList(userId, serverId, canal, gateway, repo, 
            false, GitlabManager.getBoards(projectPath), null, projectPath);
          boardList.send();
          embedDict.put(userId + "-" + serverId, boardList);
          result = "SelectBoard";
          break;

        case "AddGitlabIssue":
          GitlabIssueEmbed gitlabIssue = new GitlabIssueEmbed(userId, serverId, canal, gateway, 
            repo, false, projectPath, "Add issue", null);
            gitlabIssue.send();
          embedDict.put(userId + "-" + serverId, gitlabIssue);
          result = "InputGitlabIssue";
          break;

        case "Status":
          StatusEmbed statusEmbed = new StatusEmbed(userId, serverId, canal, gateway,
            repo, ghUser);
          statusEmbed.send();
          embedDict.put(userId + "-" + serverId, statusEmbed);
          result = "NoInput";
          break;

        case "GroupAnalysis":
          Consumer<Sprint> groupCallback = (x -> sprintGroupAnalysisCallback(userId,
            serverId, canal, gateway, repo, x));
          SprintList sprintAuxGroup = new SprintList(userId, serverId, canal, gateway, repo, 
            true, sprints, groupCallback, null);
          sprintAuxGroup.send();
          embedDict.put(userId + "-" + serverId, sprintAuxGroup);
          result = "SelectSprint";
          break;

        case "IndividualAnalysis":
          User dcUser = gateway.getUserById(Snowflake.of(mb.getId())).block();
          Consumer<Sprint> indivCallback = (x -> sprintIndividualAnalysisCallback(userId,
            serverId, canal, gateway, repo, x, dcUser, ghUser));
          SprintList sprintAuxIndiv = new SprintList(userId, serverId, canal, gateway, repo, 
            true, sprints, indivCallback, null);
          sprintAuxIndiv.send();
          embedDict.put(userId + "-" + serverId, sprintAuxIndiv);
          result = "SelectSprint";
          break;

        default:
          canal.createMessage("Sorry, I didn't understand you. What can I help you with?").block();
          break;
    }
    return result;
  }
}
