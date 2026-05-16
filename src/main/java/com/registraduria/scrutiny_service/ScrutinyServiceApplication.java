package com.registraduria.scrutiny_service;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;

@SpringBootApplication
@EnableAsync
public class ScrutinyServiceApplication {

	public static void main(String[] args) {
		SpringApplication.run(ScrutinyServiceApplication.class, args);
	}

}
