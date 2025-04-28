package com.example.telegrambotwebhook.controller;

import com.example.telegrambotwebhook.service.MessageHandlerService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import org.telegram.telegrambots.meta.api.methods.BotApiMethod;
import org.telegram.telegrambots.meta.api.objects.Update;

@RestController
@RequiredArgsConstructor
@Tag(name = "Telegram Webhook", description = "Telegram 訊息接收端點")
public class WebhookController {

    private final MessageHandlerService messageHandlerService;

    @PostMapping("/webhook/{botUsername}")
    @Operation(summary = "接收 Telegram 更新", description = "接收 Telegram 伺服器發送的訊息更新")
    public BotApiMethod<?> onUpdateReceived(
            @Parameter(description = "機器人使用者名稱", required = true) @PathVariable String botUsername,
            @Parameter(description = "Telegram 更新內容", required = true) @RequestBody Update update) {
        return messageHandlerService.processUpdate(botUsername, update);
    }
}
