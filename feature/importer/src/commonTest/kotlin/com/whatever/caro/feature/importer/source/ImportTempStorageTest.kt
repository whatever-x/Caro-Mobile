package com.whatever.caro.feature.importer.source

import io.kotest.assertions.throwables.shouldNotThrowAny
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import okio.FileSystem
import okio.Path
import kotlin.random.Random

class ImportTempStorageTest : FunSpec() {
    private val fileSystem = FileSystem.SYSTEM

    init {
        test("clearAll은 남은 파일과 하위 디렉터리를 모두 지운다") {
            withSandbox { sandbox ->
                val root = sandbox / "caro-import"
                fileSystem.createDirectories(root / "importparser-1a2b")
                fileSystem.write(root / "importparser-1a2b" / "collection.anki21b") { writeUtf8("db") }
                fileSystem.write(root / "copy-1") { writeUtf8("zip") }

                ImportTempStorage(fileSystem, root).clearAll()

                fileSystem.listOrNull(root).orEmpty() shouldBe emptyList()
            }
        }

        test("clearAll은 caro-import 밖의 파일과 이름이 비슷한 형제 디렉터리를 건드리지 않는다") {
            withSandbox { sandbox ->
                val root = sandbox / "caro-import"
                fileSystem.createDirectories(root)
                fileSystem.write(root / "copy-1") { writeUtf8("zip") }
                fileSystem.write(sandbox / "outside.txt") { writeUtf8("keep") }
                fileSystem.createDirectories(sandbox / "caro-import-other")
                fileSystem.write(sandbox / "caro-import-other" / "keep.txt") { writeUtf8("keep") }

                ImportTempStorage(fileSystem, root).clearAll()

                fileSystem.exists(sandbox / "outside.txt") shouldBe true
                fileSystem.exists(sandbox / "caro-import-other" / "keep.txt") shouldBe true
            }
        }

        test("clearAll은 디렉터리가 아직 없어도 던지지 않는다") {
            withSandbox { sandbox ->
                val storage = ImportTempStorage(fileSystem, sandbox / "caro-import")

                shouldNotThrowAny { storage.clearAll() }
            }
        }

        test("newFile이 돌려준 경로는 caro-import 바로 아래다") {
            withSandbox { sandbox ->
                val root = sandbox / "caro-import"

                val path = ImportTempStorage(fileSystem, root).newFile()

                path.parent shouldBe root
            }
        }

        test("newFile은 디렉터리가 없어도 만들어서 바로 쓸 수 있는 경로를 돌려준다") {
            withSandbox { sandbox ->
                val path = ImportTempStorage(fileSystem, sandbox / "caro-import").newFile()

                fileSystem.write(path) { writeUtf8("zip") }

                fileSystem.read(path) { readUtf8() } shouldBe "zip"
            }
        }

        test("newFile은 호출마다 다른 경로를 돌려준다") {
            withSandbox { sandbox ->
                val storage = ImportTempStorage(fileSystem, sandbox / "caro-import")

                val paths = List(5) { storage.newFile() }

                paths.toSet().size shouldBe 5
            }
        }

        test("delete는 그 파일만 지운다") {
            withSandbox { sandbox ->
                val storage = ImportTempStorage(fileSystem, sandbox / "caro-import")
                val target = storage.newFile().also { path -> fileSystem.write(path) { writeUtf8("a") } }
                val other = storage.newFile().also { path -> fileSystem.write(path) { writeUtf8("b") } }

                storage.delete(target)

                fileSystem.exists(target) shouldBe false
                fileSystem.exists(other) shouldBe true
            }
        }

        test("delete는 이미 없는 파일이어도 던지지 않는다") {
            withSandbox { sandbox ->
                val storage = ImportTempStorage(fileSystem, sandbox / "caro-import")

                shouldNotThrowAny { storage.delete(storage.newFile()) }
            }
        }

        test("기본 위치는 시스템 임시 디렉터리의 caro-import 아래 화면마다 다른 디렉터리다") {
            val first = ImportTempStorage()
            val second = ImportTempStorage()
            val firstPath = first.newFile()
            val secondPath = second.newFile()
            try {
                val base = FileSystem.SYSTEM_TEMPORARY_DIRECTORY / "caro-import"
                firstPath.parent?.parent shouldBe base
                secondPath.parent?.parent shouldBe base
                firstPath.parent shouldNotBe secondPath.parent
            } finally {
                first.clearAll()
                second.clearAll()
            }
        }

        test("clearAll은 같은 caro-import 아래 다른 화면의 디렉터리를 건드리지 않는다") {
            withSandbox { sandbox ->
                val base = sandbox / "caro-import"
                val mine = ImportTempStorage(fileSystem, base / "session-mine")
                val other = ImportTempStorage(fileSystem, base / "session-other")
                val minePath = mine.newFile().also { path -> fileSystem.write(path) { writeUtf8("a") } }
                val otherPath = other.newFile().also { path -> fileSystem.write(path) { writeUtf8("b") } }

                mine.clearAll()

                fileSystem.exists(minePath) shouldBe false
                fileSystem.exists(otherPath) shouldBe true
            }
        }

        test("clearStale은 오래된 다른 화면의 디렉터리만 지우고 자기 디렉터리와 이름이 다른 형제는 남긴다") {
            withSandbox { sandbox ->
                val base = sandbox / "caro-import"
                val storage = ImportTempStorage(fileSystem, base / "session-mine", staleAfterMillis = 0L)
                fileSystem.createDirectories(base / "session-old")
                fileSystem.write(base / "session-old" / "copy-1") { writeUtf8("old") }
                fileSystem.write(base / "unrelated.txt") { writeUtf8("keep") }
                val own = storage.newFile().also { path -> fileSystem.write(path) { writeUtf8("own") } }

                storage.clearStale()

                fileSystem.exists(base / "session-old") shouldBe false
                fileSystem.exists(own) shouldBe true
                fileSystem.exists(base / "unrelated.txt") shouldBe true
            }
        }

        test("clearStale은 기준 시간보다 새로 만든 다른 화면의 디렉터리를 남긴다") {
            withSandbox { sandbox ->
                val base = sandbox / "caro-import"
                val storage = ImportTempStorage(fileSystem, base / "session-mine", staleAfterMillis = 60L * 60 * 1000)
                fileSystem.createDirectories(base / "session-fresh")
                fileSystem.write(base / "session-fresh" / "copy-1") { writeUtf8("live") }

                storage.clearStale()

                fileSystem.exists(base / "session-fresh" / "copy-1") shouldBe true
            }
        }

        test("clearStale은 디렉터리가 아직 없어도 던지지 않는다") {
            withSandbox { sandbox ->
                val storage = ImportTempStorage(fileSystem, sandbox / "caro-import" / "session-mine")

                shouldNotThrowAny { storage.clearStale() }
            }
        }
    }

    private fun withSandbox(block: (Path) -> Unit) {
        val sandbox =
            FileSystem.SYSTEM_TEMPORARY_DIRECTORY / "caro-importer-temp-storage-test" / Random.nextLong().toULong().toString(radix = 16)
        fileSystem.createDirectories(sandbox)
        try {
            block(sandbox)
        } finally {
            fileSystem.deleteRecursively(sandbox, mustExist = false)
        }
    }
}
