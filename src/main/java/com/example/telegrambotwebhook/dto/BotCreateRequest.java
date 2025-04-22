package com.example.telegrambotwebhook.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Telegram Bot 建立請求")
public class BotCreateRequest {

    @Schema(description = "機器人使用者名稱", example = "my_telegram_bot")
    private String username;

    @Schema(description = "機器人 API Token", example = "1234567890:ABCDEFGHIJKLMNOPQRSTUVWXYZ")
    private String token;

    @Schema(description = "機器人是否啟用", example = "true", defaultValue = "true")
    private Boolean enable = true;
}
