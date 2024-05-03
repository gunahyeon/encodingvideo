package com.gu.encodingvideo.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class SwaggerConfig {

    @Bean
    public OpenAPI apiInfo() {
        Info apiInfo = new Info()
                .title("테스트 - 비디오 인코딩")
                .description("encoding video .mp4 to .webm")
                .version("1.0");

        return new OpenAPI()
                .info(apiInfo);
    }
}
