package com.example.telegrambotwebhook.service.impl;

import com.example.telegrambotwebhook.config.TelegramBotConfig;
import com.example.telegrambotwebhook.entity.BotEntity;
import com.example.telegrambotwebhook.repository.BotRepository;
import com.example.telegrambotwebhook.service.BotManager;
import com.example.telegrambotwebhook.service.TelegramBot;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.locks.ReentrantLock;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataAccessException;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.methods.updates.DeleteWebhook;
import org.telegram.telegrambots.meta.api.methods.updates.SetWebhook;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

@Slf4j
@Component
public class BotManagerImpl implements BotManager {

    private final TelegramBotConfig telegramBotConfig;
    private final BotRepository botRepository;
    private final Set<String> registeredWebhooks = new HashSet<>();
    private final ReentrantLock webhookLock = new ReentrantLock();

    public BotManagerImpl(TelegramBotConfig telegramBotConfig, BotRepository botRepository) {
        this.telegramBotConfig = telegramBotConfig;
        this.botRepository = botRepository;
    }

    @PostConstruct
    @Override
    public void init() {
        log.info("Initializing Telegram 機器人 webhook...");

        try {
            List<BotEntity> enabledBots = botRepository.findByEnableTrue();
            enabledBots.forEach(this::registerWebhook);
            log.info("Registered webhooks: {}", registeredWebhooks);
        } catch (DataAccessException e) {
            log.error("Initializing Telegram bots failed: {}", e.getMessage());
        }
    }

    @PreDestroy
    @Override
    public void deregisterAllWebhooks() {
        log.info("Deregistering all webhooks started...");

        Set<String> webhooksCopy = new HashSet<>(registeredWebhooks);
        int successCount = 0;
        int failureCount = 0;

        for (String username : webhooksCopy) {
            try {
                boolean result = deregisterWebhook(username);
                if (result) {
                    successCount++;
                } else {
                    failureCount++;
                }
            } catch (Exception e) {
                log.error("Failed to deregister webhook for user {} with message: {}", username, e.getMessage(), e);
                failureCount++;
            }
        }

        log.info("Deregistering all webhooks completed. Success: {}, Failure: {}", successCount, failureCount);
        registeredWebhooks.clear();
    }

    @Override
    public boolean registerWebhook(BotEntity botEntity) {
        log.debug("Registering webhook: {}", botEntity.getUsername());

        webhookLock.lock();
        try {
            // 檢查 webhook 是否已註冊
            if (registeredWebhooks.contains(botEntity.getUsername())) {
                log.info("機器人 {} 的 webhook 已經註冊，正在更新設定", botEntity.getUsername());
                deregisterWebhook(botEntity.getUsername());
            }

            // 創建臨時 Bot 實例用於註冊 webhook
            TelegramBot tempBot = new TelegramBot(
                    botEntity.getToken(),
                    botEntity.getUsername(),
                    telegramBotConfig.getRegisterPath() + "/" + botEntity.getUsername()
            );

            String webhookUrl = telegramBotConfig.getWebhookDomain() +
                    telegramBotConfig.getRegisterPath() + "/" +
                    botEntity.getUsername();

            try {
                SetWebhook setWebhook = SetWebhook.builder()
                        .url(webhookUrl)
                        .build();

                tempBot.setWebhook(setWebhook);
                registeredWebhooks.add(botEntity.getUsername());
                log.info("Registered webhook for bot {} with URL: {}", botEntity.getUsername(), webhookUrl);
                return true;
            } catch (TelegramApiException e) {
                log.error("Registering webhook for bot {} failed with message: {}", botEntity.getUsername(), e.getMessage(), e);
                return false;
            }
        } finally {
            webhookLock.unlock();
        }
    }

    @Override
    public boolean deregisterWebhook(String username) {
        log.debug("Deregistering webhook: {}", username);

        BotEntity botEntity = botRepository.findByUsername(username).orElse(null);
        if (botEntity == null) {
            log.warn("No bot found to deregister webhook for: {}", username);
            return false;
        }

        webhookLock.lock();
        try {
            if (!registeredWebhooks.contains(username)) {
                log.info("機器人 {} 的 webhook 尚未註冊，無需取消", username);
                return true;
            }

            // 創建臨時 Bot 實例用於取消註冊 webhook
            TelegramBot tempBot = new TelegramBot(
                    botEntity.getToken(),
                    botEntity.getUsername(),
                    null
            );

            try {
                // 從 Telegram 取消 webhook
                DeleteWebhook deleteWebhook = new DeleteWebhook();
                tempBot.execute(deleteWebhook);

                // 從註冊記錄中移除
                registeredWebhooks.remove(username);
                log.info("Deregistered webhook for bot {}", username);
                return true;
            } catch (TelegramApiException e) {
                log.error("Deregistering webhook for bot {} failed with message: {}", username, e.getMessage(), e);
                return false;
            }
        } finally {
            webhookLock.unlock();
        }
    }

    @Override
    public boolean isWebhookRegistered(String username) {
        return registeredWebhooks.contains(username);
    }

    @Override
    public void updateLocalWebhookStatus(String username, boolean registered) {
        webhookLock.lock();
        try {
            if (registered) {
                registeredWebhooks.add(username);
                log.info("已在本地標記 Bot {} 的 webhook 為已註冊", username);
            } else {
                registeredWebhooks.remove(username);
                log.info("已在本地標記 Bot {} 的 webhook 為未註冊", username);
            }
        } finally {
            webhookLock.unlock();
        }
    }
}
