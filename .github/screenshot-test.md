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

`Screenshot PR` records both platforms for PRs targeting `develop`. It downloads only the successful Golden run for the PR's exact base SHA; missing or expired artifacts fail the comparison. The Golden and PR artifacts each carry their own `scenarios.json`, so an existing scenario renders Before/After/Diff and a newly registered scenario renders After only. Removed scenarios are listed in the summary. Only changed images are copied to the PR's `screenshots/pr-<number>` companion branch, and one marker comment is updated on later runs. Pixel differences do not fail CI.

The publishing job checks out the trusted PR base commit before running `publish.py` with write access. While this workflow is first being introduced, that script is absent from the base and publishing is skipped; the compare artifacts are still available. Publishing starts for subsequent PRs after this workflow is merged into `develop`.
