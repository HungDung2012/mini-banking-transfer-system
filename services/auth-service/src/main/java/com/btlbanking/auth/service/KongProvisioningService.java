package com.btlbanking.auth.service;

import java.util.Map;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

@Service
public class KongProvisioningService {

  private final RestClient restClient;
  private final String jwtSecret;

  public KongProvisioningService(
      RestClient.Builder restClientBuilder,
      @Value("${integration.kong-admin.base-url}") String kongAdminBaseUrl,
      @Value("${auth.jwt.secret}") String jwtSecret) {
    this.restClient = restClientBuilder.baseUrl(kongAdminBaseUrl).build();
    this.jwtSecret = jwtSecret;
  }

  public void ensureConsumer(String username) {
    createConsumerIfMissing(username);
    createJwtSecretIfMissing(username);
  }

  private void createConsumerIfMissing(String username) {
    try {
      restClient.post()
          .uri("/consumers")
          .body(Map.of("username", username))
          .retrieve()
          .toBodilessEntity();
    } catch (HttpClientErrorException ex) {
      if (ex.getStatusCode() != HttpStatus.CONFLICT && ex.getStatusCode() != HttpStatus.BAD_REQUEST) {
        throw new KongProvisioningException("Unable to provision Kong consumer", ex);
      }
    } catch (RestClientException ex) {
      throw new KongProvisioningException("Unable to provision Kong consumer", ex);
    }
  }

  private void createJwtSecretIfMissing(String username) {
    try {
      restClient.post()
          .uri("/consumers/{username}/jwt", username)
          .body(Map.of(
              "algorithm", "HS256",
              "key", username,
              "secret", jwtSecret))
          .retrieve()
          .toBodilessEntity();
    } catch (HttpClientErrorException ex) {
      if (ex.getStatusCode() != HttpStatus.CONFLICT && ex.getStatusCode() != HttpStatus.BAD_REQUEST) {
        throw new KongProvisioningException("Unable to provision Kong JWT secret", ex);
      }
    } catch (RestClientException ex) {
      throw new KongProvisioningException("Unable to provision Kong JWT secret", ex);
    }
  }
}
