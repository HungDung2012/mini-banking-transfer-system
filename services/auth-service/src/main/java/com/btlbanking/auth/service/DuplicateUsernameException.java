package com.btlbanking.auth.service;

public class DuplicateUsernameException extends RuntimeException {

  public DuplicateUsernameException(String username) {
    super("Username already exists: " + username);
  }

  public DuplicateUsernameException(String username, Throwable cause) {
    super("Username already exists: " + username, cause);
  }
}