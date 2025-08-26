package com.yuesf.aireader;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;

@SpringBootApplication
@EnableAsync
public class AiReaderApplication {

    public static void main(String[] args) {
        SpringApplication.run(AiReaderApplication.class, args);
    }

}