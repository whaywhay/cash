package kz.store.cash.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.net.http.HttpClient;
import java.text.SimpleDateFormat;
import java.time.Duration;
import kz.store.cash.config.ApiClientsForRestConfig.UnauthorizedException;
import kz.store.cash.handler.ExternalClientException;
import kz.store.cash.handler.ExternalServerException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.web.client.RestClientCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.client.BufferingClientHttpRequestFactory;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.http.converter.json.Jackson2ObjectMapperBuilder;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClient.Builder;

@Slf4j
@Configuration
@EnableConfigurationProperties(RestConfiguration.HttpProps.class)
public class RestConfiguration {

  @ConfigurationProperties(prefix = "app.http")
  public record HttpProps(
      boolean enableTimeout,
      Duration connectTimeout,
      Duration readTimeout) {

  }

  @Bean
  RestClientCustomizer restClientTimeoutCustomizer(HttpProps props) {
    return builder -> {
      if (!props.enableTimeout()) {
        return;
      }
      var httpClientBuilder = HttpClient.newBuilder();
      if (props.connectTimeout() != null) {
        httpClientBuilder.connectTimeout(props.connectTimeout());
      }
      var httpClient = httpClientBuilder.build();

      var jdk = new JdkClientHttpRequestFactory(httpClient);
      if (props.readTimeout() != null) {
        jdk.setReadTimeout(props.readTimeout());
      }
      builder.requestFactory(new BufferingClientHttpRequestFactory(jdk));
    };
  }

  @Bean
  RestClientCustomizer commonStatusHandlersCustomizer() {
    return builder -> builder
        // [NEW] Спец-обработка 401/403/419 ТОЛЬКО если запрос помечен X-Auth-Retry
        .defaultStatusHandler(
            status -> {
              int v = status.value();
              return (v == 401 || v == 403 || v == 419);
            },
            (request, response) -> {
              boolean wantsRetry = request.getHeaders().containsKey("X-Auth-Retry");
              if (wantsRetry) {
                // Отдаём в DebtDiaryExecutor
                throw new UnauthorizedException(
                    "Auth required " + response.getStatusCode() + " " + request.getMethod() + " "
                        + request.getURI());
              }
              // Если метки нет — падаем как и раньше «клиентской» ошибкой
              var body = new String(response.getBody().readAllBytes());
              var msg = "Client error %s url=%s, body=%s".formatted(response.getStatusCode(),
                  request.getURI(), body);
              throw new ExternalClientException(msg);
            }
        )

        // Остальные 4xx — без изменений
        .defaultStatusHandler(
            status -> status.is4xxClientError()
                && status.value() != 401
                && status.value() != 403
                && status.value() != 419,
            (request, response) -> {
              var body = new String(response.getBody().readAllBytes());
              var msg = "Client error %s url=%s, body=%s"
                  .formatted(response.getStatusCode(), request.getURI(), body);
              log.error(msg);
              throw new ExternalClientException(msg);
            }
        )

        .defaultStatusHandler(
            HttpStatusCode::is5xxServerError,
            (request, response) -> {
              var body = new String(response.getBody().readAllBytes());
              var msg = "External server error %s url=%s, body=%s"
                  .formatted(response.getStatusCode(), request.getURI(), body);
              log.error(msg);
              throw new ExternalServerException(msg);
            }
        )
        .defaultStatusHandler(
            HttpStatusCode::is2xxSuccessful,
            (request, response) -> log.info("Successfully response {} url={}",
                response.getStatusCode(), request.getURI())
        );
  }

  @Bean
  RestClient restClient(Builder builder) {
    return builder.build();
  }

  @Bean
  ObjectMapper objectMapper() {
    var mapper = new Jackson2ObjectMapperBuilder()
        .findModulesViaServiceLoader(true)
        .dateFormat(new SimpleDateFormat())
        .failOnUnknownProperties(false)
        .failOnEmptyBeans(false)
        .build();
    mapper.findAndRegisterModules();
    mapper.configure(com.fasterxml.jackson.databind.SerializationFeature.WRITE_DATES_AS_TIMESTAMPS,
        false);
    return mapper;
  }

  @Bean
  RestClientCustomizer jacksonCustomizer(ObjectMapper mapper) {
    return builder -> builder.messageConverters(converters -> {
      converters.removeIf(c -> c instanceof MappingJackson2HttpMessageConverter);
      converters.add(new MappingJackson2HttpMessageConverter(mapper));
    });
  }
}
