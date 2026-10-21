package com.whatever.caro.feature.learning.report.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import caromobile.core.designsystem.generated.resources.Res
import caromobile.core.designsystem.generated.resources.ic_arrow_left_24
import caromobile.core.designsystem.generated.resources.ic_arrow_right_16
import caromobile.core.designsystem.generated.resources.learning_again
import caromobile.core.designsystem.generated.resources.learning_back
import caromobile.core.designsystem.generated.resources.learning_easy
import caromobile.core.designsystem.generated.resources.learning_fair
import caromobile.core.designsystem.generated.resources.report_retry
import caromobile.core.designsystem.generated.resources.report_time_am
import caromobile.core.designsystem.generated.resources.report_time_pm
import caromobile.core.designsystem.generated.resources.report_weekday_fri
import caromobile.core.designsystem.generated.resources.report_weekday_mon
import caromobile.core.designsystem.generated.resources.report_weekday_sat
import caromobile.core.designsystem.generated.resources.report_weekday_sun
import caromobile.core.designsystem.generated.resources.report_weekday_thu
import caromobile.core.designsystem.generated.resources.report_weekday_tue
import caromobile.core.designsystem.generated.resources.report_weekday_wed
import com.whatever.caro.core.designsystem.modifier.noRippleClickable
import com.whatever.caro.core.designsystem.themes.CaroTheme
import com.whatever.caro.core.model.learning.StudyRating
import kotlinx.datetime.DayOfWeek
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalTime
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource

internal val ReportMinTouchSize = 48.dp
private val ReportTopBarHeight = 56.dp
private const val HOURS_PER_HALF_DAY = 12

@Composable
internal fun ReportTopBar(
    title: String,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier =
            modifier
                .fillMaxWidth()
                .height(ReportTopBarHeight)
                .background(color = CaroTheme.color.background.brand)
                .padding(horizontal = CaroTheme.spacing.s),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier =
                Modifier
                    .size(ReportMinTouchSize)
                    .noRippleClickable(onClick = onBack)
                    .semantics { role = Role.Button },
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                painter = painterResource(Res.drawable.ic_arrow_left_24),
                contentDescription = stringResource(Res.string.learning_back),
                tint = CaroTheme.color.icon.inverse,
            )
        }
        Text(
            text = title,
            modifier = Modifier.weight(1f),
            color = CaroTheme.color.text.inverse,
            style = CaroTheme.typography.heading2,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

/**
 * 다른 화면으로 이동하는 행(학습 기록 보기, 전체 기간 보기)
 */
@Composable
internal fun ReportLinkRow(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier =
            modifier
                .fillMaxWidth()
                .heightIn(min = ReportMinTouchSize)
                .background(color = CaroTheme.color.surface.primary, shape = CaroTheme.shape.m)
                .border(width = 1.dp, color = CaroTheme.color.border.secondary, shape = CaroTheme.shape.m)
                .noRippleClickable(onClick = onClick)
                .semantics { role = Role.Button }
                .padding(horizontal = CaroTheme.spacing.l, vertical = CaroTheme.spacing.m),
        horizontalArrangement = Arrangement.spacedBy(CaroTheme.spacing.s),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = text,
            modifier = Modifier.weight(1f),
            style = CaroTheme.typography.body1,
            color = CaroTheme.color.text.primary,
        )
        Icon(
            painter = painterResource(Res.drawable.ic_arrow_right_16),
            contentDescription = null,
            tint = CaroTheme.color.icon.secondary,
        )
    }
}

@Composable
internal fun ReportMessage(
    title: String,
    description: String,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.fillMaxWidth().padding(top = CaroTheme.spacing.xl2, bottom = CaroTheme.spacing.s),
        verticalArrangement = Arrangement.spacedBy(CaroTheme.spacing.s),
    ) {
        Text(text = title, style = CaroTheme.typography.heading2, color = CaroTheme.color.text.primary)
        Text(text = description, style = CaroTheme.typography.body2.medium, color = CaroTheme.color.text.secondary)
    }
}

