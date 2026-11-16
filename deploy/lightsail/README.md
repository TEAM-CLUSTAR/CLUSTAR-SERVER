# CLUSTAR 개발 서버 배포

Lightsail 한 대에서 Caddy, PostgreSQL 16 + pgvector, Redis를 계속 실행합니다. GitHub Actions는 `develop`의 코드·배포 설정 변경을 Jib 이미지로 만들어 SSH로 전송합니다. Caddy는 비활성 앱 슬롯의 readiness에 DB와 Redis가 포함된 것을 확인한 후 트래픽을 전환합니다. 전환 후 HTTPS 응답이 새 슬롯에서 오는지도 검증합니다.

## 최초 구성

1. Lightsail에 Docker와 Compose 2.30 이상을 설치합니다. SSH 계정 `ubuntu`에서 `sudo -n docker`가 동작해야 합니다.
2. Route 53의 API 도메인을 고정 IP에 연결합니다. Lightsail 방화벽에서 80/443과 배포 러너가 접근 가능한 SSH를 허용합니다.
3. `deploy/lightsail` 파일을 서버의 임시 경로에 복사하고 `bash bootstrap.sh <API 도메인>`을 한 번 실행합니다. `/opt/clustar/infra.env`에 DB 비밀번호를 별도로 생성하고, PostgreSQL/Redis/Caddy의 데이터는 각각 Docker 볼륨에 저장합니다. 초기 빈 DB에는 벡터 확장, 768차원 벡터 테이블과 채팅 메모리 테이블을 준비합니다. 기존 RDS 데이터를 유지하려면 앱의 첫 배포 전에 별도로 복구 검증을 진행합니다.
4. 아래 GitHub Secrets를 준비하고, Google OAuth의 승인된 리다이렉트 URI에도 동일한 주소를 등록합니다. 첫 앱 배포 전에는 HTTPS가 연결되어도 Caddy가 503을 반환합니다.

앱 배포 작업은 앱 슬롯만 교체합니다. Caddy 기본 설정, PostgreSQL 또는 Redis 이미지 변경은 서버에서 따로 적용합니다. `init-db.sh`는 새 PostgreSQL 볼륨을 처음 열 때 실행됩니다.

## GitHub Secrets

| 작업 | 이름 | 값 |
| --- | --- | --- |
| 추가 | `LIGHTSAIL_HOST` | Lightsail 고정 IP 또는 해당 서버를 가리키는 DNS 이름 |
| 추가 | `LIGHTSAIL_SSH_KEY` | 서버의 `ubuntu` 계정에 공개키를 등록한 전용 배포 개인키. CI용 키는 암호 입력이 필요 없어야 합니다. |
| 추가 | `LIGHTSAIL_KNOWN_HOSTS` | `<LIGHTSAIL_HOST 값> ssh-ed25519 <서버 공개 호스트 키>`. 신뢰할 수 있는 서버 접속에서 `/etc/ssh/ssh_host_ed25519_key.pub`의 앞 두 필드를 확인합니다. |
| 확인·수정 | `GOOGLE_REDIRECT_URI` | 공개 API 도메인의 `https://<API 도메인>/oauth/google/callback` |
| 유지 | `JWT_SECRET_KEY`, `GOOGLE_CLIENT_ID`, `GOOGLE_CLIENT_SECRET`, `GOOGLE_AI_API_KEY`, `SWAGGER_USERNAME`, `SWAGGER_PASSWORD` | 기존 앱 값 |
| 유지 (선택) | `DISCORD_WEBHOOK` | 배포 알림용 |
| S3 기능 사용 시 추가 | `LIGHTSAIL_S3_ACCESS_KEY_ID`, `LIGHTSAIL_S3_SECRET_ACCESS_KEY` | 앱 버킷 접근에 필요한 최소 권한의 키 쌍. 둘 다 지정합니다. |
| 이 CD에서 참조 제거 | `EC2_DEV_HOST`, `EC2_DEV_USER`, `EC2_SSH_KEY`, `ECR_REPOSITORY`, `AWS_ACCESS_KEY_ID`, `AWS_SECRET_ACCESS_KEY`, `DEV_DB_URL`, `DEV_DB_USERNAME`, `DEV_DB_PASSWORD`, `FRONTEND_OAUTH_CALLBACK_URL` | 다른 작업에서 쓰는지 확인한 후 정리합니다. DB 연결 값은 서버의 Compose에 있습니다. |

