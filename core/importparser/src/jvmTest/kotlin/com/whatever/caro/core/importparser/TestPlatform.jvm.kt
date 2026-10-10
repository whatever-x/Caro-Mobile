package com.whatever.caro.core.importparser

internal actual val nativeArchiveLibrariesLoadable: Boolean = true

internal actual fun testLegacyKoreanDecoder(): LegacyKoreanDecoder? = JdkLegacyKoreanDecoder.createOrNull()
