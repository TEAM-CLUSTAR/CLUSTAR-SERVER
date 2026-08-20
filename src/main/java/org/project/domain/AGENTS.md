# 도메인 레이어 작업 가이드

`domain/{ai,memo,tag,user,s3}/...` 아래에서 작업할 때 적용한다. 현재 작업 경로의 레이어에 해당하는 규칙을 따른다. `ai/event`, `ai/rag`, `memo/event`는 이 문서와 함께 각 하위 문서의 특수 규칙을 적용한다.

## Controller

- HTTP 요청·응답, 경로·상태 코드, 인증 인자 전달, 요청 검증만 담당한다. 성공 응답은 `ResponseEntity<ApiResponse<T>>` 관례를 따른다.
- 요청 본문과 중첩 객체는 필요한 `@Valid`로 검증하며, 공개 API 변경 시 기존 `@Operation`, `@Schema` 문서화 관례를 따른다.
- Service만 호출한다. Repository·Entity·RAG·외부 클라이언트를 직접 호출하지 않으며 비즈니스 로직, 트랜잭션, 영속성 로직을 두지 않는다.
- 도메인 예외는 `BusinessException`과 error code로 표현하고 `GlobalExceptionHandler`에 맡긴다. Controller에서 임의의 오류 응답 형식을 만들지 않는다.

## Service

- 유스케이스 조합, 트랜잭션 경계, Entity 상태 변경, Repository 및 외부 의존성 호출을 담당한다. 조회 작업은 필요한 경우 `@Transactional(readOnly = true)`를 사용한다.
- Controller의 HTTP 타입에 새로 의존하지 않는다. 현행 `GoogleAuthService`에는 `HttpServletRequest/Response`, `ResponseEntity`를 받는 호환성 코드가 있으나 이를 새 코드의 기준으로 삼지 않는다.
- 동시성, 외부 API 호출, 실패·재시도는 영향 범위와 기존 예외·이벤트 흐름을 검토한다. Controller 응답 조립과 Repository 쿼리 구현 세부사항은 Service 계약에 넣지 않는다.
- 외부 호출 결과를 저장해야 하면 호출 실패·타임아웃·부분 성공 시의 트랜잭션 경계를 명확히 한다. `InterruptedException` 처리 시 인터럽트 상태를 복원하고, 재시도는 기존 `@Retryable` 사용 영역처럼 일시 오류에 한정한다.

## Repository

- Spring Data JPA 인터페이스와 Custom/Impl QueryDSL 구현으로 영속성·조회만 담당한다. HTTP·인증 표현·비즈니스 정책을 넣지 않는다.
- 목록·연관 조회에서는 fetch join, 페이징, 컬렉션 조인으로 인한 N+1·중복·성능 영향을 검토한다. Memo의 QueryDSL 조회는 ID 제한 뒤 fetch join하는 현행 방식을 참고한다.
- 사용자 소유 조건과 논리 삭제 조건을 누락하지 않는다. 인터페이스와 구현체가 함께 있는 경우 공개 계약은 인터페이스에, 복잡한 조회 구현은 Custom/Impl에 둔다.

## Entity

- JPA Entity의 상태, 연관관계, 생성·수정·삭제 같은 도메인 행위만 둔다. API 응답이나 HTTP 타입, Repository·외부 클라이언트에 의존하지 않는다.
- 공통 감사 시각은 `BaseEntity`를 따른다. 공개 setter보다 생성 팩토리와 의도가 드러나는 행위를 우선한다.
- 양방향 연관관계, `cascade`, `orphanRemoval`, fetch 전략, 논리 삭제 상태를 변경하면 데이터 무결성과 기존 조회 조건을 함께 검토한다. `ChatRoom`은 `@Where(is_deleted = false)`, Memo는 `isDeleted` 조건을 사용한다.
- 스키마·컬럼·연관관계 변경은 마이그레이션과 운영 데이터 영향을 별도 검토한다.

## DTO

- `request`/`response` 및 필요한 레이어 경계 데이터만 표현한다. 요청 DTO에는 Bean Validation, 공개 API 필드에는 기존 `@Schema` 관례를 적용한다.
- Entity를 응답에 직접 노출하지 않으며 DTO에 영속성·비즈니스 로직을 넣지 않는다.
- OAuth·JWT·Presigned URL·프롬프트처럼 민감하거나 외부 의존성이 있는 데이터는 필요한 최소 범위만 전달하고 실제 비밀값을 예시·로그·문서에 쓰지 않는다.
