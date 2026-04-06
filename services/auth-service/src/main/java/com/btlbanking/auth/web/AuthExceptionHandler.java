package com.btlbanking.auth.web;

import com.btlbanking.auth.service.DuplicateUsernameException;
import com.btlbanking.auth.service.KongProvisioningException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class AuthExceptionHandler {

  @ExceptionHandler(DuplicateUsernameException.class)
  public ResponseEntity<Void> handleDuplicateUsername(DuplicateUsernameException ex) {
    return ResponseEntity.status(HttpStatus.CONFLICT).build();
  }

  @ExceptionHandler(BadCredentialsException.class)
  public ResponseEntity<Void> handleBadCredentials(BadCredentialsException ex) {
    return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
  }

  @ExceptionHandler({DataIntegrityViolationException.class})
  public ResponseEntity<Void> handleDataIntegrityViolation(DataIntegrityViolationException ex) {
    return ResponseEntity.status(HttpStatus.CONFLICT).build();
  }

  @ExceptionHandler(MethodArgumentNotValidException.class)
  public ResponseEntity<Void> handleValidation(MethodArgumentNotValidException ex) {
    return ResponseEntity.badRequest().build();
  }

  @ExceptionHandler(KongProvisioningException.class)
  public ResponseEntity<Void> handleKongProvisioning(KongProvisioningException ex) {
    return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).build();
  }
}
