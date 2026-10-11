package com.whatever.caro.feature.importer.step

import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import caromobile.core.designsystem.generated.resources.Res
import caromobile.core.designsystem.generated.resources.import_a11y_table_row
import caromobile.core.designsystem.generated.resources.import_hint_table_more_columns
import caromobile.core.designsystem.generated.resources.import_label_column
import caromobile.core.designsystem.generated.resources.import_label_split_result
import caromobile.core.designsystem.generated.resources.import_label_table_info
import caromobile.core.designsystem.generated.resources.import_label_table_info_truncated
import caromobile.core.designsystem.generated.resources.import_label_table_row
import caromobile.core.designsystem.generated.resources.import_loading_resplit
import com.whatever.caro.core.designsystem.themes.CaroTheme
import com.whatever.caro.core.ui.loading.CaroLoadingCard
import com.whatever.caro.core.ui.loading.rememberDelayedVisible
import com.whatever.caro.core.util.NumberFormatter.formatWithComma
import com.whatever.caro.feature.importer.logic.TextCheckHeader
import com.whatever.caro.feature.importer.logic.TextCheckInfo
import com.whatever.caro.feature.importer.logic.TextCheckTable
import org.jetbrains.compose.resources.stringResource

private const val MAX_FILL_COLUMNS = 2
private const val CELL_MAX_LINES = 2
private val TableBorderWidth = 1.dp
private val TableStartPadding = 16.dp
private val CellEndPadding = 12.dp
private val CellVerticalPadding = 12.dp
private val MinColumnWidth = 64.dp

// 3열 이상일 때 세 번째 열이 오른쪽 끝에 이만큼 보여야 표가 옆으로 더 있다는 것을 알 수 있다.
private val ThirdColumnPeek = 40.dp

// 로딩 카드(Lottie 90, 여백 48, 문구)가 잘리지 않는 최소 본문 높이다.
private val LoadingBodyMinHeight = 176.dp

/**
 * 결과 표나 줄 카드. [isReparsing] 동안 본문은 그리지 않고 자리만 두어 높이를 지키고, 같은 자리에 로딩 카드를 보여준다.
 * [tableScrollState]는 3열 이상 표의 가로 위치다.
 */
@Composable
internal fun TextCheckResult(
    table: TextCheckTable,
    isReparsing: Boolean,
    tableScrollState: ScrollState,
    modifier: Modifier = Modifier,
) {
    val headerLabels =
        table.headers.map { header ->
            when (header) {
                is TextCheckHeader.Named -> header.text
                is TextCheckHeader.Numbered -> stringResource(Res.string.import_label_column, header.number)
            }
        }
    val readings =
        table.rows.mapIndexed { index, row ->
            val cells = headerLabels.zip(row) { label, value -> if (value.isEmpty()) label else "$label $value" }
            stringResource(Res.string.import_a11y_table_row, index + 1, cells.joinToString(", "))
        }

    Column(
        modifier = modifier.fillMaxWidth().padding(horizontal = CaroTheme.spacing.xl2),
        verticalArrangement = Arrangement.spacedBy(CaroTheme.spacing.s),
    ) {
        ResultTitleRow(info = table.info, isReparsing = isReparsing)
        if (isLargeText()) {
            ResultCards(headerLabels = headerLabels, rows = table.rows, readings = readings, isReparsing = isReparsing)
        } else {
            ResultTable(
                headerLabels = headerLabels,
                rows = table.rows,
                readings = readings,
                isReparsing = isReparsing,
                scrollState = tableScrollState,
            )
        }
        if (table.hasHiddenColumns) {
            Text(
                text = stringResource(Res.string.import_hint_table_more_columns),
                style = CaroTheme.typography.caption1.regular,
                color = CaroTheme.color.text.secondary,
            )
        }
    }
}

