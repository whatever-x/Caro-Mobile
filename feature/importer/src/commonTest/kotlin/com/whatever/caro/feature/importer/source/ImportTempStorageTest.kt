package com.whatever.caro.feature.importer.source

import io.kotest.assertions.throwables.shouldNotThrowAny
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.nulls.shouldNotBeNull
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

        test("clearStale은 같은 프로세스에서 기준 시간보다 새로 만든 다른 화면의 디렉터리를 남긴다") {
            withSandbox { sandbox ->
                val base = sandbox / "caro-import"
                val storage = storageIn(base, staleAfterMillis = 60L * 60 * 1000)
                fileSystem.createDirectories(base / "session-tag1-fresh")
                fileSystem.write(base / "session-tag1-fresh" / "copy-1") { writeUtf8("live") }

                storage.clearStale()

                fileSystem.exists(base / "session-tag1-fresh" / "copy-1") shouldBe true
            }
        }

        test("clearStale은 같은 프로세스의 다른 화면 디렉터리도 기준 시간이 지나면 지운다") {
            withSandbox { sandbox ->
                val base = sandbox / "caro-import"
                val storage = storageIn(base, staleAfterMillis = 0L)
                fileSystem.createDirectories(base / "session-tag1-old")
                fileSystem.write(base / "session-tag1-old" / "copy-1") { writeUtf8("old") }

                storage.clearStale()

                fileSystem.exists(base / "session-tag1-old") shouldBe false
            }
        }

        test("clearStale은 이전 프로세스가 만든 디렉터리를 방금 만든 것이어도 시간과 상관없이 지운다") {
            withSandbox { sandbox ->
                val base = sandbox / "caro-import"
                val storage = storageIn(base, staleAfterMillis = 60L * 60 * 1000)
                fileSystem.createDirectories(base / "session-tag0-crashed")
                fileSystem.write(base / "session-tag0-crashed" / "copy-1") { writeUtf8("study data") }
                fileSystem.write(base / "session-tag0-crashed" / "importparser-1a2b") { writeUtf8("db") }

                storage.clearStale()

                fileSystem.exists(base / "session-tag0-crashed") shouldBe false
                fileSystem.exists(base / "session-tag1-mine") shouldBe true
            }
        }

        test("clearStale은 프로세스 표식이 없는 옛 형식의 세션 디렉터리도 이전 프로세스의 것으로 보고 지운다") {
            withSandbox { sandbox ->
                val base = sandbox / "caro-import"
                val storage = storageIn(base, staleAfterMillis = 60L * 60 * 1000)
                fileSystem.createDirectories(base / "session-legacy")

                storage.clearStale()

                fileSystem.exists(base / "session-legacy") shouldBe false
            }
        }

        test("clearStale은 표식이 앞부분만 같은 다른 프로세스의 디렉터리를 현재 것으로 착각하지 않는다") {
            withSandbox { sandbox ->
                val base = sandbox / "caro-import"
                val storage = storageIn(base, staleAfterMillis = 60L * 60 * 1000)
                fileSystem.createDirectories(base / "session-tag10-other")

                storage.clearStale()

                fileSystem.exists(base / "session-tag10-other") shouldBe false
            }
        }

        test("기본 세션 디렉터리 이름은 프로세스 표식을 포함하고 같은 프로세스의 화면끼리 표식이 같다") {
            val first =
                ImportTempStorage()
                    .newFile()
                    .parent
                    ?.name
                    .orEmpty()
            val second =
                ImportTempStorage()
                    .newFile()
                    .parent
                    ?.name
                    .orEmpty()
            try {
                val pattern = Regex("session-([0-9a-f]+)-([0-9a-f]+)")
                val firstMatch = pattern.matchEntire(first)
                val secondMatch = pattern.matchEntire(second)
                firstMatch.shouldNotBeNull()
                secondMatch.shouldNotBeNull()
                firstMatch.groupValues[1] shouldBe secondMatch.groupValues[1]
                firstMatch.groupValues[2] shouldNotBe secondMatch.groupValues[2]
            } finally {
                listOf(first, second).forEach { name ->
                    fileSystem.deleteRecursively(ImportTempStorage.DEFAULT_ROOT / name, mustExist = false)
                }
            }
        }

        test("clearStale은 기준 시간이 지난 시스템 선택기 사본을 지우고 디렉터리는 남긴다") {
            withSandbox { sandbox ->
                val inbox = sandbox / "app-Inbox"
                fileSystem.createDirectories(inbox)
                fileSystem.write(inbox / "deck.apkg") { writeUtf8("study data") }
                val storage = storageIn(sandbox / "caro-import", staleAfterMillis = 0L, inboxDirectory = inbox)

                storage.clearStale()

                fileSystem.exists(inbox / "deck.apkg") shouldBe false
                fileSystem.exists(inbox) shouldBe true
            }
        }

        test("clearStale은 기준 시간보다 새로 생긴 시스템 선택기 사본을 남긴다") {
            withSandbox { sandbox ->
                val inbox = sandbox / "app-Inbox"
                fileSystem.createDirectories(inbox)
                fileSystem.write(inbox / "deck.apkg") { writeUtf8("picking now") }
                val storage = storageIn(sandbox / "caro-import", staleAfterMillis = 60L * 60 * 1000, inboxDirectory = inbox)

                storage.clearStale()

                fileSystem.exists(inbox / "deck.apkg") shouldBe true
            }
        }

        test("clearStale은 시스템 선택기 디렉터리가 없어도 던지지 않는다") {
            withSandbox { sandbox ->
                val storage = storageIn(sandbox / "caro-import", staleAfterMillis = 0L, inboxDirectory = sandbox / "missing-Inbox")

                shouldNotThrowAny { storage.clearStale() }
            }
        }

        test("clearStale은 디렉터리가 아직 없어도 던지지 않는다") {
            withSandbox { sandbox ->
                val storage = ImportTempStorage(fileSystem, sandbox / "caro-import" / "session-mine")

                shouldNotThrowAny { storage.clearStale() }
            }
        }
    }

    // 현재 프로세스 표식을 tag1로 고정한다. 이 프로세스의 화면은 session-tag1-*, 이전 프로세스의 화면은 그 밖의 이름이다.
    private fun storageIn(
        base: Path,
        staleAfterMillis: Long,
        inboxDirectory: Path? = null,
    ) = ImportTempStorage(fileSystem, base / "session-tag1-mine", staleAfterMillis, processTag = "tag1", inboxDirectory = inboxDirectory)

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
