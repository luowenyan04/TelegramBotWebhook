package com.example.telegrambotwebhook.repository;

import com.example.telegrambotwebhook.entity.BotEntity;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface BotRepository extends JpaRepository<BotEntity, Long> {

    List<BotEntity> findByEnableTrue();

    Optional<BotEntity> findByUsername(String username);
}
