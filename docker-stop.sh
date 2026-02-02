echo "=========================================="
echo "Docker 환경 중지"
echo "=========================================="

REMOVE_VOLUMES=false
if [ "$1" == "--volumes" ] || [ "$1" == "-v" ]; then
    REMOVE_VOLUMES=true
fi


echo "컨테이너 중지 중..."
if [ "$REMOVE_VOLUMES" = true ]; then
    echo "볼륨도 함께 제거됩니다 (데이터가 삭제됩니다!)"
    docker compose down -v
else
    docker compose down
fi

echo ""
echo "✓ Docker 환경이 중지되었습니다."
echo ""
echo "재시작하려면: ./docker-start.sh"
echo "볼륨 포함 삭제: ./docker-stop.sh --volumes"
