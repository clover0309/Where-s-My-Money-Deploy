DEPLOY_PATH="/home/ubuntu/wheresmymoney"
BACKEND_PATH="$DEPLOY_PATH/backend"
FRONTEND_PATH="$DEPLOY_PATH/frontend"
APP_NAME="wheresmymoney"

echo "=========================================="
echo "배포 시작"
echo "=========================================="

echo "[1/3] 백엔드 배포 중..."


PID=$(ps -ef | grep "$APP_NAME" | grep -v grep | awk '{print $2}')

if [ ! -z "$PID" ]; then
    echo "기존 프로세스 발견 (PID: $PID)"
    echo "프로세스 종료 중..."
    kill -15 $PID
    
    
    for i in {1..10}; do
        if ! ps -p $PID > /dev/null; then
            echo "프로세스 종료 완료"
            break
        fi
        echo "종료 대기 중... ($i/10)"
        sleep 1
    done
    
    if ps -p $PID > /dev/null; then
        echo "강제 종료 실행"
        kill -9 $PID
    fi
else
    echo "실행 중인 프로세스 없음"
fi

cd $BACKEND_PATH
echo "새 애플리케이션 시작 중..."
nohup java -jar *.jar \
    --spring.profiles.active=dev \
    > application.log 2>&1 &

NEW_PID=$!
echo "새 프로세스 시작 (PID: $NEW_PID)"


sleep 5
if ps -p $NEW_PID > /dev/null; then
    echo "백엔드 배포 완료"
else
    echo "백엔드 시작 실패"
    exit 1
fi

echo "[2/3] 프론트엔드 배포 중..."
echo "프론트엔드 배포 완료"

echo "[3/3] Nginx 재시작 중..."
sudo systemctl reload nginx
echo "Nginx 재시작 완료"

echo "=========================================="
echo "배포 완료!"
echo "백엔드 PID: $NEW_PID"
echo "로그 확인: tail -f $BACKEND_PATH/application.log"
echo "=========================================="
