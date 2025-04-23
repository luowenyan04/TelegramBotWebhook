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
    public void listenCacheClearMessages(String message) {
        log.info("收到快取清除消息: {}", message);

        // 清除所有機器人快取
        botService.evictAllBotCache();
        log.info("已完成所有快取清除操作");
    }
}
