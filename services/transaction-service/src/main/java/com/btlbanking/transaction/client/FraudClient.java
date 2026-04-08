package com.btlbanking.transaction.client;

import com.btlbanking.transaction.web.CreateTransferRequest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Component
public class FraudClient {

  private final RestClient restClient;

  public FraudClient(RestClient.Builder restClientBuilder,
      @Value("${integration.fraud-service.base-url:http://localhost:8083}") String baseUrl) {
    this.restClient = restClientBuilder
        .baseUrl(baseUrl)
        .build();
  }

  public FraudCheckResponse check(CreateTransferRequest request) {
    return restClient.post()
        .uri("/fraud/check")
        .contentType(MediaType.APPLICATION_JSON)
        .body(new FraudCheckRequest(
            request.sourceAccount(),
            request.destinationAccount(),
            request.amount()))
        .retrieve()
        .body(FraudCheckResponse.class);
  }
}
