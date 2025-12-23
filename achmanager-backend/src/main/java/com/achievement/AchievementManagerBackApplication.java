package com.achievement;

import lombok.extern.slf4j.Slf4j;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.transaction.annotation.EnableTransactionManagement;

@SpringBootApplication
@EnableTransactionManagement
@Slf4j
@EnableCaching
@MapperScan("com.achievement.mapper")
public class AchievementManagerBackApplication {
    public static void main(String[] args) {
        SpringApplication.run(AchievementManagerBackApplication.class, args);
        log.info("server started");
    }
}

