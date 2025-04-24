package com.example.telegrambotwebhook.controller;

import com.example.telegrambotwebhook.dto.BotCreateRequest;
import com.example.telegrambotwebhook.dto.BotIdRequest;
import com.example.telegrambotwebhook.dto.BotUpdateRequest;
import com.example.telegrambotwebhook.entity.BotEntity;
import com.example.telegrambotwebhook.service.BotService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequestMapping("/api/bots")
@RequiredArgsConstructor
@Tag(name = "Bot 管理", description = "Telegram Bot 管理相關 API")
public class BotController {

    private final BotService botService;

    @GetMapping
    @Operation(summary = "獲取所有機器人", description = "取得所有註冊的 Telegram 機器人清單")
    @ApiResponse(responseCode = "200", description = "操作成功",
            content = @Content(mediaType = "application/json",
                    schema = @Schema(implementation = BotEntity.class)))
    public ResponseEntity<List<BotEntity>> getAllBots() {
        log.debug("取得所有機器人清單");
        return ResponseEntity.ok(botService.getAllBots());
    }

    @GetMapping("/bot")
    @Operation(summary = "根據 ID 獲取機器人", description = "根據指定的 ID 取得特定的 Telegram 機器人資訊")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "找到機器人",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = BotEntity.class))),
            @ApiResponse(responseCode = "404", description = "找不到指定 ID 的機器人",
                    content = @Content)
    })
    public ResponseEntity<BotEntity> getBotById(
            @Parameter(description = "機器人 ID", required = true)
            @RequestParam Long id) {
        log.debug("查詢機器人 ID: {}", id);
        return botService.getBotById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    @Operation(summary = "建立新機器人", description = "註冊一個新的 Telegram 機器人")
    @ApiResponse(responseCode = "201", description = "機器人建立成功",
            content = @Content(mediaType = "application/json",
                    schema = @Schema(implementation = BotEntity.class)))
    public ResponseEntity<BotEntity> createBot(
            @Parameter(description = "機器人資訊", required = true)
            @RequestBody BotCreateRequest request) {
        log.info("建立新機器人: {}", request.getUsername());

        BotEntity botEntity = BotEntity.builder()
                .username(request.getUsername())
                .token(request.getToken())
                .enable(request.getEnable())
                .build();

        BotEntity savedBot = botService.createBot(botEntity);
        return ResponseEntity.status(HttpStatus.CREATED).body(savedBot);
    }

    @PutMapping
    @Operation(summary = "更新機器人", description = "更新指定 ID 的機器人資訊")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "機器人更新成功",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = BotEntity.class))),
            @ApiResponse(responseCode = "404", description = "找不到指定 ID 的機器人",
                    content = @Content)
    })
    public ResponseEntity<BotEntity> updateBot(
            @Parameter(description = "更新的機器人資訊", required = true)
            @RequestBody BotUpdateRequest request) {
        log.info("更新機器人 ID: {}", request.getId());

        BotEntity botEntity = BotEntity.builder()
                .id(request.getId())
                .username(request.getUsername())
                .token(request.getToken())
                .enable(request.getEnable())
                .build();

        BotEntity updatedBot = botService.updateBot(botEntity);
        if (updatedBot == null) {
            return ResponseEntity.notFound().build();
        }

        return ResponseEntity.ok(updatedBot);
    }

    @PutMapping("/enable")
    @Operation(summary = "啟用機器人", description = "啟用指定 ID 的機器人")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "機器人啟用成功"),
            @ApiResponse(responseCode = "404", description = "找不到指定 ID 的機器人",
                    content = @Content)
    })
    public ResponseEntity<Void> enableBot(
            @Parameter(description = "機器人 ID", required = true)
            @RequestBody BotIdRequest request) {
        log.info("啟用機器人 ID: {}", request.getId());

        Optional<BotEntity> existingBot = botService.getBotById(request.getId());
        if (existingBot.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        botService.enableBot(request.getId());
        return ResponseEntity.ok().build();
    }

    @PutMapping("/disable")
    @Operation(summary = "停用機器人", description = "停用指定 ID 的機器人")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "機器人停用成功"),
            @ApiResponse(responseCode = "404", description = "找不到指定 ID 的機器人",
                    content = @Content)
    })
    public ResponseEntity<Void> disableBot(
            @Parameter(description = "機器人 ID", required = true)
            @RequestBody BotIdRequest request) {
        log.info("停用機器人 ID: {}", request.getId());

        Optional<BotEntity> existingBot = botService.getBotById(request.getId());
        if (existingBot.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        botService.disableBot(request.getId());
        return ResponseEntity.ok().build();
    }

    @DeleteMapping
    @Operation(summary = "刪除機器人", description = "刪除指定 ID 的機器人")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "204", description = "機器人刪除成功"),
            @ApiResponse(responseCode = "404", description = "找不到指定 ID 的機器人",
                    content = @Content)
    })
    public ResponseEntity<Void> deleteBot(
            @Parameter(description = "機器人 ID", required = true)
            @RequestBody BotIdRequest request) {
        log.info("刪除機器人 ID: {}", request.getId());

        Optional<BotEntity> existingBot = botService.getBotById(request.getId());
        if (existingBot.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        botService.deleteBot(request.getId());
        return ResponseEntity.noContent().build();
    }
}
