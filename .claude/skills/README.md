# `.claude/skills`

에이전트(Claude/Codex 등)가 코딩·리뷰 시 자동 발동(description 트리거)하거나 직접 호출(`/<skill-name>`)하는 스킬 모음.

## 스킬

| 스킬 | 설명 |
|---|---|
| `_conventions` | Caro 아키텍처 규칙 요약(MVI·Koin·Navigation3·resources·expect-actual·Kotest). 다른 스킬의 컨벤션 근거. 원문은 루트 `CLAUDE.md`. |
| `caro-compose-review` | PR/diff를 위 컨벤션 체크리스트로 리뷰. `/caro-compose-review` 또는 "컴포즈 리뷰" 등으로 발동. |
| `compose-*`, `kotlin-*` | Compose/Kotlin/KMP 베스트 프랙티스. 출처 [chrisbanes/skills](https://github.com/chrisbanes/skills)(Apache-2.0)를 Caro 컨벤션으로 각색. |
| `swagger-sync` | Swagger → Kotlin DTO/API 동기화(별도 출처). |

## 비고

- Android 전용 스킬([android/skills](https://github.com/android/skills))은 KMP+iOS에 부적합하여 제외.
- 벤더링 스킬은 각 SKILL.md 상단에 출처·라이선스 표기. 상류 변경 시 원문과 diff하여 예시만 재각색.
