package com.gateflow.tracker;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@MapperScan("com.gateflow.tracker.repository")
@EnableScheduling
public class TrackerAdminApplication {

    public static void main(String[] args) {
        SpringApplication.run(TrackerAdminApplication.class, args);
    }
}
