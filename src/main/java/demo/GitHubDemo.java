package demo;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;
import org.kohsuke.github.GHAppInstallation;
import org.kohsuke.github.GHAppInstallationToken;
import org.kohsuke.github.GHIssue;
import org.kohsuke.github.GitHub;
import org.kohsuke.github.GitHubBuilder;

import managers.GithubManager;
import managers.Secrets;

//Demo para la API de Github
public final class GitHubDemo {
    private static final String APP_ID = Secrets.GITHUB_APP_ID;
    private static final Logger log = LogManager.getLogger();
    
    public static void main(final String[] args) {
        log.debug("Launching Github demo");
        try {
            String jwt = GithubManager.createJWT(APP_ID, 10000);
            GitHub appConnection = new GitHubBuilder().withJwtToken(jwt).build();
            
            long installationID = appConnection.getApp().getInstallationByRepository("Antonio-Gonzalez-Gomez","mvg-blender").getId();
            GHAppInstallation appInstallation = appConnection.getApp().getInstallationById(installationID);
            GHAppInstallationToken appInstallationToken = appInstallation.createToken().create();
            GitHub repoConnection = GitHub.connect(APP_ID, appInstallationToken.getToken());

            GHIssue issue = repoConnection.getRepository("Antonio-Gonzalez-Gomez/mvg-blender").getIssue(4);
            log.info("Issue title: " + issue.getTitle());
            log.info("Issue body: " + issue.getBody());
        } catch (Exception e) {
            log.error(e.getMessage());
        }
    }
}
