package com.btlbanking.transaction;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.btlbanking.transaction.client.AccountClient;
import com.btlbanking.transaction.client.FraudCheckResponse;
import com.btlbanking.transaction.client.FraudClient;
import java.math.BigDecimal;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
class TransferControllerIT {

  @Autowired
  MockMvc mockMvc;

  @MockBean
  FraudClient fraudClient;

  @MockBean
  AccountClient accountClient;

  @MockBean
  StringRedisTemplate redisTemplate;

  private ValueOperations<String, String> valueOperations;
  private final Map<String, String> redisStore = new ConcurrentHashMap<>();

  @BeforeEach
  void setUpRedisMock() {
    valueOperations = org.mockito.Mockito.mock(ValueOperations.class);
    org.mockito.Mockito.when(redisTemplate.opsForValue()).thenReturn(valueOperations);
    org.mockito.Mockito.when(valueOperations.get(org.mockito.ArgumentMatchers.any()))
        .thenAnswer(invocation -> redisStore.get(invocation.getArgument(0)));
    org.mockito.Mockito.doAnswer(invocation -> {
      redisStore.put(invocation.getArgument(0), invocation.getArgument(1));
      return null;
    }).when(valueOperations).set(org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any());
  }

  @Test
  void create_transfer_returns_final_status() throws Exception {
    org.mockito.Mockito.when(fraudClient.check(org.mockito.ArgumentMatchers.any()))
        .thenReturn(new FraudCheckResponse("APPROVED", "OK"));
    org.mockito.Mockito.doNothing().when(accountClient)
        .debit("100001", new BigDecimal("500"));
    org.mockito.Mockito.doNothing().when(accountClient)
        .credit("200001", new BigDecimal("500"));

    var result = mockMvc.perform(post("/transfers")
            .contentType(MediaType.APPLICATION_JSON)
            .header("Idempotency-Key", "key-1")
            .header("X-User-Id", "user-1")
            .content("{" +
                "\"sourceAccount\":\"100001\"," +
                "\"destinationAccount\":\"200001\"," +
                "\"amount\":500}"))
        .andExpect(status().isOk());
  }
}
