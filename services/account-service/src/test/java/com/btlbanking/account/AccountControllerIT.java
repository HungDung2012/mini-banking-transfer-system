package com.btlbanking.account;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.btlbanking.account.web.AccountResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
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

  @Autowired
  ObjectMapper objectMapper;

  @BeforeEach
  void cleanDatabase() {
    accountRepository.deleteAll();
  }

  @Test
  void debit_then_compensate_updates_balance() throws Exception {
    AccountResponse account = createAccount("Alice");

    mockMvc.perform(post("/accounts/debit")
            .contentType(MediaType.APPLICATION_JSON)
            .content("""
                {"accountNumber":"%s","amount":200}
                """.formatted(account.accountNumber())))
        .andExpect(status().isOk());

    mockMvc.perform(post("/accounts/compensate")
            .contentType(MediaType.APPLICATION_JSON)
            .content("""
                {"accountNumber":"%s","amount":200}
                """.formatted(account.accountNumber())))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.balance").value(1000000));
  }

  @Test
  void debit_rejects_when_it_would_overdraw_the_account() throws Exception {
    AccountResponse account = createAccount("Alice");

    mockMvc.perform(post("/accounts/debit")
            .contentType(MediaType.APPLICATION_JSON)
            .content("""
                {"accountNumber":"%s","amount":1000001}
                """.formatted(account.accountNumber())))
        .andExpect(status().isConflict());
  }

  @Test
  void get_returns_the_requested_account_by_account_number() throws Exception {
    AccountResponse account = createAccount("Alice");

    mockMvc.perform(get("/accounts/{accountNumber}", account.accountNumber()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.accountNumber").value(account.accountNumber()))
        .andExpect(jsonPath("$.ownerName").value("Alice"))
        .andExpect(jsonPath("$.balance").value(1000000));
  }

  @Test
  void get_by_owner_returns_the_requested_account() throws Exception {
    AccountResponse account = createAccount("charlie");

    mockMvc.perform(get("/accounts/by-owner/charlie"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.accountNumber").value(account.accountNumber()))
        .andExpect(jsonPath("$.ownerName").value("charlie"))
        .andExpect(jsonPath("$.balance").value(1000000));
  }

  @Test
  void concurrent_account_creation_generates_distinct_account_numbers() throws Exception {
    ExecutorService executor = Executors.newFixedThreadPool(2);
    CountDownLatch startGate = new CountDownLatch(1);

    try {
      List<Future<AccountResponse>> responses = new ArrayList<>();
      for (String ownerName : List.of("Bob", "Carol")) {
        Callable<AccountResponse> task = () -> {
          startGate.await();
          String responseBody = mockMvc.perform(post("/accounts")
                  .contentType(MediaType.APPLICATION_JSON)
                  .content("""
                      {"ownerName":"%s"}
                      """.formatted(ownerName)))
              .andReturn()
              .getResponse()
              .getContentAsString();
          return objectMapper.readValue(responseBody, AccountResponse.class);
        };
        responses.add(executor.submit(task));
      }

      startGate.countDown();

      List<AccountResponse> createdAccounts = new ArrayList<>();
      for (Future<AccountResponse> response : responses) {
        createdAccounts.add(response.get());
      }

      assertThat(createdAccounts)
          .extracting(AccountResponse::accountNumber)
          .doesNotHaveDuplicates();
    } finally {
      executor.shutdownNow();
    }
  }

  @Test
  void create_generates_a_six_digit_account_number_with_opening_balance() throws Exception {
    mockMvc.perform(post("/accounts")
            .contentType(MediaType.APPLICATION_JSON)
            .content("""
                {"ownerName":"david"}
                """))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.accountNumber").isString())
        .andExpect(jsonPath("$.accountNumber").value(org.hamcrest.Matchers.matchesPattern("\\d{6}")))
        .andExpect(jsonPath("$.ownerName").value("david"))
        .andExpect(jsonPath("$.balance").value(1000000));
  }

  private AccountResponse createAccount(String ownerName) throws Exception {
    String responseBody = mockMvc.perform(post("/accounts")
            .contentType(MediaType.APPLICATION_JSON)
            .content("""
                {"ownerName":"%s"}
                """.formatted(ownerName)))
        .andExpect(status().isCreated())
        .andReturn()
        .getResponse()
        .getContentAsString();

    return objectMapper.readValue(responseBody, AccountResponse.class);
  }
}
