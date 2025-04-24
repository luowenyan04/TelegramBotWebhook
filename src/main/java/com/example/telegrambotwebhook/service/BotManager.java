package com.example.telegrambotwebhook.service;

import com.example.telegrambotwebhook.config.TelegramBotConfig;
import com.example.telegrambotwebhook.entity.BotEntity;
import com.example.telegrambotwebhook.repository.BotRepository;
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
public class BotManager {

    private final TelegramBotConfig telegramBotConfig;
    private final BotRepository botRepository;
    private final Set<String> registeredWebhooks = new HashSet<>();
    private final ReentrantLock webhookLock = new ReentrantLock();

    public BotManager(TelegramBotConfig telegramBotConfig, BotRepository botRepository) {
        this.telegramBotConfig = telegramBotConfig;
        this.botRepository = botRepository;
    }

    @PostConstruct
    public void init() {
        log.info("開始初始化 Telegram 機器人 webhook...");

        try {
            List<BotEntity> enabledBots = botRepository.findByEnableTrue();
            enabledBots.forEach(this::registerWebhook);
            log.info("已成功註冊 {} 個機器人的 webhook", enabledBots.size());
        } catch (DataAccessException e) {
            log.error("資料庫存取錯誤，可能是資料表尚未建立: {}", e.getMessage());
            log.info("系統將繼續啟動，但機器人功能可能無法正常運作");
        }
    }

    @PreDestroy
    public void deregisterAllWebhooks() {
        log.info("系統關閉中，正在取消所有機器人的 webhook 註冊...");

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
                log.error("取消註冊機器人 {} 的 webhook 時發生錯誤: {}", username, e.getMessage(), e);
                failureCount++;
            }
        }

        log.info("所有機器人的 webhook 註冊已取消。成功: {}, 失敗: {}", successCount, failureCount);
        registeredWebhooks.clear();
    }

    public boolean registerWebhook(BotEntity botEntity) {
        log.debug("註冊機器人 webhook: {}", botEntity.getUsername());

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
                log.info("機器人 {} 設定完成 Webhook: {}", botEntity.getUsername(), webhookUrl);
                return true;
            } catch (TelegramApiException e) {
                log.error("註冊機器人 {} 的 webhook 時發生錯誤: {}", botEntity.getUsername(), e.getMessage(), e);
                return false;
            }
        } finally {
            webhookLock.unlock();
        }
    }

    public boolean deregisterWebhook(String username) {
        log.info("取消註冊機器人 webhook: {}", username);

        // 獲取 bot 實體資訊
        BotEntity botEntity = botRepository.findByUsername(username).orElse(null);
        if (botEntity == null) {
            log.warn("無法找到要取消註冊 webhook 的機器人: {}", username);
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
                log.info("機器人 {} 的 webhook 已成功取消註冊", username);
                return true;
            } catch (TelegramApiException e) {
                log.error("取消註冊機器人 {} 的 webhook 時發生錯誤: {}", username, e.getMessage(), e);
                return false;
            }
        } finally {
            webhookLock.unlock();
        }
    }

    public boolean isWebhookRegistered(String username) {
        return registeredWebhooks.contains(username);
    }

    /**
     * 僅更新本地的 webhook 註冊狀態，不調用 Telegram API
     */
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
