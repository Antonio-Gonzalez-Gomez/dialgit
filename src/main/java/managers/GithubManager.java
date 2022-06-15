package managers;

import java.io.File;
import java.io.IOException;
import java.security.Key;
import java.security.KeyFactory;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.PKCS8EncodedKeySpec;
import java.util.Date;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;
import com.google.common.io.Files;

import org.kohsuke.github.GHAppInstallation;
import org.kohsuke.github.GHAppInstallationToken;
import org.kohsuke.github.GHRepository;
import org.kohsuke.github.GitHub;
import org.kohsuke.github.GitHubBuilder;

import io.jsonwebtoken.JwtBuilder;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import model.Server;

public class GithubManager {
    private static final String APP_ID = Secrets.GITHUB_APP_ID;
    private static final String RSA_KEY = Secrets.GITHUB_RSA_KEY;
    private static final Logger log = LogManager.getLogger();
    private GitHub connector;
    private Long timestamp;

    public GithubManager() {
        log.debug("Creating Github manager");
        this.timestamp = 0l;
        updateToken();
    }

    private void updateToken() {
        if (timestamp + 60000 < System.currentTimeMillis())
            try {
                log.debug("Updating Github JWT");
                String jwt = createJWT(APP_ID, 540000); //9 minutos de caducidad
                this.connector = new GitHubBuilder().withJwtToken(jwt).build();
                this.timestamp = System.currentTimeMillis();
            } catch (Exception e) {
                log.error(e.getMessage());
            }
    }

    public GHRepository getRepo(Server server) {
        try {
            updateToken();
            GHAppInstallation appInstallation = connector.getApp().getInstallationById(server.getInstallation());
            GHAppInstallationToken appInstallationToken = appInstallation.createToken().create();
            GitHub repoConnection = GitHub.connect(APP_ID, appInstallationToken.getToken());
            return repoConnection.getRepository(server.getUrl());
        } catch (IOException e) {
            log.error(e.getMessage());
            return null;
        }
    }

    public int getInstallationId(String author, String repoName) {
        try {
            updateToken();
            return (int) connector.getApp().getInstallationByRepository(author, repoName).getId();
        } catch (IOException e) {
            log.error(e.getMessage());
            return 0;
        }
    }

    public Boolean checkRepositoryExists(String author, String repoName) {
        try {
            updateToken();
            GHRepository repo = connector.getRepository(author + "/" + repoName);
            return repo != null;
        } catch (IOException e) {
            log.error(e.getMessage());
            return false;
        }
    }

    //Original code: https://developer.okta.com/blog/2018/10/31/jwts-with-java
    private static PrivateKey getPrivateKey(String filename) throws IOException, NoSuchAlgorithmException, InvalidKeySpecException  {
      byte[] keyBytes = Files.toByteArray(new File(filename));
    
      PKCS8EncodedKeySpec spec = new PKCS8EncodedKeySpec(keyBytes);
      KeyFactory kf = KeyFactory.getInstance("RSA");
      return kf.generatePrivate(spec);
    }
    
    public static String createJWT(String githubAppId, long ttlMillis) {
      try {
        SignatureAlgorithm signatureAlgorithm = SignatureAlgorithm.RS256;
        long nowMillis = System.currentTimeMillis();
        Date now = new Date(nowMillis);
        long expMillis = nowMillis + ttlMillis;
        Date exp = new Date(expMillis);
        Key signingKey = getPrivateKey(RSA_KEY);

        JwtBuilder builder = Jwts.builder()
              .setIssuedAt(now)
              .setIssuer(githubAppId)
              .setExpiration(exp)
              .signWith(signingKey, signatureAlgorithm);
    
        return builder.compact();
      } catch (IOException | NoSuchAlgorithmException | InvalidKeySpecException ex) {
        log.error(ex.getMessage());
        return "";
      }
    }
}
