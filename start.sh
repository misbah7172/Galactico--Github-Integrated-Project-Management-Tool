#!/bin/bash

echo "🚀 AutoTrack Docker Deployment with Cloudflare Tunnel"
echo "====================================================="

# Check if .env file exists
if [ ! -f ".env" ]; then
    echo "⚠️  Creating .env file from template..."
    cp .env.example .env
    echo "✅ Please edit .env file with your configuration before continuing."
    echo "   Required: GITHUB_CLIENT_ID, GITHUB_CLIENT_SECRET, JWT_SECRET, ENCRYPTION_KEY"
    echo ""
    read -p "Press Enter after configuring .env file..."
fi

echo "🔨 Building and starting AutoTrack with Cloudflare tunnel..."
docker-compose up --build -d

echo ""
echo "🎉 AutoTrack is starting with Cloudflare tunnel!"
echo "📱 Local access: http://localhost:5000"
echo "🌐 Public access: Check Docker logs for your Cloudflare tunnel URL"
echo ""
echo "📋 To view logs: docker-compose logs -f autotrack-app"
echo "🛑 To stop: docker-compose down"
echo ""
echo "⏳ Waiting for application to be ready..."

# Wait for health check
for i in {1..30}; do
    if curl -f http://localhost:5000/actuator/health >/dev/null 2>&1; then
        echo "✅ Application is ready!"
        echo "🌐 Check the logs above for your Cloudflare tunnel URL"
        echo "📋 Run 'docker-compose logs -f autotrack-app' to see the tunnel URL"
        break
    fi
    echo "⏳ Waiting... ($i/30)"
    sleep 3
done