package com.whatever.caro.feature.learning.report.components

import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import caromobile.core.designsystem.generated.resources.Res
import caromobile.core.designsystem.generated.resources.ic_arrow_down_16
import caromobile.core.designsystem.generated.resources.ic_arrow_right_16
import caromobile.core.designsystem.generated.resources.ic_arrow_up_16
import caromobile.core.designsystem.generated.resources.report_all_period_range
import caromobile.core.designsystem.generated.resources.report_chart_day_title
import caromobile.core.designsystem.generated.resources.report_chart_description
import caromobile.core.designsystem.generated.resources.report_chart_month_label
import caromobile.core.designsystem.generated.resources.report_chart_month_title
import caromobile.core.designsystem.generated.resources.report_chart_week_title
import caromobile.core.designsystem.generated.resources.report_date_range
import caromobile.core.designsystem.generated.resources.report_load_more_error
import caromobile.core.designsystem.generated.resources.report_month_all
import caromobile.core.designsystem.generated.resources.report_month_title
import caromobile.core.designsystem.generated.resources.report_rating_title
import caromobile.core.designsystem.generated.resources.report_rating_value
import caromobile.core.designsystem.generated.resources.report_scope_all
import caromobile.core.designsystem.generated.resources.report_scope_button_description
import caromobile.core.designsystem.generated.resources.report_scope_collapsed
import caromobile.core.designsystem.generated.resources.report_scope_deleted_deck
import caromobile.core.designsystem.generated.resources.report_scope_expanded
import caromobile.core.designsystem.generated.resources.report_sheet_custom
import caromobile.core.designsystem.generated.resources.report_sheet_recent7
import caromobile.core.designsystem.generated.resources.report_stat_card_label
import caromobile.core.designsystem.generated.resources.report_stat_card_value
import caromobile.core.designsystem.generated.resources.report_stat_day_label
import caromobile.core.designsystem.generated.resources.report_stat_day_value
import caromobile.core.designsystem.generated.resources.report_stat_review_label
import caromobile.core.designsystem.generated.resources.report_stat_review_value
import caromobile.core.designsystem.generated.resources.report_week_label
import com.whatever.caro.core.designsystem.modifier.noRippleClickable
import com.whatever.caro.core.designsystem.themes.CaroTheme
import com.whatever.caro.core.model.learning.StudyRating
import com.whatever.caro.core.model.report.ReportBucket
import com.whatever.caro.core.model.report.ReportBucketUnit
import com.whatever.caro.core.model.report.ReportCounts
import com.whatever.caro.core.model.report.ReportPeriod
import com.whatever.caro.feature.learning.report.ReportPeriodPreset
import com.whatever.caro.feature.learning.report.ReportWeek
import com.whatever.caro.feature.learning.report.mvi.ReportMonthDetail
import kotlinx.datetime.LocalDate
import kotlinx.datetime.Month
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import kotlin.math.roundToInt

private val ChartHeight = 120.dp
private val ChartBarMaxWidth = 20.dp
private val ChartBarMinHeight = 2.dp
private val ChartFadeWidth = 16.dp
private val NarrowStatsWidth = 300.dp
private const val CHART_VISIBLE_BARS = 8
private const val DIMMED_ALPHA = 0.35f

/**
 * 리포트 상단의 덱 이름과 꺾쇠, 적용한 날짜. 이름과 꺾쇠를 하나의 버튼으로 묶어 덱 선택 시트를 연다.
 */
@Composable
internal fun ReportScopeHeader(
    deckId: Long?,
    deckName: String?,
    deckDeleted: Boolean,
    period: ReportPeriod?,
    isSheetOpen: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val name =
        when {
            deckId == null -> stringResource(Res.string.report_scope_all)
            deckDeleted || deckName == null -> stringResource(Res.string.report_scope_deleted_deck)
            else -> deckName
        }
    val rangeText = period.toRangeText()
    val description = stringResource(Res.string.report_scope_button_description, name, rangeText)
    val sheetState = stringResource(if (isSheetOpen) Res.string.report_scope_expanded else Res.string.report_scope_collapsed)
    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier =
                Modifier
                    .heightIn(min = ReportMinTouchSize)
                    .noRippleClickable(onClick = onClick)
                    .clearAndSetSemantics {
                        role = Role.DropdownList
                        contentDescription = description
                        stateDescription = sheetState
                    },
            horizontalArrangement = Arrangement.spacedBy(CaroTheme.spacing.s),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = name,
                modifier = Modifier.weight(1f, fill = false),
                style = CaroTheme.typography.heading1,
                color = CaroTheme.color.text.primary,
            )
            Icon(
                painter = painterResource(if (isSheetOpen) Res.drawable.ic_arrow_up_16 else Res.drawable.ic_arrow_down_16),
                contentDescription = null,
                tint = CaroTheme.color.icon.secondary,
            )
        }
        Text(text = rangeText, style = CaroTheme.typography.body2.medium, color = CaroTheme.color.text.secondary)
    }
}

