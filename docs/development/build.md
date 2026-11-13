# 개발 환경과 빌드

개발 환경을 준비하거나 Android·iOS 빌드를 수행할 때 읽습니다. 공통 작업 절차는 루트 [AGENTS.md](../../AGENTS.md)를 따릅니다. 명령은 저장소 루트에서 실행합니다.

## 개발 환경

| 대상 | 필요한 환경 |
| --- | --- |
| 공통 | JDK 17, 저장소의 Gradle Wrapper. CI는 Temurin 사용 |
| Android | Android SDK 36. compileSdk·targetSdk 36, minSdk 28 |
| iOS | macOS, Xcode, iOS SDK. 최소 배포 대상 iOS 18.2 |
| 서명 빌드 | Android 서명 설정 또는 iOS 인증서·프로비저닝 프로파일 |

환경 값의 원본은 [버전 카탈로그](../../gradle/libs.versions.toml), [Android 컨벤션](../../build-logic/convention/src/main/kotlin/AndroidApplicationPlugin.kt), [Xcode 프로젝트](../../iosApp/iosApp.xcodeproj/project.pbxproj)입니다. Xcode의 최소 도구 버전과 iOS 최소 배포 버전은 구분합니다. 저장소에는 최소 Xcode 도구 버전이 명시되어 있지 않습니다.

## 환경과 빌드 타입

| 환경 | Android flavor | 앱 ID 접미사 | iOS 스킴 |
| --- | --- | --- | --- |
| 개발 | `dev` | `.dev` | `Caro-DevDebug`, `Caro-DevRelease` |
| QA | `qa` | `.qa` | `Caro-QaDebug`, `Caro-QaRelease` |
| 운영 | `prod` | 없음 | `Caro-ProdDebug`, `Caro-ProdRelease` |

Android의 빌드 타입은 `debug`·`release`입니다. Debug는 디버깅을 허용하고 코드 축소를 끕니다. Release는 디버깅을 끄고 코드 축소를 켭니다. 환경 이름만으로 빌드 타입을 판단하지 않습니다.

iOS의 빌드 구성 이름은 `DevDebug`·`DevRelease`·`QaDebug`·`QaRelease`·`ProdDebug`·`ProdRelease`입니다. Xcode 빌드 단계가 `KOTLIN_FRAMEWORK_BUILD_TYPE`을 설정하고 `:composeApp:embedAndSignAppleFrameworkForXcode`로 공유 프레임워크를 빌드합니다.

## 앱 버전과 빌드 번호

운영 빌드 번호는 플랫폼별 기존 릴리스 규칙을 따릅니다. `1.0.3`의 운영 번호는 Android `10003001`, iOS `10000301`입니다. QA는 별도 앱 ID로 배포하므로 운영 번호와 분리하며, 이미 배포한 `10100002`보다 낮추지 않습니다. Android QA 번호는 버전 카탈로그의 `qa-version-code`, iOS QA 번호는 QaDebug와 QaRelease의 `CURRENT_PROJECT_VERSION`에서 관리합니다. QA 재배포 시에는 이 번호를 올리고 운영 설정에 복사하지 않습니다.

iOS Info.plist의 버전과 빌드 번호는 각각 `MARKETING_VERSION`과 `CURRENT_PROJECT_VERSION`을 참조합니다. 버전을 변경할 때 Info.plist에 별도의 숫자를 작성하지 않습니다.

## Android 빌드

```bash
./gradlew :androidApp:assembleDevDebug
./gradlew :androidApp:assembleQaRelease
./gradlew :androidApp:assembleProdRelease
```

현재 컨벤션은 Debug에도 `dev` 서명 구성을 지정합니다. `local.properties`의 `STORE_FILE`·`KEY_ALIAS`·`KEY_PASSWORD`·`STORE_PASSWORD` 설정과 키스토어 준비 여부를 확인합니다. 자격증명 값은 문서나 로그에 복사하지 않습니다.

## iOS 빌드

