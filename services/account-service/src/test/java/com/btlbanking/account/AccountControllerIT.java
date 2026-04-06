package com.btlbanking.account;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.btlbanking.account.domain.AccountRepository;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
class AccountControllerIT {

  @Autowired
  MockMvc mockMvc;

  @Autowired
  AccountRepository accountRepository;

  @BeforeEach
  void cleanDatabase() {
    accountRepository.deleteAll();
  }

  @Test
  void debit_then_compensate_updates_balance() throws Exception {
    mockMvc.perform(post("/accounts")
            .contentType(MediaType.APPLICATION_JSON)
            .content("""
                {"accountNumber":"100001","ownerName":"Alice","balance":1000}
                """))
        .andExpect(status().isCreated());

    mockMvc.perform(post("/accounts/debit")
            .contentType(MediaType.APPLICATION_JSON)
            .content("""
                {"accountNumber":"100001","amount":200}
                """))
        .andExpect(status().isOk());

    mockMvc.perform(post("/accounts/compensate")
            .contentType(MediaType.APPLICATION_JSON)
            .content("""
                {"accountNumber":"100001","amount":200}
                """))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.balance").value(1000));
  }

  @Test
  void debit_rejects_when_it_would_overdraw_the_account() throws Exception {
    mockMvc.perform(post("/accounts")
            .contentType(MediaType.APPLICATION_JSON)
            .content("""
                {"accountNumber":"100001","ownerName":"Alice","balance":100}
                """))
        .andExpect(status().isCreated());

    mockMvc.perform(post("/accounts/debit")
            .contentType(MediaType.APPLICATION_JSON)
            .content("""
                {"accountNumber":"100001","amount":200}
                """))
        .andExpect(status().isConflict());
  }

  @Test
  void get_returns_the_requested_account_by_account_number() throws Exception {
    mockMvc.perform(post("/accounts")
            .contentType(MediaType.APPLICATION_JSON)
            .content("""
                {"accountNumber":"100001","ownerName":"Alice","balance":1000}
                """))
        .andExpect(status().isCreated());

    mockMvc.perform(get("/accounts/100001"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.accountNumber").value("100001"))
        .andExpect(jsonPath("$.ownerName").value("Alice"))
        .andExpect(jsonPath("$.balance").value(1000));
  }

  @Test
  void duplicate_account_creation_returns_conflict_even_when_requests_race() throws Exception {
    ExecutorService executor = Executors.newFixedThreadPool(2);
    CountDownLatch startGate = new CountDownLatch(1);

    try {
      List<Future<Integer>> statuses = new ArrayList<>();
      for (int i = 0; i < 2; i++) {
        Callable<Integer> task = () -> {
          startGate.await();
          return mockMvc.perform(post("/accounts")
                  .contentType(MediaType.APPLICATION_JSON)
                  .content("""
                      {"accountNumber":"200001","ownerName":"Bob","balance":500}
                      """))
              .andReturn()
              .getResponse()
              .getStatus();
        };
        statuses.add(executor.submit(task));
      }

      startGate.countDown();

      List<Integer> results = new ArrayList<>();
      for (Future<Integer> status : statuses) {
        results.add(status.get());
      }

      assertThat(results).contains(201, 409);
    } finally {
      executor.shutdownNow();
    }
  }
}
