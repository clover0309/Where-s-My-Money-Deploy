#!/bin/bash

# Docker 개발 환경 시작 스크립트

echo "=========================================="
echo "Docker 개발 환경 시작"
echo "=========================================="

# .env 파일 확인
if [ ! -f .env ]; then
    echo "⚠️  .env 파일이 없습니다."
    echo "📝 .env.example을 복사하여 .env 파일을 생성합니다..."
    cp .env.example .env
    echo "✓ .env 파일이 생성되었습니다."
    echo "⚠️  .env 파일을 편집하여 올바른 값을 설정해주세요."
    exit 1
fi

# Docker 설치 확인
if ! command -v docker &> /dev/null; then
    echo "❌ Docker가 설치되어 있지 않습니다."
    echo "Docker를 먼저 설치해주세요: https://docs.docker.com/get-docker/"
    exit 1
fi

if ! command -v docker compose &> /dev/null; then
    echo "❌ Docker Compose가 설치되어 있지 않습니다."
    exit 1
fi

echo "✓ Docker 설치 확인 완료"

# 기존 컨테이너 중지
echo ""
echo "📦 기존 컨테이너 중지 중..."
docker compose down

# 이미지 빌드
echo ""
echo "🔨 Docker 이미지 빌드 중..."
docker compose build

# 컨테이너 시작
echo ""
echo "🚀 컨테이너 시작 중..."
docker compose up -d

# 컨테이너 상태 확인
echo ""
echo "📊 컨테이너 상태:"
docker compose ps

# 헬스체크 대기
echo ""
echo "⏳ 헬스체크 대기 중 (30초)..."
sleep 30

# 헬스체크
echo ""
echo "🏥 헬스체크 실행 중..."

BACKEND_STATUS=$(curl -s -o /dev/null -w "%{http_code}" http://localhost:8080/api/health)
FRONTEND_STATUS=$(curl -s -o /dev/null -w "%{http_code}" http://localhost)

if [ "$BACKEND_STATUS" -eq 200 ]; then
    echo "✓ 백엔드: 정상 (HTTP $BACKEND_STATUS)"
else
    echo "✗ 백엔드: 오류 (HTTP $BACKEND_STATUS)"
fi

if [ "$FRONTEND_STATUS" -eq 200 ]; then
    echo "✓ 프론트엔드: 정상 (HTTP $FRONTEND_STATUS)"
else
    echo "✗ 프론트엔드: 오류 (HTTP $FRONTEND_STATUS)"
fi

echo ""
echo "=========================================="
echo "✓ Docker 환경이 시작되었습니다!"
echo "=========================================="
echo ""
echo "📍 접속 정보:"
echo "   프론트엔드: http://localhost"
echo "   백엔드 API: http://localhost:8080/api"
echo "   헬스체크: http://localhost:8080/api/health"
echo ""
echo "📝 유용한 명령어:"
echo "   로그 확인: docker compose logs -f"
echo "   중지: docker compose down"
echo "   재시작: docker compose restart"
echo ""
