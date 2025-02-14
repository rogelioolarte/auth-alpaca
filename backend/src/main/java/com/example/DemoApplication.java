package com.example;

import com.example.utils.LoadEnv;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class DemoApplication {

	public static void main(String[] args) {
		LoadEnv.init();
		SpringApplication.run(DemoApplication.class, args);
	}
}
