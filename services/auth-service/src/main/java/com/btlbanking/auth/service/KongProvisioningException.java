package com.btlbanking.auth.service;

public class KongProvisioningException extends RuntimeException {

  public KongProvisioningException(String message) {
    super(message);
  }

  public KongProvisioningException(String message, Throwable cause) {
    super(message, cause);
  }
}
