#!/bin/bash

# 헬스체크 스크립트
# 배포 후 애플리케이션이 정상 작동하는지 확인

BACKEND_URL="http://localhost:8080/api/health"
FRONTEND_URL="http://localhost"
MAX_RETRY=30
RETRY_INTERVAL=2

echo "=========================================="
echo "헬스체크 시작"
echo "=========================================="

# 백엔드 헬스체크
echo "[1/2] 백엔드 헬스체크..."
for i in $(seq 1 $MAX_RETRY); do
    HTTP_STATUS=$(curl -s -o /dev/null -w "%{http_code}" $BACKEND_URL)
    
    if [ "$HTTP_STATUS" -eq 200 ]; then
        echo "✓ 백엔드 정상 (HTTP $HTTP_STATUS)"
        break
    elif [ $i -eq $MAX_RETRY ]; then
        echo "✗ 백엔드 응답 없음 (시도: $i/$MAX_RETRY)"
        echo "로그를 확인하세요: tail -f /home/ubuntu/wheresmymoney/backend/application.log"
        exit 1
    else
        echo "대기 중... (시도: $i/$MAX_RETRY, 상태: $HTTP_STATUS)"
        sleep $RETRY_INTERVAL
    fi
done

# 프론트엔드 헬스체크
echo "[2/2] 프론트엔드 헬스체크..."
HTTP_STATUS=$(curl -s -o /dev/null -w "%{http_code}" $FRONTEND_URL)

if [ "$HTTP_STATUS" -eq 200 ]; then
    echo "✓ 프론트엔드 정상 (HTTP $HTTP_STATUS)"
else
    echo "✗ 프론트엔드 오류 (HTTP $HTTP_STATUS)"
    echo "Nginx 상태 확인: sudo systemctl status nginx"
    exit 1
fi

echo "=========================================="
echo "✓ 모든 헬스체크 통과!"
echo "=========================================="
