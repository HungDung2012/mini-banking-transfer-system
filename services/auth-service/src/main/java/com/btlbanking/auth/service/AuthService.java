package com.btlbanking.auth.service;

import com.btlbanking.auth.user.UserEntity;
import com.btlbanking.auth.user.UserRepository;
import com.btlbanking.auth.web.AuthRequest;
import com.btlbanking.auth.web.AuthResponse;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.observation.annotation.Observed;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Service
@Transactional
public class AuthService {
  private static final Logger log = LoggerFactory.getLogger(AuthService.class);

  private final UserRepository userRepository;
  private final PasswordEncoder passwordEncoder;
  private final JwtService jwtService;
  private final KongProvisioningService kongProvisioningService;
  private final MeterRegistry meterRegistry;

  public AuthService(UserRepository userRepository, PasswordEncoder passwordEncoder,
      JwtService jwtService, KongProvisioningService kongProvisioningService,
      MeterRegistry meterRegistry) {
    this.userRepository = userRepository;
    this.passwordEncoder = passwordEncoder;
    this.jwtService = jwtService;
    this.kongProvisioningService = kongProvisioningService;
    this.meterRegistry = meterRegistry;
  }

  @Observed(name = "banking.auth.register", contextualName = "auth-register")
  public AuthResponse register(AuthRequest request) {
    log.info("Processing register request for username={}", request.username());
    if (userRepository.existsByUsername(request.username())) {
      meterRegistry.counter("banking.auth.register.failures", "reason", "duplicate-username").increment();
      throw new DuplicateUsernameException(request.username());
    }

    var user = new UserEntity();
    user.setUsername(request.username());
    user.setPasswordHash(passwordEncoder.encode(request.password()));

    try {
      var savedUser = userRepository.save(user);
      kongProvisioningService.ensureConsumer(savedUser.getUsername());
      meterRegistry.counter("banking.auth.register.success").increment();
      log.info("Registered user username={} userId={}", savedUser.getUsername(), savedUser.getId());
      return new AuthResponse(jwtService.generateToken(savedUser.getUsername(), savedUser.getId()));
    } catch (DataIntegrityViolationException ex) {
      meterRegistry.counter("banking.auth.register.failures", "reason", "data-integrity").increment();
      throw new DuplicateUsernameException(request.username(), ex);
    }
  }

  @Observed(name = "banking.auth.login", contextualName = "auth-login")
  public AuthResponse login(AuthRequest request) {
    log.info("Processing login request for username={}", request.username());
    var user = userRepository.findByUsername(request.username())
        .orElseThrow(() -> new BadCredentialsException("Invalid credentials"));
    if (!passwordEncoder.matches(request.password(), user.getPasswordHash())) {
      meterRegistry.counter("banking.auth.login.failures", "reason", "bad-credentials").increment();
      throw new BadCredentialsException("Invalid credentials");
    }
    kongProvisioningService.ensureConsumer(user.getUsername());
    meterRegistry.counter("banking.auth.login.success").increment();
    log.info("Authenticated user username={} userId={}", user.getUsername(), user.getId());
    return new AuthResponse(jwtService.generateToken(user.getUsername(), user.getId()));
  }
}
