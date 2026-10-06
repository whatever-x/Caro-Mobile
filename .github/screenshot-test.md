# Screenshot Test

`scripts/screenshot/scenarios.json` registers the screenshot scenarios across feature modules (currently 14). Each fixed `commonTest` scenario is rendered by Android `androidHostTest` and iOS `iosSimulatorArm64Test`, without a ViewModel or API.

## Local runs

Prepare the usual `local.properties` build values, then run:

```bash
./gradlew recordRoborazziAndroidHostTest
./gradlew recordRoborazziIosSimulatorArm64
./gradlew compareRoborazziAndroidHostTest
./gradlew compareRoborazziIosSimulatorArm64
```

Root task names execute across all feature modules with that task. The record tasks write local baselines; compare tasks compare against those files. Local Xcode 27 may require SwiftPM's native build system due to `spmForKmp` compatibility. CI pins Xcode 26.6.

PNG files in `feature/*/build/outputs/roborazzi/<target>/` are ignored build output. Do not commit them to feature branches. Roborazzi writes JSON results to `build/test-results/roborazzi/<target>/results-summary.json` and an HTML report to `build/reports/roborazzi/<target>/index.html` in each module.

## GitHub Actions flow

The workflows use Roborazzi Gradle tasks, Bash/`jq`, Artifact actions and the comment actions from the [official PR comment sample](https://github.com/takahirom/roborazzi-compare-on-github-comment-sample). No Python scripts or Pillow dependency are used.

1. **Screenshot Golden** (`screenshot-golden.yml`): on a `develop` push, records Android/iOS screenshots, validates PNG names against `scenarios.json`, and uploads complete Golden artifacts for 90 days. Each artifact includes its scenario registry.
2. **Screenshot PR** (`screenshot-pr.yml`): on a PR targeting `develop`, downloads a successful Golden run whose head SHA exactly matches the PR base SHA. It restores those images into each module's Roborazzi output directory and runs `compareRoborazziAndroidHostTest` / `compareRoborazziIosSimulatorArm64`. The workflow validates every registered scenario's JSON result and collects only changed/new images. Roborazzi HTML/JSON diagnostics are uploaded separately, including on failure where available.
3. **Screenshot Comment** (`screenshot-comment.yml`): triggered by a successful PR comparison through `workflow_run`. It verifies the report files and that the PR is still open with the same head/base SHA, publishes images to `screenshots/pr-<number>`, and uses `peter-evans/find-comment` / `peter-evans/create-or-update-comment` to maintain one marker comment. The actions are pinned to commit SHAs.
4. **Slack Notify Screenshot Result** (in `screenshot-comment.yml`): runs after comparison completes and the publisher finishes or skips, including comparison/publication failures. Reuses `SLACK_WEBHOOK_URL` and reports the overall screenshot CI result, Android/iOS job results, failed step names, and PR/job/artifact links. Slack Block Kit headers/dividers separate results, failure reasons, and log links; a primary button opens the PR, or the comparison run if no PR identity is available. Message aggregation and the v4.0.0 Slack action payload live directly in the workflow; no separate shell or jq files are required. Partial reruns use each platform's latest result up to the notified attempt. Cancelled/skipped results are distinct from failures; pixel differences remain report-only. This notification covers screenshot CI, independently of the existing Caro Mobile CI notification.

The message aggregation step includes an inline self-check for partial reruns, publication failure, cancellation and button fallback before exporting message outputs.

## Report behavior

- Existing scenario changed: show Roborazzi's generated `_compare.png` (Before/Diff/After in one image).
- New scenario: show only its `_actual.png` (After).
- Removed scenario: list its removal as text, using the Golden and PR registries.
- No changes: skip branch publication and new comment creation; replace an existing screenshot comment with “시각적 변경 없음”.

Pixel differences are report-only. Build, rendering, missing/expired Golden, missing registered results/images, invalid reports and publication errors fail CI. A missing Golden must never be silently replaced by a different commit's images.

Companion branches contain only report images and have independent Git history. Repeated reports overwrite the same PR-specific branch; image links use the published commit SHA to avoid displaying cached results. These branches are not merged into `develop` and are currently retained without automatic deletion.

## Activation and permissions

`workflow_run` workflows must exist on the repository's default branch. The comment workflow starts after this PR is merged into `develop`; this PR can validate comparison artifacts, while actual branch/comment publication requires a subsequent PR run after the workflow is installed.

PR comparison has read permissions. The separate publisher has write permissions and runs the default-branch workflow, without executing code or scripts from the PR or downloaded artifacts. Publication is limited to same-repository PRs; fork PRs can compare and produce artifacts but do not publish images/comments.

Slack notifications also run from the default branch and are limited to same-repository PRs. Install the updated workflow on the default branch before expecting live notifications. No additional Slack secret is required.
