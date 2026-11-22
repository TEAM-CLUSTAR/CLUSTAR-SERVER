# 개발 서버 모니터링

별도 Compose 프로젝트에서 Prometheus, Grafana, node-exporter, cAdvisor, PostgreSQL exporter, Redis exporter를 실행합니다. 애플리케이션 배포 슬롯과 독립적으로 유지되며 기존 Docker 네트워크를 통해 지표를 수집합니다. Prometheus는 30초마다 수집하고 최대 7일 또는 2GB로 보관합니다. Blue/green 중 비활성 슬롯의 수집 상태가 0인 것은 정상입니다.

## 설치 및 변경

기존 Lightsail 애플리케이션을 먼저 설치하고 DNS의 Grafana 호스트명을 해당 서버 공인 IP로 연결합니다. 방화벽에는 80/443을 열어야 Caddy가 인증서를 발급하고 갱신할 수 있습니다. 저장소 체크아웃에서 다음을 실행합니다.

```bash
GRAFANA_DOMAIN=<grafana-호스트명> bash deploy/monitoring/install.sh
```

설치 스크립트는 배포 잠금을 사용하고 별도 PostgreSQL 모니터링 계정, 비밀값, Grafana용 Caddy 설정을 만듭니다. 모니터링 설정을 갱신할 때도 같은 스크립트를 실행합니다. Caddy 설정 복구 표식이 있으면 먼저 기존 배포 복구 절차를 따릅니다. `/opt/clustar-monitoring/monitoring.env`를 안전하게 백업해야 기존 Grafana 관리자와 DB 모니터링 계정을 재사용할 수 있습니다. DB 계정이 존재하지만 환경 파일이 없으면 설치가 중단됩니다. 비밀번호는 해당 파일의 `GRAFANA_ADMIN_PASSWORD`이며 최초 로그인 후 변경합니다. Grafana 계정 변경은 Grafana 데이터 볼륨에 저장되므로 초기 환경변수는 이후 로그인 비밀번호를 갱신하지 않습니다.

Grafana는 HTTPS와 로그인으로 접근합니다. Prometheus와 exporter는 호스트에 포트를 공개하지 않고 Grafana의 호스트 포트는 루프백에만 바인딩합니다. 공개 프록시의 Prometheus actuator 경로는 차단합니다. Grafana 대시보드는 개발 서버(호스트·컨테이너), Spring 및 JVM(HTTP·배포 슬롯 포함), PostgreSQL, Redis로 나뉩니다. cAdvisor는 Docker 소켓과 호스트 정보에 접근하므로 이미지 업데이트와 호스트 접근 권한 관리에 주의합니다. 컨테이너 간 메트릭 네트워크는 암호화되지 않습니다.

## 디스코드 알림

Grafana는 1분마다 규칙을 평가하며, 조건이 지정된 시간 동안 지속되면 알림을 발생시킵니다.

| 조건 | 지속 시간 |
| --- | --- |
| 서버 CPU 사용률 85% 초과 | 10분 |
| 서버 가용 메모리 15% 미만 | 5분 |
| 루트 디스크 사용률 85% 초과 | 10분 |
| Spring blue·green 슬롯 모두 응답 없음 | 3분 |
| PostgreSQL 또는 해당 exporter 연결 실패 | 3분 |
| Redis 또는 해당 exporter 연결 실패 | 3분 |
| JVM 힙 사용률 85% 초과 | 10분 |
| 서버 지표 exporter 응답 없음 | 3분 |

알림은 수신 경로를 연결해야 전송됩니다. Grafana의 **Alerting → Contact points**에서 Discord 유형의 수신처를 만들고 웹훅 URL을 입력한 뒤 테스트합니다. **Notification policies**에서 `service=clustar-dev` 라벨에 해당 수신처를 연결합니다. 정상 복구 알림도 받을 수 있습니다. 웹훅 URL은 비밀번호처럼 취급하고 저장소에 커밋하지 않습니다. 규칙은 파일로 관리되므로 변경 후 설치 스크립트를 실행해야 반영되며 이때 Grafana가 잠시 재시작됩니다.

Grafana 데이터와 Prometheus 데이터는 Docker 볼륨에 저장됩니다. 시스템 디스크 및 메모리 사용량이 늘어나며 별도 백업은 구성하지 않습니다. PostgreSQL·Redis·모니터링이 한 서버를 공유하므로 서버 장애 시 모두 함께 영향을 받습니다. Grafana 또는 서버 자체가 내려가면 자체 알림을 전송할 수 없습니다. GitHub Actions Secrets에 추가할 항목은 없습니다.
