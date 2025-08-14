package kz.store.cash.config;

import java.net.URI;
import java.util.Map;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties("api")
public record ApiUrlProperties(
    Map<String, ApiProperty> urlBasic
) {

  public record ApiProperty(
      URI baseUrl,
      String username,
      String password
  ) {
  }
}
