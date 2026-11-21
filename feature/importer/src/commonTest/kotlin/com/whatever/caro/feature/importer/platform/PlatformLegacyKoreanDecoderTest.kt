package com.whatever.caro.feature.importer.platform

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.shouldBe
import okio.ByteString.Companion.decodeHex

class PlatformLegacyKoreanDecoderTest : FunSpec() {
    init {
        test("플랫폼 CP949 디코더는 EUC-KR에 없는 확장 한글을 잃지 않고 읽는다") {
            val decoder = platformLegacyKoreanDecoder() ?: error("CP949 decoder unavailable on this platform")

            decoder.decodeOrNull("8c63b9e6b0a2c7cf20c164".decodeHex().toByteArray()) shouldBe "똠방각하 햏"
        }

        test("CP949로도 잘못된 바이트는 대체 문자를 넣지 않고 null을 돌려준다") {
            val decoder = platformLegacyKoreanDecoder() ?: error("CP949 decoder unavailable on this platform")

            decoder.decodeOrNull("6f6b2cfffe81200d0a".decodeHex().toByteArray()).shouldBeNull()
        }
    }
}
