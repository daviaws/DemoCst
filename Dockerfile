FROM openjdk:22-jdk-slim
 
WORKDIR /app
 
RUN apt-get update && apt-get install -y \
    x11-apps \
    libxext6 \
    libxrender1 \
    libxtst6 \
    libxi6 \
    libxrandr2 \
    libasound2 \
    && rm -rf /var/lib/apt/lists/*
 
COPY . .
RUN chmod +x gradlew
RUN ./gradlew build
 
CMD ["sh", "-c", "./gradlew run --no-daemon"]