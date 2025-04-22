package com.example.telegrambotwebhook.service;

import com.example.telegrambotwebhook.config.BotConfig;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.bots.TelegramWebhookBot;
import org.telegram.telegrambots.meta.api.methods.BotApiMethod;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.methods.updates.SetWebhook;
import org.telegram.telegrambots.meta.api.objects.Update;

@Slf4j
@Component
public class TelegramBot extends TelegramWebhookBot {

    private final BotConfig config;

    public TelegramBot(BotConfig config) {
        super(config.getBotToken());
        this.config = config;
    }

    @PostConstruct
    public void init() {
        SetWebhook webhook = SetWebhook.builder()
                .url(config.getWebhookDomain() + config.getRegisterPath())
                .build();
        log.info("設定完成 Webhook: {}", webhook);
    }

    @Override
    public String getBotUsername() {
        return config.getBotUsername();
    }

    @Override
    public String getBotPath() {
        return config.getWebhookDomain();
    }

    @Override
    public BotApiMethod<?> onWebhookUpdateReceived(Update update) {
        if (update.hasMessage() && update.getMessage().hasText()) {
            String messageText = update.getMessage().getText();
            long chatId = update.getMessage().getChatId();

            return handleMessage(chatId, messageText);
        }

        return null;
    }

    private SendMessage handleMessage(long chatId, String messageText) {
        log.info("收到訊息: {}", messageText);
        SendMessage message = new SendMessage();
        message.setChatId(String.valueOf(chatId));

        if (messageText.equals("/start")) {
            message.setText("您好！歡迎使用此 Telegram Bot。");
        } else {
            message.setText("您發送的訊息: " + messageText);
        }

        return message;
    }
}
