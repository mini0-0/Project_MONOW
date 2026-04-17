package com.monow.monow_api;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.web.bind.annotation.RestController;

@SpringBootApplication(scanBasePackages = "com.monow")
public class MonowApiApplication {
	public static void main(String[] args) {
		SpringApplication.run(MonowApiApplication.class, args);
	}

}
