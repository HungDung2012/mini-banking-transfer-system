package com.btlbanking.auth.service;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Base64;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class JwtService {

  private static final String HEADER_JSON = "{\"alg\":\"HS256\",\"typ\":\"JWT\"}";

  private final String secret;

  public JwtService(@Value("${auth.jwt.secret}") String secret) {
    this.secret = secret;
  }

  public String generateToken(String username, Long userId) {
    var issuedAt = Instant.now().getEpochSecond();
    var expiresAt = issuedAt + 3600;
    var payloadJson = "{\"sub\":\"" + escape(username) + "\",\"uid\":" + userId
        + ",\"iat\":" + issuedAt + ",\"exp\":" + expiresAt + "}";
    var header = base64Url(HEADER_JSON.getBytes(StandardCharsets.UTF_8));
    var payload = base64Url(payloadJson.getBytes(StandardCharsets.UTF_8));
    var signature = sign(header + "." + payload);
    return header + "." + payload + "." + signature;
  }

  private String sign(String data) {
    try {
      var mac = Mac.getInstance("HmacSHA256");
      mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
      return base64Url(mac.doFinal(data.getBytes(StandardCharsets.UTF_8)));
    } catch (Exception ex) {
      throw new IllegalStateException("Unable to sign JWT", ex);
    }
  }

  private static String base64Url(byte[] bytes) {
    return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
  }

  private static String escape(String value) {
    return value.replace("\\", "\\\\").replace("\"", "\\\"");
  }
}