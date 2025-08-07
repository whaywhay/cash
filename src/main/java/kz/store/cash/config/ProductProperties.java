package kz.store.cash.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.product")
public record ProductProperties(
    String universalProductBarcode,
    String organizationName,
    String cashierName,
    int productReturnPeriod
) {

}
