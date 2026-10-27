package kz.store.cash.handler;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.util.concurrent.ExecutionException;
import kz.store.cash.config.ApiClientsForRestConfig.UnauthorizedException;
import kz.store.cash.fx.component.UiNotificationService;
import org.junit.jupiter.api.Test;

class GlobalExceptionHandlerTest {

  UiNotificationService ui = mock(UiNotificationService.class);
  GlobalExceptionHandler handler = new GlobalExceptionHandler(ui);

  // ---------- прямые обработчики ----------

  @Test void businessErrorGoesToBusinessChannelWithOriginalMessage() {
    handler.handleBusinessException(new BusinessException("Нет открытой смены"));

    verify(ui).showBusinessError("Нет открытой смены");
    verifyNoMoreInteractions(ui);
  }

  @Test void validationErrorGoesToValidationChannel() {
    handler.handleValidationException(new ValidationException("Не заполнен адрес"));

    verify(ui).showValidationError("Не заполнен адрес");
    verifyNoMoreInteractions(ui);
  }

  @Test void systemAndExternalErrorsGoToGenericErrorChannel() {
    handler.handleSystemException(new IllegalStateException("boom"));
    handler.handleSystemException(new ExternalResponseError("Token missing"));

    verify(ui).showError("boom");
    verify(ui).showError("Token missing");
    verify(ui, never()).showBusinessError(anyString());
    verify(ui, never()).showValidationError(anyString());
  }

  // ---------- маршрутизация исключений JavaFX ----------

  @Test void javaFxBusinessExceptionIsRoutedToBusinessChannel() {
    handler.handleJavaFxException(new BusinessException("msg"));

    verify(ui).showBusinessError("msg");
    verifyNoMoreInteractions(ui);
  }

  @Test void javaFxValidationExceptionIsRoutedToValidationChannel() {
    handler.handleJavaFxException(new ValidationException("msg"));

    verify(ui).showValidationError("msg");
    verifyNoMoreInteractions(ui);
  }

  @Test void javaFxExternalResponseErrorIsRoutedToGenericErrorChannel() {
    handler.handleJavaFxException(new ExternalResponseError("bad token"));

    verify(ui).showError("bad token");
    verifyNoMoreInteractions(ui);
  }

  @Test void externalClientAndServerErrorsAreShownWithTheirDetails() {
    handler.handleJavaFxException(new ExternalClientException("Client error 422 body=x"));
    handler.handleJavaFxException(new ExternalServerException("502 down"));
    handler.handleJavaFxException(new UnauthorizedException("Auth required 401"));

    verify(ui).showError("Client error 422 body=x");
    verify(ui).showError("External server ran into trouble: 502 down");
    verify(ui).showError("Auth required 401");
  }

  @Test void businessExceptionWrappedByReflectionIsUnwrapped() {
    handler.handleJavaFxException(new InvocationTargetException(new BusinessException("inner")));

    verify(ui).showBusinessError("inner");
    verifyNoMoreInteractions(ui);
  }

  @Test void businessExceptionBuriedUnderSeveralWrappersIsUnwrapped() {
    var wrapped = new RuntimeException("wrapper",
        new ExecutionException(new InvocationTargetException(new ValidationException("deep"))));

    handler.handleJavaFxException(wrapped);

    verify(ui).showValidationError("deep");
    verifyNoMoreInteractions(ui);
  }

  @Test void wrappedPlainExceptionShowsTheRootCauseMessageNotTheWrapper() {
    handler.handleJavaFxException(new RuntimeException("outer", new IOException("disk full")));

    verify(ui).showError("disk full");
    verifyNoMoreInteractions(ui);
  }

  @Test void invocationTargetWithoutCauseIsHandledAsGenericError() {
    handler.handleJavaFxException(new InvocationTargetException(null));

    verify(ui).showError(null);
    verify(ui, never()).showBusinessError(any());
  }

  @Test void nonExceptionThrowableIsWrappedAndShownAsSystemError() {
    handler.handleJavaFxException(new AssertionError("invariant"));

    verify(ui).showError("java.lang.AssertionError: invariant");
    verifyNoMoreInteractions(ui);
  }

  @Test void exceptionWithoutMessageStillReachesTheUser() {
    handler.handleJavaFxException(new NullPointerException());

    verify(ui).showError(null);
  }
}
