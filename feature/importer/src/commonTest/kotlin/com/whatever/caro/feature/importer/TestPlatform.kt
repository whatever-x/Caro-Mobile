package com.whatever.caro.feature.importer

/** sqlite-bundled와 zstd-kmp의 네이티브 코드를 이 테스트 타깃 프로세스에서 로드할 수 있는지 여부. */
internal expect val nativeArchiveLibrariesLoadable: Boolean
