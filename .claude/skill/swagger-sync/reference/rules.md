# Kotlin DTO 코딩 규칙

생성된 DTO 코드는 아래 규칙을 따라 보정한 뒤 `:core:remote` 모듈로 옮긴다.

## 클래스

- 모든 DTO는 `data class`, 모든 프로퍼티는 `val`(불변)로 선언한다.
- 파일 상단에 다음 주석을 넣는다:
  `// AUTO-GENERATED FROM SWAGGER — 직접 수정하지 마세요`
- 모든 DTO 클래스에 `@kotlinx.serialization.Serializable`을 붙인다.

## 응답 래퍼는 공유 제네릭을 재사용

- `ApiResponseXxxResponse` 같은 응답 래퍼 스키마는 **별도 클래스로 생성하지 않는다.**
- 모든 응답은 이미 존재하는 공유 제네릭 `com.whatever.caro.core.remote.dto.base.ApiResponseDto<T>`로 받는다.
  - 시그니처: `ApiResponseDto(success: Boolean, data: T? = null, error: ErrorDetailDto? = null)`
- 이 규칙 때문에 `ApiResponseDto.kt` 자체는 수정하지 않고, 호출부(`AuthApi` 등)에서 `ApiResponseDto<XxxResponse>` 형태로 사용한다.

## 클래스 네이밍

- **요청 DTO**: Swagger Schema 이름 그대로 사용한다 (예: `CreateDeckRequest`).
  - 기존 코드가 스펙과 어긋난 이름을 쓰고 있으면(예: `TokenRefreshRequest` → 스펙 `RefreshTokenRequest`), 스펙 이름으로 정정한다.
- **응답 DTO (`ApiResponseXxx` 안의 내부 서버 DTO)**: Swagger Schema 이름 그대로 유지한다 (예: `CreateDeckResponse`, `SocialLoginResponse`). `Dto` 접미사를 붙이지 않는다.
- **공유/에러 DTO (응답 래퍼와 무관한 독립 객체)**: 이름 끝에 `Dto`를 붙인다 (예: `ErrorDetailDto`, `FieldErrorDto`).
- **enum**은 기존 `:core:model`에 정의되어 있으면 그것을 재사용한다 (예: `SocialLoginRequest.provider: SocialLoginType`). `:core:model`은 절대 수정/생성하지 않는다.

## 패키지 / 디렉토리

- 패키지 prefix: `com.whatever.caro.core.remote.dto`
- 태그별 디렉토리: `dto/{tag}/` — `tag`는 OpenAPI `tags[].name`을 lowerCamelCase로 변환 (예: `User` → `user`, `Nickname` → `nickname`, `Auth` → `auth`, `Deck` → `deck`).
- 요청/응답으로 한 단계 더 나눔: `dto/{tag}/request/`, `dto/{tag}/response/`
- 공유 인프라:
  - `dto/base/ApiResponseDto.kt` — 응답 공통 래퍼 (수정 금지)
  - `dto/error/ErrorDetailDto.kt`, `dto/error/FieldErrorDto.kt` — 에러 공통 DTO

## 타입 매핑

- 날짜: `kotlinx.datetime.LocalDate`
- 날짜+시간: `kotlinx.datetime.LocalDateTime`
- 날짜/시간 필드를 `String`으로 두지 않는다.

## nullable 처리

- **신규 DTO**: 스펙의 `required` 목록에 포함된 필드는 non-null, 그 외는 nullable(`?`)이며 기본값 `= null`을 부여한다.
- **기존 DTO 갱신**: 기존 코드의 nullability를 유지한다.
  - 호출부가 non-null로 사용 중인 필드를 스펙만 보고 nullable로 바꾸면 cascading 수정(`orEmpty()` 등)이 불가피하므로, 의도된 non-null은 그대로 둔다.
  - 단, 스펙에서 **필드 자체가 사라졌거나 타입이 변경된 경우**(breaking change)는 그에 맞춰 반영하고 호출부를 함께 수정한다.

