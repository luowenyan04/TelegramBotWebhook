# Telegram Bot 高可用性架構

這個專案實現了一個支援高可用性的 Telegram Bot 管理系統，允許使用者透過 API 管理多個 Telegram Bot，並確保系統的穩定性和可擴展性。整合了 Docker 和 ngrok，便於本地開發和測試。

## 功能特點

- 支援多 Bot 管理：創建、更新、啟用、停用和刪除 Telegram Bot
- 高可用性架構：支援多節點部署，確保系統穩定運行
- Webhook 自動註冊：自動處理與 Telegram API 的 Webhook 註冊
- 快取機制：使用本地快取提高性能，並透過 Kafka 同步多節點的快取
- Swagger API 文檔：提供完整的 API 說明文檔
- 基於 Docker 的部署：簡化系統部署和擴展
- 整合 ngrok：直接在 Docker 環境中啟用外部訪問

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
```

- **Nginx**：負載均衡器，分發來自 Telegram 的請求
- **Spring Boot 應用**：處理 Webhook 和 Bot 管理
- **MySQL**：存儲 Bot 配置和狀態
- **Kafka**：提供節點間的通信機制，用於快取同步
- **ngrok**：提供臨時公網地址，便於本地測試

## 快速開始

### 前置需求

- Docker 和 Docker Compose
- ngrok 帳號和 authtoken (免費版即可)

### 初次設置與運行

1. 克隆專案
   ```bash
   git clone https://github.com/yourusername/telegram-bot-webhook.git
   cd telegram-bot-webhook
   ```

2. 設置 ngrok authtoken
   在 `docker-compose.yml` 中設置您的 ngrok authtoken：
   ```yaml
   ngrok:
     environment:
       NGROK_AUTHTOKEN: your_ngrok_authtoken_here
   ```

### 使用 ngrok 地址更新配置

獲取到 ngrok 公網地址後，您需要更新 Spring Boot 應用的 webhookDomain 配置：

1. 通過 API 設置：
   ```bash
   curl -X POST http://localhost/api/config/webhook-domain \
     -H "Content-Type: application/json" \
     -d '{"domain": "https://xxxx-xx-xx-xx-xx.ngrok.io"}'
   ```

2. 或直接在應用節點的環境變數中設置（如果重新啟動容器）：
   ```yaml
   app-node-1:
     environment:
       TELEGRAM_BOT_WEBHOOK_DOMAIN: https://xxxx-xx-xx-xx-xx.ngrok.io
   ```

### API 使用

訪問 Swagger UI 了解和測試 API：
```
http://localhost/swagger-ui/index.html
```
或使用 ngrok 地址：
```
https://xxxx-xx-xx-xx-xx.ngrok.io/swagger-ui/index.html
```

主要 API 端點：
- `POST /api/bots` - 創建新 Bot
- `PUT /api/bots` - 更新 Bot
- `PUT /api/bots/enable` - 啟用 Bot
- `PUT /api/bots/disable` - 停用 Bot
- `DELETE /api/bots` - 刪除 Bot
- `GET /api/bots` - 查詢所有 Bot
- `GET /api/bots/bot?id={id}` - 查詢特定 Bot

## 配置說明

### application.yml

主要配置項：
```yaml
spring:
  datasource:
    url: jdbc:mysql://localhost:3306/telegrambot?useSSL=false&serverTimezone=UTC&allowPublicKeyRetrieval=true
    username: botuser
    password: botpassword
  
  kafka:
    bootstrap-servers: localhost:9092
    consumer:
      group-id: telegram-bot-group

kafka:
  topic:
    cache-clear: bot-cache-clear
    bot-update: bot-update
    webhook-registered: webhook-registered

