# Memo 이벤트

- 메모 생성과 삭제에 따른 파일·이미지 후속 처리를 담당한다. 이벤트에는 처리에 필요한 식별자와 키만 담고, 외부 부수 효과를 Service 본 트랜잭션에 결합하지 않는다.
- 삭제 리스너는 현재 `@TransactionalEventListener(phase = AFTER_COMMIT)`을 기본 경계로 사용한다. S3 삭제 실패는 `S3DeletionHandler`의 기록·재처리 방식을 기본값으로 삼으며, 대체 방식은 커밋 순서·실패 복구·재처리 보장을 설명한 뒤 제안한다.
