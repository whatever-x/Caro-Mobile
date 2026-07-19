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
- **추가 확인 사항 (하위 DTO에는 `Dto` 접미사)**: 요청/응답 DTO 안에서만 쓰이는 하위 DTO에는 이름 끝에 `Dto`를 붙인다. "하위 DTO"는 다음 두 경우를 모두 포함한다.
  - **DTO 안에 중첩 선언된 클래스/enum**: 예) `CreateDeckResponse` 내부에 `Foobar` 클래스가 있으면 `Foobar` → `FoobarDto`. (`CreateCardItem` 내부 enum `CardType` → `CardTypeDto`)
  - **DTO의 필드/리스트 요소로 참조되는 별도 스키마**: 그 스키마 자체가 엔드포인트의 직접 요청 본문/응답 타입이 아니라면 `Dto`를 붙인다. 예) `CreateCardsRequest.items: List<CreateCardItem>`에서 `CreateCardItem`은 직접 페이로드가 아니므로 `CreateCardItem` → `CreateCardItemDto` (파일명·클래스·참조 모두 변경).
  - **예외**: 해당 스키마가 어떤 엔드포인트의 **직접 요청 본문이거나 직접(unwrap된) 응답 타입이기도 하면** 스펙 이름을 그대로 유지한다(`Dto` 미부착). 예) `CardResponse`는 `CreateCardsResponse`에 중첩되지만 `GET /v1/cards/{id}`의 직접 응답이므로 `CardResponse` 유지.
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

- **신규 DTO**: 스펙의 `required` 목록에 포함된 필드는 non-null, 그 외는 nullable(`?`)이다.
- **기존 DTO 갱신**: 기존 코드의 nullability를 유지한다.
  - 호출부가 non-null로 사용 중인 필드를 스펙만 보고 nullable로 바꾸면 cascading 수정(`orEmpty()` 등)이 불가피하므로, 의도된 non-null은 그대로 둔다.
  - 단, 스펙에서 **필드 자체가 사라졌거나 타입이 변경된 경우**(breaking change)는 그에 맞춰 반영하고 호출부를 함께 수정한다.
  - 신규 DTO 생성때와 마찬가지로 non-null, nullable을 구분하고 default 기본값은 설정하지 않는다.

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
- 헤더 파라미터 중 `Device-Id`, `Accept-Language`, `Client-Timezone`은 인프라에서 자동 주입되므로 DTO/요청 파라미터로 추가하지 않는다.

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

---

# RemoteDataSource 매핑

- API 인터페이스는 인프라(Ktorfit)의 호출 창구일 뿐이고, `:core:data`(Repository)가 실제로
  소비하는 진입점은 **DataSource**다. 태그별 Ktorfit API를 **도메인 단위 DataSource**로 감싸고
  Koin `remoteModule`에 binding까지 추가한다. (DTO·API만 만들면 Repository가 쓸 수 없다.)

## 위치 / 네이밍

- 패키지: `com.whatever.caro.core.remote.datasource.{domain}`
- `{domain}` 디렉토리: **도메인(기능) 단위** (예: `auth`, `card`, `deck`, `profile`).
  태그명과 반드시 1:1일 필요는 없다 — 한 도메인 DataSource가 **여러 태그 API를 묶을 수 있다**
  (예: `deck` = `DeckApi` + `DeckCardInformationApi`, `profile` = `NicknameApi` + `UserApi`).
- 인터페이스: `public interface {Domain}DataSource` — **`Remote` 접두를 붙이지 않고** 도메인 이름
  그대로 (예: `CardDataSource`, `DeckDataSource`, `ProfileDataSource`).
- 구현체: `internal class Remote{Domain}DataSourceImpl(...) : {Domain}DataSource` — **구현체에만**
  `Remote` 접두를 붙인다(리포지토리/데이터 계층 타입과 혼동 방지).
- `Local{Domain}DataSource`(datastore)와 짝을 이루는 도메인이라도 원격 인터페이스는 도메인 이름
  (`AuthDataSource`)을 쓴다. 원격/로컬 구분은 **구현체 접두**(`Remote…Impl` / `Local…Impl`)와
  **소비처 파라미터명**(`remoteAuthDataSource` vs `localAuthDataSource`)으로 표현한다.
- 형식 참고: 같은 패키지의 `CardDataSource` / `RemoteCardDataSourceImpl`.

## 메서드 시그니처

