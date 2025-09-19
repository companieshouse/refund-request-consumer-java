package uk.gov.companieshouse.requestrefund.consumer;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class Application {

	public static final String NAMESPACE = "refund-request-consumer";
	public static void main(String[] args) {
	SpringApplication.run(Application.class, args);
	}

}
