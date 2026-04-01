package com.btlbanking.transaction;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.jayway.jsonpath.JsonPath;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
class TransferControllerIT {

  @Autowired
  MockMvc mockMvc;

  @Test
  void create_transfer_persists_and_get_by_id_returns_status() throws Exception {
    var result = mockMvc.perform(post("/transfers")
            .contentType(MediaType.APPLICATION_JSON)
            .header("Idempotency-Key", "key-1")
            .header("X-User-Id", "user-1")
            .content("{" +
                "\"sourceAccount\":\"100001\"," +
                "\"destinationAccount\":\"200001\"," +
                "\"amount\":500}"))
        .andExpect(status().isOk())
        .andReturn();

    var transferId = JsonPath.read(result.getResponse().getContentAsString(), "$.transferId");
    mockMvc.perform(get("/transfers/{id}", transferId))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.status").exists());
  }

  @Test
  void get_missing_transfer_returns_404() throws Exception {
    mockMvc.perform(get("/transfers/{id}", UUID.randomUUID()))
        .andExpect(status().isNotFound());
  }
}
