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

    @Override
    public List<BotEntity> getAllBots() {
        return botRepository.findAll();
    }

    @Override
    public List<BotEntity> getEnabledBots() {
        return botRepository.findByEnableTrue();
    }

    @Override
    @Cacheable(value = CacheConfig.BOT_CACHE, key = "#username")
    public Optional<BotEntity> getBotByUsername(String username) {
        return botRepository.findByUsername(username);
    }

    @Override
    @Cacheable(value = CacheConfig.BOT_CACHE, key = "#id")
    public Optional<BotEntity> getBotById(Long id) {
        return botRepository.findById(id);
    }

    @Override
    @Transactional
    public BotEntity createBot(BotEntity botEntity) {
        log.info("Create new bot: {}", botEntity.getUsername());
        return saveBot(botEntity);
    }

    @Override
    @Transactional
    public BotEntity updateBot(BotEntity botEntity) {
        log.info("Update bot ID: {}", botEntity.getId());

        Optional<BotEntity> existingBot = getBotById(botEntity.getId());
        if (existingBot.isEmpty()) {
            log.warn("Bot not found for update, ID: {}", botEntity.getId());
            return null;
        }

        return saveBot(botEntity);
    }

    @Override
    @Transactional
    @CacheEvict(value = CacheConfig.BOT_CACHE, key = "#botEntity.username")
    public BotEntity saveBot(BotEntity botEntity) {
        log.info("Saving bot: {}", botEntity.getUsername());

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

        BotEntity savedBot = botRepository.save(botEntity);

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

        if (savedBot.getId() != null) {
            evictBotCacheById(savedBot.getId());
        }

        return savedBot;
    }

    @Override
    @Transactional
    public void enableBot(Long id) {
        log.info("Enable bot ID: {}", id);
        Optional<BotEntity> existingBot = getBotById(id);

        if (existingBot.isEmpty()) {
            log.warn("Bot not found for enable, ID: {}", id);
            return;
        }

        BotEntity bot = existingBot.get();
        String username = bot.getUsername();
        boolean wasEnabled = Boolean.TRUE.equals(bot.getEnable());

        if (wasEnabled) {
            log.debug("Bot already enabled, username: {}", username);
            return;
        }

        bot.setEnable(true);
        botRepository.save(bot);

        evictBotCache(username);
        evictBotCacheById(id);

        log.info("Registering bot webhook: {}", username);
        botManager.registerWebhook(bot);
    }

    @Override
    @Transactional
    public void disableBot(Long id) {
        log.info("Desable bot ID: {}", id);
        Optional<BotEntity> existingBot = getBotById(id);

        if (existingBot.isEmpty()) {
            log.warn("Bot not found for disabled, ID: {}", id);
            return;
        }

        BotEntity bot = existingBot.get();
        String username = bot.getUsername();

        if (Boolean.FALSE.equals(bot.getEnable())) {
            log.info("Bot already disabled, username: {}", username);
            return;
        }

        bot.setEnable(false);
        botRepository.save(bot);

        evictBotCache(username);
        evictBotCacheById(id);

        if (botManager.isWebhookRegistered(username)) {
            log.info("Deregistering webhook: {}", username);
            botManager.deregisterWebhook(username);
        }
    }

    @Override
    @Transactional
    public void deleteBot(Long id) {
        log.info("Deleting bot ID: {}", id);
        Optional<BotEntity> existingBot = getBotById(id);

        if (existingBot.isEmpty()) {
            log.warn("Bot not found for delete, ID: {}", id);
            return;
        }

        BotEntity bot = existingBot.get();
        String username = bot.getUsername();

        if (botManager.isWebhookRegistered(username)) {
            log.info("Deregistering bot with webhook: {}", username);
            botManager.deregisterWebhook(username);
        }

        botRepository.deleteById(id);

        evictBotCache(username);
        evictBotCacheById(id);

        log.info("Bot deleted: {}", username);
    }

    @Override
    @CacheEvict(value = CacheConfig.BOT_CACHE, key = "#username")
    public void evictBotCache(String username) {
        log.debug("clear bot cache: {}", username);
    }


    @Override
    @CacheEvict(value = CacheConfig.BOT_CACHE, key = "#id")
    public void evictBotCacheById(Long id) {
        log.debug("clear bot cache: {}", id);
    }

    @Override
    @CacheEvict(value = CacheConfig.BOT_CACHE, allEntries = true)
    public void evictAllBotCache() {
        log.debug("clear all bot cache");
    }

    @Override
    public boolean botExists(String username) {
        return botRepository.findByUsername(username).isPresent();
    }

    @Override
    public boolean botExistsAndEnabled(String username) {
        Optional<BotEntity> botOpt = getBotByUsername(username);
        return botOpt.isPresent() && Boolean.TRUE.equals(botOpt.get().getEnable());
    }

    @Override
    public long getBotCount() {
        return botRepository.count();
    }

    @Override
    public long getEnabledBotCount() {
        return getEnabledBots().size();
    }
}
