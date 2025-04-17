package com.demo.docker;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;

@SpringBootApplication
@EnableFeignClients
public class DockerApp1 {
    public static void main(String[] args) {
        SpringApplication.run(DockerApp1.class, args);
    }
}
