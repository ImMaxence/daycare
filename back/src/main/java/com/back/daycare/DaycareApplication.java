package com.back.daycare;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication
@ConfigurationPropertiesScan
public class DaycareApplication {

	public static void main(String[] args) {
		SpringApplication.run(DaycareApplication.class, args);
	}

}
