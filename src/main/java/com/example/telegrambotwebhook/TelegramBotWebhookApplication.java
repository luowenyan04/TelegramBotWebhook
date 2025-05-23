package com.example.telegrambotwebhook;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.kafka.annotation.EnableKafka;

@SpringBootApplication
@EnableCaching
@EnableConfigurationProperties
@EnableKafka
public class TelegramBotWebhookApplication {

    public static void main(String[] args) {
        SpringApplication.run(TelegramBotWebhookApplication.class, args);
    }

}
