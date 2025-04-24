package com.example.telegrambotwebhook.service;

import com.example.telegrambotwebhook.entity.BotEntity;

public interface BotManager {
    /**
     * 初始化所有啟用的 Bot 的 Webhook
     */
    void init();

    /**
     * 系統關閉時取消所有的 Webhook 註冊
     */
    void deregisterAllWebhooks();

    /**
     * 註冊單個機器人的 Webhook
     *
     * @param botEntity 機器人實體
     * @return 是否註冊成功
     */
    boolean registerWebhook(BotEntity botEntity);

    /**
     * 取消註冊機器人的 Webhook
     *
     * @param username 機器人使用者名稱
     * @return 是否取消成功
     */
    boolean deregisterWebhook(String username);

    /**
     * 檢查機器人的 Webhook 是否已註冊
     *
     * @param username 機器人使用者名稱
     * @return 是否已註冊
     */
    boolean isWebhookRegistered(String username);

    /**
     * 僅更新本地的 webhook 註冊狀態，不調用 Telegram API
     *
     * @param username 機器人使用者名稱
     * @param registered 註冊狀態
     */
    void updateLocalWebhookStatus(String username, boolean registered);
}
