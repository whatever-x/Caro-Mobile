package com.whatever.caro.core.importparser

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.string.shouldContain
import java.io.File

/**
 * zstd-kmp는 JNI가 클래스와 필드를 이름으로 찾는데 자체 keep 규칙을 싣지 않는다.
 * 릴리스 R8이 이름을 바꾸면 anki21b 압축 해제가 Error로 죽으므로 규칙 파일이 사라지면 여기서 먼저 잡는다.
 * 규칙 파일이 소비자에게 전달되는지는 `:androidApp:minifyDevReleaseWithR8` 결과의 configuration.txt로 확인한다.
 */
class ConsumerKeepRulesTest : FunSpec() {
    init {
        test("consumer-rules.pro가 zstd JNI 클래스를 keep한다") {
            File("consumer-rules.pro").readText() shouldContain "-keep class com.squareup.zstd.** { *; }"
        }
    }
}