전용 SSH 키를 준비할 때는 기존 Lightsail 키로 서버에 접속해 새 공개키를 등록합니다. 아래 예시는 고정 IP를 `LIGHTSAIL_HOST`로 사용하는 경우입니다. `LIGHTSAIL_KNOWN_HOSTS`의 호스트 값도 똑같이 지정합니다.

```bash
lightsail_ip='Lightsail 고정 IP로 바꾸세요'
existing_key='/absolute/path/to/existing-lightsail-key.pem'
ssh-keygen -t ed25519 -f ~/.ssh/clustar-dev-ci -C clustar-dev-ci -N ''
ssh -i "$existing_key" "ubuntu@$lightsail_ip" 'umask 077; cat >> ~/.ssh/authorized_keys' < ~/.ssh/clustar-dev-ci.pub
gh secret set LIGHTSAIL_SSH_KEY < ~/.ssh/clustar-dev-ci
gh secret set LIGHTSAIL_HOST --body "$lightsail_ip"
ssh -i "$existing_key" "ubuntu@$lightsail_ip" 'cut -d " " -f 1,2 /etc/ssh/ssh_host_ed25519_key.pub' |
  sed "s/^/$lightsail_ip /" | gh secret set LIGHTSAIL_KNOWN_HOSTS
```

## 장애 대응과 백업

- Actions의 [develop-cd] 워크플로에서 `develop`을 선택해 재실행할 수 있습니다. 배포 전 readiness가 실패하면 기존 앱을 유지합니다. 전환 후 확인이 실패하면 기존 Caddy 경로를 복원합니다.
- 이전 슬롯 컨테이너가 보존되어 있으면 서버에서 `bash /opt/clustar/deploy.sh rollback`으로 되돌릴 수 있습니다. 실패 후보가 이전 슬롯을 이미 교체했다면 필요한 커밋의 이미지로 다시 배포합니다.
- 서버에는 커밋 SHA 태그로 앱 이미지를 보관합니다. 디스크 정리 시 현재 및 직전 슬롯이 참조하는 이미지를 확인한 후 나머지 태그를 선별해 제거합니다.
- 블루그린 앱은 DB를 공유합니다. 배포에 스키마 변경이 포함되면 양쪽 버전이 같이 읽고 쓸 수 있는지 확인합니다. 앱 롤백은 DB 스키마를 되돌리지 않습니다. Caddy 전환 후 이전 슬롯에 30초의 유예 시간을 두고 Spring이 종료를 기다립니다. 30초를 넘는 스트리밍 연결에는 재연결이 필요할 수 있습니다.
- S3 고아 객체 정리는 DB가 기존 S3 객체의 참조를 모르는 상태에서 파일이 삭제될 수 있어 Lightsail에서 비활성화되어 있습니다. S3를 사용하는 기능 자체에는 별도로 버킷 권한이 필요합니다.
- Lightsail에서는 로그인 없이 테스트 토큰을 발급하는 `/access`를 비활성화합니다.
- DB 백업은 다른 머신에서 아래처럼 받습니다. 주기적으로 별도 저장소에 보관하고 복구도 검증합니다.

  ```bash
  umask 077
  ssh -i "$existing_key" "ubuntu@$lightsail_ip" 'sudo -n docker exec clustar-postgres-1 pg_dump -U postgres -Fc clustar' > clustar.dump
  ```

[develop-cd]: ../../.github/workflows/develop-cd.yml
