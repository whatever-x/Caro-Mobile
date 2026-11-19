# 데이터 계층과 매핑

[전체 아키텍처](../../ARCHITECTURE.md)의 상세 지침입니다.
이 문서의 주제에 해당하는 코드를 구현·변경·리뷰하기 전에 읽습니다.
공통 작업 규칙은 루트 [AGENTS.md](../../AGENTS.md)를 따릅니다. 빌드·테스트 지침은 [문서 탐색 목록](../index.md)에서 선택합니다.

## DataSource 구현체 이름

원격 DataSource 인터페이스 이름은 Repository나 데이터 계층의 다른 타입과 혼동될 수 있습니다.
이 경우 구현 클래스 이름에 `Remote` 접두사를 붙입니다.
예: `RemoteProfileDataSourceImpl : ProfileDataSource`.

## DTO와 도메인 모델 매핑

DTO와 도메인 모델 사이의 매핑은 `:core:data`의 `mapper` 패키지에서 관리합니다.
패키지 경로는 `com.whatever.caro.core.data.mapper`입니다.
RepositoryImpl 내부에 인라인 매핑 함수를 두지 않습니다.
`private` 매핑 확장함수도 RepositoryImpl 내부에 두지 않습니다.
금지 예시는 `private fun XxxResponse.toModel()`입니다.

매핑 코드는 다음 규칙을 따릅니다.

- 파일명은 `{Domain}Mapper.kt`로 지정합니다.
- DTO(`XxxResponse` 또는 `XxxResponseDto`)의 변환 함수는 `internal` 확장함수로 정의합니다. 함수 이름은 `to{Model}`로 지정합니다.
- enum·필드 헬퍼는 `private`으로 정의합니다.
- 매핑 전용 상수(`FIELD_FRONT` 등)는 mapper 파일의 최상위 `private const val`로 정의합니다.
- 기존 예시는 `AuthMapper.kt`와 `DeckMapper.kt`에서 확인합니다.
- `remote`/DataSource 계층은 매핑하지 않습니다. DTO를 그대로 반환합니다.
- DTO는 항상 data(Repository) 계층의 mapper에서 도메인 모델로 변환합니다.
- 각 mapper의 별도 단위 테스트는 `commonTest`에 `{Domain}MapperTest.kt`로 작성합니다.
