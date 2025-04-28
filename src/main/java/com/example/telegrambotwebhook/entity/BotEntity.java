package com.example.telegrambotwebhook.entity;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "telegram_bots")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Telegram Bot 實體")
public class BotEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Schema(description = "機器人 ID", example = "1")
    private Long id;

    @Schema(description = "機器人使用者名稱", example = "my_telegram_bot")
    private String username;

    @Schema(description = "機器人 API Token", example = "1234567890:ABCDEFGHIJKLMNOPQRSTUVWXYZ")
    private String token;

    @Schema(description = "機器人是否啟用", example = "true", defaultValue = "true")
    private Boolean enable;
}
