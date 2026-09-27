# Screenshot Test

`scripts/screenshot/scenarios.json` is the explicit list of 14 screens. Each feature has one fixed `commonTest` scenario rendered by Android `androidHostTest` and iOS `iosSimulatorArm64Test`. Screens receive fixed UI state and no-op events; tests do not call a ViewModel or API.

## Local runs

Set the project's usual `local.properties` build values, then run:

```bash
bash scripts/screenshot/run.sh android
bash scripts/screenshot/run.sh ios
bash scripts/screenshot/run.sh android compare
bash scripts/screenshot/run.sh ios compare
```

The iOS command uses the native SwiftPM build layout when local Xcode is 27 or newer. This keeps the existing `spmForKmp` Google Sign-In interop build working until that plugin supports SwiftPM's new output layout. CI pins Xcode 26.6.

PNG files under `feature/*/build/outputs/roborazzi` are ignored local output. Do not commit them. The `collect.py` command fails if any named scenario is missing or unexpected:

```bash
python3 scripts/screenshot/collect.py androidHostTest build/screenshot-android
python3 scripts/screenshot/collect.py iosSimulatorArm64 build/screenshot-ios
```

## Golden and PR rollout

The `Screenshot Golden` workflow runs on `develop` pushes and uploads complete Android and iOS artifacts for 90 days. A failed capture or incomplete 14-image set prevents upload. The PR workflow resolves only a successful Golden run whose head SHA equals the PR base SHA. If none exists or its artifact expired, the PR job fails with a rebase or regeneration message.

The first PR introducing the Golden workflow cannot have a base artifact yet. Merge that PR into `develop`, wait for its first successful Golden run, and then enable PR comparison in a follow-up PR. Image differences are report-only; build, rendering, missing image, comparison, and Artifact failures remain CI failures.
