package com.example.telegrambotwebhook.service.impl;

import com.example.telegrambotwebhook.entity.BotEntity;
import com.example.telegrambotwebhook.service.BotService;
import com.example.telegrambotwebhook.service.MessageHandlerService;
import com.example.telegrambotwebhook.service.TelegramBot;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.telegram.telegrambots.meta.api.methods.BotApiMethod;
import org.telegram.telegrambots.meta.api.objects.Update;

@Slf4j
@Service
@RequiredArgsConstructor
public class MessageHandlerServiceImpl implements MessageHandlerService {

    private final BotService botService;

    /**
     * 處理來自 Telegram 的更新
     */
    @Override
    public BotApiMethod<?> processUpdate(String username, Update update) {
        log.debug("處理來自 {} 的更新", username);

        // 從 BotService 獲取機器人實體（使用其快取機制）
        BotEntity botEntity = botService.getBotByUsername(username)
                .orElse(null);

        if (botEntity == null || !Boolean.TRUE.equals(botEntity.getEnable())) {
            log.warn("機器人 {} 不存在或未啟用", username);
            return null;
        }

        // 創建臨時 Bot 實例處理更新
        TelegramBot tempBot = new TelegramBot(
                botEntity.getToken(),
                botEntity.getUsername(),
                null  // 不需要 botPath，因為只用於處理消息
        );

        return tempBot.onWebhookUpdateReceived(update);
    }
}
