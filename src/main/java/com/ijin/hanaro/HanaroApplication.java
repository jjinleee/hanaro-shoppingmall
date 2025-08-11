package com.ijin.hanaro;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class HanaroApplication {

	public static void main(String[] args) {
		SpringApplication.run(HanaroApplication.class, args);
	}

}
