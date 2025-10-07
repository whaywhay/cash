package kz.store.cash.handler;

public class ExternalResponseError extends RuntimeException {

  public ExternalResponseError(String message) {
    super(message);
  }
}
