package kz.store.cash.config;

import kz.store.cash.service.diary.TokenHolder;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

@Configuration
@RequiredArgsConstructor
public class ApiClientsForRestConfig {

  @Bean
  public TokenHolder tokenHolder() {
    return new TokenHolder();
  }

  /**
   * RAW: только для POST /token (без заголовков авторизации)
   */
  @Bean(name = "rawHydraClient")
  public RestClient rawHydraClient(RestClient.Builder baseBuilder) {
    return baseBuilder.clone().build();
  }

  /**
   * HYDRA: все защищённые вызовы — автоматически подставляет Bearer
   */
  @Bean(name = "hydraClient")
  public RestClient hydraClient(RestClient.Builder baseBuilder, TokenHolder tokenHolder) {
    return baseBuilder.clone()
        .requestInterceptor((request, body, execution) -> {
          // [NEW] Метка для глобального обработчика: этот запрос можно retry при 401/403/419
          request.getHeaders().add("X-Auth-Retry", "1");
          var token = tokenHolder.get();
          if (token != null && !token.isBlank()) {
            request.getHeaders().setBearerAuth(token);
          }
          return execution.execute(request, body);
        })
        .build();
  }

  @Bean(name = "oneCClient")
  public RestClient oneCClient(RestClient.Builder builder) {
    return builder.clone().build();
  }

  /**
   * Своё исключение для 401
   */
  public static class UnauthorizedException extends RuntimeException {

    public UnauthorizedException(String msg) {
      super(msg);
    }
  }
}