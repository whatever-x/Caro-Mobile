# Screenshot Test 스크립트

이 디렉터리는 Android/iOS 스크린샷 테스트의 **시나리오 목록과 실행·수집 절차**를 관리합니다. 화면을 그리는 테스트 코드는 각 `feature` 모듈에 있고, 이곳의 스크립트가 그 결과를 Golden Artifact로 모읍니다. 이미지 원본을 저장하는 디렉터리는 아닙니다.

## 파일별 역할

| 파일 | 역할 |
| --- | --- |
| `scenarios.json` | 9개 feature 모듈의 14개 화면 시나리오 ID 목록. 수집 시 생성 PNG와 대조합니다. |
| `collect.py` | 각 모듈의 PNG 이름이 `scenarios.json`과 정확히 일치하는지 확인하고, 등록된 이미지와 시나리오 목록을 하나의 Artifact 디렉터리로 복사합니다. 누락·추가 이미지가 있으면 실패합니다. |

## 이미지가 이동하는 경로

1. Gradle의 Roborazzi 작업이 각 모듈의 `feature/<모듈>/build/outputs/roborazzi/<테스트 타깃>/`에 PNG를 생성합니다. 로컬 실행과 CI 러너 실행 모두 같은 방식입니다.
2. `develop` push의 [Screenshot Golden 워크플로](../../.github/workflows/screenshot-golden.yml)가 `collect.py`로 전체 이미지를 검증·수집한 뒤 Android/iOS Golden Artifact를 90일간 보관하도록 구성되어 있습니다.

PR의 base SHA에 해당하는 Golden을 내려받아 Before/After/Diff를 생성하고 PR 댓글에 표시하는 단계는 **아직 이 브랜치에 포함되지 않았습니다.** 로컬 `compare`는 PR 보고서를 게시하지 않습니다.

`build/` 아래 PNG와 수집·비교 결과는 `.gitignore`로 제외되는 **재생성 가능한 작업 산출물**입니다. 삭제해도 소스에는 영향이 없습니다. 다만 로컬 Roborazzi `compare`를 다시 실행하려면 먼저 `record`로 로컬 기준 이미지를 만들어야 합니다. CI의 Golden은 저장소가 아니라 GitHub Actions Artifact에 보관됩니다.

## 로컬 실행

저장소 루트에서 실행합니다. Android는 Android SDK, iOS는 macOS/Xcode와 iOS Simulator 빌드 환경이 필요합니다. 프로젝트 빌드에 필요한 `local.properties`도 준비되어 있어야 합니다.

```bash
./gradlew recordRoborazziAndroidHostTest
./gradlew recordRoborazziIosSimulatorArm64

# 같은 로컬 이미지에 대한 Roborazzi 비교
./gradlew compareRoborazziAndroidHostTest
./gradlew compareRoborazziIosSimulatorArm64
```

루트에서 작업 이름만 지정하면 해당 작업이 있는 feature 모듈들이 실행됩니다. 현재 `spmForKmp` 호환성 문제로 로컬 Xcode 27에서는 iOS 명령이 실패할 수 있습니다. CI는 Xcode 26.6을 선택합니다.

CI와 같은 전체 이미지 목록 검증·수집은 다음과 같이 실행할 수 있습니다.

```bash
python3 scripts/screenshot/collect.py androidHostTest build/screenshot-golden-android
python3 scripts/screenshot/collect.py iosSimulatorArm64 build/screenshot-golden-ios
```

`collect.py`는 지정한 출력 디렉터리를 초기화하므로 플랫폼마다 별도 디렉터리를 사용합니다. CI에서는 Android와 iOS가 별도 러너에서 실행되므로 각각 `build/screenshot-golden`을 사용합니다.

`collect.py`는 등록된 시나리오 수를 자동으로 계산하고, 목록과 실제 생성 PNG가 일치하지 않으면 실패합니다. 수집한 `scenarios.json`은 이후 PR 비교에서 Golden에 없는 신규 상태를 구분하는 데 사용할 수 있습니다. `develop` Golden 워크플로의 실제 Artifact 생성 여부는 해당 워크플로 실행 결과에서 확인해야 합니다.
