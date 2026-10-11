package com.whatever.caro.feature.importer.di

import com.whatever.caro.core.importparser.ImportParser
import com.whatever.caro.feature.importer.ImportDeckViewModel
import com.whatever.caro.feature.importer.platform.platformIoDispatcher
import com.whatever.caro.feature.importer.save.ImportSaveCoordinator
import com.whatever.caro.feature.importer.save.ImportSaveCoordinatorFactory
import com.whatever.caro.feature.importer.source.ImportDispatchers
import com.whatever.caro.feature.importer.source.ImportFileCopy
import com.whatever.caro.feature.importer.source.ImportLimitsConfig
import com.whatever.caro.feature.importer.source.ImportSourceReader
import com.whatever.caro.feature.importer.source.ImportTempStorage
import com.whatever.caro.feature.importer.source.ParserImportSourceReader
import kotlinx.coroutines.Dispatchers
import okio.FileSystem
import org.koin.dsl.module
import org.koin.plugin.module.dsl.viewModel

val importerModule =
    module {
        viewModel<ImportDeckViewModel>()
        single { ImportParser(FileSystem.SYSTEM) }
        single<ImportSourceReader> { ParserImportSourceReader(get(), get()) }
        single { ImportLimitsConfig() }
        single { ImportDispatchers(platformIoDispatcher(), Dispatchers.Default) }
        single { ImportFileCopy(FileSystem.SYSTEM) }
        // 화면마다 임시 디렉터리가 달라야 앞 화면의 정리가 새 화면이 방금 복사한 파일을 지우지 않는다.
        factory { ImportTempStorage() }
        // 조정 객체는 저장 흐름마다 새로 만들어야 하므로 팩토리만 single로 둔다.
        single<ImportSaveCoordinatorFactory> { ImportSaveCoordinatorFactory { ImportSaveCoordinator(get(), get()) } }
    }
