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

    @Override
    public BotApiMethod<?> processUpdate(String username, Update update) {
        log.debug("Process Message From Bot {}, Update: {}", username, update);

        BotEntity botEntity = botService.getBotByUsername(username)
                .orElse(null);

        if (botEntity == null || !Boolean.TRUE.equals(botEntity.getEnable())) {
            log.warn("Bot Not Found or Not Enabled: {}", username);
            return null;
        }

        TelegramBot tempBot = new TelegramBot(
                botEntity.getToken(),
                botEntity.getUsername(),
                null  // 不需要 botPath，因為只用於處理消息
        );

        return tempBot.onWebhookUpdateReceived(update);
    }
}
