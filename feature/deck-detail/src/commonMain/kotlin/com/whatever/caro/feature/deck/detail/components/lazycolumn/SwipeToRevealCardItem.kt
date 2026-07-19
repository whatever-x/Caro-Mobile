package com.whatever.caro.feature.deck.detail.components.lazycolumn

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import caromobile.core.designsystem.generated.resources.Res
import caromobile.core.designsystem.generated.resources.deck_detail_button_card_edit
import caromobile.core.designsystem.generated.resources.ic_edit_16
import com.whatever.caro.core.designsystem.themes.CaroTheme
import com.whatever.caro.feature.deck.detail.model.CardItem
import com.whatever.caro.feature.deck.detail.model.CardReviewState
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import kotlin.math.roundToInt

private val RevealActionWidth = 72.dp
private val EditActionIconSize = 16.dp
private val EditActionSpacing = 4.dp

// 이만큼만 왼쪽으로 끌거나(FLING_VELOCITY_THRESHOLD 이상으로) 살짝 튕겨도 끝까지 열린다.
private val OpenTriggerDistance = 12.dp
private const val FLING_VELOCITY_THRESHOLD = 500f

// 열 때는 살짝 튕기는 맛(overshoot), 닫을 때는 오버슈트로 뒤 배경이 새지 않게 바운스 없이 감속.
private val OpenAnimationSpec =
    spring<Float>(
        dampingRatio = Spring.DampingRatioMediumBouncy,
        stiffness = Spring.StiffnessMediumLow,
    )
private val CloseAnimationSpec =
    spring<Float>(
        dampingRatio = Spring.DampingRatioNoBouncy,
        stiffness = Spring.StiffnessMediumLow,
    )

/**
 * 카드 아이템을 오른쪽 -> 왼쪽으로 스와이프하면 뒤에서 "수정" 버튼이 드러난다.
 *
 * "수정" 배경은 아이템 전체를 채우고 카드와 동일한 [CaroTheme.shape] 로 라운드 처리하므로,
 * 스와이프 시 위에 있는 흰 카드의 라운드 모서리가 파란 배경을 덮으며 밀려난다.
 * 스와이프 오프셋은 순수 UI 상호작용 상태이므로 이 컴포넌트 안에서만 관리한다.
 * - 수정 진입: 드러난 버튼 탭 -> [onEdit]
 * - 열린 상태에서 카드 본문 탭 -> 닫기 (수정으로 이동하지 않음)
 */
@Composable
internal fun SwipeToRevealCardItem(
    card: CardItem,
    onEdit: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val density = LocalDensity.current
    val revealPx = with(density) { RevealActionWidth.toPx() }
    val openTriggerPx = with(density) { OpenTriggerDistance.toPx() }
    val offsetX = remember { Animatable(0f) }
    val scope = rememberCoroutineScope()

    Box(
        modifier =
            modifier
                .fillMaxWidth()
                .clipToBounds(),
    ) {
        Box(
            modifier =
                Modifier
                    .matchParentSize()
                    .clip(CaroTheme.shape.m)
                    .background(CaroTheme.color.surface.brand),
            contentAlignment = Alignment.CenterEnd,
        ) {
            Column(
                modifier =
                    Modifier
                        .fillMaxHeight()
                        .width(RevealActionWidth)
                        .clickable(onClick = onEdit),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(EditActionSpacing, Alignment.CenterVertically),
            ) {
                Icon(
                    modifier = Modifier.size(EditActionIconSize),
                    painter = painterResource(Res.drawable.ic_edit_16),
                    contentDescription = null,
                    tint = CaroTheme.color.icon.inverse,
                )
                Text(
                    text = stringResource(Res.string.deck_detail_button_card_edit),
                    style = CaroTheme.typography.caption1.regular,
                    color = CaroTheme.color.text.inverse,
                )
            }
        }

        DeckCardItem(
            card = card,
            onClick = {
                if (offsetX.value != 0f) {
                    scope.launch { offsetX.animateTo(0f, animationSpec = CloseAnimationSpec) }
                }
            },
            modifier =
                Modifier
                    .offset { IntOffset(x = offsetX.value.roundToInt(), y = 0) }
                    .draggable(
                        orientation = Orientation.Horizontal,
                        state =
                            rememberDraggableState { delta ->
                                scope.launch {
                                    offsetX.snapTo((offsetX.value + delta).coerceIn(-revealPx, 0f))
                                }
                            },
                        onDragStopped = { velocity ->
                            val shouldOpen =
                                when {
                                    // 왼쪽으로 튕김 -> 열기
                                    velocity <= -FLING_VELOCITY_THRESHOLD -> true

                                    // 오른쪽으로 튕김 -> 닫기
                                    velocity >= FLING_VELOCITY_THRESHOLD -> false

                                    // 조금만 끌어도 열기
                                    else -> offsetX.value <= -openTriggerPx
                                }
                            offsetX.animateTo(
                                targetValue = if (shouldOpen) -revealPx else 0f,
                                animationSpec = if (shouldOpen) OpenAnimationSpec else CloseAnimationSpec,
                                // 손가락 튕김 관성을 스프링으로 전달
                                initialVelocity = velocity,
                            )
                        },
                    ),
        )
    }
}

@Preview
@Composable
private fun SwipeToRevealCardItemPreview() {
    CaroTheme {
        Column(
            modifier =
                Modifier
                    .background(color = CaroTheme.color.background.primary)
                    .padding(CaroTheme.spacing.xl),
            verticalArrangement = Arrangement.spacedBy(CaroTheme.spacing.m),
        ) {
            SwipeToRevealCardItem(
                card =
                    CardItem(
                        front = "apple",
                        back = "사과",
                        reviewCount = 3,
                        reviewState = CardReviewState.NEW,
                    ),
                onEdit = { },
            )
        }
    }
}