@Composable
private fun ResultTitleRow(
    info: TextCheckInfo,
    isReparsing: Boolean,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(CaroTheme.spacing.s),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = stringResource(Res.string.import_label_split_result),
            style = CaroTheme.typography.body2.semiBold,
            color = CaroTheme.color.text.primary,
        )
        // 다시 나누는 동안 이전 값은 화면에 두되 스크린 리더에는 비운다. 같은 노드의 문구가 바뀌어야 결과가 나왔을 때 알려진다.
        Text(
            text = infoText(info),
            modifier =
                Modifier.weight(1f).then(
                    if (isReparsing) {
                        Modifier.clearAndSetSemantics { liveRegion = LiveRegionMode.Polite }
                    } else {
                        Modifier.semantics { liveRegion = LiveRegionMode.Polite }
                    },
                ),
            style = CaroTheme.typography.caption1.regular,
            color = CaroTheme.color.text.secondary,
            textAlign = TextAlign.End,
        )
    }
}

@Composable
private fun infoText(info: TextCheckInfo): String =
    if (info.columnCount > info.shownColumnCount) {
        stringResource(
            Res.string.import_label_table_info_truncated,
            info.columnCount.formatWithComma(),
            info.shownColumnCount.formatWithComma(),
            info.rowCount.formatWithComma(),
            info.shownRowCount.formatWithComma(),
        )
    } else {
        stringResource(
            Res.string.import_label_table_info,
            info.columnCount.formatWithComma(),
            info.rowCount.formatWithComma(),
            info.shownRowCount.formatWithComma(),
        )
    }

/** 이전 결과는 화면과 접근성 트리에서 함께 숨긴다. 숨기지 않으면 새 결과가 오기 전까지 낡은 값이 맞는 것처럼 읽힌다. */
private fun Modifier.hiddenWhile(isHidden: Boolean): Modifier = if (isHidden) alpha(0f).clearAndSetSemantics { } else this

@Composable
private fun ResultTable(
    headerLabels: List<String>,
    rows: List<List<String>>,
    readings: List<String>,
    isReparsing: Boolean,
    scrollState: ScrollState,
) {
    val shape = CaroTheme.shape.m
    var headerHeight by remember { mutableIntStateOf(0) }
    val isLoadingCardVisible = rememberDelayedVisible(isReparsing)

    Box(
        modifier =
            Modifier
                .fillMaxWidth()
                .clip(shape)
                .background(CaroTheme.color.surface.primary)
                .border(TableBorderWidth, CaroTheme.color.border.secondary, shape)
                .padding(TableBorderWidth),
    ) {
        BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
            val isFillWidth = headerLabels.size <= MAX_FILL_COLUMNS
            // 열 폭은 보이는 폭에서 정한다. 폭이 달라도 세 번째 열이 같은 만큼 걸쳐 보인다.
            val columnWidth =
                if (isFillWidth) null else ((maxWidth - TableStartPadding - ThirdColumnPeek) / 2).coerceAtLeast(MinColumnWidth)
            val tableWidth =
                if (columnWidth == null) Modifier.fillMaxWidth() else Modifier.width(TableStartPadding + columnWidth * headerLabels.size)
            val bodyHeight = if (isReparsing) Modifier.heightIn(min = LoadingBodyMinHeight) else Modifier

            Column(modifier = if (isFillWidth) Modifier else Modifier.fillMaxWidth().horizontalScroll(scrollState)) {
                Column(modifier = tableWidth) {
                    TableHeaderRow(
                        labels = headerLabels,
                        columnWidth = columnWidth,
                        modifier = Modifier.onSizeChanged { size -> headerHeight = size.height },
                    )
                    TableDivider()
                    Column(modifier = bodyHeight.hiddenWhile(isReparsing)) {
                        rows.forEachIndexed { index, row ->
                            TableDataRow(cells = row, reading = readings[index], columnWidth = columnWidth)
                        }
                    }
                }
            }
            if (isLoadingCardVisible) {
                Box(
                    modifier = Modifier.matchParentSize().padding(top = with(LocalDensity.current) { headerHeight.toDp() }),
                    contentAlignment = Alignment.Center,
                ) {
                    CaroLoadingCard(message = stringResource(Res.string.import_loading_resplit))
                }
            }
        }
    }
}

