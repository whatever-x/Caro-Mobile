package com.whatever.caro.core.importparser

/** sqlite-bundled와 zstd-kmp의 네이티브 코드를 이 테스트 타깃 프로세스에서 로드할 수 있는지 여부. */
internal expect val nativeArchiveLibrariesLoadable: Boolean

/** 테스트 타깃에서 쓸 수 있는 CP949 디코더. 없으면 null이다. */
internal expect fun testLegacyKoreanDecoder(): LegacyKoreanDecoder?