/**
 * 기간 칩. "직접 설정"은 기간 직접 설정 시트를 연다.
 */
@Composable
internal fun ReportPeriodChips(
    selected: ReportPeriodPreset,
    onSelect: (ReportPeriodPreset) -> Unit,
    modifier: Modifier = Modifier,
) {
    FlowRow(modifier = modifier, horizontalArrangement = Arrangement.spacedBy(CaroTheme.spacing.s)) {
        listOf(
            ReportPeriodPreset.ALL to Res.string.report_scope_all,
            ReportPeriodPreset.RECENT_7 to Res.string.report_sheet_recent7,
            ReportPeriodPreset.CUSTOM to Res.string.report_sheet_custom,
        ).forEach { (preset, label) ->
            ReportChip(text = stringResource(label), isSelected = selected == preset, onClick = { onSelect(preset) })
        }
    }
}

@Composable
internal fun ReportPeriod?.toRangeText(): String =
    if (this == null) {
        stringResource(Res.string.report_all_period_range)
    } else {
        stringResource(Res.string.report_date_range, startDate.toReportDateText(), endDate.toReportDateText())
    }

/**
 * 평가 횟수, 학습한 카드, 학습일을 한 카드에 둔다. 좁은 화면에서는 세로로 쌓는다.
 */
@Composable
internal fun ReportStats(
    counts: ReportCounts,
    modifier: Modifier = Modifier,
) {
    val items =
        listOf(
            stringResource(Res.string.report_stat_review_value, counts.reviewCount.formatCount()) to
                stringResource(Res.string.report_stat_review_label),
            stringResource(Res.string.report_stat_card_value, counts.uniqueCardCount.formatCount()) to
                stringResource(Res.string.report_stat_card_label),
            stringResource(Res.string.report_stat_day_value, counts.studyDayCount.formatCount()) to
                stringResource(Res.string.report_stat_day_label),
        )
    BoxWithConstraints(
        modifier =
            modifier
                .fillMaxWidth()
                .background(color = CaroTheme.color.surface.primary, shape = CaroTheme.shape.l)
                .border(width = 1.dp, color = CaroTheme.color.border.secondary, shape = CaroTheme.shape.l)
                .padding(vertical = CaroTheme.spacing.xl),
    ) {
        if (maxWidth < NarrowStatsWidth) {
            Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(CaroTheme.spacing.l)) {
                items.forEach { (value, label) -> ReportStat(value = value, label = label, modifier = Modifier.fillMaxWidth()) }
            }
        } else {
            Row(verticalAlignment = Alignment.CenterVertically) {
                items.forEachIndexed { index, (value, label) ->
                    if (index > 0) {
                        Spacer(modifier = Modifier.width(1.dp).height(32.dp).background(CaroTheme.color.divider.secondary))
                    }
                    ReportStat(value = value, label = label, modifier = Modifier.weight(1f))
                }
            }
        }
    }
}

@Composable
private fun ReportStat(
    value: String,
    label: String,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(CaroTheme.spacing.xs),
    ) {
        Text(text = value, style = CaroTheme.typography.display, color = CaroTheme.color.text.primary)
        Text(text = label, style = CaroTheme.typography.body2.medium, color = CaroTheme.color.text.secondary)
    }
}

