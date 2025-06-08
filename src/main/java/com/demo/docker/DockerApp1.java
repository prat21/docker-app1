package com.demo.docker;

import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.context.annotation.Bean;

import java.io.File;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@SpringBootApplication
@EnableFeignClients
public class DockerApp1 {
    public static void main(String[] args) {
        SpringApplication.run(DockerApp1.class, args);
    }

    @Bean
    public CommandLineRunner commandLineRunner() {
        return args -> {
            try {
                File directory = new File("/fusevol");
                var files = directory.listFiles();
                System.out.println("Files in cloud storage bucket volume22: " + Stream.of(files)
                        .filter(file -> !file.isDirectory())
                        .map(File::getName)
                        .collect(Collectors.toSet()));
            } catch(Exception ex){
                System.out.println("No such file or directory. Exception:" + ex.getMessage());
            }
        };
    }
}
