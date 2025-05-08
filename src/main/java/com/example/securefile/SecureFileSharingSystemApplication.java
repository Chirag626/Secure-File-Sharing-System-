package com.example.securefile;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class SecureFileSharingSystemApplication {
    public static void main(String[] args) {
        SpringApplication.run(SecureFileSharingSystemApplication.class, args);
    }
}