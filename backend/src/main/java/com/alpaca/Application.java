package com.alpaca;

import com.alpaca.utils.LoadEnv;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class Application {

    public static void main(String[] args) {
        LoadEnv.init();
        SpringApplication.run(Application.class, args);
    }
}
