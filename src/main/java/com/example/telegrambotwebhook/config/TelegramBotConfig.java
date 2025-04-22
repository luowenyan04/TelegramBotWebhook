package com.example.telegrambotwebhook.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Getter
@Setter
@Configuration
@ConfigurationProperties(prefix = "telegram-bot")
public class TelegramBotConfig {

    private String webhookDomain;
    private String registerPath;
}
