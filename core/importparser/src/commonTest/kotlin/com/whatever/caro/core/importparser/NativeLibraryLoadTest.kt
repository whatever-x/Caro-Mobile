package com.whatever.caro.core.importparser

import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import com.squareup.zstd.okio.zstdCompress
import com.squareup.zstd.okio.zstdDecompress
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldNotBeEmpty
import okio.Buffer
import okio.buffer
import okio.use

class NativeLibraryLoadTest : FunSpec() {
    init {
        test("BundledSQLiteDriver 네이티브 라이브러리를 로드해 메모리 DB를 연다").config(enabled = nativeArchiveLibrariesLoadable) {
            val connection = BundledSQLiteDriver().open(":memory:")
            val version =
                connection.prepare("SELECT sqlite_version()").use { statement ->
                    statement.step()
                    statement.getText(0)
                }
            connection.close()

            version.shouldNotBeEmpty()
        }

        test("zstd 네이티브 라이브러리로 압축한 바이트를 같은 내용으로 푼다").config(enabled = nativeArchiveLibrariesLoadable) {
            val compressed = Buffer()
            compressed.zstdCompress().buffer().use { sink -> sink.writeUtf8("카로 zstd") }

            val restored = compressed.zstdDecompress().buffer().use { source -> source.readUtf8() }

            restored shouldBe "카로 zstd"
        }
    }
}
