package com.example.telegrambotwebhook.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.servers.Server;
import java.util.List;
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
                        .description("Telegram Bot 管理 API")
                        .version("v1.0.0")
                        .contact(new Contact()
                                .name("開發團隊")
                                .email("developer@example.com"))
                        .license(new License()
                                .name("MIT License")
                                .url("https://opensource.org/licenses/MIT")))
                .servers(List.of(
                        new Server()
                                .url("/")  // 使用相對路徑
                                .description("現在的環境")
                ));
    }
}
