package com.whatever.caro.feature.importer.source

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.longs.shouldBeLessThan
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.job
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runTest
import okio.Buffer
import okio.ByteString.Companion.toByteString
import okio.FileSystem
import okio.Path
import okio.Source
import okio.Timeout
import kotlin.random.Random

class ImportFileCopyTest : FunSpec() {
    private val fileSystem = FileSystem.SYSTEM
    private val fileCopy = ImportFileCopy(fileSystem)

    init {
        test("정확히 maxBytes인 원본은 복사에 성공한다") {
            runTest {
                withSandbox { sandbox ->
                    val result = fileCopy.copy(Buffer().write(content(MAX_BYTES)), sandbox / "copy.bin", MAX_BYTES)

                    result shouldBe ImportCopyResult.Copied(bytes = MAX_BYTES)
                }
            }
        }

        test("복사한 파일은 원본과 바이트가 같다") {
            runTest {
                withSandbox { sandbox ->
                    val destination = sandbox / "copy.bin"

                    fileCopy.copy(Buffer().write(content(MAX_BYTES)), destination, MAX_BYTES)

                    fileSystem.read(destination) { readByteString() } shouldBe content(MAX_BYTES).toByteString()
                }
            }
        }

        test("maxBytes보다 1바이트 큰 원본은 TooLarge를 돌려준다") {
            runTest {
                withSandbox { sandbox ->
                    val result = fileCopy.copy(Buffer().write(content(MAX_BYTES + 1)), sandbox / "copy.bin", MAX_BYTES)

                    result shouldBe ImportCopyResult.TooLarge
                }
            }
        }

        test("maxBytes를 넘으면 쓰던 부분 파일을 지운다") {
            runTest {
                withSandbox { sandbox ->
                    val destination = sandbox / "copy.bin"

                    fileCopy.copy(Buffer().write(content(MAX_BYTES + 1)), destination, MAX_BYTES)

                    fileSystem.exists(destination) shouldBe false
                }
            }
        }

        test("maxBytes를 넘는 순간 읽기를 멈추고 나머지는 읽지 않는다") {
            runTest {
                withSandbox { sandbox ->
                    val source = CountingSource(totalBytes = 1_000_000L)

                    fileCopy.copy(source, sandbox / "copy.bin", MAX_BYTES)

                    source.pulledBytes shouldBeLessThan MAX_BYTES + READ_SLACK_BYTES
                }
            }
        }

        test("코루틴이 취소되면 쓰던 부분 파일을 지운다") {
            runTest {
                withSandbox { sandbox ->
                    val destination = sandbox / "copy.bin"
                    val job =
                        launch {
                            val self = currentCoroutineContext().job
                            val source = CountingSource(totalBytes = 1_000_000L, onRead = { reads -> if (reads == 2) self.cancel() })
                            fileCopy.copy(source, destination, maxBytes = 2_000_000L)
                        }

                    job.join()

                    job.isCancelled shouldBe true
                    fileSystem.exists(destination) shouldBe false
                }
            }
        }
    }

    private suspend fun withSandbox(block: suspend (Path) -> Unit) {
        val sandbox =
            FileSystem.SYSTEM_TEMPORARY_DIRECTORY / "caro-importer-file-copy-test" / Random.nextLong().toULong().toString(radix = 16)
        fileSystem.createDirectories(sandbox)
        try {
            block(sandbox)
        } finally {
            fileSystem.deleteRecursively(sandbox, mustExist = false)
        }
    }

    private fun content(size: Long): ByteArray = ByteArray(size.toInt()) { index -> (index % 251).toByte() }

    // 한 번에 돌려주는 바이트를 8KiB로 제한해 복사 반복문이 여러 번 돌게 한다.
    private class CountingSource(
        private val totalBytes: Long,
        private val onRead: (reads: Int) -> Unit = {},
    ) : Source {
        var pulledBytes = 0L
            private set
        private var reads = 0

        override fun read(
            sink: Buffer,
            byteCount: Long,
        ): Long {
            if (pulledBytes >= totalBytes) return -1
            val chunk = minOf(byteCount, totalBytes - pulledBytes, CHUNK_BYTES)
            sink.write(ByteArray(chunk.toInt()))
            pulledBytes += chunk
            reads += 1
            onRead(reads)
            return chunk
        }

        override fun timeout(): Timeout = Timeout.NONE

        override fun close() = Unit
    }

    private companion object {
        // 여러 okio 세그먼트(8KiB)에 걸치도록 잡는다.
        const val MAX_BYTES = 20_000L
        const val CHUNK_BYTES = 8_192L
        const val READ_SLACK_BYTES = 64L * 1024
    }
}
