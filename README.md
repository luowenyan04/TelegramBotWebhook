# Telegram Bot Multi-Node Webhook Example

## 系統架構

```
                      [ Telegram Server ]
                              |
                              | (Webhook POST)
                              v
                    [ ngrok 臨時公網地址 ]
                              |
                          [ Nginx ]
                              |
                +-------------+--------------+
                |                            |
        [ Spring Boot 節點 A ]       [ Spring Boot 節點 B ]
                |                            |
         +------+-------+           +--------+------+
         |              |           |               |
   handler(botA)   handler(botB)  handler(botA)   handler(botB)
         |              |           |               |
         +------+-------+           +--------+------+
                |                            |
                +-------------+--------------+
                              |
                            MySQL
```

## 快速開始

### 前置需求

- Docker 和 Docker Compose
- ngrok 帳號和 authtoken（免費版即可，請從 [ngrok 官網](https://ngrok.com/) 獲取）

### 自動化啟動（推薦）

我們提供了自動化腳本，讓您只需執行幾個簡單的步驟即可完成所有設置：

1. 克隆專案
   ```bash
   git clone https://github.com/yourusername/telegram-bot-webhook.git
   cd telegram-bot-webhook
   ```

2. 為啟動腳本添加執行權限
   ```bash
   chmod +x start.sh
   ```

3. 執行自動化啟動腳本（提供您的 ngrok authtoken）
   ```bash
   NGROK_AUTHTOKEN=your_token_here ./start.sh
   ```

腳本將自動完成以下操作：

- 配置 ngrok authtoken
- 啟動所有必要的 Docker 服務
- 自動獲取 ngrok 公網網址
- 配置 webhook 域名到應用程式
- 重啟應用服務以確保配置生效
- 顯示系統的訪問地址和說明

完成後，您可以通過顯示的 Swagger UI 地址（如 `https://xxxx-xx-xx-xx-xx.ngrok-free.app/swagger-ui/index.html`）來訪問和管理您的 Telegram Bot API。

### 手動設置

如果需要手動配置系統，請按照以下步驟：

1. 克隆專案並進入目錄
   ```bash
   git clone https://github.com/yourusername/telegram-bot-webhook.git
   cd telegram-bot-webhook
   ```

2. 在 `docker-compose.yml` 中設置 ngrok authtoken
   ```yaml
   ngrok:
     environment:
       NGROK_AUTHTOKEN: your_ngrok_authtoken_here
   ```

3. 啟動基礎設施服務
   ```bash
   docker-compose up -d mysql zookeeper kafka ngrok
   ```

4. 獲取 ngrok 公網地址
    - 開啟瀏覽器訪問 http://localhost:4040
    - 找到並記錄 https 公網網址

5. 更新 webhook 域名設定
   ```bash
   # 修改 docker-compose.yml 中的配置
   # TELEGRAM_BOT_WEBHOOK_DOMAIN: https://xxxx-xx-xx-xx-xx.ngrok-free.app
   ```

6. 啟動應用服務
   ```bash
   docker-compose up -d app-node-1 app-node-2 nginx
   ```

7. 重新啟動應用服務以確保配置生效
   ```bash
   docker-compose restart app-node-1 app-node-2
   ```

## API 使用

訪問 Swagger UI 了解和測試 API：

```
https://xxxx-xx-xx-xx-xx.ngrok-free.app/swagger-ui/index.html
```

主要 API 端點：

- `POST /api/bots` - 創建新 Bot
- `PUT /api/bots` - 更新 Bot
- `PUT /api/bots/enable` - 啟用 Bot
- `PUT /api/bots/disable` - 停用 Bot
- `DELETE /api/bots` - 刪除 Bot
- `GET /api/bots` - 查詢所有 Bot
- `GET /api/bots/bot?id={id}` - 查詢特定 Bot
- `POST /api/config/webhook-domain` - 更新 Webhook 域名

## 配置說明

### application.yml

主要配置項：

```yaml
spring:
  datasource:
    url: jdbc:mysql://mysql:3306/telegrambot?useSSL=false&serverTimezone=UTC&allowPublicKeyRetrieval=true&createDatabaseIfNotExist=true

  kafka:
    bootstrap-servers: kafka:29092

kafka:
  topic:
    cache-clear: bot-cache-clear     # 快取清除主題
    bot-update: bot-update           # Bot 更新通知主題
    webhook-registered: webhook-registered  # Webhook 註冊通知主題

telegram-bot:
  webhookDomain: ${TELEGRAM_BOT_WEBHOOK_DOMAIN:https://your-domain.com}
  registerPath: /webhook
```

### docker-compose.yml

服務配置：

```yaml
version: '3.8'

services:
  mysql:
    image: mysql:8.0
    # ...設定略...

  zookeeper:
    image: confluentinc/cp-zookeeper:latest
    # ...設定略...

  kafka:
    image: confluentinc/cp-kafka:latest
    # ...設定略...

  app-node-1:
    build: .
    environment:
      TELEGRAM_BOT_WEBHOOK_DOMAIN: https://xxxx-xx-xx-xx-xx.ngrok-free.app
      # ...其他設定略...

  app-node-2:
  # ...設定類似 app-node-1...

  nginx:
    image: nginx:latest
    # ...設定略...

  ngrok:
    image: ngrok/ngrok:latest
    environment:
      NGROK_AUTHTOKEN: your_ngrok_authtoken_here
    # ...設定略...
```

## 高可用性說明

這個系統透過以下方式實現高可用性：

1. **多節點部署**：可以同時運行多個應用節點
2. **負載均衡**：Nginx 分發請求到不同節點
3. **快取同步**：透過 Kafka 通知所有節點更新快取
4. **Webhook 管理**：確保 Webhook 只註冊一次，避免衝突

當一個節點更新了 Bot 資訊或註冊了 Webhook，它會發送消息通知其他節點更新其本地狀態，確保所有節點保持一致。

## 開發指南

### 開發環境設置

1. 克隆專案並在本地開發環境中打開
2. 確保安裝了 Java 21 和 Maven
3. 在 IDE 中運行應用程式之前，確保執行了 Docker 服務：
   ```bash
   docker-compose up -d mysql zookeeper kafka
   ```

### 新增功能

1. 在 `src/main/java/com/example/telegrambotwebhook` 目錄中添加相關代碼
2. 更新 API 文檔（使用 Swagger 註解）
3. 編寫單元測試
4. 構建並測試

## 故障排除

常見問題解決方案：

### MySQL 連接錯誤

- 確保 MySQL 服務正常運行
- 檢查連接字串，特別是 `allowPublicKeyRetrieval=true` 設定
- 如果出現 "Public Key Retrieval is not allowed" 錯誤，確保在連接字串中添加 `allowPublicKeyRetrieval=true`

### Webhook 註冊失敗

- 確保 ngrok 正常運行並獲取到公網地址
- 檢查 `TELEGRAM_BOT_WEBHOOK_DOMAIN` 環境變數是否設置正確
- 檢查 Telegram Bot Token 是否有效
- 使用 API 更新 webhook 域名：
  ```bash
  curl -X POST https://xxxx-xx-xx-xx-xx.ngrok-free.app/api/config/webhook-domain \
    -H "Content-Type: application/json" \
    -d '{"domain": "https://xxxx-xx-xx-xx-xx.ngrok-free.app"}'
  ```

### 節點間通信問題

- 確保 Kafka 服務正常運行
- 檢查 Kafka 主題配置是否正確

### ngrok 相關問題

- 如果 ngrok 啟動失敗，檢查 authtoken 是否正確
- 使用 `docker-compose logs ngrok` 查看詳細日誌
- 免費版 ngrok 有連接數和帶寬限制，如需長期使用請考慮升級

## 貢獻指南

歡迎提交 Pull Request 和 Issue！

## 授權

[MIT](LICENSE)