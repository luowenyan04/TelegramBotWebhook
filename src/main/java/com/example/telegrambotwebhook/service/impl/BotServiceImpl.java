package com.example.telegrambotwebhook.service.impl;

import com.example.telegrambotwebhook.config.CacheConfig;
import com.example.telegrambotwebhook.entity.BotEntity;
import com.example.telegrambotwebhook.repository.BotRepository;
import com.example.telegrambotwebhook.service.BotManager;
import com.example.telegrambotwebhook.service.BotService;
import jakarta.transaction.Transactional;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class BotServiceImpl implements BotService {

    private final BotRepository botRepository;
    private final BotManager botManager;

    /**
     * 獲取所有機器人
     */
    @Override
    public List<BotEntity> getAllBots() {
        log.debug("獲取所有機器人");
        return botRepository.findAll();
    }

    /**
     * 獲取所有啟用的機器人
     */
    @Override
    public List<BotEntity> getEnabledBots() {
        log.debug("獲取所有啟用的機器人");
        return botRepository.findByEnableTrue();
    }

    /**
     * 根據用戶名獲取機器人 (帶快取)
     */
    @Override
    @Cacheable(value = CacheConfig.BOT_CACHE, key = "#username")
    public Optional<BotEntity> getBotByUsername(String username) {
        log.debug("通過用戶名獲取機器人: {}", username);
        return botRepository.findByUsername(username);
    }

    /**
     * 根據ID獲取機器人 (帶快取)
     */
    @Override
    @Cacheable(value = CacheConfig.BOT_CACHE, key = "#id")
    public Optional<BotEntity> getBotById(Long id) {
        log.debug("通過ID獲取機器人: {}", id);
        return botRepository.findById(id);
    }

    /**
     * 創建新機器人
     */
    @Override
    @Transactional
    public BotEntity createBot(BotEntity botEntity) {
        log.info("創建新機器人: {}", botEntity.getUsername());
        return saveBot(botEntity);
    }

    /**
     * 更新機器人資訊
     */
    @Override
    @Transactional
    public BotEntity updateBot(BotEntity botEntity) {
        log.info("更新機器人 ID: {}", botEntity.getId());

        Optional<BotEntity> existingBot = getBotById(botEntity.getId());
        if (existingBot.isEmpty()) {
            log.warn("找不到要更新的機器人 ID: {}", botEntity.getId());
            return null;
        }

        return saveBot(botEntity);
    }

    /**
     * 儲存或更新機器人
     */
    @Override
    @Transactional
    @CacheEvict(value = CacheConfig.BOT_CACHE, key = "#botEntity.username")
    public BotEntity saveBot(BotEntity botEntity) {
        log.info("儲存機器人: {}", botEntity.getUsername());

        // 檢查是否為新機器人
        boolean isNewBot = botEntity.getId() == null;

        // 檢查機器人狀態變更
        boolean wasEnabled = false;
        if (!isNewBot) {
            Optional<BotEntity> existingBot = botRepository.findById(botEntity.getId());
            if (existingBot.isPresent()) {
                wasEnabled = Boolean.TRUE.equals(existingBot.get().getEnable());

                // 如果使用者名稱變更，還需要清除舊使用者名稱的快取
                if (!existingBot.get().getUsername().equals(botEntity.getUsername())) {
                    evictBotCache(existingBot.get().getUsername());
                }
            }
        }

        // 儲存到資料庫
        BotEntity savedBot = botRepository.save(botEntity);

        // 處理狀態變更
        boolean isEnabled = Boolean.TRUE.equals(savedBot.getEnable());

        if (isEnabled) {
            // 如果是已啟用的機器人，則註冊 webhook
            if (isNewBot || !wasEnabled || !botManager.isWebhookRegistered(savedBot.getUsername())) {
                log.info("註冊機器人 webhook: {}", savedBot.getUsername());
                botManager.registerWebhook(savedBot);
            }
        } else if (wasEnabled) {
            // 如果從啟用變為停用，則取消註冊 webhook
            if (botManager.isWebhookRegistered(savedBot.getUsername())) {
                log.info("取消註冊已停用的機器人 webhook: {}", savedBot.getUsername());
                botManager.deregisterWebhook(savedBot.getUsername());
            }
        }

        // 同時清除ID快取
        if (savedBot.getId() != null) {
            evictBotCacheById(savedBot.getId());
        }

        return savedBot;
    }

    /**
     * 啟用機器人
     */
    @Override
    @Transactional
    public void enableBot(Long id) {
        log.info("啟用機器人 ID: {}", id);
        Optional<BotEntity> existingBot = getBotById(id);

        if (existingBot.isEmpty()) {
            log.warn("找不到要啟用的機器人 ID: {}", id);
            return;
        }

        BotEntity bot = existingBot.get();
        String username = bot.getUsername();
        boolean wasEnabled = Boolean.TRUE.equals(bot.getEnable());

        // 如果已經啟用，則不做任何操作
        if (wasEnabled) {
            log.info("機器人 {} 已經是啟用狀態", username);
            return;
        }

        // 更新資料庫中的狀態
        bot.setEnable(true);
        botRepository.save(bot);

        // 清除快取
        evictBotCache(username);
        evictBotCacheById(id);

        // 註冊 webhook
        log.info("註冊機器人 webhook: {}", username);
        botManager.registerWebhook(bot);
    }

    /**
     * 停用機器人
     */
    @Override
    @Transactional
    public void disableBot(Long id) {
        log.info("停用機器人 ID: {}", id);
        Optional<BotEntity> existingBot = getBotById(id);

        if (existingBot.isEmpty()) {
            log.warn("找不到要停用的機器人 ID: {}", id);
            return;
        }

        BotEntity bot = existingBot.get();
        String username = bot.getUsername();

        // 如果已經停用，則不做任何操作
        if (Boolean.FALSE.equals(bot.getEnable())) {
            log.info("機器人 {} 已經是停用狀態", username);
            return;
        }

        // 更新資料庫中的狀態
        bot.setEnable(false);
        botRepository.save(bot);

        // 清除快取
        evictBotCache(username);
        evictBotCacheById(id);

        // 從 Telegram 取消註冊這個機器人的 webhook
        if (botManager.isWebhookRegistered(username)) {
            log.info("取消註冊機器人 webhook: {}", username);
            botManager.deregisterWebhook(username);
        }
    }

    /**
     * 刪除機器人
     */
    @Override
    @Transactional
    public void deleteBot(Long id) {
        log.info("刪除機器人 ID: {}", id);
        Optional<BotEntity> existingBot = getBotById(id);

        if (existingBot.isEmpty()) {
            log.warn("找不到要刪除的機器人 ID: {}", id);
            return;
        }

        BotEntity bot = existingBot.get();
        String username = bot.getUsername();

        // 從 Telegram 取消註冊這個機器人的 webhook
        if (botManager.isWebhookRegistered(username)) {
            log.info("取消註冊將被刪除的機器人 webhook: {}", username);
            botManager.deregisterWebhook(username);
        }

        // 從資料庫刪除
        botRepository.deleteById(id);

        // 清除快取
        evictBotCache(username);
        evictBotCacheById(id);

        log.info("機器人 {} 已從資料庫中刪除", username);
    }

    /**
     * 清除特定使用者名稱的機器人快取
     */
    @Override
    @CacheEvict(value = CacheConfig.BOT_CACHE, key = "#username")
    public void evictBotCache(String username) {
        log.debug("清除機器人快取: {}", username);
    }

    /**
     * 清除特定ID的機器人快取
     */
    @Override
    @CacheEvict(value = CacheConfig.BOT_CACHE, key = "#id")
    public void evictBotCacheById(Long id) {
        log.debug("清除機器人ID快取: {}", id);
    }

    /**
     * 清除所有機器人快取
     */
    @Override
    @CacheEvict(value = CacheConfig.BOT_CACHE, allEntries = true)
    public void evictAllBotCache() {
        log.info("開始清除所有機器人快取");
        long botCount = botRepository.count();
        log.info("系統中共有 {} 個機器人的快取資料被清除", botCount);
        log.info("快取清除成功完成");
    }

    /**
     * 檢查機器人是否存在
     */
    @Override
    public boolean botExists(String username) {
        return botRepository.findByUsername(username).isPresent();
    }

    /**
     * 檢查機器人是否存在且已啟用
     */
    @Override
    public boolean botExistsAndEnabled(String username) {
        Optional<BotEntity> botOpt = getBotByUsername(username);
        return botOpt.isPresent() && Boolean.TRUE.equals(botOpt.get().getEnable());
    }

    /**
     * 獲取機器人總數
     */
    @Override
    public long getBotCount() {
        return botRepository.count();
    }

    /**
     * 獲取啟用的機器人總數
     */
    @Override
    public long getEnabledBotCount() {
        return getEnabledBots().size();
    }
}
