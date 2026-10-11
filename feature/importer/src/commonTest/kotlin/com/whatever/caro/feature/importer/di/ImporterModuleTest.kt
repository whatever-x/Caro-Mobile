package com.whatever.caro.feature.importer.di

import com.whatever.caro.core.data.repository.card.CardRepository
import com.whatever.caro.core.data.repository.deck.DeckRepository
import com.whatever.caro.core.viewmodel.ExceptionFilter
import com.whatever.caro.feature.importer.ImportDeckViewModel
import com.whatever.caro.feature.importer.save.ImportSaveCoordinatorFactory
import com.whatever.caro.feature.importer.source.ImportTempStorage
import dev.mokkery.mock
import io.kotest.assertions.throwables.shouldNotThrowAny
import io.kotest.core.spec.style.FunSpec
import io.kotest.koin.KoinExtension
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import okio.FileSystem
import org.koin.dsl.module
import org.koin.test.KoinTest
import org.koin.test.get

@OptIn(ExperimentalCoroutinesApi::class)
class ImporterModuleTest :
    FunSpec(),
    KoinTest {
    init {
        // importerModule이 밖에서 받는 의존성만 대신 채운다. 앱에서는 data 모듈과 앱 정책 모듈이 채운다.
        val externalDependenciesModule =
            module {
                single<DeckRepository> { mock<DeckRepository>() }
                single<CardRepository> { mock<CardRepository>() }
                single<ExceptionFilter> { ExceptionFilter.None }
            }

        extensions(KoinExtension(listOf(importerModule, externalDependenciesModule)))

        beforeTest {
            Dispatchers.setMain(StandardTestDispatcher())
        }

        afterTest {
            Dispatchers.resetMain()
        }

        test("importerModule은 ImportDeckViewModel을 만들 수 있다") {
            shouldNotThrowAny { get<ImportDeckViewModel>() }
        }

        test("ImportSaveCoordinatorFactory는 부를 때마다 새 조정 객체를 만든다") {
            val factory: ImportSaveCoordinatorFactory = get()

            // 조정 객체가 키별 첫 전송 시각을 기억하므로 저장 흐름이 하나 끝나면 다음 흐름은 빈 상태에서 시작해야 한다.
            (factory.create() === factory.create()) shouldBe false
        }

        test("ImportTempStorage는 요청마다 다른 임시 디렉터리를 쓴다") {
            val firstDirectory = get<ImportTempStorage>().newFile().parent
            val secondDirectory = get<ImportTempStorage>().newFile().parent

            try {
                // 앞 화면이 정리하며 지우는 디렉터리를 다음 화면이 함께 쓰면 방금 복사한 파일이 지워진다.
                firstDirectory shouldNotBe secondDirectory
            } finally {
                listOfNotNull(firstDirectory, secondDirectory).forEach { directory ->
                    FileSystem.SYSTEM.deleteRecursively(directory, mustExist = false)
                }
            }
        }
    }
}
