package com.btlbanking.auth.service;

import com.btlbanking.auth.user.UserEntity;
import com.btlbanking.auth.user.UserRepository;
import com.btlbanking.auth.web.AuthRequest;
import com.btlbanking.auth.web.AuthResponse;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class AuthService {

  private final UserRepository userRepository;
  private final PasswordEncoder passwordEncoder;
  private final JwtService jwtService;
  private final KongProvisioningService kongProvisioningService;

  public AuthService(UserRepository userRepository, PasswordEncoder passwordEncoder,
      JwtService jwtService, KongProvisioningService kongProvisioningService) {
    this.userRepository = userRepository;
    this.passwordEncoder = passwordEncoder;
    this.jwtService = jwtService;
    this.kongProvisioningService = kongProvisioningService;
  }

  public AuthResponse register(AuthRequest request) {
    if (userRepository.existsByUsername(request.username())) {
      throw new DuplicateUsernameException(request.username());
    }

    var user = new UserEntity();
    user.setUsername(request.username());
    user.setPasswordHash(passwordEncoder.encode(request.password()));

    try {
      var savedUser = userRepository.save(user);
      kongProvisioningService.ensureConsumer(savedUser.getUsername());
      return new AuthResponse(jwtService.generateToken(savedUser.getUsername(), savedUser.getId()));
    } catch (DataIntegrityViolationException ex) {
      throw new DuplicateUsernameException(request.username(), ex);
    }
  }

  public AuthResponse login(AuthRequest request) {
    var user = userRepository.findByUsername(request.username())
        .orElseThrow(() -> new BadCredentialsException("Invalid credentials"));
    if (!passwordEncoder.matches(request.password(), user.getPasswordHash())) {
      throw new BadCredentialsException("Invalid credentials");
    }
    kongProvisioningService.ensureConsumer(user.getUsername());
    return new AuthResponse(jwtService.generateToken(user.getUsername(), user.getId()));
  }
}
