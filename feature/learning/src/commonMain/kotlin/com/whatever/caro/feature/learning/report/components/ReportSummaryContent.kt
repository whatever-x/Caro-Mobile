package com.whatever.caro.feature.learning.report.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
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
import caromobile.core.designsystem.generated.resources.ic_arrow_up_16
import caromobile.core.designsystem.generated.resources.report_all_period_range
import caromobile.core.designsystem.generated.resources.report_chart_caption
import caromobile.core.designsystem.generated.resources.report_chart_description
import caromobile.core.designsystem.generated.resources.report_chart_month_label
import caromobile.core.designsystem.generated.resources.report_chart_rest
import caromobile.core.designsystem.generated.resources.report_chart_title
import caromobile.core.designsystem.generated.resources.report_date_range
import caromobile.core.designsystem.generated.resources.report_rating_caption
import caromobile.core.designsystem.generated.resources.report_rating_title
import caromobile.core.designsystem.generated.resources.report_rating_value
import caromobile.core.designsystem.generated.resources.report_scope_all
import caromobile.core.designsystem.generated.resources.report_scope_button_description
import caromobile.core.designsystem.generated.resources.report_scope_deleted_deck
import caromobile.core.designsystem.generated.resources.report_stat_card_label
import caromobile.core.designsystem.generated.resources.report_stat_card_value
import caromobile.core.designsystem.generated.resources.report_stat_day_label
import caromobile.core.designsystem.generated.resources.report_stat_day_value
import caromobile.core.designsystem.generated.resources.report_stat_review_label
import caromobile.core.designsystem.generated.resources.report_stat_review_value
import com.whatever.caro.core.designsystem.modifier.noRippleClickable
import com.whatever.caro.core.designsystem.themes.CaroTheme
import com.whatever.caro.core.model.learning.StudyRating
import com.whatever.caro.core.model.report.ReportBucket
import com.whatever.caro.core.model.report.ReportBucketUnit
import com.whatever.caro.core.model.report.ReportCounts
import com.whatever.caro.core.model.report.ReportPeriod
import kotlinx.datetime.LocalDate
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource

private val ChartHeight = 120.dp
private val ChartBarMaxWidth = 20.dp
private val NarrowStatsWidth = 300.dp
private const val CHART_MAX_LABELS = 7

/**
 * 리포트 상단의 덱 이름과 꺾쇠. 이름과 꺾쇠를 하나의 버튼으로 묶고 그 아래에 적용한 날짜를 보여준다.
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
    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier =
                Modifier
                    .heightIn(min = ReportMinTouchSize)
                    .noRippleClickable(onClick = onClick)
                    .clearAndSetSemantics {
                        role = Role.DropdownList
                        contentDescription = description
                        stateDescription = if (isSheetOpen) "expanded" else "collapsed"
                    }.padding(vertical = CaroTheme.spacing.m),
            horizontalArrangement = Arrangement.spacedBy(CaroTheme.spacing.s),
            verticalAlignment = Alignment.Bottom,
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
                modifier = Modifier.padding(bottom = CaroTheme.spacing.xs),
                tint = CaroTheme.color.icon.secondary,
            )
        }
        Text(text = rangeText, style = CaroTheme.typography.body2.medium, color = CaroTheme.color.text.secondary)
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
 * 평가 횟수, 학습한 카드, 학습일. 좁은 화면에서는 세로로 쌓는다.
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
    BoxWithConstraints(modifier = modifier.fillMaxWidth()) {
        if (maxWidth < NarrowStatsWidth) {
            Column(verticalArrangement = Arrangement.spacedBy(CaroTheme.spacing.l)) {
                items.forEach { (value, label) -> ReportStat(value = value, label = label) }
            }
        } else {
            Row(horizontalArrangement = Arrangement.spacedBy(CaroTheme.spacing.m)) {
                items.forEach { (value, label) -> ReportStat(value = value, label = label, modifier = Modifier.weight(1f)) }
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
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(CaroTheme.spacing.xs)) {
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
 * 학습량 차트. 평가가 없는 구간은 막대 없이 둔다. 휴식일은 날짜 아래에 표시한다.
 */
