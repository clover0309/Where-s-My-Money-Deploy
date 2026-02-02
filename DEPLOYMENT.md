# GitHub Actions CI/CD 파이프라인 설정 가이드

이 프로젝트는 GitHub Actions를 사용하여 자동으로 빌드, 테스트 및 배포를 수행합니다.

## 🔧 필수 설정

### 1. GitHub Secrets 설정

GitHub 저장소의 **Settings > Secrets and variables > Actions**에서 다음 secrets를 추가하세요:

#### 서버 접속 정보
- `SERVER_HOST`: Ubuntu 서버의 IP 주소 또는 도메인
  - 예: `123.45.67.89` 또는 `myserver.com`
  
- `SERVER_USER`: SSH 접속 사용자명
  - 예: `ubuntu` 또는 `root`
  
- `SSH_PRIVATE_KEY`: SSH 개인키 (Private Key)
  - 서버 접속용 SSH 개인키 전체 내용
  
- `DEPLOY_PATH`: 배포 디렉토리 경로
  - 예: `/home/ubuntu/wheresmymoney` 또는 `/var/www/app`

#### API 키 정보
- `KIWOOM_APP_KEY`: 키움증권 앱 키
- `KIWOOM_SECRET_KEY`: 키움증권 시크릿 키
- `JWT_SECRET_KEY`: JWT 암호화 키

### 2. SSH 키 생성 및 설정

#### Ubuntu 서버에서 SSH 키 생성

```bash
# 로컬 또는 서버에서 SSH 키 생성
ssh-keygen -t rsa -b 4096 -C "github-actions"

# 공개키를 서버의 authorized_keys에 추가
cat ~/.ssh/id_rsa.pub >> ~/.ssh/authorized_keys
chmod 600 ~/.ssh/authorized_keys

# 개인키 내용을 복사 (GitHub Secrets에 등록)
cat ~/.ssh/id_rsa
```

#### GitHub Secrets에 개인키 등록

1. 위 명령어로 출력된 개인키 전체 내용 복사
2. GitHub > Settings > Secrets > Actions > New repository secret
3. Name: `SSH_PRIVATE_KEY`
4. Value: 복사한 개인키 전체 내용 붙여넣기

### 3. Ubuntu 서버 준비

#### Java 17 설치
```bash
sudo apt update
sudo apt install openjdk-17-jdk -y
java -version
```

#### Nginx 설치 (프론트엔드 서빙용)
```bash
sudo apt install nginx -y
sudo systemctl start nginx
sudo systemctl enable nginx
```

#### 배포 디렉토리 생성
```bash
mkdir -p /home/ubuntu/wheresmymoney/backend
mkdir -p /home/ubuntu/wheresmymoney/frontend
```

### 4. Nginx 설정

`/etc/nginx/sites-available/wheresmymoney` 파일 생성:

```nginx
server {
    listen 80;
    server_name your-domain.com;  # 또는 서버 IP

    # 프론트엔드
    location / {
        root /home/ubuntu/wheresmymoney/frontend;
        index index.html;
        try_files $uri $uri/ /index.html;
    }

    # 백엔드 API 프록시
    location /api {
        proxy_pass http://localhost:8080;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto $scheme;
    }
}
```

#### Nginx 설정 활성화
```bash
sudo ln -s /etc/nginx/sites-available/wheresmymoney /etc/nginx/sites-enabled/
sudo nginx -t
sudo systemctl reload nginx
```

## 🚀 배포 프로세스

### 자동 배포 트리거

다음 상황에서 자동으로 배포가 실행됩니다:

1. **main 브랜치에 push할 때**
   ```bash
   git push origin main
   ```

2. **Pull Request가 main으로 merge될 때**

### 배포 단계

1. **빌드 단계**
   - 백엔드: Gradle로 JAR 파일 생성
   - 프론트엔드: React 앱 빌드

2. **테스트 단계**
   - 백엔드: JUnit 테스트 실행
   - 프론트엔드: React 테스트 실행

3. **배포 단계**
   - 빌드 결과물을 Ubuntu 서버로 전송
   - 기존 백엔드 프로세스 종료
   - 새 버전 백엔드 시작
   - 프론트엔드 파일 업데이트

## 📊 배포 모니터링

### GitHub Actions 탭에서 확인

1. GitHub 저장소의 **Actions** 탭 클릭
2. 각 워크플로우 실행 내역 확인
3. 실패한 단계가 있다면 로그 확인

### 서버에서 로그 확인

```bash
# 백엔드 로그 확인
tail -f /home/ubuntu/wheresmymoney/backend/application.log

# Nginx 로그 확인
sudo tail -f /var/log/nginx/access.log
sudo tail -f /var/log/nginx/error.log

# 백엔드 프로세스 확인
ps -ef | grep wheresmymoney
```

## 🔍 트러블슈팅

### SSH 연결 실패
- SSH 키가 올바르게 설정되었는지 확인
- 서버 방화벽에서 SSH 포트(22) 열려있는지 확인
- `SERVER_HOST`, `SERVER_USER` 값이 정확한지 확인

### 빌드 실패
- `application-secret.yml` 관련 에러: GitHub Secrets 값 확인
- Gradle 에러: Java 버전 확인 (17 필요)
- npm 에러: Node.js 버전 확인 (20 권장)

### 배포 후 서비스 안됨
- 백엔드 프로세스 실행 확인: `ps -ef | grep wheresmymoney`
- 포트 8080이 열려있는지 확인: `netstat -nltp | grep 8080`
- Nginx 설정 확인: `sudo nginx -t`

## 📝 수동 배포 (필요시)

GitHub Actions 없이 수동으로 배포하려면:

```bash
# 백엔드 빌드
cd backend
./gradlew clean build -x test

# 서버로 전송
scp build/libs/*.jar user@server:/path/to/deploy/backend/

# 서버에서 실행
ssh user@server
cd /path/to/deploy/backend
java -jar *.jar
```

## 🎯 다음 단계 (선택사항)

- [ ] Docker 컨테이너화
- [ ] 무중단 배포 (Blue-Green Deployment)
- [ ] 자동 롤백 설정
- [ ] Slack/Discord 알림 연동
- [ ] 성능 모니터링 도구 추가
