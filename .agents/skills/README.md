# 에이전트 스킬

Caro-Mobile에서 공유하는 에이전트 스킬 모음입니다. Compose·Kotlin·KMP 코드 구현과 리뷰, API 동기화 등에 필요한 작업 절차와 참고 파일을 패키지별로 관리합니다.

**이 README는 사용자(개발자)가 읽는 소개·설치 안내입니다.** 에이전트용 관리 규칙은 `AGENTS.md`, 작업별 지침은 각 패키지의 `SKILL.md`에서 관리합니다.

## 설치와 사용

공유 스킬 원본은 이 저장소의 `.agents/skills/`에 있습니다. 각 스킬의 목적과 적용 조건은 [스킬 탐색 목록](index.md)과 해당 `SKILL.md`에서 확인하세요. 스킬 간 상대 참조가 있으므로 전체 묶음을 사용하고, 일부만 설치한다면 참조하는 형제 스킬도 함께 준비하세요. 이 스킬들은 Caro-Mobile의 KMP, Compose, 모듈 구성을 전제로 합니다.

아래 명령은 저장소 루트에서 실행합니다. 개발자별 로컬 설정과 설치본, 자격증명은 Git에 공유하지 않습니다.

### Claude Code

1. Claude Code를 설치하고 로그인한 뒤 Caro-Mobile 저장소를 체크아웃합니다. 자세한 내용은 [Claude Code 스킬 안내](https://code.claude.com/docs/en/skills)를 참고하세요.
2. Claude Code의 프로젝트 스킬 경로인 `.claude/skills/`에 공유 원본을 가리키는 상대 심볼릭 링크를 만듭니다. 이미 같은 이름의 파일이나 링크가 있으면 덮어쓰지 않고 건너뜁니다. 기존 항목은 직접 확인해 정리하세요.

    ```bash
    mkdir -p .claude/skills
    for d in .agents/skills/*/; do
      [ -f "${d}SKILL.md" ] || continue
      name=$(basename "$d")
      if [ -e ".claude/skills/$name" ] || [ -L ".claude/skills/$name" ]; then
        echo "기존 항목 확인: .claude/skills/$name"
        continue
      fi
      ln -s "../../.agents/skills/$name" ".claude/skills/$name"
    done
    ```

3. `.claude/skills/<skill-name>/SKILL.md`가 공유 원본을 가리키는지 확인합니다. `.claude/`는 Git에서 제외됩니다. 새 스킬이 추가되면 위 명령을 다시 실행해 링크를 추가하세요.
4. 저장소에서 Claude Code 세션을 열고 `/` 메뉴에서 스킬 이름을 확인합니다. 예를 들어 `/caro-compose-review`를 입력해 해당 스킬을 명시적으로 호출할 수 있습니다. 목록에 없으면 링크와 공식 안내의 문제 해결 항목을 확인하세요.

### Codex

1. Codex를 설치하고 로그인한 뒤 Caro-Mobile 저장소를 체크아웃합니다. [Codex 스킬 안내](https://learn.chatgpt.com/docs/build-skills)를 참고하세요.
2. 프로젝트 스킬 경로가 공유 원본 `.agents/skills/<skill-name>/SKILL.md`이므로 별도의 설치 명령은 필요하지 않습니다. 저장소 루트에서 다음 명령으로 파일을 확인합니다.

    ```bash
    ls .agents/skills/*/SKILL.md
    ```

3. 저장소를 작업 디렉토리로 Codex 세션을 시작하고 CLI에서는 `/skills` 메뉴에서 이름을 확인합니다. `$caro-compose-review`처럼 이름을 지정하거나 관련 작업을 요청해 사용할 수 있습니다. 목록에 보이지 않으면 새 세션을 열고 경로와 공식 안내를 확인하세요.

### Antigravity

1. Antigravity를 설치하고 로그인한 뒤 Caro-Mobile 저장소를 체크아웃합니다. [Antigravity 스킬 안내](https://antigravity.google/docs/skills)를 참고하세요.
2. 프로젝트 스킬 경로가 공유 원본 `.agents/skills/<skill-name>/SKILL.md`이므로 별도의 설치 명령은 필요하지 않습니다. 저장소 루트에서 다음 명령으로 파일을 확인합니다.

    ```bash
    ls .agents/skills/*/SKILL.md
    ```

3. 저장소를 workspace로 열고 에이전트 세션에서 스킬 이름을 명시해 사용을 요청합니다. Antigravity IDE에서는 에이전트 패널의 Customizations 메뉴에서 활성 스킬을 확인할 수 있습니다. 보이지 않으면 workspace 경로와 공식 안내를 확인하세요.

원본을 직접 사용하는 경우 저장소를 갱신하면 변경된 스킬을 사용할 수 있습니다. 별도 복사 설치본은 원본이 변경되면 다시 설치해야 합니다.

## 패키지 구조

스킬마다 `<skill-name>/` 디렉토리를 사용합니다. 아래는 공통 구조를 설명하는 예시이며, 보조 디렉토리는 필요한 경우에만 둡니다.

```text
skills/
├── README.md                 # 사용자용 소개·설치 안내
├── AGENTS.md                 # 에이전트용 스킬 관리 지침
├── index.md                  # 에이전트용 스킬 탐색 목록
└── <skill-name>/
    ├── SKILL.md              # 스킬 메타데이터·작업 절차
    ├── reference/            # 선택: 상세 참고 문서
    ├── examples/             # 선택: 사용·적용 예제
    └── scripts/              # 선택: 실행 스크립트
```