- DataSource 메서드는 감싸는 API 메서드를 **그대로 위임**한다(추가 로직·매핑 없음). 도메인 모델
  변환은 Repository의 몫이다.
- 메서드 명: API의 `request` 접두를 떼고 **도메인 관점에서 자연스러운 이름**으로 짓는다.
  조회는 관례적으로 `get…`을 쓴다 (예: `requestCreateDeck` → `createDeck`,
  `requestDecks` → `getDecks`, `requestRandomNickname` → `getRandomNickname`,
  `requestUpdateNickname` → `changeNickname`).
- 파라미터: 경로/쿼리/바디를 도메인 친화적 이름으로 노출한다
  (예: API `requestDeleteCard(id)` → DataSource `deleteCard(cardId: Long)`).
- 반환 타입: **감싸는 API 메서드의 반환 타입(응답 DTO, inner) 그대로.** 래퍼 unwrap은 이미
  API 계층(`ApiResponseDto<T>` → `T`)에서 처리되었다.

## 인증 qualifier 분리 (같은 API, 다른 qualifier → DataSource 2개)

- 한 API 인터페이스가 `apiModule`에서 **AUTH·NON_AUTH 두 qualifier로 모두 등록**된 경우
  (= 같은 인터페이스의 엔드포인트가 인증/비인증을 혼용, 예: `AuthApi`), 이를 감싸는 DataSource도
  **qualifier별로 2개**로 나눈다.
- **이유(단순 분류가 아님)**: 두 그룹은 서로 다른 HTTP 클라이언트(AUTH = auth interceptor 有 /
  NON_AUTH = 無)를 탄다. 특히 토큰 재발급(`refresh`)·소셜 로그인은 반드시 NON_AUTH 클라이언트를
  타야 한다(AUTH 클라이언트로 보내면 만료 토큰 → 인터셉터가 재발급 호출 → 다시 만료… 무한 루프).
  그래서 인프라(`AuthTokenProvider`)가 **NON_AUTH DataSource에만** 의존할 수 있도록 분리한다.
  → 분리는 유지한다. 합치지 않는다.
- 네이밍: qualifier를 반영해 `AuthDataSource`(AUTH) / `NonAuthDataSource`(NON_AUTH),
  구현체 `RemoteAuthDataSourceImpl` / `RemoteNonAuthDataSourceImpl`.
- 형식 참고: `AuthDataSource` / `NonAuthDataSource` (둘 다 `AuthApi`를 서로 다른 qualifier로 감쌈).

## Koin 바인딩

- 등록 위치: `core/remote/src/commonMain/kotlin/com/whatever/caro/core/remote/di/RemoteModule.kt`
- **일반(전부 AUTH, qualifier 모호성 없음)**: shortcut DSL로 구현체를 바인딩하고 인터페이스로
  노출한다. 생성자의 API 인자는 Koin 컴파일러 플러그인이 `get()`으로 자동 주입한다.
  ```kotlin
  single<RemoteCardDataSourceImpl>() bind CardDataSource::class
  single<RemoteDeckDataSourceImpl>() bind DeckDataSource::class
  ```
- **qualifier 분리**: 생성자 API 인자에 qualifier가 필요하므로 plain DSL로 명시 주입한다.
  (같은 타입 `AuthApi`를 서로 다른 qualifier로 주입해야 해 shortcut DSL이 모호성을 해소할 수 없다.)
  ```kotlin
  single<AuthDataSource> {
      RemoteAuthDataSourceImpl(
          authApi = get(named(NetworkClient.Caro.AUTH)),
      )
  }
  single<NonAuthDataSource> {
      RemoteNonAuthDataSourceImpl(
          nonAuthApi = get(named(NetworkClient.Caro.NON_AUTH)),
      )
  }
  ```

## 변경 처리

- **신규 태그/도메인**: `{Domain}DataSource` + `Remote{Domain}DataSourceImpl` 새로 만들고
  `remoteModule`에 binding 추가. 여러 태그를 한 도메인으로 묶을지는 기존 구조(deck/profile)를
  참고해 판단하고, 애매하면 태그당 1개로 두거나 사용자에게 확인한다.
- **신규 엔드포인트(기존 도메인)**: 해당 DataSource 인터페이스 + 구현체에 메서드만 추가.
- **삭제된 엔드포인트**: 메서드 제거. Repository 등 호출부도 함께 정리한다.
- **삭제된 태그/도메인**: DataSource 인터페이스·구현체 삭제 + `remoteModule` binding 제거 +
  모든 호출부 정리.
