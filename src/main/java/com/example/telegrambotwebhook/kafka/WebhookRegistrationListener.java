package com.example.telegrambotwebhook.kafka;

import com.example.telegrambotwebhook.service.BotManager;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class WebhookRegistrationListener {

    private final BotManager botManager;

    @KafkaListener(topics = "${kafka.topic.webhook-registered}", groupId = "${spring.kafka.consumer.group-id}")
    public void listenWebhookRegistrationMessages(String username) {
        log.info("收到 webhook 註冊通知: {}", username);
        botManager.updateLocalWebhookStatus(username, true);
        log.info("Bot {} 的本地 webhook 註冊狀態已更新", username);
    }
}
