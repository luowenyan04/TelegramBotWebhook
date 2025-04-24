package com.example.telegrambotwebhook.service;

import com.example.telegrambotwebhook.entity.BotEntity;
import java.util.List;
import java.util.Optional;

public interface BotService {
    /**
     * 發送 Bot 更新通知
     */
    void notifyBotUpdated(String username);

    /**
     * 發送 Webhook 註冊通知
     */
    void notifyWebhookRegistered(String username);

    /**
     * 獲取所有機器人
     */
    List<BotEntity> getAllBots();

    /**
     * 獲取所有啟用的機器人
     */
    List<BotEntity> getEnabledBots();

    /**
     * 根據用戶名獲取機器人 (帶快取)
     */
    Optional<BotEntity> getBotByUsername(String username);

    /**
     * 根據ID獲取機器人 (帶快取)
     */
    Optional<BotEntity> getBotById(Long id);

    /**
     * 創建新機器人
     */
    BotEntity createBot(BotEntity botEntity);

    /**
     * 更新機器人資訊
     */
    BotEntity updateBot(BotEntity botEntity);

    /**
     * 儲存或更新機器人
     */
    BotEntity saveBot(BotEntity botEntity);

    /**
     * 啟用機器人
     */
    void enableBot(Long id);

    /**
     * 停用機器人
     */
    void disableBot(Long id);

    /**
     * 刪除機器人
     */
    void deleteBot(Long id);

    /**
     * 清除特定使用者名稱的機器人快取
     */
    void evictBotCache(String username);

    /**
     * 清除特定ID的機器人快取
     */
    void evictBotCacheById(Long id);

    /**
     * 清除所有機器人快取
     */
    void evictAllBotCache();

    /**
     * 檢查機器人是否存在
     */
    boolean botExists(String username);

    /**
     * 檢查機器人是否存在且已啟用
     */
    boolean botExistsAndEnabled(String username);

    /**
     * 獲取機器人總數
     */
    long getBotCount();

    /**
     * 獲取啟用的機器人總數
     */
    long getEnabledBotCount();
}
