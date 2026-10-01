def status_label:
  {success: "성공 ✅", failure: "실패 ❌", cancelled: "취소 ⏹️", skipped: "건너뜀 ⏭️",
   timed_out: "시간 초과 ❌", action_required: "조치 필요 ⚠️"}[. // "unknown"] // "결과 확인 필요 ⚠️";
def escape: gsub("&"; "&amp;") | gsub("<"; "&lt;") | gsub(">"; "&gt;");

$event[0].workflow_run as $run |
# Failed-job reruns retain the earlier successful platform; ignore later attempts.
[.[].jobs[] | select(.run_attempt <= $run.run_attempt)] |
group_by(.name) | map(max_by([.run_attempt, .id])) as $jobs |
(if $run.conclusion != "success" then $run.conclusion
 elif $publish == "cancelled" then "cancelled"
 elif $publish == "success" and
   all("android", "ios"; . as $platform |
     any($jobs[]; .name == "Compare / " + $platform and .conclusion == "success")) then "success"
 else "failure" end) as $result |
($run.pull_requests[0] // $prs[0][0] // null) as $pr |
(if $pr then $pr.html_url // ($run.repository.html_url + "/pull/" + ($pr.number | tostring)) else null end) as $pr_url |
(if $pr then "PR #\($pr.number)" else ($run.head_branch[0:80] | escape) end) as $identity |
($identity + " 스크린샷 CI " + ($result | status_label)) as $title |
(["android", "ios" | . as $platform |
    ([$jobs[] | select(.name == "Compare / " + $platform)][0] // {}) as $job |
    {name: (if $platform == "android" then "Android" else "iOS" end), job: $job}
]) as $platforms |
([$platforms[] | "• *\(.name)*: \(.job.conclusion | status_label)"] +
 ["• *PR 이미지 리포트*: \($publish | status_label)"] | join("\n")) as $results |
([$platforms[] | select(.job.conclusion != "success" and .job.conclusion != "cancelled" and .job.conclusion != "skipped") |
  "*\(.name) — \(.job.conclusion | status_label)*\n" +
  ([.job.steps[]? | select(.conclusion == "failure" or .conclusion == "timed_out") | .name | escape] |
   if length > 0 then join("\n") else "실패 단계는 작업 로그에서 확인해 주세요." end)] +
 [if $publish == "failure" then "*PR 이미지 리포트 게시 실패*" else empty end] | join("\n\n")) as $reasons |
([$platforms[] | select(.job.html_url) | "<\(.job.html_url)|\(.name) 로그>"] +
 ["<\($publisher_url)|리포트 게시 로그>", "<\($run.html_url)|전체 CI / 진단 아티팩트>"] | join(" · ")) as $links |
"이미지 차이는 report-only이며 테스트 실패와 별개입니다." as $note |
{text: ([$title, $results, $reasons, $links, $pr_url // empty, $note] | join("\n")), blocks: ([
  {type: "header", text: {type: "plain_text", text: $title, emoji: true}},
  {type: "divider"},
  {type: "section", text: {type: "mrkdwn", text: ("*플랫폼별 결과*\n" + $results)}},
  {type: "divider"},
  (if $reasons != "" then {type: "section", text: {type: "mrkdwn", text: ("*실패 사유*\n" + $reasons)}} else empty end),
  {type: "section", text: {type: "mrkdwn", text: ("*바로 확인*\n" + $links)}},
  (if $pr_url then {type: "actions", elements: [
    {type: "button", text: {type: "plain_text", text: "\($identity) 바로가기 ↗", emoji: true},
     style: "primary", url: $pr_url}
  ]} else empty end),
  {type: "divider"},
  {type: "context", elements: [{type: "mrkdwn", text: $note}]}
])}
