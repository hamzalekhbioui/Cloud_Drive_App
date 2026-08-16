package com.cloud.drive;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class CloudDriveApplication {

	public static void main(String[] args) {
		SpringApplication.run(CloudDriveApplication.class, args);
	}

}
