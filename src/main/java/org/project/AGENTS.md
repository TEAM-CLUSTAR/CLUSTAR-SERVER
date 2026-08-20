# 하드 스킬: 애플리케이션 코드

이 문서는 `src/main/java/org/project`의 Java·Spring Boot 코드 작업에 적용한다. 도메인 레이어 작업은 `domain/AGENTS.md`를 추가로 따른다.

## 구조와 API

- 공통 관심사는 `global`, 기능은 `domain/{ai,memo,tag,user,s3}` 아래에 둔다. Controller·Service·Repository·Entity·DTO의 상세 책임은 도메인 문서를 따른다.
- HTTP 성공 응답은 `global.response.ApiResponse`와 `ResponseEntity`를 사용한다. 생성은 `ApiResponse.created`, 일반 성공은 `ApiResponse.ok` 관례를 따른다.
- 새 API는 기존 Controller의 `/api/v1` 경로 규칙을 따르며, 운영 경로·응답 계약을 깨는 변경은 신규 계약 추가를 먼저 검토한다.
- 요청 본문과 중첩 객체는 필요한 `@Valid` 및 Bean Validation으로 검증한다. 공개 API 변경 시 기존 `@Operation`, `@Schema` 문서화 관례를 따른다.

## 예외·보안·설정

- 입력·도메인 오류는 `BusinessException`과 도메인별 error code로 표현한다. `IllegalArgumentException` 등 임의 예외로 API 오류 계약을 만들지 않으며 `GlobalExceptionHandler` 흐름을 따른다.
- Security filter 수준 예외는 전역 예외 처리기가 잡지 못할 수 있으므로 기존 필터 응답 흐름을 따른다. CORS, whitelist, JWT, Swagger 보안 변경 시 `global/config/security`와 실제 클라이언트 영향을 함께 확인한다.
- 환경별 값은 `src/main/resources/application-*.yml`과 외부 환경 설정으로 관리한다. QueryDSL 생성 소스(`src/main/generated/querydsl`)와 `build/` 산출물은 수정하지 않는다.

## 데이터·트랜잭션·비동기

- local/dev는 `ddl-auto: update`, perf는 `create`, test는 `create-drop`을 사용한다. 파괴적 스키마 변경, `NOT NULL`·unique 제약, 컬럼·연관관계 변경은 기존 데이터·배포 순서·롤백 가능성을 검토한다.
- 외부 API·S3·AI 모델 호출과 DB 트랜잭션 경계를 분명히 한다. 부수 효과는 가능한 경우 커밋 이후 이벤트로 분리하고, `REQUIRES_NEW`는 독립 실패 기록처럼 필요한 경우에만 사용한다.
- `@Async`, 재시도, 인터럽트 처리에서 실패가 유실되지 않게 로그·재처리 가능성·트랜잭션 경계를 검토한다. `InterruptedException` 처리 시 인터럽트 상태를 복원하고 실패를 호출자에게 전달한다.
