package com.whatever.caro.core.data.repository.library

import com.whatever.caro.core.model.exception.CaroServerException
import com.whatever.caro.core.remote.datasource.library.LibraryDataSource
import com.whatever.caro.core.remote.dto.library.LibraryCardResponse
import com.whatever.caro.core.remote.dto.library.LibraryCopyResponse
import com.whatever.caro.core.remote.dto.library.LibraryDeckResponse
import com.whatever.caro.core.remote.dto.library.LibraryDetailResponse
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

class LibraryRepositoryImplTest :
    FunSpec({
        test("목록, 미리보기와 copy 결과를 개인 덱 모델까지 연결한다") {
            val source = FakeLibraryDataSource()
            val repository = LibraryRepositoryImpl(source)
            repository.list().single().id shouldBe 2L
            repository
                .detail(2)
                .cards
                .single()
                .front shouldBe "apply"
            val copied = repository.copy(2, "request-key")
            copied.id shouldBe 10L
            copied.title shouldBe "단어 (2)"
            copied.todayCompleteCount shouldBe 0
            source.copyCalls shouldBe 1
            source.keys shouldBe listOf("request-key")
        }
        test("copy 오류는 자동 재요청 없이 화면에 전달한다") {
            val source = FakeLibraryDataSource()
            val error = CaroServerException("C999", "server error", "fixture")
            source.failure = error
            val repository = LibraryRepositoryImpl(source)
            shouldThrow<CaroServerException> { repository.copy(2, "same-key") } shouldBe error
            source.copyCalls shouldBe 1
        }
    })

private class FakeLibraryDataSource : LibraryDataSource {
    var copyCalls = 0
    val keys = mutableListOf<String>()
    var failure: Throwable? = null

    override suspend fun list() = listOf(LibraryDeckResponse(2, "단어", "뜻", 1))

    override suspend fun detail(libraryDeckId: Long) =
        LibraryDetailResponse(
            libraryDeckId,
            "단어",
            "뜻",
            1,
            listOf(LibraryCardResponse(3, "apply", "지원하다", 0)),
        )

    override suspend fun copy(
        libraryDeckId: Long,
        requestKey: String,
    ): LibraryCopyResponse {
        copyCalls++
        keys.add(requestKey)
        failure?.let { throw it }
        return LibraryCopyResponse(10, "단어 (2)", "뜻", 1)
    }
}