@Composable
internal fun ReportSectionTitle(
    title: String,
    modifier: Modifier = Modifier,
    caption: String? = null,
) {
    Column(modifier = modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(CaroTheme.spacing.xs)) {
        Text(text = title, style = CaroTheme.typography.heading2, color = CaroTheme.color.text.primary)
        caption?.let { Text(text = it, style = CaroTheme.typography.caption1.regular, color = CaroTheme.color.text.tertiary) }
    }
}

/**
 * 평가 횟수 차트. 막대가 [CHART_VISIBLE_BARS]개를 넘으면 차트만 가로로 넘기고,
 * 처음에는 기록이 있는 가장 최근 막대가 오른쪽 끝에 오도록 둔다. 화면 밖에 막대가 더 있는 쪽은 흐리게 덮는다.
 * 월 막대와 주 막대만 누를 수 있다. [selected]가 있으면 그 막대만 진하게 보여준다.
 */
@Composable
internal fun ReportChart(
    buckets: List<ReportBucket>,
    unit: ReportBucketUnit,
    selected: ReportBucket?,
    onClickBar: (ReportBucket) -> Unit,
    modifier: Modifier = Modifier,
) {
    val maxCount = (buckets.maxOfOrNull { it.reviewCount } ?: 0L).coerceAtLeast(1L)
    val labels = buckets.mapIndexed { index, bucket -> bucket.label(unit = unit, previous = buckets.getOrNull(index - 1)) }
    Column(modifier = modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(CaroTheme.spacing.m)) {
        ReportSectionTitle(title = stringResource(unit.chartTitle()))
        BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
            val slot = maxWidth / buckets.size.coerceIn(1, CHART_VISIBLE_BARS)
            val slotPx = with(LocalDensity.current) { slot.toPx() }
            val lastActive = buckets.indexOfLast { it.reviewCount > 0 }
            val scrollState =
                remember(buckets, slotPx) {
                    ScrollState(initial = ((lastActive + 1) * slotPx - constraints.maxWidth).roundToInt().coerceAtLeast(0))
                }
            Row(modifier = Modifier.horizontalScroll(scrollState)) {
                buckets.forEachIndexed { index, bucket ->
                    ChartBar(
                        bucket = bucket,
                        label = labels[index],
                        fraction = bucket.reviewCount.toFloat() / maxCount,
                        isDimmed = selected != null && bucket != selected,
                        isSelected = bucket == selected,
                        onClick = if (unit == ReportBucketUnit.DAY) null else ({ onClickBar(bucket) }),
                        modifier = Modifier.width(slot),
                    )
                }
            }
            val fadeColor = CaroTheme.color.background.primary
            Row(modifier = Modifier.matchParentSize()) {
                if (scrollState.canScrollBackward) {
                    Box(
                        modifier =
                            Modifier
                                .width(ChartFadeWidth)
                                .fillMaxHeight()
                                .background(Brush.horizontalGradient(listOf(fadeColor, Color.Transparent))),
                    )
                }
                Spacer(modifier = Modifier.weight(1f))
                if (scrollState.canScrollForward) {
                    Box(
                        modifier =
                            Modifier
                                .width(ChartFadeWidth)
                                .fillMaxHeight()
                                .background(Brush.horizontalGradient(listOf(Color.Transparent, fadeColor))),
                    )
                }
            }
        }
    }
}

@Composable
private fun ChartBar(
    bucket: ReportBucket,
    label: String,
    fraction: Float,
    isDimmed: Boolean,
    isSelected: Boolean,
    onClick: (() -> Unit)?,
    modifier: Modifier = Modifier,
) {
    val description = stringResource(Res.string.report_chart_description, label, bucket.reviewCount.formatCount())
    Column(
        modifier =
            modifier
                .then(if (onClick != null) Modifier.noRippleClickable(role = Role.Button, onClick = onClick) else Modifier)
                .semantics(mergeDescendants = true) { contentDescription = description },
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(CaroTheme.spacing.s),
    ) {
        BoxWithConstraints(modifier = Modifier.fillMaxWidth().height(ChartHeight), contentAlignment = Alignment.BottomCenter) {
            val barColor = CaroTheme.color.surface.brand
            Box(
                modifier =
                    Modifier
                        .width(minOf(ChartBarMaxWidth, maxWidth * 0.6f))
                        .height(maxOf(ChartBarMinHeight, ChartHeight * fraction))
                        .background(
                            color = if (isDimmed) barColor.copy(alpha = DIMMED_ALPHA) else barColor,
                            shape = RoundedCornerShape(topStart = 4.dp, topEnd = 4.dp),
                        ),
            )
        }
        Text(
            text = label,
            style = if (isSelected) CaroTheme.typography.caption1.medium else CaroTheme.typography.caption1.regular,
            color =
                when {
                    isSelected -> CaroTheme.color.text.primary
                    isDimmed -> CaroTheme.color.text.tertiary
                    else -> CaroTheme.color.text.secondary
                },
            textAlign = TextAlign.Center,
            maxLines = 1,
            modifier = Modifier.clearAndSetSemantics {},
        )
    }
}

