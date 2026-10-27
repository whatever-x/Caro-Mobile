#!/bin/sh
set -eu

validator="$(dirname "$0")/validate-ios-app-id.sh"

should_pass() {
    CONFIGURATION="$1" CARO_ADMOB_APP_ID="$2" sh "$validator" >/dev/null 2>&1
}

should_fail() {
    if should_pass "$1" "$2"; then
        echo "Expected $1 to reject the App ID" >&2
        exit 1
    fi
}

should_pass DevDebug ""
should_pass QaRelease "ca-app-pub-3940256099942544~1458002511"
should_pass ProdDebug "ca-app-pub-1234567890123456~1234567890"
should_pass ProdRelease "ca-app-pub-1234567890123456~1234567890"
should_fail ProdDebug ""
should_fail ProdRelease " "
should_fail ProdRelease "invalid"
should_fail ProdRelease "ca-app-pub-1234567890123456~1234567890 "
should_fail ProdRelease "ca-app-pub-1234567890123456~1234567890
invalid"
should_fail ProdDebug "ca-app-pub-3940256099942544~1458002511"
should_fail ProdRelease "ca-app-pub-3940256099942544~1458002511"
echo "iOS AdMob App ID validation passed"
