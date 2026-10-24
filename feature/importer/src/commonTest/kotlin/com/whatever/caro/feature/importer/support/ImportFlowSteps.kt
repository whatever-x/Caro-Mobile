package com.whatever.caro.feature.importer.support

import com.whatever.caro.feature.importer.ImportFixtures
import com.whatever.caro.feature.importer.mvi.ImportDeckIntent
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle

internal const val EMPTY_SIDE_TEXT = ImportFixtures.EMPTY_SIDE_KO
internal const val FILE_DESCRIPTION_TEMPLATE = "{file}에서 가져왔어요"
internal const val PASTE_DESCRIPTION = "붙여 넣은 내용으로 만들었어요"

/** 파일을 고르고 선택 단계의 [다음]까지 눌러 Mapping에 간다. 읽기 대본이 첫 읽기와 apkg의 다시 읽기를 차례로 돌려줘야 한다. */
internal fun TestScope.openMappingFromFile(
    harness: ImportViewModelHarness,
    fileName: String,
) {
    harness.pick(FakePickedImportFile.ofSize(fileName, size = 100))
    advanceUntilIdle()
    harness.send(ImportDeckIntent.ClickSelectionNext)
    advanceUntilIdle()
}

/** fixture 파일을 실제 파서 리더로 읽어 Mapping까지 간다. */
internal fun TestScope.openMappingFromFixture(
    harness: ImportViewModelHarness,
    relativePath: String,
) {
    harness.pick(FakePickedImportFile.fromFixture(relativePath))
    advanceUntilIdle()
    harness.send(ImportDeckIntent.ClickSelectionNext)
    advanceUntilIdle()
}

internal fun TestScope.openMappingFromPaste(
    harness: ImportViewModelHarness,
    text: String = "a\tb",
) {
    harness.pasteNext(text)
    advanceUntilIdle()
    harness.send(ImportDeckIntent.ClickSelectionNext)
    advanceUntilIdle()
}

internal fun TestScope.openPreview(harness: ImportViewModelHarness) {
    harness.send(ImportDeckIntent.ClickMappingNext(EMPTY_SIDE_TEXT))
    advanceUntilIdle()
}

internal fun TestScope.openDeckInfo(harness: ImportViewModelHarness) {
    harness.send(ImportDeckIntent.ClickPreviewNext(FILE_DESCRIPTION_TEMPLATE, PASTE_DESCRIPTION))
    advanceUntilIdle()
}
