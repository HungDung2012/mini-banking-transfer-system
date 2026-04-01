package com.btlbanking.transaction.service;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.NOT_FOUND)
public class TransferNotFoundException extends RuntimeException {

  public TransferNotFoundException(String message) {
    super(message);
  }
}
