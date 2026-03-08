@echo off
echo 🚀 AutoTrack Docker Deployment with Cloudflare Tunnel
echo =====================================================

REM Check if .env file exists
if not exist ".env" (
    echo ⚠️  Creating .env file from template...
    copy .env.example .env
    echo ✅ Please edit .env file with your configuration before continuing.
    echo    Required: GITHUB_CLIENT_ID, GITHUB_CLIENT_SECRET, JWT_SECRET, ENCRYPTION_KEY
    echo.
    pause
)

echo 🔨 Building and starting AutoTrack with Cloudflare tunnel...
docker-compose up --build -d

echo.
echo 🎉 AutoTrack is starting with Cloudflare tunnel!
echo 📱 Local access: http://localhost:5000
echo 🌐 Public access: Check Docker logs for your Cloudflare tunnel URL
echo.
echo 📋 To view logs: docker-compose logs -f autotrack-app
echo 🛑 To stop: docker-compose down
echo.
echo ⏳ Waiting for application to be ready...

REM Wait for health check
for /L %%i in (1,1,30) do (
    curl -f http://localhost:5000/actuator/health >nul 2>&1
    if not errorlevel 1 (
        echo ✅ Application is ready!
        echo 🌐 Check the logs above for your Cloudflare tunnel URL
        echo 📋 Run 'docker-compose logs -f autotrack-app' to see the tunnel URL
        goto :end
    )
    echo ⏳ Waiting... (%%i/30)
    timeout /t 3 /nobreak >nul
)

:end
pause