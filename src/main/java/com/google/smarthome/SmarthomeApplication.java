package com.google.smarthome;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication(scanBasePackages = {"com.google.*"})
public class SmarthomeApplication {

	public static void main(String[] args) {
		SpringApplication.run(SmarthomeApplication.class, args);
	}

}
