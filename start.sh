#!/bin/bash
set -e

echo "正在啟動服務..."

if [ -z "$NGROK_AUTHTOKEN" ]; then
  echo "使用預設 ngrok authtoken"
else
  echo "設定 ngrok authtoken..."
  perl -i -pe "s/NGROK_AUTHTOKEN:.*/NGROK_AUTHTOKEN: $NGROK_AUTHTOKEN/g" docker-compose.yml
fi

# 清理備份檔
if [ -f "docker-compose.yml''" ]; then
  rm "docker-compose.yml''"
fi

echo "啟動基礎設施服務..."
docker-compose up -d mysql zookeeper kafka

echo "啟動 ngrok 服務..."
docker-compose up -d ngrok
echo "等待 ngrok 服務啟動..."
sleep 8  # 等待 ngrok 啟動並建立連接

echo "嘗試獲取 ngrok 公網網址..."
NGROK_PUBLIC_URL=""

# 嘗試獲取 ngrok 公網網址
for i in {1..6}; do
  NGROK_PUBLIC_URL=$(curl -s http://localhost:4040/api/tunnels | grep -o "https://[^\"]*\.ngrok[-a-z]*\.app" | head -1)
  if [ ! -z "$NGROK_PUBLIC_URL" ]; then break; fi

  NGROK_PUBLIC_URL=$(curl -s http://localhost:4040/api/tunnels/http | grep -o "https://[^\"]*\.ngrok[-a-z]*\.app" | head -1)
  if [ ! -z "$NGROK_PUBLIC_URL" ]; then break; fi

  NGROK_PUBLIC_URL=$(curl -s http://localhost:4040/api/tunnels | grep -o "https://[^\"]*\.ngrok\.io" | head -1)
  if [ ! -z "$NGROK_PUBLIC_URL" ]; then break; fi

  echo "嘗試 $i/6: 等待 ngrok 網址..."
  sleep 3
done

if [ -z "$NGROK_PUBLIC_URL" ]; then
  echo "請手動輸入 ngrok 網址 (查看 http://localhost:4040):"
  read NGROK_PUBLIC_URL
  if [ -z "$NGROK_PUBLIC_URL" ]; then
    echo "未提供有效的 ngrok 網址，系統將無法正確配置。"
    exit 1
  fi
fi

echo "成功獲取 ngrok 公網網址: $NGROK_PUBLIC_URL"

echo "更新 webhook 域名設定..."
perl -i -pe "s|TELEGRAM_BOT_WEBHOOK_DOMAIN:.*|TELEGRAM_BOT_WEBHOOK_DOMAIN: $NGROK_PUBLIC_URL|g" docker-compose.yml

echo "啟動 nginx 和應用節點..."
docker-compose up -d nginx app-node-1 app-node-2

echo "✅ 所有服務已啟動！"
echo ""
echo "系統訪問地址:"
echo "📡 API: $NGROK_PUBLIC_URL/api"
echo "📖 Swagger UI: $NGROK_PUBLIC_URL/swagger-ui/index.html"
echo "🔄 Webhook 路徑: $NGROK_PUBLIC_URL/webhook"
echo "🔍 ngrok 管理界面: http://localhost:4040"