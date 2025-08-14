package kz.store.cash.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.net.http.HttpClient;
import java.text.SimpleDateFormat;
import java.time.Duration;
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
  RestClient restClient(Builder builder) {
    return builder
        .defaultStatusHandler(HttpStatusCode::is4xxClientError,
            (request, response) -> {
              var body = new String(response.getBody().readAllBytes());
              var message = "Client error %s url=%s, body=%s"
                  .formatted(response.getStatusCode(), request.getURI(), body);
              log.error(message);
              throw new ExternalClientException(message);
            })
        .defaultStatusHandler(HttpStatusCode::is5xxServerError,
            (request, response) -> {
              var body = new String(response.getBody().readAllBytes());
              var message = "External server error %s url=%s, body=%s"
                  .formatted(response.getStatusCode(), request.getURI(), body);
              log.error(message);
              throw new ExternalServerException(message);
            })
        .defaultStatusHandler(HttpStatusCode::is2xxSuccessful,
            (request, response) -> log.info("Successfully response {} url={}",
                response.getStatusCode(), request.getURI()))
        .build();
  }

  @Bean
  ObjectMapper objectMapper() {
    return new Jackson2ObjectMapperBuilder()
        .dateFormat(new SimpleDateFormat())
        .failOnUnknownProperties(false)
        .failOnEmptyBeans(false)
        .build();
  }
}