@Composable
internal fun ReportPrimaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    Box(
        modifier =
            modifier
                .fillMaxWidth()
                .heightIn(min = ReportMinTouchSize)
                .background(
                    color = if (enabled) CaroTheme.color.surface.brand else CaroTheme.color.surface.disabled,
                    shape = CaroTheme.shape.m,
                ).noRippleClickable(enabled = enabled, onClick = onClick)
                .semantics { role = Role.Button }
                .padding(horizontal = CaroTheme.spacing.l, vertical = CaroTheme.spacing.m),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = text,
            style = CaroTheme.typography.body1,
            color = CaroTheme.color.text.inverse,
        )
    }
}

/**
 * 목록을 더 불러오지 못했을 때 하단에 보여주는 재시도 행
 */
@Composable
internal fun ReportLoadMoreError(
    message: String,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.fillMaxWidth().padding(vertical = CaroTheme.spacing.s),
        horizontalArrangement = Arrangement.spacedBy(CaroTheme.spacing.s),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = message,
            modifier = Modifier.weight(1f),
            style = CaroTheme.typography.body2.medium,
            color = CaroTheme.color.text.secondary,
        )
        Box(
            modifier =
                Modifier
                    .heightIn(min = ReportMinTouchSize)
                    .noRippleClickable(onClick = onRetry)
                    .semantics { role = Role.Button }
                    .padding(horizontal = CaroTheme.spacing.s),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = stringResource(Res.string.report_retry),
                style = CaroTheme.typography.body2.semiBold,
                color = CaroTheme.color.text.secondary,
            )
        }
    }
}

/**
 * 평가 이름과 색 점. 색만으로 평가를 구분하지 않도록 이름을 함께 둔다.
 */
@Composable
internal fun ReportRatingLabel(
    rating: StudyRating,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(modifier = Modifier.size(8.dp).background(color = rating.color(), shape = CaroTheme.shape.xxl))
        Text(
            text = stringResource(rating.label()),
            style = CaroTheme.typography.body2.medium,
            color = CaroTheme.color.text.primary,
        )
    }
}

@Composable
internal fun StudyRating.color(): Color =
    when (this) {
        StudyRating.EASY -> CaroTheme.color.surface.brand
        StudyRating.FAIR -> CaroTheme.color.surface.inverse
        StudyRating.AGAIN -> CaroTheme.color.surface.accent
    }

internal fun StudyRating.label(): StringResource =
    when (this) {
        StudyRating.EASY -> Res.string.learning_easy
        StudyRating.FAIR -> Res.string.learning_fair
        StudyRating.AGAIN -> Res.string.learning_again
    }

internal fun DayOfWeek.label(): StringResource =
    when (this) {
        DayOfWeek.MONDAY -> Res.string.report_weekday_mon
        DayOfWeek.TUESDAY -> Res.string.report_weekday_tue
        DayOfWeek.WEDNESDAY -> Res.string.report_weekday_wed
        DayOfWeek.THURSDAY -> Res.string.report_weekday_thu
        DayOfWeek.FRIDAY -> Res.string.report_weekday_fri
        DayOfWeek.SATURDAY -> Res.string.report_weekday_sat
        DayOfWeek.SUNDAY -> Res.string.report_weekday_sun
    }

@Composable
internal fun LocalTime.toReportTimeText(): String {
    val minuteText = minute.toString().padStart(2, '0')
    val hour12 = (hour % HOURS_PER_HALF_DAY).let { if (it == 0) HOURS_PER_HALF_DAY else it }
    val resource = if (hour < HOURS_PER_HALF_DAY) Res.string.report_time_am else Res.string.report_time_pm
    return stringResource(resource, hour12, minuteText)
}

/**
 * 2026.10.15 형식
 */
internal fun LocalDate.toReportDateText(): String =
    "$year.${month.ordinal.plus(1).toString().padStart(2, '0')}.${day.toString().padStart(2, '0')}"

/**
 * 10.15 형식
 */
internal fun LocalDate.toShortDateText(): String = "${month.ordinal + 1}.${day.toString().padStart(2, '0')}"

/**
 * 1,280 형식. 횟수는 음수가 아니다.
 */
internal fun Long.formatCount(): String =
    toString()
        .reversed()
        .chunked(3)
        .joinToString(",")
        .reversed()
