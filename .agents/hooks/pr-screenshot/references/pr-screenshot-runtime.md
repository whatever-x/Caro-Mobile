# PR 스크린샷 훅 동작·검사 레퍼런스

훅 감지 방식·입출력을 확인하거나 훅을 수정·진단·검사할 때 읽습니다. 사용자 확인과 실행 경계는 [본문](../pr-screenshot.md#사용자-확인-기준), 등록·활성화는 [설정 레퍼런스](pr-screenshot-setup.md)를 따릅니다.

## 동작

```text
사용 중인 에이전트가 gh pr create 실행
  → 호환되는 PostToolUse / Bash 훅이 명령과 결과 확인
  → 에이전트에게 PR URL과 후속 지침 전달
  → 에이전트가 실제 diff·스크린샷 시나리오 확인
  → UI 영향 또는 불확실성이 있으면 사용자에게 실행 여부 질문
  → 승인 후 workflow_dispatch 실행
  → CI가 비교 이미지·PR 댓글·Slack 결과 처리
```

훅은 [원본 JSON](../pr-screenshot.json)의 인라인 `jq` 명령으로 이벤트를 처리합니다. 입력 명령을 다시 실행하거나 GitHub에 접근하지 않습니다. 훅이 직접 UI 영향을 판단하거나 사용자에게 질문하거나 PR 댓글을 작성하지는 않습니다. 에이전트가 판단·승인 절차를 수행하며, 승인 후 실행된 CI가 스크린샷 댓글을 게시합니다. 이미지 차이는 report-only입니다.

## 입력과 출력

아래는 현재 원본의 계약입니다. 다른 에이전트에 등록할 때는 공식 지침과 대조해야 합니다.

| 설정·필드 | 역할 |
| --- | --- |
| `PostToolUse` | 도구 결과가 나온 뒤 확인 |
| `matcher: ^Bash$` | `Bash` 이벤트 확인. 사용 중인 에이전트의 실제 셸 도구 매핑은 별도 확인 |
| `type: command` | 인라인 `jq` 명령으로 JSON 이벤트 처리 |
| `timeout: 5` | 훅 실행 제한 시간 5초 |
| `tool_input.command` | 실행한 명령에서 `gh pr create` 확인 |
| `tool_response` | 결과 객체 또는 문자열에서 대상 PR URL과 실패 상태 확인 |
| `hookSpecificOutput.additionalContext` | 에이전트에게 PR URL과 판단·질문 절차를 후속 맥락으로 전달 |

## 감지 범위

훅은 `gh pr create` 명령과 `whatever-x/Caro-Mobile`의 PR URL을 확인합니다. 다음 입력은 제외합니다.

- 명시적 실패·중단
- 진행 중이거나 백그라운드에서 실행 중인 명령
- `--dry-run`
- 다른 저장소의 PR URL
- 다른 도구 또는 이벤트

이 감지는 셸 명령 문자열에 의존하는 휴리스틱입니다. 임의의 래퍼·웹·API·다른 PR 생성 도구를 모두 감지하지는 않습니다. 훅이 없거나 감지하지 못해도 에이전트는 [본문의 절차](../pr-screenshot.md#사용자-확인-기준)를 직접 적용합니다.

## 오프라인 검사

저장소 루트에서 실행합니다. 실제 PR·CI·댓글은 생성하지 않습니다. `output` 형식과 `stdout` 형식 모두에서 후속 지침을 만들고, 실패·중단·백그라운드 실행·다른 저장소·dry-run에는 지침을 만들지 않는지 확인합니다.

```bash
set -o pipefail
for config in .agents/hooks/pr-screenshot/pr-screenshot.json; do
  hook=$(jq -r '.hooks.PostToolUse[0].hooks[0].command' "$config")
  event='{"hook_event_name":"PostToolUse","tool_name":"Bash","tool_input":{"command":"gh pr create"},"tool_response":{"exit_code":0,"output":"https://github.com/whatever-x/Caro-Mobile/pull/168"}}'
  printf '%s\n' "$event" | bash -c "$hook" \
    | jq -e '.hookSpecificOutput.additionalContext | contains("AGENTS.md") and contains("docs/index.md") and contains(".agents/hooks/pr-screenshot/pr-screenshot.md") and contains("UI")' || exit 1
  printf '%s\n' "$event" | jq '.tool_response = {stdout: .tool_response.output, stderr: "", interrupted: false}' \
    | bash -c "$hook" | jq -e '.hookSpecificOutput.hookEventName == "PostToolUse"' || exit 1
  for mutation in \
    '.tool_response.exit_code = 1' \
    '.tool_response.interrupted = true' \
    '.tool_response.backgroundTaskId = "pending"' \
    '.tool_response.session_id = 123' \
    '.tool_input.command = "gh pr create --dry-run"' \
    '.tool_response.output = "https://github.com/other/repo/pull/168"' \
    '.tool_name = "Read"' \
    '.hook_event_name = "PostToolUseFailure"'; do
    printf '%s\n' "$event" | jq "$mutation" | bash -c "$hook" \
      | jq -es 'length == 0' || exit 1
  done
  printf '%s: OK\n' "$config"
done
```

원본 검사에서 `OK`이면 기본 감지·제외 동작을 확인한 것입니다. 실제 사용 중인 에이전트 세션에서의 감지, 사용자 질문, CI 렌더링·댓글·Slack 전달까지 검증한 것은 아닙니다.
