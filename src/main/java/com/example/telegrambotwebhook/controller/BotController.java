package com.example.telegrambotwebhook.controller;

import com.example.telegrambotwebhook.dto.BotCreateRequest;
import com.example.telegrambotwebhook.dto.BotIdRequest;
import com.example.telegrambotwebhook.dto.BotUpdateRequest;
import com.example.telegrambotwebhook.entity.BotEntity;
import com.example.telegrambotwebhook.service.BotService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequestMapping("/api/bots")
@RequiredArgsConstructor
@Tag(name = "Bot 管理", description = "Telegram Bot 管理相關 API")
public class BotController {

    private final BotService botService;
    private final KafkaTemplate<String, String> kafkaTemplate;

    @Value("${kafka.topic.bot-update}")
    private String botUpdateTopic;

    @Value("${kafka.topic.webhook-registered}")
    private String webhookRegisteredTopic;

    /**
     * 發送 Bot 更新通知
     */
    private void notifyBotUpdated(String username) {
        log.info("發送 Bot 更新通知: {}", username);
        kafkaTemplate.send(botUpdateTopic, username);
    }

    /**
     * 發送 Webhook 註冊通知
     */
    private void notifyWebhookRegistered(String username) {
        log.info("發送 Webhook 註冊通知: {}", username);
        kafkaTemplate.send(webhookRegisteredTopic, username);
    }

    @GetMapping
    @Operation(summary = "獲取所有機器人", description = "取得所有註冊的 Telegram 機器人清單")
    public ResponseEntity<List<BotEntity>> getAllBots() {
        log.debug("取得所有機器人清單");
        return ResponseEntity.ok(botService.getAllBots());
    }

    @GetMapping("/bot")
    @Operation(summary = "根據 ID 獲取機器人", description = "根據指定的 ID 取得特定的 Telegram 機器人資訊")
    public ResponseEntity<BotEntity> getBotById(
            @Parameter(description = "機器人 ID", required = true)
            @RequestParam Long id) {
        log.debug("查詢機器人 ID: {}", id);
        return botService.getBotById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    @Operation(summary = "建立新機器人", description = "註冊一個新的 Telegram 機器人")
    public ResponseEntity<BotEntity> createBot(
            @Parameter(description = "機器人資訊", required = true)
            @RequestBody BotCreateRequest request) {
        log.debug("建立新機器人: {}", request.getUsername());

        BotEntity botEntity = BotEntity.builder()
                .username(request.getUsername())
                .token(request.getToken())
                .enable(request.getEnable())
                .build();

        BotEntity savedBot = botService.createBot(botEntity);

        // 發送通知
        notifyBotUpdated(savedBot.getUsername());

        if (Boolean.TRUE.equals(savedBot.getEnable())) {
            notifyWebhookRegistered(savedBot.getUsername());
        }

        return ResponseEntity.status(HttpStatus.CREATED).body(savedBot);
    }

    @PutMapping
    @Operation(summary = "更新機器人", description = "更新指定 ID 的機器人資訊")
    public ResponseEntity<BotEntity> updateBot(
            @Parameter(description = "更新的機器人資訊", required = true)
            @RequestBody BotUpdateRequest request) {
        log.debug("更新機器人 ID: {}", request.getId());

        Optional<BotEntity> existingBot = botService.getBotById(request.getId());
        if (existingBot.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        boolean wasEnabled = Boolean.TRUE.equals(existingBot.get().getEnable());
        boolean willBeEnabled = Boolean.TRUE.equals(request.getEnable());

        BotEntity botEntity = BotEntity.builder()
                .id(request.getId())
                .username(request.getUsername())
                .token(request.getToken())
                .enable(request.getEnable())
                .build();

        BotEntity updatedBot = botService.updateBot(botEntity);
        if (updatedBot == null) {
            return ResponseEntity.notFound().build();
        }

        // 發送通知
        notifyBotUpdated(updatedBot.getUsername());

        if (willBeEnabled && !wasEnabled) {
            notifyWebhookRegistered(updatedBot.getUsername());
        }

        return ResponseEntity.ok(updatedBot);
    }

    @PutMapping("/enable")
    @Operation(summary = "啟用機器人", description = "啟用指定 ID 的機器人")
    public ResponseEntity<Void> enableBot(
            @Parameter(description = "機器人 ID", required = true)
            @RequestBody BotIdRequest request) {
        log.debug("啟用機器人 ID: {}", request.getId());

        Optional<BotEntity> existingBot = botService.getBotById(request.getId());
        if (existingBot.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        String username = existingBot.get().getUsername();
        boolean wasEnabled = Boolean.TRUE.equals(existingBot.get().getEnable());

        botService.enableBot(request.getId());

        // 發送通知
        notifyBotUpdated(username);

        if (!wasEnabled) {
            notifyWebhookRegistered(username);
        }

        return ResponseEntity.ok().build();
    }

    @PutMapping("/disable")
    @Operation(summary = "停用機器人", description = "停用指定 ID 的機器人")
    public ResponseEntity<Void> disableBot(
            @Parameter(description = "機器人 ID", required = true)
            @RequestBody BotIdRequest request) {
        log.debug("停用機器人 ID: {}", request.getId());

        Optional<BotEntity> existingBot = botService.getBotById(request.getId());
        if (existingBot.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        String username = existingBot.get().getUsername();
        botService.disableBot(request.getId());

        // 發送通知
        notifyBotUpdated(username);

        return ResponseEntity.ok().build();
    }

    @DeleteMapping
    @Operation(summary = "刪除機器人", description = "刪除指定 ID 的機器人")
    public ResponseEntity<Void> deleteBot(
            @Parameter(description = "機器人 ID", required = true)
            @RequestBody BotIdRequest request) {
        log.debug("刪除機器人 ID: {}", request.getId());

        Optional<BotEntity> existingBot = botService.getBotById(request.getId());
        if (existingBot.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        String username = existingBot.get().getUsername();
        botService.deleteBot(request.getId());

        // 發送通知
        notifyBotUpdated(username);

        return ResponseEntity.noContent().build();
    }
}
