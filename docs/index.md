# 프로젝트 문서 탐색 목록

루트 지침을 읽은 뒤, 사용자 요청과 관련된 디렉토리에서 필요한 문서를 선택해 직접 읽습니다.
문서를 작성하거나 수정할 때는 [문서 관리 지침](AGENTS.md)을 따릅니다.

## architecture/ — 아키텍처

프로젝트 구조, 모듈 책임과 의존 관계, 코드 설계 규칙을 관리합니다.
코드 구현·리뷰 또는 모듈 의존성·플랫폼 구성을 변경할 때는 [전체 아키텍처](../ARCHITECTURE.md)를 먼저 읽고 관련 상세 문서를 선택합니다.

| 문서 | 다루는 내용 |
| --- | --- |
| [모듈 경계와 플랫폼 코드](architecture/modules-platform.md) | 모듈 역할·의존 관계·Android/iOS 진입점·플랫폼 코드 |
| [Feature와 UI](architecture/feature-ui.md) | 화면 구성·MVI·상태와 이벤트 처리 |
| [데이터 계층](architecture/data-layer.md) | Repository·DataSource·모델 변환 |
| [내비게이션](architecture/navigation.md) | 화면 이동·경로 정의·모듈 간 연결 |
| [의존성 주입](architecture/dependency-injection.md) | Koin 구성·의존성 등록 |
| [빌드 로직과 플러그인](architecture/convention-plugins.md) | 빌드 로직 구조·컨벤션 플러그인·BuildKonfig 설정 |

## development/ — 개발 환경과 검증

개발 환경 준비, 플랫폼별 빌드, 포맷과 테스트 방법을 관리합니다.

| 문서 | 다루는 내용 |
| --- | --- |
| [개발 환경과 빌드](development/build.md) | 필수 도구·환경 설정·Android/iOS 빌드·포맷 명령 |
| [테스트 안내](development/testing.md) | 테스트 도구·소스셋·실행 명령·커버리지 |
| [스크린샷 테스트 규칙](development/screenshot-testing.md) | 소스셋 책임·공통 시나리오·로케일·치수·결과 해석 |

## ci/ — CI 운영

CI의 실행 조건과 검사·게시·알림 결과를 확인하는 방법을 관리합니다.

| 문서 | 다루는 내용 |
| --- | --- |
| [CI 검사 안내](ci/checks.md) | 일반 PR 검사·커버리지 댓글·Slack 알림 |
| [스크린샷 CI](ci/screenshot-test.md) | 수동 실행·Golden·이미지 비교·PR 댓글·Slack 결과 확인 |

## collaboration/ — Git 협업

브랜치·커밋·PR 작업 규칙을 관리합니다.

| 문서 | 다루는 내용 |
| --- | --- |
| [Git 작업 지침](collaboration/git-workflow.md) | 기준 브랜치·커밋 메시지·PR 형식·PR 생성 시 확인할 정책 |

## 관련 에이전트 지침

에이전트 문서·스킬·훅은 `.agents/`에서 관리합니다.

| 문서 | 다루는 내용 |
| --- | --- |
| [에이전트 관리 지침](../.agents/AGENTS.md) | `.agents/` 내부 문서·스킬·훅의 작성과 수정 |
| [스킬 탐색 목록](../.agents/skills/index.md) | 작업에 적용할 스킬 선택 |
| [스킬 관리 지침](../.agents/skills/AGENTS.md) | 스킬 사용·설치·작성·수정 |
| [훅 탐색 목록](../.agents/hooks/index.md) | 훅 설치·설정 관리에 필요한 패키지 탐색 |
| [훅 관리 지침](../.agents/hooks/AGENTS.md) | 훅 설치·자동 실행과 후속 작업·작성·수정·검사 |
