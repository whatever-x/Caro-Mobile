# 문의 및 버그 제보 개발 설계

## 모듈과 책임

| 위치 | 책임 |
|---|---|
| `feature/setting` | 설정 메뉴, 외부 폼 열기, 열기 결과와 실제 복귀 추적, 평가 요청 시점 결정 |
| `core/inapp-review` | 재사용 가능한 평가 requester, 앱 전체의 30일 요청 간격, 플랫폼 평가 API 연결 |
| `core/data`·`core/datastore` | 요청 시각의 원자적 예약과 영속 저장 |
| `core/designsystem` Compose resources | 한국어·기본 언어 메뉴명과 언어별 고객용 폼 URL |
| `composeApp`·`iosApp` | Kotlin 화면과 Swift의 최신 StoreKit API 연결 |

## 리소스

- `setting_menu_report_bug`: 한국어 ‘문의 및 버그 제보’, 기본 언어 ‘Contact Us and Report Bugs’.
- `values-ko/strings.xml`의 `setting_report_bug_url`은 기존 한국어 고객용 폼을 사용한다.
- 기본 언어의 같은 키는 한국어 폼 설명에 연결된 [영문 고객용 폼](https://docs.google.com/forms/d/e/1FAIpQLSeTkuyIor0qCRH1zFNTo2lgjGuvJS3kBbTzCeZLfVjsB2KMzg/viewform)을 사용한다.
- 영문 폼의 제목은 ‘Caro | Customer Support & Feedback’이며 질문과 선택지도 영어로 제공한다. 한국어 이외의 앱 언어에서는 기본 리소스에 따라 이 폼을 바로 연다.

## 이벤트와 상태 전이

`SettingViewModel`은 `BaseViewModel<SettingState, SettingIntent, SettingSideEffect>`를 사용한다. UI 상태는 StateFlow로 표시하고 평가 요청은 일회성 SideEffect로 처리한다.

| 입력 | 조건 | 처리 |
|---|---|---|
| 문의 메뉴 선택 | 새 열기 시도 | 시도 식별값과 대기를 먼저 등록한 뒤 외부 폼 열기 요청 |
| 열기 결과 | 현재 시도의 성공 | 실제 백그라운드·복귀 이력과 함께 소비 여부 판단 |
| 열기 결과 | 실패·취소 또는 오래된 식별값 | 현재 실패·취소는 대기 해제, 오래된 결과는 무시 |
| 실제 앱 백그라운드 | 문의 대기 중 | 해당 시도의 백그라운드 통과 표시 |
| 실제 앱 복귀 | 문의 열기 성공과 백그라운드 통과 모두 충족 | 대기를 먼저 해제한 뒤 평가 SideEffect 1회 방출 |
| 그 밖의 복귀 | 일반 복귀·중복 복귀·화면 이동 | 평가 요청 없음 |

열기 결과가 백그라운드 전환이나 복귀보다 늦게 와도 같은 시도의 이력을 유지한다.
대기 값은 ViewModel 수명에만 유지하며 프로세스 재생성에는 복원하지 않는다.
Android 구성 변경으로 같은 ViewModel에 화면이 다시 연결되면, 이미 열기에 성공한 문의 대기와 실제 백그라운드 이력은 유지한다.
구성 변경에서도 이전 화면의 observer와 자식 작업은 정리한다. 아직 완료되지 않은 열기 작업은 취소하며, 실제 화면 이탈에서는 문의 대기도 해제한다.
새로운 폼 열기는 새 대기를 만들지만, 공용 30일 간격을 우회하지 않는다.

## Route 연결

- 약관·개인정보 처리방침은 기존 `LocalUriHandler`로 연다. 문의 폼은 플랫폼 열기 결과를 확인하는 별도 경계로 처리한다.
- 문의 열기와 평가 요청은 Route 수명에 묶인 자식 코루틴에서 실행한다. 새 시도나 화면 이탈 시 기존 열기 작업을 취소한다.
- 열기 실패와 취소는 실제 성공으로 등록하지 않는다. OS가 URL 열기를 수락한 결과와 폼 로딩·제출 여부는 구분한다.
- `InquiryLifecycleEffect`는 화면의 내비게이션 생명주기 대신 실제 host Activity·scene의 이벤트를 전달하고, 화면 해제 시 관찰자를 제거한다.
- 평가 SideEffect는 공용 `rememberInAppReviewRequester()`의 `requestReview()`로 처리한다. 장기 수집에는 최신 requester를 참조한다.

## Android

- 현재 단일 Activity 앱의 host 생명주기를 관찰한다. `Activity.isChangingConfigurations`인 종료는 백그라운드로 전달하지 않는다.
- `core/inapp-review`의 Android 소스셋에서 기존 `com.google.android.play:review-ktx:2.0.2`를 사용한다.
- ReviewInfo 준비 후 공용 간격 예약을 확인한다. Activity가 종료·파괴되지 않았고 생명주기가 `RESUMED`일 때만 리뷰 흐름을 시작한다.
- SDK Task 대기는 호출 코루틴의 취소를 전파한다. Activity 정지 시 자동 제거되는 리스너에 suspend 작업의 완료를 의존하지 않는다.
- 요청 실패나 표시 제한은 정상 설정 흐름을 변경하지 않는다.

## iOS

- 문의 화면이 속한 scene의 실제 백그라운드·활성화 알림을 관찰한다. 단순 비활성화나 화면 이동은 앱 복귀로 처리하지 않는다.
- URL 열기는 `UIApplication.open`의 비동기 completion 결과를 사용한다. 완료 순서가 생명주기 이벤트보다 늦어도 같은 시도에 연결한다.
- `composeApp` 진입점에서 Swift 콜백을 공용 provider에 전달한다. Swift는 요청 시점의 활성 `windowScene`에서 `AppStore.requestReview(in:)`를 호출한다.
- 비활성 또는 연결되지 않은 scene에서는 요청하지 않는다. deprecated된 `SKStoreReviewController`는 사용하지 않는다.

## 공용 요청 간격과 재사용

- `InAppReviewRequester`는 문의 기능을 참조하지 않는다. 다른 feature도 동일한 공용 모듈을 사용할 수 있다.
- 최소 간격은 공용 gate의 `30.days` 한 곳에서 정의한다. 충분한 사용 경험과 적절한 요청 시점은 각 feature가 결정한다.
- `ReviewRequestRepository`와 `LocalReviewRequestDataSource`가 저장값 확인과 예약을 단일 DataStore 트랜잭션으로 수행한다. 동시 요청과 새 requester 인스턴스가 같은 기록을 공유한다.
- 마지막 요청 예약 시각은 프로세스 재생성·로그아웃 후에도 유지한다. 시계가 뒤로 바뀌면 새 요청을 보수적으로 차단한다.
- 예약이 저장된 뒤 화면이 비활성화되거나 SDK가 실패해도 간격 기록은 유지한다. 평가창 표시나 평가 완료를 뜻하는 기록은 아니다.

## 외부 계약과 데이터

- 문의 입력·제출·저장은 기존 Google Forms와 연결된 관리 시트가 처리한다.
- 앱은 폼 제출 결과를 조회하지 않으며 문의 본문, 제출 식별값과 평가 점수를 저장하지 않는다.
- 본 기능에 신규 Caro Backend API는 없다.
- 평가 API 결과를 실제 평가창 노출이나 평가 완료로 판정하지 않는다.
- 감사 스낵바와 제출 여부 확인 팝업은 없다.

## 수용 조건별 검증

| 조건 | 확인 방법 | 기대 결과 |
|---|---|---|
| 문의 후 첫 복귀 | 열기 성공 → 백그라운드 → 복귀 | 리뷰 SideEffect 1회 |
| 백그라운드 없는 재개 | 열기 성공 → 재개 | 요청 없음 |
| 일반·중복 복귀 | 일반 복귀 또는 첫 요청 후 재복귀 | 요청 없음 |
| 열기 미성공·취소 | 현재 시도 실패·취소 후 복귀 | 요청 없음 |
| 늦은 열기 결과 | 실제 복귀 후 성공 결과 도착 | 해당 시도만 1회 소비 |
| 오래된 결과 | 새 시도 이후 이전 결과 도착 | 현재 대기와 평가 요청에 영향 없음 |
| 새 문의 폼 | 첫 요청 후 30일 이내 다시 열기·복귀 | 평가 API 실행 없음 |
| 30일 경계 | 마지막 예약 직전·정확한 30일 시점 | 직전 차단, 경계 허용 |
| 재생성·동시 요청 | 저장소 재오픈·여러 호출의 동시 예약 | 기록 유지·예약 1회 |
| 프로세스 재생성 | 새 ViewModel로 재개 | 이전 대기 복원 없음 |
| Android 구성 변경 | 화면 회전 | 문의 복귀로 처리하지 않음 |
| 문의 중 Android 구성 변경 | 열기 성공·실제 백그라운드 후 Activity 재생성·복귀 | 같은 ViewModel의 문의 대기를 유지하고 최초 복귀 1회 소비 |
| 평가 요청 중 앱 이탈 | ReviewInfo 응답 전에 Activity 중지 | 평가창 시작 없음 |
| 플랫폼 평가 미표시 | 스토어 제한·요청 실패 | 설정 이용 계속 |

## 연결

- [화면 설계](inquiry-design.md)
- [대표 이슈](https://github.com/whatever-x/Caro-Workflow/issues/20)

- [Figma 화면과 UIUX 정책](https://www.figma.com/design/PVw3ZFpRQf2JL3gv2AVOg9/Caro?node-id=58276-3266)

- [기획 설계서](https://app.slack.com/files/U0C5215G1CM/F0CBXE63DBP)
