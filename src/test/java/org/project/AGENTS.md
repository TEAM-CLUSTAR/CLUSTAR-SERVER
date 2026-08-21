# 하드 스킬: 테스트와 빌드

- 테스트는 기본적으로 `src/test/java/org/project`에서 운영 코드 패키지 구조를 따른다. 현재 JUnit 5, Spring Boot Test, `spring-security-test`를 사용하며, 더 적합한 테스트 구성은 격리성·실행 시간·신뢰성 근거와 함께 제안할 수 있다.
- 비즈니스 로직·API 계약·Repository 조회를 변경하면 관련 테스트의 추가 또는 수정을 검토한다. Controller는 인증·응답 계약, Repository는 조회 조건·N+1 영향, Service는 유스케이스와 트랜잭션 경계를 우선 검증한다.
- 빠른 검증은 관련 테스트를 대상으로 수행하고, 전체 테스트는 `./gradlew test`, 전체 빌드는 `./gradlew build`, 커버리지 보고서는 `./gradlew jacocoTestReport`를 사용한다.
- CI의 전체 검증 명령은 `./gradlew clean build -Dspring.profiles.active=test`이다. 테스트 결과와 실행하지 못한 검증은 PR 본문에 명시한다.
