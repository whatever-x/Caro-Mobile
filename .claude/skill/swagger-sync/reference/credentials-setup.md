# Swagger 자격증명 설정 방법

Swagger 스펙은 HTTP Basic(브라우저 시스템 팝업) 방식으로 보호된다.
fetch 스크립트는 아래 순서로 자격증명을 찾는다.

1. 환경변수가 있으면 그것을 사용한다.
2. 없으면 .claude/skill/swagger-sync/의 `.swagger-credentials` 파일을 읽는다.
3. 둘 다 없으면 에러로 중단한다.

## 필요한 값

| 키 | 설명 |
|---|---|
| `SWAGGER_USER` | 로그인 ID |
| `SWAGGER_PASS` | 로그인 PW |
| `SWAGGER_URL` | OpenAPI **스펙 JSON** 엔드포인트 (보통 `/v3/api-docs`). Swagger UI 페이지(`/swagger-ui/index.html` 등)는 안 된다 — 그 페이지로 보내면 fetch 스크립트가 리다이렉트 에러로 중단된다. 스크립트 인자로 전달하면 그 값이 우선한다. |

---

## 방법 1: 환경변수 (권장 — 자동화/n8n 호환)

환경변수는 모든 실행 환경에서 동작한다. n8n 등 자동화 도구를 쓸 계획이면
이 방법을 사용한다.

### macOS / Linux

작업 전 터미널에서 입력한다. (맨 앞 공백은 셸 히스토리에 안 남기기 위함)

```bash
 export SWAGGER_USER='실제ID'
 export SWAGGER_PASS='실제PW'
 export SWAGGER_URL='https://api.example.com/v3/api-docs'
```

PW를 화면에 안 띄우고 입력하려면:

```bash
read -rs SWAGGER_PASS && export SWAGGER_PASS
```

### Windows (PowerShell)

```powershell
$env:SWAGGER_USER='실제ID'
$env:SWAGGER_PASS='실제PW'
$env:SWAGGER_URL='https://api.example.com/v3/api-docs'
```

환경변수는 터미널 세션 동안만 유지된다. 터미널을 새로 열면 다시 설정해야 한다.

---

## 방법 2: .swagger-credentials 파일 (편의용 — 매번 입력 불필요)

`.claude/skill/swagger-sync/.swagger-credentials` 파일을 만들면, 한 번 작성 후
계속 재사용할 수 있다. 이 파일은 `.gitignore`에 등록되어 커밋되지 않는다.

`.swagger-credentials` 예시 (KEY=VALUE 형식):

```
SWAGGER_USER=실제ID
SWAGGER_PASS=실제PW
SWAGGER_URL=https://api.example.com/v3/api-docs
```

> 주의: 이 파일은 절대 git에 커밋하지 않는다. `.gitignore` 등록을 반드시 확인한다.
> n8n 등 자동화 환경에서 이 파일이 없어 문제가 생기면 방법 1(환경변수)로 전환한다.

---

## 우선순위 정리

환경변수가 설정되어 있으면 파일보다 환경변수가 우선한다.
따라서 평소에는 파일로 편하게 쓰고, 자동화 시에는 환경변수로 덮어쓰면 된다.
