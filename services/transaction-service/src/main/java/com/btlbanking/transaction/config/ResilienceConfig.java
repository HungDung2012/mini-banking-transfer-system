package com.btlbanking.transaction.config;

import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import java.time.Duration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ResilienceConfig {

  @Bean
  public CircuitBreakerRegistry circuitBreakerRegistry() {
    CircuitBreakerConfig config = CircuitBreakerConfig.custom()
        .slidingWindowSize(5)
        .minimumNumberOfCalls(3)
        .failureRateThreshold(50f)
        .waitDurationInOpenState(Duration.ofSeconds(30))
        .permittedNumberOfCallsInHalfOpenState(2)
        .recordExceptions(RuntimeException.class)
        .build();
    return CircuitBreakerRegistry.of(config);
  }

  @Bean
  public CircuitBreaker fraudCircuitBreaker(CircuitBreakerRegistry registry) {
    return registry.circuitBreaker("fraud-client");
  }

  @Bean
  public CircuitBreaker accountCircuitBreaker(CircuitBreakerRegistry registry) {
    return registry.circuitBreaker("account-client");
  }
}
