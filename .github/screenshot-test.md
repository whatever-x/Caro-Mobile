# Screenshot Test

`scripts/screenshot/scenarios.json` is the explicit list of 14 screens. Each feature has one fixed `commonTest` scenario rendered by Android `androidHostTest` and iOS `iosSimulatorArm64Test`. Screens receive fixed UI state and no-op events; tests do not call a ViewModel or API.

## Local runs

Set the project's usual `local.properties` build values, then run:

```bash
./gradlew recordRoborazziAndroidHostTest
./gradlew recordRoborazziIosSimulatorArm64
./gradlew compareRoborazziAndroidHostTest
./gradlew compareRoborazziIosSimulatorArm64
```

Running a task by name from the root executes it in every feature module that has the task. Local iOS runs may fail on Xcode 27 due to the current `spmForKmp` compatibility issue. CI pins Xcode 26.6.

PNG files under `feature/*/build/outputs/roborazzi` are ignored local output. Do not commit them. The `collect.py` command fails if any named scenario is missing or unexpected:

```bash
python3 scripts/screenshot/collect.py androidHostTest build/screenshot-android
python3 scripts/screenshot/collect.py iosSimulatorArm64 build/screenshot-ios
```

## Golden and PR rollout

The `Screenshot Golden` workflow runs on `develop` pushes and uploads complete Android and iOS artifacts for 90 days. A failed capture or incomplete registered image set prevents upload. Each artifact also includes its scenario list. The PR workflow resolves only a successful Golden run whose head SHA equals the PR base SHA. If none exists or its artifact expired, the PR job fails with a rebase or regeneration message.

The first PR introducing the Golden workflow cannot have a base artifact yet. Merge that PR into `develop`, wait for its first successful Golden run, and then enable PR comparison in a follow-up PR. Image differences are report-only; build, rendering, missing image, comparison, and Artifact failures remain CI failures.