telegram-bot:
  webhookDomain: ${TELEGRAM_BOT_WEBHOOK_DOMAIN:https://your-domain.com}
  registerPath: /webhook
```

### nginx/conf.d/default.conf

Nginx 配置範例：
```nginx
upstream telegram_bot_servers {
    server app-node-1:8080;
    server app-node-2:8080;
    # 選擇負載均衡策略，預設為輪詢
    # ip_hash;     # 按客戶端 IP 分配
    # least_conn;  # 按最少連接數分配
}

server {
    listen 80;
    server_name localhost;

    location /webhook {
        proxy_pass http://telegram_bot_servers;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
    }

    location /api {
        proxy_pass http://telegram_bot_servers;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
    }

    location /swagger-ui {
        proxy_pass http://telegram_bot_servers;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
    }
}
```

### Dockerfile

簡單的多階段構建 Dockerfile：
```dockerfile
# 構建階段
FROM maven:3.8.4-openjdk-21-slim AS builder
WORKDIR /app
COPY pom.xml .
COPY src ./src
RUN mvn clean package -DskipTests

# 運行階段
FROM openjdk:21-jdk-slim
WORKDIR /app
COPY --from=builder /app/target/*.jar app.jar
ENTRYPOINT ["java", "-jar", "app.jar"]
```

## 高可用性說明

這個系統透過以下方式實現高可用性：

1. **多節點部署**：可以同時運行多個應用節點
2. **負載均衡**：Nginx 分發請求到不同節點
3. **快取同步**：透過 Kafka 通知所有節點更新快取
4. **Webhook 管理**：確保 Webhook 只註冊一次，避免衝突

當一個節點更新了 Bot 資訊或註冊了 Webhook，它會發送消息通知其他節點更新其本地狀態，確保所有節點保持一致。

## 開發指南

### 新增功能

1. 在 `src/main/java/com/example/telegrambotwebhook` 目錄中添加相關代碼
2. 更新 API 文檔（使用 Swagger 註解）
3. 編寫單元測試
4. 構建並測試

### Docker 架構介紹

專案的 Docker 架構包含以下服務：

1. **MySQL**：資料庫服務
2. **Zookeeper & Kafka**：消息佇列服務
3. **Spring Boot 應用節點**：兩個或多個處理業務邏輯的應用節點
4. **Nginx**：負載均衡服務，分發請求到不同的應用節點
5. **ngrok**：提供公網訪問的臨時隧道服務

完整的 `docker-compose.yml` 範例：

```yaml
version: '3.8'

services:
  mysql:
    image: mysql:8.0
    environment:
      MYSQL_ROOT_PASSWORD: root
      MYSQL_DATABASE: telegrambot
      MYSQL_USER: botuser
      MYSQL_PASSWORD: botpassword
    volumes:
      - mysql-data:/var/lib/mysql
    
  zookeeper:
    image: confluentinc/cp-zookeeper:latest
    environment:
      ZOOKEEPER_CLIENT_PORT: 2181
      
  kafka:
    image: confluentinc/cp-kafka:latest
    depends_on:
      - zookeeper
    environment:
      KAFKA_BROKER_ID: 1
      KAFKA_ZOOKEEPER_CONNECT: zookeeper:2181
      KAFKA_ADVERTISED_LISTENERS: PLAINTEXT://kafka:29092,PLAINTEXT_HOST://localhost:9092
      KAFKA_LISTENER_SECURITY_PROTOCOL_MAP: PLAINTEXT:PLAINTEXT,PLAINTEXT_HOST:PLAINTEXT
      KAFKA_INTER_BROKER_LISTENER_NAME: PLAINTEXT
      KAFKA_OFFSETS_TOPIC_REPLICATION_FACTOR: 1
      
  app-node-1:
    build: .
    environment:
      SPRING_DATASOURCE_URL: jdbc:mysql://mysql:3306/telegrambot?useSSL=false&allowPublicKeyRetrieval=true
      SPRING_KAFKA_BOOTSTRAP_SERVERS: kafka:29092
      TELEGRAM_BOT_WEBHOOK_DOMAIN: https://xxxx-xx-xx-xx-xx.ngrok.io
      
  app-node-2:
    build: .
    environment:
      SPRING_DATASOURCE_URL: jdbc:mysql://mysql:3306/telegrambot?useSSL=false&allowPublicKeyRetrieval=true
      SPRING_KAFKA_BOOTSTRAP_SERVERS: kafka:29092
      TELEGRAM_BOT_WEBHOOK_DOMAIN: https://xxxx-xx-xx-xx-xx.ngrok.io
      
  nginx:
    image: nginx:latest
    ports:
      - "80:80"
    volumes:
      - ./nginx/conf.d:/etc/nginx/conf.d
      
  ngrok:
    image: ngrok/ngrok:latest
    ports:
      - "4040:4040"
    environment:
      NGROK_AUTHTOKEN: your_ngrok_authtoken_here
    command: http nginx:80

volumes:
  mysql-data:
```

## 故障排除

常見問題：

1. **MySQL 連接錯誤**：
    - 確保 MySQL 服務正常運行
    - 檢查連接字符串，特別是 `allowPublicKeyRetrieval=true` 設定
    - 如果出現 "Public Key Retrieval is not allowed" 錯誤，確保在連接字符串中添加 `allowPublicKeyRetrieval=true`

2. **Webhook 註冊失敗**：
    - 確保 ngrok 正常運行並獲取到公網地址
    - 檢查 `TELEGRAM_BOT_WEBHOOK_DOMAIN` 環境變數是否設置正確
    - 檢查 Telegram Bot Token 是否有效

3. **節點間通信問題**：
    - 確保 Kafka 服務正常運行
    - 檢查 Kafka 主題配置是否正確

4. **ngrok 相關問題**：
    - 如果 ngrok 啟動失敗，檢查 authtoken 是否正確
    - 使用 `docker-compose logs ngrok` 查看詳細日志
    - 免費版 ngrok 有連接數和帶寬限制，如需長期使用請考慮升級

## 貢獻指南

歡迎提交 Pull Request 和 Issue！

## 授權

[MIT](LICENSE)