# Docker 배포 가이드

## 🐳 Docker 컨테이너화 개요

이 프로젝트는 Docker와 Docker Compose를 사용하여 컨테이너화되어 있습니다.

### 컨테이너 구성
- **Frontend**: Nginx 기반 React 애플리케이션
- **Backend**: Spring Boot 애플리케이션 (Java 17)
- **Database**: MySQL 8.0

## 🚀 빠른 시작

### 1. 사전 요구사항
- Docker 20.10 이상
- Docker Compose 2.0 이상

#### Ubuntu 서버에 Docker 설치
```bash
# Docker 설치
curl -fsSL https://get.docker.com -o get-docker.sh
sudo sh get-docker.sh

# Docker Compose 설치
sudo apt-get update
sudo apt-get install docker-compose-plugin

# 현재 사용자를 docker 그룹에 추가
sudo usermod -aG docker $USER
newgrp docker

# 설치 확인
docker --version
docker compose version
```

### 2. 환경 변수 설정

`.env` 파일 생성 (`.env.example` 참고):
```bash
cp .env.example .env
```

`.env` 파일 편집:
```env
# MySQL 환경 변수
MYSQL_ROOT_PASSWORD=your_secure_root_password
MYSQL_DATABASE=wheresmymoney
MYSQL_USER=wheresmymoney_user
MYSQL_PASSWORD=your_secure_password

# 키움증권 API
KIWOOM_APP_KEY=your_kiwoom_app_key
KIWOOM_SECRET_KEY=your_kiwoom_secret_key

# JWT
JWT_SECRET_KEY=your_jwt_secret_key_at_least_32_characters
```

### 3. 로컬에서 실행

```bash
# 모든 컨테이너 빌드 및 시작
docker compose up -d

# 로그 확인
docker compose logs -f

# 컨테이너 상태 확인
docker compose ps

# 중지
docker compose down

# 완전 제거 (볼륨 포함)
docker compose down -v
```

### 4. 접속

- **프론트엔드**: http://localhost
- **백엔드 API**: http://localhost:8080/api
- **헬스체크**: http://localhost:8080/api/health
- **MySQL**: localhost:3306

## 📦 Docker 이미지 빌드

### 백엔드 이미지 빌드
```bash
cd backend
./gradlew clean build -x test
docker build -t wheresmymoney-backend:latest .
```

### 프론트엔드 이미지 빌드
```bash
cd frontend
docker build -t wheresmymoney-frontend:latest .
```

## 🔄 GitHub Actions CI/CD 배포

### GitHub Secrets 설정

다음 secrets를 GitHub 저장소에 추가하세요:

#### 서버 정보
- `SERVER_HOST`: 서버 IP 또는 도메인
- `SERVER_USER`: SSH 사용자명
- `SSH_PRIVATE_KEY`: SSH 개인키
- `DEPLOY_PATH`: 배포 경로 (예: /home/ubuntu/wheresmymoney)

#### 데이터베이스
- `MYSQL_ROOT_PASSWORD`: MySQL root 비밀번호
- `MYSQL_DATABASE`: 데이터베이스 이름
- `MYSQL_USER`: MySQL 사용자명
- `MYSQL_PASSWORD`: MySQL 비밀번호

#### API 키
- `KIWOOM_APP_KEY`: 키움증권 앱 키
- `KIWOOM_SECRET_KEY`: 키움증권 시크릿 키
- `JWT_SECRET_KEY`: JWT 암호화 키

### 자동 배포 프로세스

1. **코드 Push**: main 브랜치에 push
2. **빌드 & 테스트**: 백엔드와 프론트엔드 빌드 및 테스트
3. **Docker 이미지 빌드**: GitHub Container Registry에 푸시
4. **서버 배포**: Ubuntu 서버에 Docker Compose로 배포

```bash
git add .
git commit -m "Update application"
git push origin main
```

## 🖥️ 서버 배포 설정

### 1. 서버 준비

```bash
# 배포 디렉토리 생성
mkdir -p /home/ubuntu/wheresmymoney
cd /home/ubuntu/wheresmymoney

# GitHub Container Registry 로그인
echo YOUR_GITHUB_TOKEN | docker login ghcr.io -u YOUR_GITHUB_USERNAME --password-stdin
```

