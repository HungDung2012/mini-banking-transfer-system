package com.btlbanking.fraud;

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
class FraudControllerIT {

  @Autowired
  MockMvc mockMvc;

  @Test
  void large_transfer_is_rejected() throws Exception {
    mockMvc.perform(post("/fraud/check")
            .contentType(MediaType.APPLICATION_JSON)
            .content("""
                {"sourceAccount":"100001","destinationAccount":"200001","amount":150000000}
                """))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.decision").value("REJECTED"));
  }

  @Test
  void invalid_payload_is_rejected_with_bad_request() throws Exception {
    mockMvc.perform(post("/fraud/check")
            .contentType(MediaType.APPLICATION_JSON)
            .content("""
                {"destinationAccount":"200001","amount":1000}
                """))
        .andExpect(status().isBadRequest());
  }

  @Test
  void small_transfer_is_approved() throws Exception {
    mockMvc.perform(post("/fraud/check")
            .contentType(MediaType.APPLICATION_JSON)
            .content("""
                {"sourceAccount":"100001","destinationAccount":"200001","amount":5000}
                """))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.decision").value("APPROVED"))
        .andExpect(jsonPath("$.reason").value("OK"));
  }
}