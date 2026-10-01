#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")"
temporary=$(mktemp -d)
trap 'rm -rf "$temporary"' EXIT

cat > "$temporary/event.json" <<'JSON'
{"workflow_run":{"conclusion":"success","run_attempt":2,"head_branch":"build/test","html_url":"https://github.com/example/repo/actions/runs/1","repository":{"html_url":"https://github.com/example/repo"},"pull_requests":[{"number":123}]}}
JSON
cat > "$temporary/jobs.json" <<'JSON'
[{"jobs":[
  {"id":1,"run_attempt":1,"name":"Compare / android","conclusion":"success","html_url":"https://github.com/example/repo/actions/runs/1/job/1"},
  {"id":2,"run_attempt":1,"name":"Compare / ios","conclusion":"failure","steps":[{"name":"Old failure","conclusion":"failure"}]}
]},{"jobs":[
  {"id":3,"run_attempt":2,"name":"Compare / ios","conclusion":"success"},
  {"id":4,"run_attempt":3,"name":"Compare / ios","conclusion":"failure"}
]}]
JSON
printf '[]\n' > "$temporary/prs.json"

payload() {
  jq --slurpfile event "$temporary/event.json" --slurpfile prs "$temporary/prs.json" --arg publish "$1" \
    --arg publisher_url 'https://github.com/example/repo/actions/runs/2' \
    -f slack-payload.jq "$temporary/jobs.json"
}

# Partial reruns use each platform's latest result at the notified attempt.
payload success | jq -e '.text | contains("PR #123 스크린샷 CI 성공") and contains("*Android*: 성공") and contains("*iOS*: 성공") and contains("Old failure") == false' > /dev/null
payload success | jq -e '[.blocks[].type] == ["header", "divider", "section", "divider", "section", "actions", "divider", "context"] and (.blocks[] | select(.type == "actions") | .elements[0] | .type == "button" and .text.text == "PR #123 바로가기 ↗" and .url == "https://github.com/example/repo/pull/123")' > /dev/null
payload failure | jq -e '.text | contains("스크린샷 CI 실패") and contains("*PR 이미지 리포트*: 실패")' > /dev/null
payload cancelled | jq -e '.text | contains("스크린샷 CI 취소")' > /dev/null

jq '.workflow_run.conclusion = "failure"' "$temporary/event.json" > "$temporary/update.json"
mv "$temporary/update.json" "$temporary/event.json"
jq '.[1].jobs[0] |= (.conclusion = "failure" | .steps = [{name:"Download Golden <missing> & \"expired\"",conclusion:"failure"}])' "$temporary/jobs.json" > "$temporary/update.json"
mv "$temporary/update.json" "$temporary/jobs.json"
payload skipped | jq -e '.text | contains("스크린샷 CI 실패") and contains("*Android*: 성공") and contains("*iOS*: 실패") and contains("&lt;missing&gt; &amp; \"expired\"") and contains("*PR 이미지 리포트*: 건너뜀")' > /dev/null

payload skipped | jq -e 'any(.blocks[]; .type == "section" and (.text.text | startswith("*실패 사유*\n") and contains("*iOS — 실패")))' > /dev/null

jq '.[0].jobs[0].conclusion = "failure"' "$temporary/jobs.json" > "$temporary/update.json"
mv "$temporary/update.json" "$temporary/jobs.json"
payload skipped | jq -e '.text | contains("*Android*: 실패") and contains("*iOS*: 실패")' > /dev/null

jq '.workflow_run.conclusion = "cancelled" | .workflow_run.pull_requests = []' "$temporary/event.json" > "$temporary/update.json"
mv "$temporary/update.json" "$temporary/event.json"
printf '[{"jobs":[]}]\n' > "$temporary/jobs.json"
payload skipped | jq -e 'all(.blocks[]; .type != "actions")' > /dev/null
payload skipped | jq -e '.text | contains("build/test 스크린샷 CI 취소") and contains("결과 확인 필요")' > /dev/null
jq '.workflow_run.conclusion = "success"' "$temporary/event.json" > "$temporary/update.json"
mv "$temporary/update.json" "$temporary/event.json"
payload success | jq -e '.text | contains("스크린샷 CI 실패") and contains("결과 확인 필요")' > /dev/null
printf '[{"number":123,"html_url":"https://github.com/example/repo/pull/123"}]\n' > "$temporary/prs.json"
payload success | jq -e '.text | contains("PR #123") and contains("https://github.com/example/repo/pull/123")' > /dev/null
echo 'Screenshot Slack payload: 11 checks passed'
