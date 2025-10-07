package kz.store.cash.handler;

import kz.store.cash.fx.component.UiNotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

@Slf4j
@Component
@ControllerAdvice
@RequiredArgsConstructor
public class GlobalExceptionHandler {

  private final UiNotificationService uiNotificationService;

  @ExceptionHandler(BusinessException.class)
  public void handleBusinessException(BusinessException e) {
    log.warn("Business error:", e);
    uiNotificationService.showBusinessError(e.getMessage());
  }

  @ExceptionHandler(ValidationException.class)
  public void handleValidationException(ValidationException e) {
    log.warn("Validation error:", e);
    uiNotificationService.showValidationError(e.getMessage());
  }

  @ExceptionHandler(Exception.class)
  public void handleSystemException(Exception e) {
    log.error("System error:", e);
    uiNotificationService.showError(e.getMessage());
  }

  @ExceptionHandler(ExternalResponseError.class)
  public void handleSystemException(ExternalResponseError e) {
    log.error("ExternalResponseError error:", e);
    uiNotificationService.showError(e.getMessage());
  }

  public void handleJavaFxException(Throwable e) {
    Throwable rootCause = unwrapCause(e);
    log.info("JavaFx exception: {}", rootCause.getClass().getName());
    switch (rootCause) {
      case BusinessException be -> handleBusinessException(be);
      case ValidationException ve -> handleValidationException(ve);
      case ExternalResponseError ex -> handleSystemException(ex);
      case Exception ex -> handleSystemException(ex);
      default -> handleSystemException(new RuntimeException(rootCause));
    }
  }

  private Throwable unwrapCause(Throwable e) {
    if (e instanceof java.lang.reflect.InvocationTargetException ite && ite.getCause() != null) {
      return unwrapCause(ite.getCause()); // рекурсивно достаем корень
    }
    if (e.getCause() != null && e != e.getCause()) {
      return unwrapCause(e.getCause());
    }
    return e;
  }
}
