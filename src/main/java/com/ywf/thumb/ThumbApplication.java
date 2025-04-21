package com.ywf.thumb;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@MapperScan("com.ywf.thumb.mapper")
@EnableScheduling
public class ThumbApplication {

    public static void main(String[] args) {
        SpringApplication.run(ThumbApplication.class, args);
    }

}
