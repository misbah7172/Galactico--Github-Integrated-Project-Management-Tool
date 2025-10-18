# Build stage
FROM maven:3.9.4-amazoncorretto-17 AS build

WORKDIR /app

# Copy pom.xml and download dependencies (for better caching)
COPY pom.xml .
RUN mvn dependency:go-offline -B

# Copy source code and build
COPY src ./src
RUN mvn clean package -DskipTests

# Runtime stage
FROM amazoncorretto:17-alpine3.18

# Install curl, bash, and download cloudflared
RUN apk add --no-cache \
    curl \
    bash \
    tzdata \
    && curl -L --output cloudflared.deb https://github.com/cloudflare/cloudflared/releases/latest/download/cloudflared-linux-amd64 \
    && install -m755 cloudflared.deb /usr/local/bin/cloudflared \
    && rm cloudflared.deb

# Create non-root user
RUN addgroup -g 1001 autotrack && \
    adduser -D -s /bin/bash -u 1001 -G autotrack autotrack

# Set working directory
WORKDIR /app

# Copy JAR from build stage
COPY --from=build /app/target/*.jar app.jar

# Create startup script
COPY <<EOF /app/start.sh
#!/bin/bash
set -e

echo "Starting AutoTrack application with Cloudflare tunnel..."

# Start the Spring Boot application in background
java \$JAVA_OPTS -jar app.jar &
APP_PID=\$!

echo "Application started with PID: \$APP_PID"
echo "Waiting for application to be ready..."

# Wait for application to be ready
for i in {1..30}; do
    if curl -f http://localhost:5000/actuator/health >/dev/null 2>&1; then
        echo "Application is ready!"
        break
    fi
    echo "Waiting for application... (\$i/30)"
    sleep 2
done

# Start Cloudflare tunnel with authentication
echo "Starting Cloudflare tunnel..."
echo "Authenticating with Cloudflare..."

# Set the tunnel token and start the tunnel
export TUNNEL_TOKEN=34EiSXp1nxwkIhmbPIOi4JZr3ay_3ZGiXzsZc56w3J81vwzD1
cloudflared tunnel --url http://localhost:5000 &
CF_PID=\$!

echo "Cloudflare tunnel started with PID: \$CF_PID"
echo "Your application will be accessible via Cloudflare tunnel"
echo "Check the logs above for the tunnel URL"

# Wait for either process to exit
wait \$APP_PID \$CF_PID
EOF

RUN chmod +x /app/start.sh

# Change ownership
RUN chown -R autotrack:autotrack /app

# Switch to non-root user
USER autotrack

# Health check
HEALTHCHECK --interval=30s --timeout=10s --start-period=30s --retries=3 \
    CMD curl -f http://localhost:5000/actuator/health || exit 1

# Expose port
EXPOSE 5000

# Set JVM options for containerized environment
ENV JAVA_OPTS="-XX:+UseContainerSupport -XX:MaxRAMPercentage=75.0 -Djava.security.egd=file:/dev/./urandom"

# Environment variables for database connection (already configured in application.properties)
ENV SPRING_PROFILES_ACTIVE=prod
ENV BASE_URL=https://misbah7172.loca.lt

# Run the startup script
ENTRYPOINT ["/app/start.sh"]
