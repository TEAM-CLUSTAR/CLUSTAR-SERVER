# 소프트 스킬: 협업과 GitHub 작업

이 문서는 이슈·PR·브랜치·워크플로우·배포 관련 작업에 적용한다. 일반 코드 변경만 수행할 때 모든 항목을 점검할 필요는 없다.

## 이슈와 PR

- 이슈는 `.github/ISSUE_TEMPLATE`의 Feature, Fix, Chore, Init, Other 중 작업 성격에 맞는 템플릿을 사용한다. `목적`, `작업 상세 내용`, `유의사항`을 모두 작성한다.
- PR은 `.github/PULL_REQUEST_TEMPLATE.md`를 사용한다. `Related Issue`에 `- close #이슈번호`, `Summary`, `Question & PR point`, `Postman`을 작성하고 실행한 검증 결과를 Summary 또는 Question에 남긴다.

## Git과 CI/CD

- 브랜치는 `develop` 기준으로 생성하고 기존 관례인 `<type>/#<issue-number>/<summary>`를 사용한다. 사용 중인 type은 `feat`, `feature`, `fix`, `chore`, `refactor`, `test`, `init`, `docs`, `hotfix`다.
- 커밋 메시지는 `[Type] 작업 요약` 형식을 사용한다. `Type`은 `Feat`, `Fix`, `Refactor`, `Test`, `Chore`, `Docs` 등 작업 성격을 나타내는 파스칼 표기 접두사로 작성한다.
  - 예: `[Feat] 텍스트 검색을 단어 기반 매칭 + 랭킹으로 개선`
  - 예: `[Refactor] 검색/추천 예외처리 보강`
  - 예: `[Test] 테스트 코드`
  - 예: `[Chore] 로그 레벨 하향`
- `develop` 대상 PR의 `src/**` 또는 `build.gradle` 변경은 `develop-ci.yml`에서 `./gradlew clean build -Dspring.profiles.active=test`와 JaCoCo 보고서를 실행한다. 문서만 변경하는 PR은 이 경로 조건에 포함되지 않는다.
- `develop` 대상 PR이 병합되면 동일 경로 조건에서 `develop-cd.yml`이 Jib 이미지 빌드·배포·실패 시 롤백을 수행한다. 워크플로우·배포 설정 변경은 별도 검토 대상으로 취급한다.