/** 열 이름은 줄을 읽을 때 함께 읽으므로 따로 읽히지 않게 접근성 트리에서 뺀다. */
@Composable
private fun TableHeaderRow(
    labels: List<String>,
    columnWidth: Dp?,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier =
            modifier
                .fillMaxWidth()
                .background(CaroTheme.color.surface.secondary)
                .clearAndSetSemantics { }
                .padding(start = TableStartPadding, top = CellVerticalPadding, bottom = CellVerticalPadding),
    ) {
        labels.forEach { label ->
            CellText(
                text = label,
                columnWidth = columnWidth,
                style = CaroTheme.typography.caption1.regular,
                color = CaroTheme.color.text.secondary,
            )
        }
    }
}

/** 셀을 따로 읽으면 어느 열인지 알 수 없어 한 줄을 한 번에 읽는다. */
@Composable
private fun TableDataRow(
    cells: List<String>,
    reading: String,
    columnWidth: Dp?,
) {
    Column(modifier = Modifier.fillMaxWidth().clearAndSetSemantics { contentDescription = reading }) {
        Row(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(start = TableStartPadding, top = CellVerticalPadding, bottom = CellVerticalPadding),
        ) {
            cells.forEach { cell ->
                CellText(text = cell, columnWidth = columnWidth, style = CaroTheme.typography.label1, color = CaroTheme.color.text.primary)
            }
        }
        TableDivider()
    }
}

@Composable
private fun TableDivider() {
    Box(modifier = Modifier.fillMaxWidth().height(TableBorderWidth).background(CaroTheme.color.divider.secondary))
}

/** [columnWidth]가 null이면 남는 폭을 열 수만큼 나눈다. */
@Composable
private fun RowScope.CellText(
    text: String,
    columnWidth: Dp?,
    style: TextStyle,
    color: Color,
) {
    Text(
        text = text,
        modifier = (if (columnWidth == null) Modifier.weight(1f) else Modifier.width(columnWidth)).padding(end = CellEndPadding),
        style = style,
        color = color,
        maxLines = CELL_MAX_LINES,
        overflow = TextOverflow.Ellipsis,
    )
}

@Composable
private fun ResultCards(
    headerLabels: List<String>,
    rows: List<List<String>>,
    readings: List<String>,
    isReparsing: Boolean,
) {
    val isLoadingCardVisible = rememberDelayedVisible(isReparsing)

    Box(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.fillMaxWidth().hiddenWhile(isReparsing),
            verticalArrangement = Arrangement.spacedBy(CaroTheme.spacing.m),
        ) {
            rows.forEachIndexed { index, row ->
                RowCard(rowNumber = index + 1, headerLabels = headerLabels, cells = row, reading = readings[index])
            }
        }
        if (isLoadingCardVisible) {
            // 카드가 세로로 길어 가운데에 두면 화면 밖일 수 있어 위쪽에 둔다.
            Box(modifier = Modifier.matchParentSize(), contentAlignment = Alignment.TopCenter) {
                CaroLoadingCard(message = stringResource(Res.string.import_loading_resplit))
            }
        }
    }
}

/** 한 줄 카드 안의 `열 N: 값` 줄은 따로 읽지 않고 카드 전체를 한 번에 읽는다. */
@Composable
private fun RowCard(
    rowNumber: Int,
    headerLabels: List<String>,
    cells: List<String>,
    reading: String,
) {
    val shape = CaroTheme.shape.m
    Column(
        modifier =
            Modifier
                .fillMaxWidth()
                .clip(shape)
                .background(CaroTheme.color.surface.primary)
                .border(TableBorderWidth, CaroTheme.color.border.secondary, shape)
                .clearAndSetSemantics { contentDescription = reading }
                .padding(CaroTheme.spacing.l),
        verticalArrangement = Arrangement.spacedBy(CaroTheme.spacing.xs),
    ) {
        Text(
            text = stringResource(Res.string.import_label_table_row, rowNumber),
            style = CaroTheme.typography.caption1.regular,
            color = CaroTheme.color.text.secondary,
        )
        headerLabels.zip(cells).forEach { (label, cell) ->
            Text(
                text = "$label: $cell",
                style = CaroTheme.typography.label1,
                color = CaroTheme.color.text.primary,
                maxLines = CELL_MAX_LINES,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}
