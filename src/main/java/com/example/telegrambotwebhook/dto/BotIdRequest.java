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
@Schema(description = "機器人 ID 請求")
public class BotIdRequest {

    @Schema(description = "機器人 ID")
    private Long id;
}
