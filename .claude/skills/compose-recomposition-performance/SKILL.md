---
name: compose-recomposition-performance
description: >
  Compose recomposition 성능, skippable/restartable composable, 컴파일러 리포트, recomposition 카운트,
  cross-phase back-writing, composition vs layout/draw에서의 frame-rate State 읽기를 조사할 때 사용한다.
  "리컴포지션 많아", "스크롤 버벅", "왜 자꾸 recompose", "안정성/stability", "성능 느려" 맥락에서 발동.
---

# Compose recomposition 성능

> 출처: chrisbanes/skills (Apache-2.0) · 벤더링 2026-06-09. 라우터 스킬. 일부 하위 스킬은 벤더링하지 않아 상류 참조로 표기.
> Caro 컨벤션 단일 출처: [`../_conventions/SKILL.md`](../_conventions/SKILL.md)

라우터 전용 — 깊은 수정은 아래 초점 스킬에.

## 세 축

1. **파라미터 안정성 / skipping** — 이 restartable composable을 skip할 수 있나; 인자가 stable·비교 가능한가?
2. **State를 어디서 읽나** — frame-rate State를 composition에서 읽나 layout/draw에서 읽나?
3. **phase 간 back-writing** — 나중 phase가 앞 phase를 invalidate하는 snapshot state를 쓰나?
   (예: composition 중 map/list 변경; `onSizeChanged`(layout)가 형제가 composition에서 읽는 state를 씀.)

축 2·3은 종종 겹친다. 축 1은 독립적.

## 라우팅

| 주된 의심 | 다음 |
|---|---|
| skipping, 불안정 파라미터, 컴파일러 churn | (상류 `compose-stability-diagnostics` — 미벤더링) |
| frame-rate State 읽기 phase | (상류 `compose-state-deferred-reads` — 미벤더링) |
| composition 중 `putAll`/map 재구성/cross-row `height(state)` | 위 deferred-reads § back-writing |
| composable body의 focus 기반 부수작업 | [`../compose-side-effects/SKILL.md`](../compose-side-effects/SKILL.md) — `snapshotFlow` |

> Caro 참고: `compose-stability-diagnostics`, `compose-state-deferred-reads`는 핵심 세트에서 제외했다(필요 시
> 동일 절차로 추가). 현재는 원문을 참조하라:
> `https://github.com/chrisbanes/skills/tree/main/skills/compose-stability-diagnostics`,
> `.../compose-state-deferred-reads`.

## 리뷰 순서

1. 한 전환(focus 이동, 삽입, 스크롤)을 재현하고 어떤 composable이 recompose되는지 기록.
2. 안 바뀐 lazy item에서 카운트가 튀면, stability 탓하기 전에 back-writing(composition 변경, cross-row 측정) 확인.
3. 스크롤/애니메이션 중 매 프레임 카운트가 오르면 deferred reads 확인.
4. stable 데이터인데 skipping 실패하면 파라미터 안정성·컴파일러 리포트 확인.
5. 각 수정 후 재측정.

## 헛다리

다음은 보통 recomposition 카운트를 줄이지 **못한다**: 같은 입력에 `remember(index)` 래핑, read-only 파생 맵의
identity 캐시, lambda 캡처를 안정화하지 않은 hoisting(매 프레임 새 lambda 인스턴스가 skipping 무력화).

## 적용 안 되는 경우

- recomposition이 실제 데이터 변경을 추적하거나, 버그가 비용이 아니라 정확성일 때.
- 프로파일러/컴파일러 신호가 문제를 시사하지 않을 때.

## 관련

- [`../compose-state-authoring/SKILL.md`](../compose-state-authoring/SKILL.md) — `mutableState*` 안전 작성.
- [`../compose-side-effects/SKILL.md`](../compose-side-effects/SKILL.md) — focus 부수작업의 `snapshotFlow`.
- [`../_conventions/SKILL.md`](../_conventions/SKILL.md).
