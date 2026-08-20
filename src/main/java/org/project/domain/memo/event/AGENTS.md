# Memo 이벤트

- 메모 생성과 삭제에 따른 파일·이미지 후속 처리를 담당한다. 이벤트에는 처리에 필요한 식별자와 키만 담고, 외부 부수 효과를 Service 본 트랜잭션에 결합하지 않는다.
- 삭제 리스너는 `@TransactionalEventListener(phase = AFTER_COMMIT)`을 사용한다. S3 삭제 실패는 `S3DeletionHandler`의 기록·재처리 흐름을 유지하며, 커밋 전 삭제나 실패 무시를 하지 않는다.
