# Caro Figma 전체 동선 Prototype QA

- 검토일: 2026-07-23
- Figma: [8. 전체 동선 Prototype QA](https://www.figma.com/design/PVw3ZFpRQf2JL3gv2AVOg9/Caro-Design?node-id=56794-2217&m=dev)
- 방식: 기존 설계 화면을 별도 QA 페이지에 복제한 뒤 프로토타입 인터랙션 연결
- 원본 페이지 변경: 없음

## 검증 결과

- QA 섹션: 9개
- 화면 간 프로토타입 연결: 46개
- 자동 전환: 2개
- 목적지가 없거나 끊어진 연결: 0개
- 증빙 이미지: 9개

Figma에서 각 섹션의 첫 번째 `[QA]` 프레임을 선택한 뒤 Present를 실행하면 해당 동선을 확인할 수 있다.

| 동선 | 연결한 경로 | 연결 수 | 결과 | 증빙 |
|---|---|---:|---|---|
| 01 신규 가입 | Splash → Login → Profile 생성 → Home | 4 | PASS | [01-signup.png](./01-signup.png) |
| 02 자동 로그인·세션 만료 | Splash → Home, Home → Login | 2 | PASS | [02-auto-session.png](./02-auto-session.png) |
| 03 홈·덱 생성 | 빈 Home → 덱 생성 → 생성된 Home → 덱 상세 | 3 | PASS | [03-home-deck.png](./03-home-deck.png) |
| 04 덱 수정·카드 일괄 삭제 | 덱 편집 → 수정 완료, 카드 선택 → 삭제 확인 → 덱 상세 | 7 | PASS | [04-deck-manage.png](./04-deck-manage.png) |
| 05 카드 생성 | 덱 상세 → 카드 생성 → 저장 → 덱 상세 | 2 | PASS | [05-card-create.png](./05-card-create.png) |
| 06 카드 상세·수정·삭제 | 덱 상세 → 카드 상세 → 뒤집기 → 수정/삭제 → 복귀 | 7 | PASS | [06-card-detail.png](./06-card-detail.png) |
| 07 설정·닉네임·로그아웃 | Home → 설정 → 닉네임 변경 → 설정, 로그아웃 → Login | 5 | PASS | [07-settings.png](./07-settings.png) |
| 08 일일 학습 | 덱 상세 → 학습 → 평가 → 완료 → Home, 중단 → 덱 상세 | 9 | PASS | [08-daily-study.png](./08-daily-study.png) |
| 09 전체 학습 | 덱 상세 → 전체 학습 → 완료 → Home, 중단 → 덱 상세 | 7 | PASS | [09-all-study.png](./09-all-study.png) |

## 확인 방법

1. Figma의 `8. 전체 동선 Prototype QA` 페이지를 연다.
2. 확인할 섹션의 첫 번째 `[QA]` 프레임을 선택한다.
3. Present를 실행한다.
4. 화면에 표시된 실제 CTA·카드·아이콘을 클릭한다.
5. `02 · 자동 로그인 및 세션 만료`의 세션 만료는 Home 하단의 `QA 세션 만료` 버튼으로 시뮬레이션한다.

## 체크 중 보완한 사항

- 카드 생성은 `0장 추가됨` 상태 대신 저장 가능한 `1장 추가됨` 상태로 교체했다.
- 카드 일괄 삭제는 0개 선택 상태 대신 1개 선택 상태로 교체했다.
- 카드 일괄 삭제에 삭제 확인 다이얼로그와 확인 취소 경로를 추가했다.
- 일일/전체 학습에 완료 경로뿐 아니라 중단·계속 학습 경로도 연결했다.

## 범위

이번 결과는 Figma 프로토타입 연결과 정적 화면 증빙이다. 실제 앱 빌드에서의 터치·백스택·API 성공/실패 동작은 별도의 에뮬레이터 QA 대상이다.
