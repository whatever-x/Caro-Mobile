package com.whatever.caro.feature.importer.platform

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import okio.FileSystem
import okio.Path
import okio.buffer
import okio.use
import kotlin.random.Random

// 선택기가 만든 사본은 앱 tmp에 있고 선택기가 다시 지워 주지 않으므로, 크기 조회와 읽기, 정리가 경로 기준으로 동작하는지 확인한다.
class InboxCopyPickedFileTest : FunSpec() {
    private val fileSystem = FileSystem.SYSTEM
    private val directory: Path =
        FileSystem.SYSTEM_TEMPORARY_DIRECTORY / "caro-inbox-test-${Random.nextLong().toULong().toString(radix = 16)}"

    private fun copyOf(
        name: String,
        bytes: ByteArray,
    ): Path {
        fileSystem.createDirectories(directory)
        return (directory / name).also { path -> fileSystem.write(path) { write(bytes) } }
    }

    init {
        afterTest { fileSystem.deleteRecursively(directory, mustExist = false) }

        test("이름은 경로의 마지막 구간이고 크기는 사본의 바이트 수다") {
            val file = InboxCopyPickedFile(copyOf("deck.apkg", ByteArray(5) { it.toByte() }))

            listOf(file.displayName(), file.sizeBytes()) shouldBe listOf("deck.apkg", 5L)
        }

        test("열 때마다 처음부터 읽는 원본을 돌려준다") {
            val file = InboxCopyPickedFile(copyOf("deck.apkg", byteArrayOf(1, 2, 3)))

            val reads = List(2) { file.openSource().buffer().use { source -> source.readByteArray().toList() } }

            reads shouldBe listOf(listOf<Byte>(1, 2, 3), listOf<Byte>(1, 2, 3))
        }

        test("정리하면 사본을 지운다") {
            val path = copyOf("deck.apkg", byteArrayOf(1))

            InboxCopyPickedFile(path).discard()

            fileSystem.exists(path) shouldBe false
        }

        test("이미 지워진 사본을 다시 정리해도 실패하지 않는다") {
            val file = InboxCopyPickedFile(copyOf("deck.apkg", byteArrayOf(1)))

            file.discard()
            file.discard()

            file.sizeBytes() shouldBe null
        }
    }
}
