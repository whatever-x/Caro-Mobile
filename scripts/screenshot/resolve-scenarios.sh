#!/usr/bin/env bash
set -euo pipefail

platform=${1:?platform is required}
case "$platform" in
  android|ios) ;;
  *) echo "Unknown platform: $platform" >&2; exit 1 ;;
esac

python3 - <<'PY'
import json
import sys

def unique_keys(pairs):
    result = {}
    for key, value in pairs:
        if key in result:
            raise ValueError(f"duplicate key: {key}")
        result[key] = value
    return result

for path in ("scripts/screenshot/scenarios.json", "scripts/screenshot/scenarios.android.json"):
    try:
        with open(path, encoding="utf-8") as source:
            json.load(source, object_pairs_hook=unique_keys)
    except (OSError, ValueError) as error:
        sys.exit(f"{path}: {error}")
PY

jq -e -n --arg platform "$platform" \
  --slurpfile common scripts/screenshot/scenarios.json \
  --slurpfile android scripts/screenshot/scenarios.android.json '
  def valid:
    type == "object" and all(to_entries[];
      (.key | test("^[A-Za-z0-9][A-Za-z0-9._-]*$")) and
      (.value | type == "array" and length > 0 and length == (unique | length) and
        all(.[]; type == "string" and test("^[A-Za-z0-9][A-Za-z0-9._-]*$") and
          (test("_(actual|compare)$") | not))));
  if ($common | length) == 1 and ($android | length) == 1 and
    ($common[0] | valid and length > 0) and ($android[0] | valid) then
    (if $platform == "android" then
      reduce ($android[0] | to_entries[]) as $entry ($common[0];
        .[$entry.key] = ((.[$entry.key] // []) + $entry.value))
    else $common[0] end) |
    if valid then . else error("duplicate or invalid platform scenarios") end
  else error("invalid scenario manifest") end
'
