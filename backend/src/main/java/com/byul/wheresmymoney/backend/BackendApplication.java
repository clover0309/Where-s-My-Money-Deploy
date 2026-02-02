package com.byul.wheresmymoney.backend;

import java.util.TimeZone;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration;

import jakarta.annotation.PostConstruct;

@SpringBootApplication(exclude = SecurityAutoConfiguration.class)
public class BackendApplication {

	@PostConstruct
	public void init() {
		// JVM 타임존을 한국 시간으로 설정
		TimeZone.setDefault(TimeZone.getTimeZone("Asia/Seoul"));
		System.out.println("Spring Boot 타임존 설정: " + TimeZone.getDefault().getID());
	}

	public static void main(String[] args) {

        SpringApplication.run(BackendApplication.class, args);

	}

}