private fun ReportBucketUnit.chartTitle(): StringResource =
    when (this) {
        ReportBucketUnit.DAY -> Res.string.report_chart_day_title
        ReportBucketUnit.WEEK -> Res.string.report_chart_week_title
        ReportBucketUnit.MONTH -> Res.string.report_chart_month_title
    }

/**
 * 일은 첫 막대와 달이 바뀌는 막대에 월을 붙이고(10.1), 월은 해가 바뀌는 막대에 연도를 붙인다(26.1).
 */
@Composable
private fun ReportBucket.label(
    unit: ReportBucketUnit,
    previous: ReportBucket?,
): String =
    when (unit) {
        ReportBucketUnit.DAY -> {
            if (previous == null || previous.startDate.month != startDate.month) startDate.toMonthDayText() else startDate.day.toString()
        }

        ReportBucketUnit.WEEK -> {
            startDate.toShortDateText()
        }

        ReportBucketUnit.MONTH -> {
            if (previous != null && previous.startDate.year != startDate.year) {
                "${(startDate.year % 100).toString().padStart(2, '0')}.${startDate.month.number()}"
            } else {
                stringResource(Res.string.report_chart_month_label, startDate.month.number())
            }
        }
    }

private fun Month.number(): Int = ordinal + 1

/**
 * 10.1 형식
 */
private fun LocalDate.toMonthDayText(): String = "${month.number()}.$day"

/**
 * 월 막대를 누르면 차트 아래에 여는 주차 목록
 */
@Composable
internal fun ReportMonthDetailPanel(
    detail: ReportMonthDetail,
    onClickMonthAll: () -> Unit,
    onClickWeek: (ReportWeek) -> Unit,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val month =
        detail.bucket.startDate.month
            .number()
    Column(
        modifier =
            modifier
                .fillMaxWidth()
                .background(color = CaroTheme.color.surface.primary, shape = CaroTheme.shape.l)
                .border(width = 1.dp, color = CaroTheme.color.border.secondary, shape = CaroTheme.shape.l)
                .padding(horizontal = CaroTheme.spacing.l, vertical = CaroTheme.spacing.m),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = stringResource(Res.string.report_month_title, month, detail.bucket.reviewCount.formatCount()),
                modifier = Modifier.weight(1f),
                style = CaroTheme.typography.heading3,
                color = CaroTheme.color.text.primary,
            )
            Row(
                modifier =
                    Modifier
                        .heightIn(min = ReportMinTouchSize)
                        .noRippleClickable(role = Role.Button, onClick = onClickMonthAll),
                horizontalArrangement = Arrangement.spacedBy(CaroTheme.spacing.xs),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = stringResource(Res.string.report_month_all, month),
                    style = CaroTheme.typography.body2.medium,
                    color = CaroTheme.color.text.secondary,
                )
                Icon(
                    painter = painterResource(Res.drawable.ic_arrow_right_16),
                    contentDescription = null,
                    tint = CaroTheme.color.icon.secondary,
                )
            }
        }
        val weeks = detail.weeks
        when {
            weeks != null -> {
                val maxCount = (weeks.maxOfOrNull { it.reviewCount } ?: 0L).coerceAtLeast(1L)
                weeks.forEach { week ->
                    WeekRow(week = week, fraction = week.reviewCount.toFloat() / maxCount, onClick = { onClickWeek(week) })
                }
            }

            detail.hasError -> {
                ReportLoadMoreError(message = stringResource(Res.string.report_load_more_error), onRetry = onRetry)
            }

            else -> {
                Spacer(
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .padding(vertical = CaroTheme.spacing.s)
                            .height(160.dp)
                            .background(color = CaroTheme.color.surface.tertiary, shape = CaroTheme.shape.m),
                )
            }
        }
    }
}

