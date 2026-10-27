#!/bin/sh
set -eu

case "${CONFIGURATION:-}" in
    Prod*) ;;
    *) exit 0 ;;
esac

app_id="${CARO_ADMOB_APP_ID:-}"
if [ "${#app_id}" -ne 38 ] || ! printf '%s\n' "$app_id" | LC_ALL=C grep -Eq '^ca-app-pub-[0-9]{16}~[0-9]{10}$'; then
    echo "error: 운영 iOS 빌드에는 유효한 CARO_ADMOB_APP_ID가 필요합니다." >&2
    exit 1
fi

case "$app_id" in
    ca-app-pub-3940256099942544~*)
        echo "error: 운영 iOS 빌드에는 Google 테스트 App ID를 사용할 수 없습니다." >&2
        exit 1
        ;;
esac
