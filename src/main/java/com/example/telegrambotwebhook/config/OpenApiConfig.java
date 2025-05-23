package com.example.telegrambotwebhook.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Value("${telegram-bot.webhookDomain}")
    private String webhookDomain;

    @Bean
    public OpenAPI telegramBotOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("Telegram Bot API")
                        .description("Telegram Bot API"));
    }
}