`iosApp/`의 Xcode 프로젝트는 KMP에서 생성한 `ComposeApp` 정적 프레임워크를 사용합니다.

Xcode에서 `iosApp/iosApp.xcodeproj`를 열고 대상 환경의 스킴을 선택합니다. 개발 환경의 시뮬레이터 빌드 예시는 다음과 같습니다.

```bash
xcodebuild \
  -project iosApp/iosApp.xcodeproj \
  -scheme Caro-DevDebug \
  -configuration DevDebug \
  -sdk iphonesimulator \
  -destination 'generic/platform=iOS Simulator' \
  build
```

실기기·아카이브 빌드에는 대상 앱의 인증서와 프로비저닝 프로파일이 필요합니다. [Fastlane 설정](../../fastlane/Fastfile)의 `dev_match`·`qa_match`·`prod_match`는 서명 프로파일을 가져옵니다. `qa_deploy`는 빌드 후 외부 배포까지 수행하므로 로컬 빌드 검사 명령으로 사용하지 않습니다.

## 포맷

Spotless는 Kotlin·Gradle Kotlin DSL에 ktlint 1.8.0을 적용합니다. Compose 리소스 XML과 Android 리소스·Manifest의 포맷 대상은 [루트 빌드 설정](../../build.gradle.kts)을 확인합니다. [.editorconfig](../../.editorconfig)는 `@Composable` 함수의 이름 규칙을 예외 처리합니다.

```bash
./gradlew spotlessCheck
./gradlew spotlessApply
```

커밋 전에 `spotlessApply`를 실행하고 변경된 포맷을 확인합니다. 테스트 절차는 [테스트 안내](testing.md)를 따릅니다.

## AdMob 개발 설정

Android와 iOS의 dev 및 QA 설정에는 Google 공식 테스트 광고 App ID와 전면광고 단위를 사용합니다. Android 운영 설정에서 App ID 또는 광고 단위가 없으면 SDK 초기화와 광고 요청을 생략합니다. iOS는 SDK가 시작 시 App ID를 검사하므로 운영 빌드에 형식에 맞는 App ID가 없거나 Google 테스트 App ID를 사용하면 빌드를 중단합니다. 이 검사는 ID가 실제 운영 계정에 속하는지 확인하지 않습니다. iOS에서 App ID가 있고 광고 단위가 없으면 앱의 광고 초기화와 요청을 생략합니다.

Android 운영 빌드는 저장소 `gradle.properties`의 `caro.admob.android.appId`와 `caro.admob.android.interstitialId`에 Caro 운영 ID를 기본값으로 둡니다. 필요한 경우 Gradle `-P` 옵션으로 덮어쓸 수 있습니다. iOS는 Xcode 프로젝트의 ProdDebug와 ProdRelease에 `CARO_ADMOB_APP_ID`와 `CARO_ADMOB_INTERSTITIAL_ID`를 설정합니다. 개발 및 QA 구성은 Google 테스트 ID를 유지합니다. 운영 ID는 비밀키가 아니며, AdMob 앱과 광고 단위를 교체하면 이 기본값도 함께 갱신합니다.

배포 전에는 AdMob에서 앱 승인과 app-ads.txt 인증, 각 Caro 앱에 연결된 동의 메시지의 게시 상태를 확인합니다. SDK 초기화 성공만으로 동의 메시지 설정이 완료됐다고 판단하지 않습니다. iOS는 App Store의 마케팅 URL에 app-ads.txt를 제공하는 호스트의 개발자 웹사이트를 연결해야 하며, 버전 초안에만 저장된 URL은 공개 스토어에 반영된 것으로 간주하지 않습니다.

광고는 장수와 무관하게 일일 학습의 완료 저장이 성공한 뒤 기기별 하루 최대 한 번 표시합니다. 전체 학습 완료에는 광고를 표시하지 않습니다. 개발 검증에도 테스트 광고만 사용하며 운영 광고를 직접 클릭하지 않습니다.