@Composable
private fun WeekRow(
    week: ReportWeek,
    fraction: Float,
    onClick: () -> Unit,
) {
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .heightIn(min = 52.dp)
                .noRippleClickable(role = Role.Button, onClick = onClick),
        horizontalArrangement = Arrangement.spacedBy(CaroTheme.spacing.m),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.width(96.dp)) {
            Text(
                text = stringResource(Res.string.report_week_label, week.number),
                style = CaroTheme.typography.body2.semiBold,
                color = CaroTheme.color.text.primary,
            )
            Text(
                text =
                    stringResource(
                        Res.string.report_date_range,
                        week.period.startDate.toMonthDayText(),
                        week.period.endDate.toMonthDayText(),
                    ),
                style = CaroTheme.typography.caption1.regular,
                color = CaroTheme.color.text.tertiary,
            )
        }
        Box(
            modifier =
                Modifier
                    .weight(1f)
                    .height(8.dp)
                    .background(color = CaroTheme.color.surface.tertiary, shape = CaroTheme.shape.xs),
        ) {
            if (week.reviewCount > 0) {
                Box(
                    modifier =
                        Modifier
                            .fillMaxWidth(fraction = fraction)
                            .height(8.dp)
                            .background(color = CaroTheme.color.surface.brand, shape = CaroTheme.shape.xs),
                )
            }
        }
        Text(
            text = stringResource(Res.string.report_rating_value, week.reviewCount.formatCount()),
            style = CaroTheme.typography.body2.semiBold,
            color = CaroTheme.color.text.primary,
        )
        Icon(
            painter = painterResource(Res.drawable.ic_arrow_right_16),
            contentDescription = null,
            tint = CaroTheme.color.icon.secondary,
        )
    }
}

/**
 * 평가 분포. 한 줄 막대와 함께 평가 이름과 횟수를 글자로 보여준다.
 */
@Composable
internal fun ReportRatingDistribution(
    counts: ReportCounts,
    modifier: Modifier = Modifier,
) {
    val ratings =
        listOf(
            StudyRating.EASY to counts.ratings.easy,
            StudyRating.FAIR to counts.ratings.fair,
            StudyRating.AGAIN to counts.ratings.again,
        )
    Column(modifier = modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(CaroTheme.spacing.m)) {
        ReportSectionTitle(title = stringResource(Res.string.report_rating_title))
        if (counts.ratings.total > 0) {
            Row(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .height(12.dp)
                        .clip(CaroTheme.shape.xs)
                        .clearAndSetSemantics {},
                horizontalArrangement = Arrangement.spacedBy(2.dp),
            ) {
                ratings.filter { (_, count) -> count > 0 }.forEach { (rating, count) ->
                    Box(modifier = Modifier.weight(count.toFloat()).fillMaxHeight().background(rating.color()))
                }
            }
        }
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(CaroTheme.spacing.l),
            verticalArrangement = Arrangement.spacedBy(CaroTheme.spacing.xs),
        ) {
            ratings.forEach { (rating, count) ->
                Row(horizontalArrangement = Arrangement.spacedBy(CaroTheme.spacing.s), verticalAlignment = Alignment.CenterVertically) {
                    ReportRatingLabel(rating = rating)
                    Text(
                        text = stringResource(Res.string.report_rating_value, count.formatCount()),
                        style = CaroTheme.typography.body2.semiBold,
                        color = CaroTheme.color.text.primary,
                    )
                }
            }
        }
    }
}

/**
 * 리포트를 불러오는 동안 수치 영역 자리에 둔다. 0회로 먼저 보여주지 않는다.
 */
@Composable
internal fun ReportSkeleton(modifier: Modifier = Modifier) {
    Column(modifier = modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(CaroTheme.spacing.xl3)) {
        listOf(88.dp, 180.dp, 60.dp).forEach { height ->
            Spacer(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .height(height)
                        .background(color = CaroTheme.color.surface.tertiary, shape = CaroTheme.shape.m),
            )
        }
    }
}