### 2. 수동 배포

```bash
# 최신 코드 가져오기
git clone https://github.com/YOUR_USERNAME/Wheres-My-Money.git
cd Wheres-My-Money

# .env 파일 설정
cp .env.example .env
nano .env

# 배포
docker compose up -d

# 헬스체크
curl http://localhost:8080/api/health
```

### 3. 업데이트

```bash
# 최신 이미지 pull
docker compose pull

# 컨테이너 재시작
docker compose up -d

# 또는 한 번에
docker compose pull && docker compose up -d
```

## 🔍 모니터링 & 트러블슈팅

### 로그 확인

```bash
# 모든 컨테이너 로그
docker compose logs -f

# 특정 컨테이너 로그
docker compose logs -f backend
docker compose logs -f frontend
docker compose logs -f database

# 최근 100줄만
docker compose logs --tail=100 backend
```

### 컨테이너 상태 확인

```bash
# 실행 중인 컨테이너
docker compose ps

# 리소스 사용량
docker stats

# 컨테이너 내부 접속
docker compose exec backend sh
docker compose exec database mysql -u root -p
```

### 헬스체크

```bash
# 백엔드 헬스체크
curl http://localhost:8080/api/health

# 프론트엔드 확인
curl http://localhost

# 모든 컨테이너 헬스 상태
docker compose ps
```

### 데이터베이스 백업

```bash
# 백업
docker compose exec database mysqldump -u root -p$MYSQL_ROOT_PASSWORD wheresmymoney > backup.sql

# 복원
docker compose exec -T database mysql -u root -p$MYSQL_ROOT_PASSWORD wheresmymoney < backup.sql
```

## 🔧 고급 설정

### 포트 변경

`docker-compose.yml` 파일에서 포트 매핑 수정:
```yaml
services:
  frontend:
    ports:
      - "8000:80"  # 호스트:컨테이너
  backend:
    ports:
      - "8081:8080"
```

### 볼륨 마운트 (개발 모드)

```yaml
services:
  backend:
    volumes:
      - ./backend:/app
      - /app/build
```

### 프로덕션 최적화

```yaml
services:
  backend:
    deploy:
      resources:
        limits:
          cpus: '2'
          memory: 2G
        reservations:
          cpus: '1'
          memory: 1G
    restart: unless-stopped
```

## 🛡️ 보안

### SSL/TLS 설정 (Nginx)

Nginx에 Let's Encrypt 인증서 추가:
```bash
# Certbot 설치
sudo apt-get install certbot python3-certbot-nginx

# 인증서 발급
sudo certbot --nginx -d yourdomain.com
```

### 방화벽 설정

```bash
# UFW 방화벽 설정
sudo ufw allow 22/tcp
sudo ufw allow 80/tcp
sudo ufw allow 443/tcp
sudo ufw enable
```

## 📊 성능 최적화

### Docker 이미지 크기 줄이기

- 멀티 스테이지 빌드 사용 (이미 적용됨)
- Alpine 리눅스 베이스 이미지 사용
- .dockerignore 파일 최적화

### 컨테이너 리소스 제한

```yaml
deploy:
  resources:
    limits:
      cpus: '1'
      memory: 512M
```

## 🔄 롤백

문제 발생 시 이전 버전으로 롤백:

```bash
# 이전 이미지 태그 확인
docker images

# 특정 버전으로 실행
docker compose down
docker tag ghcr.io/username/backend:sha-xxxxx ghcr.io/username/backend:latest
docker compose up -d
```

## 📝 일반적인 문제 해결

### 컨테이너가 시작되지 않음
```bash
docker compose logs backend
docker compose ps
```

### 데이터베이스 연결 실패
- `.env` 파일의 데이터베이스 정보 확인
- 네트워크 설정 확인: `docker network inspect wheresmymoney_wheresmymoney-network`

### 포트 충돌
```bash
# 사용 중인 포트 확인
sudo netstat -nltp | grep 8080
sudo lsof -i :8080
```

### 디스크 공간 부족
```bash
# 사용하지 않는 이미지/컨테이너 정리
docker system prune -a

# 볼륨 정리
docker volume prune
```
