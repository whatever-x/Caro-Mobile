package com.whatever.caro.feature.importer

// Android 호스트 테스트는 기기용 ABI 네이티브 라이브러리만 받으므로 호스트 JVM에서 sqlite-bundled와 zstd-kmp를 로드할 수 없다.
internal actual val nativeArchiveLibrariesLoadable: Boolean = false
