package org.exoplatform.appcenter.service;

public class ApplicationNotFoundException extends Exception {

  private static final long serialVersionUID = 5357136264162068261L;

  public ApplicationNotFoundException() {
  }

  public ApplicationNotFoundException(String message) {
    super(message);
  }

  public ApplicationNotFoundException(String message, Throwable e) {
    super(message, e);
  }

}
