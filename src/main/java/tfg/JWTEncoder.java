package tfg;

import java.io.File;
import java.security.Key;
import java.security.KeyFactory;
import java.security.PrivateKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.util.Date;

import com.google.common.io.Files;

import io.jsonwebtoken.JwtBuilder;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;

public final class JWTEncoder {
  private static String RSA_KEY = Secrets.GITHUB_RSA_KEY; 
   
   static PrivateKey get(String filename) throws Exception {
      byte[] keyBytes = Files.toByteArray(new File(filename));
    
      PKCS8EncodedKeySpec spec = new PKCS8EncodedKeySpec(keyBytes);
      KeyFactory kf = KeyFactory.getInstance("RSA");
      return kf.generatePrivate(spec);
    }
    
    public static String createJWT(String githubAppId, long ttlMillis) throws Exception {
      SignatureAlgorithm signatureAlgorithm = SignatureAlgorithm.RS256;
    
      long nowMillis = System.currentTimeMillis();
      Date now = new Date(nowMillis);
      Key signingKey = get(RSA_KEY);
    
      JwtBuilder builder = Jwts.builder()
              .setIssuedAt(now)
              .setIssuer(githubAppId)
              .signWith(signingKey, signatureAlgorithm);
    
      if (ttlMillis > 0) {
        long expMillis = nowMillis + ttlMillis;
        Date exp = new Date(expMillis);
        builder.setExpiration(exp);
      }
      return builder.compact();
    }

}
