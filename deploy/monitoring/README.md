# 개발 서버 모니터링

별도 Compose 프로젝트에서 Prometheus, Grafana, node-exporter, cAdvisor, PostgreSQL exporter, Redis exporter를 실행합니다. 애플리케이션 배포 슬롯과 독립적으로 유지되며 기존 Docker 네트워크를 통해 지표를 수집합니다. Prometheus는 30초마다 수집하고 최대 7일 또는 2GB로 보관합니다. Blue/green 중 비활성 슬롯의 수집 상태가 0인 것은 정상입니다.

## 설치 및 변경

기존 Lightsail 애플리케이션을 먼저 설치하고 DNS의 Grafana 호스트명을 해당 서버 공인 IP로 연결합니다. 방화벽에는 80/443을 열어야 Caddy가 인증서를 발급하고 갱신할 수 있습니다. 저장소 체크아웃에서 다음을 실행합니다.

```bash
GRAFANA_DOMAIN=<grafana-호스트명> bash deploy/monitoring/install.sh
```

설치 스크립트는 배포 잠금을 사용하고 별도 PostgreSQL 모니터링 계정, 비밀값, Grafana용 Caddy 설정을 만듭니다. 모니터링 설정을 갱신할 때도 같은 스크립트를 실행합니다. Caddy 설정 복구 표식이 있으면 먼저 기존 배포 복구 절차를 따릅니다. `/opt/clustar-monitoring/monitoring.env`를 안전하게 백업해야 기존 Grafana 관리자와 DB 모니터링 계정을 재사용할 수 있습니다. DB 계정이 존재하지만 환경 파일이 없으면 설치가 중단됩니다. 비밀번호는 해당 파일의 `GRAFANA_ADMIN_PASSWORD`이며 최초 로그인 후 변경합니다. Grafana 계정 변경은 Grafana 데이터 볼륨에 저장되므로 초기 환경변수는 이후 로그인 비밀번호를 갱신하지 않습니다.

Grafana는 HTTPS와 로그인으로 접근합니다. Prometheus와 exporter는 호스트에 포트를 공개하지 않고 Grafana의 호스트 포트는 루프백에만 바인딩합니다. 공개 프록시의 Prometheus actuator 경로는 차단합니다. Grafana 대시보드는 개발 서버(호스트·컨테이너), Spring 및 JVM(HTTP·배포 슬롯 포함), PostgreSQL, Redis로 나뉩니다. cAdvisor는 Docker 소켓과 호스트 정보에 접근하므로 이미지 업데이트와 호스트 접근 권한 관리에 주의합니다. 컨테이너 간 메트릭 네트워크는 암호화되지 않습니다.

Grafana 데이터와 Prometheus 데이터는 Docker 볼륨에 저장됩니다. 시스템 디스크 및 메모리 사용량이 늘어나며 자동 알림과 별도 백업은 구성하지 않습니다. PostgreSQL·Redis·모니터링이 한 서버를 공유하므로 서버 장애 시 모두 함께 영향을 받습니다. GitHub Actions Secrets에 추가할 항목은 없습니다.
