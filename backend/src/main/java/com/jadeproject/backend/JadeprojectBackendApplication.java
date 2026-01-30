package com.jadeproject.backend;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class JadeprojectBackendApplication {

	public static void main(String[] args) {
		SpringApplication.run(JadeprojectBackendApplication.class, args);
	}

}
