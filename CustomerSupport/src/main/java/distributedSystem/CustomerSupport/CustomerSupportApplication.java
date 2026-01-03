package distributedSystem.CustomerSupport;

import distributedSystem.CustomerSupport.config.GeminiProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

@EnableConfigurationProperties(GeminiProperties.class)
@SpringBootApplication
public class CustomerSupportApplication {

	public static void main(String[] args) {
		SpringApplication.run(CustomerSupportApplication.class, args);
	}

}
