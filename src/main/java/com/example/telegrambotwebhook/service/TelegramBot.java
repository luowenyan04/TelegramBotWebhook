package com.example.telegrambotwebhook.service;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.telegram.telegrambots.bots.TelegramWebhookBot;
import org.telegram.telegrambots.meta.api.methods.BotApiMethod;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.methods.updates.DeleteWebhook;
import org.telegram.telegrambots.meta.api.methods.updates.SetWebhook;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

@Slf4j
public class TelegramBot extends TelegramWebhookBot {

    @Getter
    private final String botUsername;
    private final String botPath;

    public TelegramBot(String botToken, String botUsername, String botPath) {
        super(botToken);
        this.botUsername = botUsername;
        this.botPath = botPath;
    }

    @Override
    public String getBotPath() {
        return botPath;
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
        log.info("Bot {} received message: {}", botUsername, messageText);
        SendMessage message = new SendMessage();
        message.setChatId(String.valueOf(chatId));

        if (messageText.equals("/start")) {
            message.setText("您好！歡迎使用 " + botUsername + " Bot。");
        } else {
            message.setText("您發送給 " + botUsername + " 的訊息: " + messageText);
        }

        return message;
    }

    /**
     * 設定 Webhook URL
     *
     * @param setWebhook Webhook 設定資訊
     * @throws TelegramApiException 如果設定失敗
     */
    public void setWebhook(SetWebhook setWebhook) throws TelegramApiException {
        log.info("Setting webhook for Bot {}, Webhook: {}", botUsername, setWebhook.getUrl());
        execute(setWebhook);
    }

    /**
     * 移除 Webhook 設定
     *
     * @throws TelegramApiException 如果移除失敗
     */
    public void deleteWebhook() throws TelegramApiException {
        log.info("Deleting webhook for Bot {}", botUsername);
        DeleteWebhook deleteWebhook = new DeleteWebhook();
        execute(deleteWebhook);
        log.info("Bot {} deleted webhook", botUsername);
    }
}
