package com.dtvi.Pet.Ecommerce;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication(scanBasePackages = "com.dtvi.Pet.Ecommerce")
@EnableScheduling
public class PetEcommerceApplication {

	public static void main(String[] args) {
		SpringApplication.run(PetEcommerceApplication.class, args);
	}

}
