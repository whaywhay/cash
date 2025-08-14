package kz.store.cash.handler;

public class ExternalServerException extends RuntimeException {

  public ExternalServerException(String message) {
    super("External server ran into trouble: " + message);
  }

}
