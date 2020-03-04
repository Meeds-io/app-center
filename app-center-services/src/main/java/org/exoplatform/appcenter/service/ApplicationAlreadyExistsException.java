package org.exoplatform.appcenter.service;

public class ApplicationAlreadyExistsException extends Exception {

  private static final long serialVersionUID = 5357136264162068261L;

  public ApplicationAlreadyExistsException() {
  }

  public ApplicationAlreadyExistsException(String message) {
    super(message);
  }

  public ApplicationAlreadyExistsException(String message, Throwable e) {
    super(message, e);
  }

}
