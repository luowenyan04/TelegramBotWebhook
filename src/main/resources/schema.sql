CREATE TABLE IF NOT EXISTS telegram_bots (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    username VARCHAR(255) NOT NULL,
    token VARCHAR(255) NOT NULL,
    enable BOOLEAN DEFAULT TRUE,
    CONSTRAINT uk_telegram_bots_username UNIQUE (username),
    INDEX idx_telegram_bots_enable (enable)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;