package com.yuesf.aireader;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;
import org.mybatis.spring.annotation.MapperScan;

@SpringBootApplication
@EnableAsync
@MapperScan(basePackages = {"com.yuesf.aireader.mapper", "com.yuesf.aireader.mapper.tracking"})
public class AiReaderApplication {

    public static void main(String[] args) {
        SpringApplication.run(AiReaderApplication.class, args);
    }

}
