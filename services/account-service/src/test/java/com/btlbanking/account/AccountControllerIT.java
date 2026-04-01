package com.btlbanking.account;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

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

  @Test
  void debit_then_credit_then_compensate_updates_balance() throws Exception {
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
}
