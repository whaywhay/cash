package kz.store.cash;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@ConfigurationPropertiesScan
@SpringBootApplication
public class CashApplication {

	public static void main(String[] args) {
		SpringApplication.run(CashApplication.class, args);
	}

}
