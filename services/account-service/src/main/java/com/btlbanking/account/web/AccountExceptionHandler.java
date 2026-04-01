package com.btlbanking.account.web;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class AccountExceptionHandler {

  @ExceptionHandler({
      DataIntegrityViolationException.class,
      ObjectOptimisticLockingFailureException.class
  })
  public ProblemDetail handleConflict(Exception exception) {
    return ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT,
        exception.getMessage() == null ? "Conflict" : exception.getMessage());
  }
}
