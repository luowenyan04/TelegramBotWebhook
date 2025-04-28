package com.example.telegrambotwebhook.controller;

import com.example.telegrambotwebhook.service.BotService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequestMapping("/api/cache")
@RequiredArgsConstructor
@Tag(name = "快取管理", description = "系統快取管理相關 API")
public class CacheController {

    private final BotService botService;
    private final KafkaTemplate<String, String> kafkaTemplate;

    @PostMapping("/clear")
    @Operation(summary = "清除所有快取", description = "直接清除系統中所有機器人相關的快取")
    public ResponseEntity<Void> clearAllCache() {
        log.debug("API 請求清除所有快取");
        botService.evictAllBotCache();
        return ResponseEntity.ok().build();
    }

    @PostMapping("/clear/kafka")
    @Operation(summary = "透過 Kafka 清除快取", description = "發送消息到 Kafka 觸發快取清除")
    public ResponseEntity<Void> clearCacheViaKafka() {
        log.debug("發送 Kafka 消息以清除快取");
        kafkaTemplate.send("bot-cache-clear", "清除所有快取的請求");
        return ResponseEntity.ok().build();
    }
}
