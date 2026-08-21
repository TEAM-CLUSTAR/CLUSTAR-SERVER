# CLUSTAR Server 작업 가이드

## 공통 원칙

- 이슈 범위를 벗어난 리팩터링·설계 변경은 별도 이슈로 분리하고, 변경은 검증 가능한 범위에서 최소화한다.
- 작업 대상 경로에 더 가까운 `AGENTS.md`가 이 문서보다 우선한다. 문서와 코드가 충돌하거나 규칙 변경이 필요하면 변경 범위를 PR에서 함께 검토한다.
- 이 문서의 규칙과 코드의 기존 패턴은 안전한 기본값이자 최소 기준이며, 설계의 상한선은 아니다. 더 나은 대안이 있으면 문제·대안·영향 범위·검증 방법을 이슈나 PR에 명확히 제시하고 합의 후 적용할 수 있다.
- API 키, 비밀번호, 토큰, 서버 주소 등 민감정보를 코드·문서·로그에 하드코딩하지 않는다.

## 작업별 안내

| 작업 유형 | 확인할 문서 |
|---|---|
| 운영 Java 코드, API, 설정, 보안, 영속성 | `src/main/java/org/project/AGENTS.md` |
| 도메인 Controller·Service·Repository·Entity·DTO | `src/main/java/org/project/domain/AGENTS.md` 및 더 가까운 특수 문서 |
| 테스트 작성·수정·실행 | `src/test/java/org/project/AGENTS.md` |
| 이슈·PR·브랜치·CI/CD·GitHub 워크플로우 | `.github/AGENTS.md` |

루트 문서는 모든 세부 점검을 강제하지 않는다. 해당 작업 유형의 문서만 읽고 적용한다.
