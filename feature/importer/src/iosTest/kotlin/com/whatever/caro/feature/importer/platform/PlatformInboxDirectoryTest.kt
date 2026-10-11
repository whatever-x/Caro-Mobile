package com.whatever.caro.feature.importer.platform

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import okio.FileSystem

class PlatformInboxDirectoryTest : FunSpec() {
    init {
        test("선택기 사본 디렉터리는 앱 tmp 바로 아래 번들 식별자에 -Inbox를 붙인 이름이다") {
            inboxDirectoryFor("com.whatever.caro") shouldBe FileSystem.SYSTEM_TEMPORARY_DIRECTORY / "com.whatever.caro-Inbox"
        }

        test("번들 식별자를 알 수 없으면 지울 디렉터리도 없다") {
            inboxDirectoryFor(null) shouldBe null
        }
    }
}
