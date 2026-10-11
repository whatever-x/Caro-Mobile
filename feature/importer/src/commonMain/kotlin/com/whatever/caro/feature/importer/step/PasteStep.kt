package com.whatever.caro.feature.importer.step

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.exclude
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredHeightIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import caromobile.core.designsystem.generated.resources.Res
import caromobile.core.designsystem.generated.resources.import_button_next
import caromobile.core.designsystem.generated.resources.import_hint_paste_columns
import caromobile.core.designsystem.generated.resources.import_hint_paste_row
import caromobile.core.designsystem.generated.resources.import_reason_paste_too_long
import caromobile.core.designsystem.generated.resources.import_subtitle_paste
import com.whatever.caro.core.designsystem.components.CaroTextArea
import com.whatever.caro.core.designsystem.themes.CaroTheme
import com.whatever.caro.core.util.NumberFormatter.formatWithComma
import com.whatever.caro.feature.importer.component.ImportBottomBar
import com.whatever.caro.feature.importer.component.ImportStepTitle
import com.whatever.caro.feature.importer.logic.PasteBlockReason
import com.whatever.caro.feature.importer.logic.PasteInputMinHeight
import com.whatever.caro.feature.importer.logic.pasteBlockReason
import com.whatever.caro.feature.importer.logic.shouldHidePasteGuide
import org.jetbrains.compose.resources.stringResource

/** 상한을 넘은 글은 자르지 않고 [다음]만 막는다. 자르면 알리지 않은 채 카드가 사라진다. */
@Composable
internal fun PasteStep(
    text: String,
    maxPasteChars: Int,
    onTextChange: (String) -> Unit,
    onNext: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val density = LocalDensity.current
    val ime = WindowInsets.ime
    // 키보드가 올라오는 애니메이션 동안 매 프레임 바뀌는 높이를 구성에서 읽으면 최대 100만 자 입력칸까지 다시 구성하므로 보이는지 여부만 읽는다.
    val isImeVisible by remember(ime, density) { derivedStateOf { ime.getBottom(density) > 0 } }

    // 제목과 안내를 보였을 때 잰 높이다. 숨긴 동안 입력칸이 이만큼 커지므로 보였다면 남았을 높이를 구하는 데 쓴다.
    var titleHeight by remember { mutableIntStateOf(0) }
    var guideHeight by remember { mutableIntStateOf(0) }
    var inputHeightWithGuide by remember { mutableIntStateOf(Int.MAX_VALUE) }
    val hideGuide = shouldHidePasteGuide(isImeVisible, with(density) { inputHeightWithGuide.toDp() })

    val fieldLabel = stringResource(Res.string.import_subtitle_paste)
    val blockReason = pasteBlockReason(text, maxPasteChars)
    val tooLongReason =
        if (blockReason == PasteBlockReason.TooLong) {
            stringResource(Res.string.import_reason_paste_too_long, maxPasteChars.formatWithComma())
        } else {
            null
        }

    Column(
        modifier =
            modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.ime.exclude(WindowInsets.navigationBars))
                .padding(top = CaroTheme.spacing.xl),
    ) {
        if (!hideGuide) {
            ImportStepTitle(
                text = fieldLabel,
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .onSizeChanged { size -> titleHeight = size.height }
                        .padding(start = CaroTheme.spacing.xl2, end = CaroTheme.spacing.xl2, bottom = CaroTheme.spacing.xl2),
            )
        }
        // onSizeChanged는 requiredHeightIn 앞에 둬야 최소 높이로 올리기 전의 남는 높이를 잰다. heightIn은 weight가 준 높이를 넘지 못해 쓰지 않는다.
        CaroTextArea(
            value = text,
            onValueChange = onTextChange,
            modifier =
                Modifier
                    .weight(1f)
                    .onSizeChanged { size ->
                        inputHeightWithGuide = if (hideGuide) size.height - titleHeight - guideHeight else size.height
                    }.requiredHeightIn(min = PasteInputMinHeight)
                    .padding(horizontal = CaroTheme.spacing.xl2),
            fillHeight = true,
            semanticsLabel = fieldLabel,
        )
        if (!hideGuide) {
            Column(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .onSizeChanged { size -> guideHeight = size.height }
                        .padding(start = CaroTheme.spacing.xl2, end = CaroTheme.spacing.xl2, top = CaroTheme.spacing.s),
            ) {
                PasteGuideLine(text = stringResource(Res.string.import_hint_paste_row))
                PasteGuideLine(text = stringResource(Res.string.import_hint_paste_columns))
            }
        }
        ImportBottomBar(
            buttonText = stringResource(Res.string.import_button_next),
            onClick = onNext,
            enabled = blockReason == null,
            reason = tooLongReason,
        )
    }
}

@Composable
private fun PasteGuideLine(text: String) {
    Text(
        text = text,
        style = CaroTheme.typography.caption1.regular,
        color = CaroTheme.color.text.secondary,
    )
}
