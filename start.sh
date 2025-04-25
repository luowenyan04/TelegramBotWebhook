#!/bin/bash
set -e

echo "正在啟動服務..."

if [ -z "$NGROK_AUTHTOKEN" ]; then
  echo "請提供 ngrok authtoken，例如: NGROK_AUTHTOKEN=your_token ./start.sh"
  exit 1
fi

echo "設定 ngrok authtoken..."
perl -i -pe "s/NGROK_AUTHTOKEN:.*/NGROK_AUTHTOKEN: $NGROK_AUTHTOKEN/g" docker-compose.yml

# 如果有舊的備份檔案，刪除它
if [ -f "docker-compose.yml''" ]; then
  echo "清理舊的備份檔案..."
  rm "docker-compose.yml''"
fi

echo "啟動基礎設施服務..."
docker-compose up -d mysql zookeeper kafka ngrok

echo "正在等待 ngrok 服務啟動..."
sleep 15

echo "嘗試獲取 ngrok 公網網址..."
NGROK_PUBLIC_URL=""

# 嘗試多次獲取 ngrok 公網網址
for i in {1..5}; do
  # 嘗試第一種 API 路徑
  NGROK_PUBLIC_URL=$(curl -s http://localhost:4040/api/tunnels | grep -o "https://[^\"]*\.ngrok[-a-z]*\.app" | head -1)

  if [ ! -z "$NGROK_PUBLIC_URL" ]; then
    break
  fi

  # 嘗試第二種 API 路徑
  NGROK_PUBLIC_URL=$(curl -s http://localhost:4040/api/tunnels/http | grep -o "https://[^\"]*\.ngrok[-a-z]*\.app" | head -1)

  if [ ! -z "$NGROK_PUBLIC_URL" ]; then
    break
  fi

  # 嘗試舊版格式
  NGROK_PUBLIC_URL=$(curl -s http://localhost:4040/api/tunnels | grep -o "https://[^\"]*\.ngrok\.io" | head -1)

  if [ ! -z "$NGROK_PUBLIC_URL" ]; then
    break
  fi

  echo "嘗試 $i/5: 尚未獲取到 ngrok 公網網址，等待 5 秒後重試..."
  sleep 5
done

if [ -z "$NGROK_PUBLIC_URL" ]; then
  echo "無法自動獲取 ngrok 公網網址。"
  echo "請手動開啟瀏覽器訪問 http://localhost:4040 查看 ngrok 狀態。"
  echo "然後輸入您看到的 https 公網網址 (例如: https://xxxx-xx-xx-xx-xx.ngrok-free.app):"
  read NGROK_PUBLIC_URL

  if [ -z "$NGROK_PUBLIC_URL" ]; then
    echo "未提供有效的 ngrok 網址，系統將無法正確配置。"
    exit 1
  fi
fi

echo "成功獲取 ngrok 公網網址: $NGROK_PUBLIC_URL"

echo "更新 webhook 域名設定..."
perl -i -pe "s|TELEGRAM_BOT_WEBHOOK_DOMAIN:.*|TELEGRAM_BOT_WEBHOOK_DOMAIN: $NGROK_PUBLIC_URL|g" docker-compose.yml

echo "啟動應用服務..."
docker-compose up -d app-node-1 app-node-2 nginx

echo "等待應用服務啟動完成..."
sleep 10

echo "重新啟動應用服務以確保新配置生效..."
docker-compose restart app-node-1 app-node-2

echo "✅ 所有服務已啟動！"
echo ""
echo "系統訪問地址:"
echo "📡 API: $NGROK_PUBLIC_URL/api"
echo "📖 Swagger UI: $NGROK_PUBLIC_URL/swagger-ui/index.html"
echo "🔄 Webhook 路徑: $NGROK_PUBLIC_URL/webhook"
echo "🔍 ngrok 管理界面: http://localhost:4040"
echo ""
echo "如需手動更新 webhook 域名，請使用以下命令:"
echo "curl -X POST $NGROK_PUBLIC_URL/api/config/webhook-domain -H \"Content-Type: application/json\" -d '{\"domain\": \"$NGROK_PUBLIC_URL\"}'"