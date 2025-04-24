package com.example.telegrambotwebhook.kafka;

import com.example.telegrambotwebhook.service.BotService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class CacheClearListener {

    private final BotService botService;

    @KafkaListener(topics = "${kafka.topic.cache-clear}", groupId = "${spring.kafka.consumer.group-id}")
    public void clearAllBotCache(String message) {
        log.info("Received cache clear message: {}", message);
        botService.evictAllBotCache();
    }

    @KafkaListener(topics = "${kafka.topic.bot-update}", groupId = "${spring.kafka.consumer.group-id}")
    public void clearBotUpdateCache(String username) {
        log.info("Received bot update message: {}", username);
        botService.evictBotCache(username);
    }
}
