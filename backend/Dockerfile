# 백엔드 Dockerfile - 멀티 스테이지 빌드
# Stage 1: 빌드 단계
FROM eclipse-temurin:17-jdk-alpine AS builder

WORKDIR /app

# Gradle Wrapper 및 빌드 파일 복사
COPY gradlew .
COPY gradle gradle
COPY build.gradle .
COPY settings.gradle .

# application-secret.yml 생성 (빌드 시)
RUN mkdir -p src/main/resources && \
    echo "# Placeholder for build" > src/main/resources/application-secret.yml

# 소스 코드 복사
COPY src src

# Gradle 실행 권한 부여
RUN chmod +x gradlew

# 애플리케이션 빌드
RUN ./gradlew clean build -x test

# Stage 2: 실행 단계
FROM eclipse-temurin:17-jre-alpine

# 작업 디렉토리 설정
WORKDIR /app

# 빌드된 JAR 파일 복사
COPY --from=builder /app/build/libs/*.jar app.jar

# 포트 노출
EXPOSE 8080

# 헬스체크
HEALTHCHECK --interval=30s --timeout=3s --start-period=40s --retries=3 \
  CMD wget --no-verbose --tries=1 --spider http://localhost:8080/api/health || exit 1

# 애플리케이션 실행
ENTRYPOINT ["java", "-jar", "app.jar"]
