#!/usr/bin/env bash
set -euo pipefail

platform=${1:?android or ios required}
mode=${2:-record}
case "$platform" in
  android) suffix=AndroidHostTest ;;
  ios) suffix=IosSimulatorArm64 ;;
  *) echo "Unknown platform: $platform" >&2; exit 2 ;;
esac
case "$mode" in
  record|compare) ;;
  *) echo "Unknown mode: $mode" >&2; exit 2 ;;
esac

tasks=()
for module in card deck deck-detail home learning login profile setting splash; do
  tasks+=(":feature:$module:${mode}Roborazzi$suffix")
done

# Xcode 27 defaults to Swift Build, whose new output layout is not understood by
# the current spmForKmp plugin in :feature:login. Keep the native SwiftPM layout
# for local screenshot runs; CI pins Xcode 26.6 and does not need this wrapper.
if [[ "$platform" == ios ]] && [[ $(xcodebuild -version | awk '/^Xcode / {split($2, parts, "."); print parts[1]}') -ge 27 ]]; then
  PATH="$(cd "$(dirname "$0")/tools" && pwd):$PATH" ./gradlew --no-daemon "${tasks[@]}" --stacktrace
else
  ./gradlew "${tasks[@]}" --stacktrace
fi
