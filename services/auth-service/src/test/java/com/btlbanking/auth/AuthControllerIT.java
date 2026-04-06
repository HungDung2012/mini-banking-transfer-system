package com.btlbanking.auth;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.btlbanking.auth.service.KongProvisioningException;
import com.btlbanking.auth.service.KongProvisioningService;
import com.btlbanking.auth.user.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;

@SpringBootTest
@AutoConfigureMockMvc
class AuthControllerIT {

  @Autowired
  MockMvc mockMvc;

  @Autowired
  UserRepository userRepository;

  @MockBean
  KongProvisioningService kongProvisioningService;

  @BeforeEach
  void cleanDatabase() {
    userRepository.deleteAll();
  }

  @Test
  void register_then_login_returns_jwt() throws Exception {
    mockMvc.perform(post("/auth/register")
            .contentType(MediaType.APPLICATION_JSON)
            .content("""
                {"username":"alice","password":"secret123"}
                """))
        .andExpect(status().isCreated());

    verify(kongProvisioningService).ensureConsumer("alice");

    mockMvc.perform(post("/auth/login")
            .contentType(MediaType.APPLICATION_JSON)
            .content("""
                {"username":"alice","password":"secret123"}
                """))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.token").isNotEmpty());
  }

  @Test
  void duplicate_registration_returns_conflict() throws Exception {
    register("alice", "secret123");

    mockMvc.perform(post("/auth/register")
            .contentType(MediaType.APPLICATION_JSON)
            .content("""
                {"username":"alice","password":"secret123"}
                """))
        .andExpect(status().isConflict());
  }

  @Test
  void login_with_bad_password_returns_unauthorized() throws Exception {
    register("alice", "secret123");

    mockMvc.perform(post("/auth/login")
            .contentType(MediaType.APPLICATION_JSON)
            .content("""
                {"username":"alice","password":"wrongpass"}
                """))
        .andExpect(status().isUnauthorized());
  }

  @Test
  void invalid_registration_payload_returns_bad_request() throws Exception {
    mockMvc.perform(post("/auth/register")
            .contentType(MediaType.APPLICATION_JSON)
            .content("""
                {"username":"","password":""}
                """))
        .andExpect(status().isBadRequest());
  }

  @Test
  void registration_returns_service_unavailable_and_rolls_back_when_kong_provisioning_fails() throws Exception {
    doThrow(new KongProvisioningException("Kong unavailable"))
        .when(kongProvisioningService).ensureConsumer("alice");

    mockMvc.perform(post("/auth/register")
            .contentType(MediaType.APPLICATION_JSON)
            .content("""
                {"username":"alice","password":"secret123"}
                """))
        .andExpect(status().isServiceUnavailable());

    org.assertj.core.api.Assertions.assertThat(userRepository.findByUsername("alice")).isEmpty();
  }

  private void register(String username, String password) throws Exception {
    mockMvc.perform(post("/auth/register")
            .contentType(MediaType.APPLICATION_JSON)
            .content("""
                {"username":"%s","password":"%s"}
                """.formatted(username, password)))
        .andExpect(status().isCreated());
  }
}