## 네이밍

- 스펙 필드명이 snake_case이면 프로퍼티는 camelCase로 변환한다.
- 이 경우 `kotlinx.serialization.SerialName`을 추가하고 `@SerialName("원본_필드명")`을 붙여 직렬화 이름을 보존한다.

## enum

- enum 값 네이밍은 UPPER_SNAKE_CASE.
- 원본 값과 다르면 각 entry에 `@SerialName("원본값")`을 붙인다.
- enum이 `:core:model`에 이미 존재하면 그것을 import해서 쓰고 새로 만들지 않는다.

## 직렬화 라이브러리

- `kotlinx.serialization` 어노테이션을 사용한다.

## 기타

- 스펙의 `description`이 있는 필드에는 KDoc 주석을 단다.

---

# Ktorfit API 인터페이스 매핑

- DTO만으로는 사용자가 엔드포인트를 호출할 수 없다. **태그별로 Ktorfit 인터페이스**를
만들고 Koin `apiModule`에 binding까지 추가한다.
- - 헤더 파라미터 중 `Device-Id`, `Accept-Language`, `Client-Timezone`은 인프라에서 자동 주입되므로 DTO/요청 파라미터로 추가하지 않는다.

## 위치 / 네이밍

- 패키지: `com.whatever.caro.core.remote.api`
- 파일: 태그당 하나 → `{Tag}Api.kt` (예: `DeckApi.kt`, `UserApi.kt`, `NicknameApi.kt`). Tag는 PascalCase.
- 클래스 가시성: `internal interface`
- 형식 참고: 같은 패키지의 `AuthApi.kt`

## 메서드 시그니처

- HTTP 어노테이션은 `de.jensklingenberg.ktorfit.http.*`의 `@GET`/`@POST`/`@PUT`/`@PATCH`/`@DELETE` 사용.
- 경로 파라미터: `@Path("{name}") name: Type`
- 요청 본문: `@Body request: XxxRequest`
- 쿼리 파라미터: `@Query("name") name: Type`
- 메서드 명: `request` 접두 + 스펙의 `operationId`를 PascalCase로 변환 (예: `createDeck` → `requestCreateDeck`).
  - HTTP 메서드와 의미가 중복되는 `get`/`fetch` 접두는 생략 가능 (예: `getRandomNickname` → `requestRandomNickname`).
- 반환 타입: **응답 DTO(inner)**만 적는다. 응답 래퍼 `ApiResponseDto<T>` 는 인프라(`CaroBaseResponseConverter`)가 자동으로 벗겨내므로 시그니처에 노출하지 않는다.
- 자동 주입 헤더(`Device-Id`, `Accept-Language`, `Client-Timezone`)는 파라미터로 추가하지 않는다.

## Koin 바인딩

- 등록 위치: `core/remote/src/commonMain/kotlin/com/whatever/caro/core/remote/di/ApiModule.kt`
- 인증이 필요한 엔드포인트는 **AUTH** qualifier, 명시적으로 `security: []`로 비활성화한 엔드포인트만 **NON_AUTH**.
- 한 API 인터페이스 안의 메서드가 인증/비인증을 섞어 쓰면 인터페이스를 분리하거나 binding을 둘 다 등록한다 (`AuthApi`처럼).
- 형식:
  ```kotlin
  single<DeckApi> {
      get<Ktorfit>(named(NetworkClient.Caro.AUTH)).createDeckApi()
  }
  ```
  `createXxxApi()` 확장 함수는 Ktorfit KSP가 컴파일 시 생성한다.

## 변경 처리

- **신규 태그**: `{Tag}Api.kt` 새 파일 + `apiModule`에 binding 추가.
- **신규 엔드포인트(기존 태그)**: 해당 `{Tag}Api.kt`에 메서드만 추가.
- **삭제된 엔드포인트**: 메서드 제거. 호출부도 함께 정리한다.
- **삭제된 태그**: 인터페이스 파일 삭제 + `apiModule` binding 제거 + 모든 호출부 정리.