@Composable
internal fun ReportChart(
    buckets: List<ReportBucket>,
    unit: ReportBucketUnit,
    restDates: List<LocalDate>,
    modifier: Modifier = Modifier,
) {
    val maxCount = (buckets.maxOfOrNull { it.reviewCount } ?: 0L).coerceAtLeast(1L)
    val barColor = CaroTheme.color.surface.brand
    val labelStep = ((buckets.size + CHART_MAX_LABELS - 1) / CHART_MAX_LABELS).coerceAtLeast(1)
    val chartDescription =
        buckets
            .map { stringResource(Res.string.report_chart_description, it.label(unit), it.reviewCount.formatCount()) }
            .joinToString()
    Column(modifier = modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(CaroTheme.spacing.s)) {
        ReportSectionTitle(title = stringResource(Res.string.report_chart_title))
        Canvas(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .height(ChartHeight)
                    .semantics { contentDescription = chartDescription },
        ) {
            if (buckets.isEmpty()) return@Canvas
            val slot = size.width / buckets.size
            val barWidth = minOf(ChartBarMaxWidth.toPx(), slot * 0.6f)
            buckets.forEachIndexed { index, bucket ->
                if (bucket.reviewCount == 0L) return@forEachIndexed
                val barHeight = size.height * bucket.reviewCount / maxCount
                drawRoundRect(
                    color = barColor,
                    topLeft = Offset(x = slot * index + (slot - barWidth) / 2, y = size.height - barHeight),
                    size = Size(width = barWidth, height = barHeight),
                    cornerRadius = CornerRadius(x = 4.dp.toPx(), y = 4.dp.toPx()),
                )
            }
        }
        Row(modifier = Modifier.fillMaxWidth().clearAndSetSemantics {}) {
            buckets.forEachIndexed { index, bucket ->
                Column(modifier = Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                    val showLabel = index % labelStep == 0 || index == buckets.lastIndex
                    Text(
                        text = if (showLabel) bucket.label(unit) else "",
                        style = CaroTheme.typography.caption1.regular,
                        color = CaroTheme.color.text.secondary,
                        textAlign = TextAlign.Center,
                        maxLines = 1,
                    )
                    if (unit == ReportBucketUnit.DAY && bucket.startDate in restDates) {
                        Text(
                            text = stringResource(Res.string.report_chart_rest),
                            style = CaroTheme.typography.caption1.regular,
                            color = CaroTheme.color.text.tertiary,
                        )
                    }
                }
            }
        }
        Text(
            text = stringResource(Res.string.report_chart_caption),
            modifier = Modifier.fillMaxWidth(),
            style = CaroTheme.typography.caption1.regular,
            color = CaroTheme.color.text.tertiary,
            textAlign = TextAlign.End,
        )
    }
}

@Composable
private fun ReportBucket.label(unit: ReportBucketUnit): String =
    when (unit) {
        ReportBucketUnit.DAY -> startDate.day.toString()
        ReportBucketUnit.WEEK -> startDate.toShortDateText()
        ReportBucketUnit.MONTH -> stringResource(Res.string.report_chart_month_label, startDate.month.ordinal + 1)
    }

/**
 * 평가 분포. 막대와 함께 평가 이름과 횟수를 글자로 보여준다.
 */
@Composable
internal fun ReportRatingDistribution(
    counts: ReportCounts,
    modifier: Modifier = Modifier,
) {
    val total = counts.ratings.total
    Column(modifier = modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(CaroTheme.spacing.m)) {
        ReportSectionTitle(
            title = stringResource(Res.string.report_rating_title),
            caption = stringResource(Res.string.report_rating_caption),
        )
        listOf(
            StudyRating.EASY to counts.ratings.easy,
            StudyRating.FAIR to counts.ratings.fair,
            StudyRating.AGAIN to counts.ratings.again,
        ).forEach { (rating, count) ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(CaroTheme.spacing.m),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                ReportRatingLabel(rating = rating, modifier = Modifier.width(92.dp))
                Box(
                    modifier =
                        Modifier
                            .weight(1f)
                            .height(8.dp)
                            .background(color = CaroTheme.color.surface.tertiary, shape = CaroTheme.shape.xs),
                ) {
                    if (count > 0 && total > 0) {
                        Box(
                            modifier =
                                Modifier
                                    .fillMaxWidth(fraction = count.toFloat() / total)
                                    .height(8.dp)
                                    .background(color = rating.color(), shape = CaroTheme.shape.xs),
                        )
                    }
                }
                Text(
                    text = stringResource(Res.string.report_rating_value, count.formatCount()),
                    style = CaroTheme.typography.body2.semiBold,
                    color = CaroTheme.color.text.primary,
                )
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
        listOf(120.dp, 88.dp, 180.dp).forEach { height ->
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

@Composable
internal fun ReportLifetimeCard(
    title: String,
    counts: ReportCounts,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier =
            modifier
                .fillMaxWidth()
                .background(color = CaroTheme.color.surface.tertiary, shape = CaroTheme.shape.l)
                .padding(horizontal = CaroTheme.spacing.xl, vertical = CaroTheme.spacing.l),
        verticalArrangement = Arrangement.spacedBy(CaroTheme.spacing.m),
    ) {
        Text(text = title, style = CaroTheme.typography.body2.semiBold, color = CaroTheme.color.text.secondary)
        ReportStats(counts = counts)
    }
}
