package com.achievement.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
@Configuration
@Slf4j
public class OpenApiConfig {

    @Bean
    public OpenAPI achievementOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("成果物管理系统接口文档")
                        .version("1.0")
                        .description("成果物管理系统后端接口文档"));
    }
}
