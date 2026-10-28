---
name: caro-screenshot-testing
description: >
  Caro-Mobile 스크린샷 시나리오·캡처를 추가·수정·삭제하거나 Android/iOS 및 ko/en 캡처 범위를
  점검할 때 사용한다. 일반 문서 조회나 ViewModel·단위 테스트 작성만으로는 적용하지 않는다.
---

# Caro 스크린샷 테스트

공통 UI 시나리오 하나를 Android/iOS와 ko/en에서 실행하고, 등록 목록·캡처 결과·Golden의 동일성을 유지한다.

## 작업 절차

1. [개발자 가이드](../../../docs/development/screenshot-testing.md)의 소스셋·시나리오 작성·캡처 설정을 읽는다.
   해당 feature의 `commonTest` 시나리오와 양쪽 플랫폼 래퍼를 함께 확인한다.
2. 고정 상태·데이터·ID·렌더러·필요한 동작 단언은 feature의 공통 시나리오 목록에서 관리한다.
   순수 Screen에 상태와 콜백을 전달하고 다른 feature나 production DI를 연결하지 않는다.
3. ID 추가·삭제는 `scripts/screenshot/scenarios.json`에도 반영한다. 이 JSON은 기대 목록이며 테스트를 생성하지 않는다.
   언어·크기·밀도·폰트·variant 변경은 `capture-config.json`을 기준으로 한다. 플랫폼별 로케일 복원과 캡처 API는
   `test-support/screenshot/` 도우미를 사용한다.
4. 변경된 모듈의 양쪽 플랫폼 캡처와 관련 동작 테스트를 실행하고, 파일명·측정 치수·등록 누락·플랫폼 대칭성을 확인한다.
   사용자가 실행 순서를 지정했다면 그 경계를 따른다. 로컬 명령·출력 경로는 개발자 가이드를 따른다.
5. Before/After 원본을 직접 확인한다. 기록 후 비교 통과만으로 시각적 승인을 주장하지 않는다.
   UI 차이와 빌드·렌더링·메타데이터 오류를 구분해 보고한다.

CI 실행·Golden 호환성·게시·사용자 승인에는 [스크린샷 CI](../../../docs/ci/screenshot-test.md)를 적용한다.
정확한 base SHA의 호환 Golden이 없으면 다른 SHA나 PR 이미지로 대체하지 않는다. CI dispatch·게시 권한은
시나리오 수정 요청에서 추론하지 않는다.

상태·이벤트·콜백 테스트의 선택 기준은 [Compose UI 테스트 패턴](../compose-ui-testing-patterns/SKILL.md)을 참고한다.
