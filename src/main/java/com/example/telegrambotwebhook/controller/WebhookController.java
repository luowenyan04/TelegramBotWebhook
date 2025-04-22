package com.example.telegrambotwebhook.controller;

import com.example.telegrambotwebhook.service.MessageHandlerService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import org.telegram.telegrambots.meta.api.methods.BotApiMethod;
import org.telegram.telegrambots.meta.api.objects.Update;

@Slf4j
@RestController
@RequiredArgsConstructor
@Tag(name = "Telegram Webhook", description = "Telegram 訊息接收端點")
public class WebhookController {

    private final MessageHandlerService messageHandlerService;

    @PostMapping("/webhook/{botUsername}")
    @Operation(summary = "接收 Telegram 更新", description = "接收 Telegram 伺服器發送的訊息更新")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "成功處理更新",
                    content = @Content(mediaType = "application/json")),
            @ApiResponse(responseCode = "400", description = "請求無效",
                    content = @Content),
            @ApiResponse(responseCode = "404", description = "找不到對應的機器人",
                    content = @Content)
    })
    public BotApiMethod<?> onUpdateReceived(
            @Parameter(description = "機器人使用者名稱", required = true) @PathVariable String botUsername,
            @Parameter(description = "Telegram 更新內容", required = true) @RequestBody Update update) {
        log.debug("接收到來自 {} 的 webhook 請求", botUsername);
        return messageHandlerService.processUpdate(botUsername, update);
    }
}
