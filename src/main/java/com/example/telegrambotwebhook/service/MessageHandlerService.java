package com.example.telegrambotwebhook.service;

import org.telegram.telegrambots.meta.api.methods.BotApiMethod;
import org.telegram.telegrambots.meta.api.objects.Update;

public interface MessageHandlerService {
    /**
     * 處理來自 Telegram 的更新
     *
     * @param username Bot 的使用者名稱
     * @param update Telegram 的更新內容
     * @return 處理結果
     */
    BotApiMethod<?> processUpdate(String username, Update update);
}
