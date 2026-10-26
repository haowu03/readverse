package com.readverse;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@MapperScan("com.readverse.**.mapper")
@EnableScheduling
public class ReadVerseApplication {

    public static void main(String[] args) {
        SpringApplication.run(ReadVerseApplication.class, args);
    }
}
