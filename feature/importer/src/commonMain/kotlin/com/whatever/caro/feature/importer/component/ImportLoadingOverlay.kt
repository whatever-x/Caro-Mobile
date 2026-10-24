package com.whatever.caro.feature.importer.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.semantics.clearAndSetSemantics
import caromobile.core.designsystem.generated.resources.Res
import caromobile.core.designsystem.generated.resources.import_loading_creating_deck
import caromobile.core.designsystem.generated.resources.import_loading_file
import caromobile.core.designsystem.generated.resources.import_loading_paste
import caromobile.core.designsystem.generated.resources.import_loading_preview
import caromobile.core.designsystem.generated.resources.import_loading_resplit
import com.whatever.caro.core.designsystem.themes.CaroTheme
import com.whatever.caro.core.ui.loading.CaroLoadingCard
import com.whatever.caro.core.ui.loading.rememberDelayedVisible
import com.whatever.caro.feature.importer.logic.truncateFileName
import com.whatever.caro.feature.importer.mvi.ImportLoading
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource

/**
 * 단계 위를 덮는 읽기 표시. 뒤로 가기는 막지 않으므로 NavigationBackHandler를 두지 않고 판단은 ViewModel이 한다.
 * 로딩이 끝나면 [content] 안의 [ImportStepTitle]에 포커스를 둔다.
 */
@Composable
internal fun ImportLoadingOverlay(
    loading: ImportLoading?,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    val isLoading = loading != null
    val titleFocus = remember { ImportTitleFocus() }
    var hasLoaded by remember { mutableStateOf(false) }

    LaunchedEffect(isLoading) {
        if (isLoading) {
            hasLoaded = true
        } else if (hasLoaded) {
            hasLoaded = false
            titleFocus.requestFocus()
        }
    }

    CompositionLocalProvider(LocalImportTitleFocus provides titleFocus) {
        ImportBlockingBox(
            isBlocking = isLoading,
            modifier = modifier,
            card = { loading?.let { ImportLoadingCard(it) } },
            content = content,
        )
    }
}

/** 덱을 만드는 동안 상단 바까지 덮는다. */
@Composable
internal fun ImportCreatingDeckOverlay(
    isCreating: Boolean,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    ImportBlockingBox(
        isBlocking = isCreating,
        modifier = modifier,
        card = { ImportLoadingMessageCard(Res.string.import_loading_creating_deck) },
        content = content,
    )
}

/**
 * 입력 차단, semantics 비우기, 포커스 해제는 바로 하고 스크림과 카드는 200ms 뒤에 보여 짧은 읽기에서 깜빡이지 않게 한다.
 * 읽는 동안에도 뒤로 가기는 쓸 수 있어야 하므로 Back과 Escape 키는 막지 않는다. 키보드 Tab이 가려진 내용으로 들어가지 못하게 포커스도 막는다.
 */
@Composable
private fun ImportBlockingBox(
    isBlocking: Boolean,
    modifier: Modifier,
    card: @Composable () -> Unit,
    content: @Composable () -> Unit,
) {
    val focusManager = LocalFocusManager.current
    val isCardVisible = rememberDelayedVisible(isBlocking)

    LaunchedEffect(isBlocking) {
        if (isBlocking) focusManager.clearFocus(force = true)
    }

    Box(
        modifier =
            modifier
                .fillMaxSize()
                .onPreviewKeyEvent { event -> isBlocking && event.key != Key.Back && event.key != Key.Escape },
    ) {
        Box(
            modifier =
                Modifier
                    .fillMaxSize()
                    .focusProperties { canFocus = !isBlocking }
                    .then(if (isBlocking) Modifier.clearAndSetSemantics { } else Modifier),
        ) {
            content()
        }

        if (isBlocking) {
            Box(modifier = Modifier.fillMaxSize().consumeAllPointerInput())
        }

        if (isCardVisible) {
            // 큰 글자에서 카드가 화면보다 길 때만 스크롤되고, 평소에는 가운데에 놓인다.
            Column(
                modifier =
                    Modifier
                        .fillMaxSize()
                        .background(CaroTheme.color.overlay.dim)
                        .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                card()
            }
        }
    }
}

@Composable
private fun ImportLoadingCard(loading: ImportLoading) {
    when (loading) {
        is ImportLoading.Parsing -> {
            if (loading.fileName == null) {
                ImportLoadingMessageCard(Res.string.import_loading_paste)
            } else {
                ImportLoadingMessageCard(Res.string.import_loading_file, loading.fileName)
            }
        }

        is ImportLoading.Reparsing -> {
            ImportLoadingMessageCard(Res.string.import_loading_file, loading.fileName)
        }

        ImportLoading.BuildingPreview -> {
            ImportLoadingMessageCard(Res.string.import_loading_preview)
        }

        ImportLoading.TextReparsing -> {
            ImportLoadingMessageCard(Res.string.import_loading_resplit)
        }
    }
}

@Composable
private fun ImportLoadingMessageCard(
    message: StringResource,
    fileName: String? = null,
) {
    CaroLoadingCard(
        message = stringResource(message),
        modifier = Modifier.padding(horizontal = CaroTheme.spacing.xl2),
        detail = fileName?.let(::truncateFileName),
    )
}

private fun Modifier.consumeAllPointerInput(): Modifier =
    pointerInput(Unit) {
        awaitPointerEventScope {
            while (true) {
                awaitPointerEvent().changes.forEach { change -> change.consume() }
            }
        }
    }
