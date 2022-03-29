package demo;

import org.kohsuke.github.GHAppInstallation;
import org.kohsuke.github.GHAppInstallationToken;
import org.kohsuke.github.GHIssue;
import org.kohsuke.github.GitHub;
import org.kohsuke.github.GitHubBuilder;

import tfg.JWTEncoder;
import tfg.Secrets;

public final class GitHubDemo {
    private static final String APP_ID = Secrets.GITHUB_APP_ID;
    
    public static void main(final String[] args) {
        try {
            String jwt = JWTEncoder.createJWT(APP_ID, 3600);
            GitHub appConnection = new GitHubBuilder().withJwtToken(jwt).build();
            //github.getApp().listInstallations().forEach(x -> System.out.println(x.getId()));
            long installationID = appConnection.getApp().getInstallationByRepository("Antonio-Gonzalez-Gomez","mvg-blender").getId();
            System.out.println(installationID);
            GHAppInstallation appInstallation = appConnection.getApp().getInstallationById(installationID);
            GHAppInstallationToken appInstallationToken = appInstallation.createToken().create();
            GitHub repoConnection = GitHub.connect(APP_ID, appInstallationToken.getToken());
            GHIssue issue = repoConnection.getRepository("Antonio-Gonzalez-Gomez/mvg-blender").getIssue(4);
            System.out.println(issue.getTitle());
            System.out.println(issue.getBody());
        } catch (Exception e) {
            System.out.println(e.getMessage());
        }
    }
}
