package com.whatever.caro.core.designsystem.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.triStateToggleable
import androidx.compose.material3.Icon
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.state.ToggleableState
import androidx.compose.ui.unit.dp
import caromobile.core.designsystem.generated.resources.Res
import caromobile.core.designsystem.generated.resources.checkbox_state_partially_selected
import caromobile.core.designsystem.generated.resources.ic_check_16
import caromobile.core.designsystem.generated.resources.ic_minus_16
import com.whatever.caro.core.designsystem.themes.CaroTheme
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource

private val CheckboxSize = 22.dp
private val BorderThickness = 1.dp

/**
 * [onClick]이 null이면 장식용이라 상태 semantics를 주지 않는다.
 * 부모 줄이 triStateToggleable이나 toggleable로 상태를 노출해야 하고, 일부 선택 설명만 병합되어 읽힌다.
 * [onClick]이 있으면 보이는 모양은 22dp로 두고 누르는 영역만 최소 48dp로 넓힌다.
 */
@Composable
fun CaroCheckbox(
    state: ToggleableState,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    enabled: Boolean = true,
) {
    val isChecked = state != ToggleableState.Off
    val backgroundColor = if (isChecked) CaroTheme.color.surface.brand else Color.Transparent
    val borderColor = if (isChecked) CaroTheme.color.border.brand else CaroTheme.color.border.primary
    val glyph =
        when (state) {
            ToggleableState.On -> Res.drawable.ic_check_16
            ToggleableState.Indeterminate -> Res.drawable.ic_minus_16
            ToggleableState.Off -> null
        }
    val partiallySelectedDescription = stringResource(Res.string.checkbox_state_partially_selected)

    Box(
        modifier =
            modifier
                .then(
                    if (onClick != null) {
                        Modifier
                            .triStateToggleable(
                                state = state,
                                enabled = enabled,
                                interactionSource = null,
                                indication = null,
                                role = Role.Checkbox,
                                onClick = onClick,
                            ).minimumInteractiveComponentSize()
                    } else {
                        Modifier
                    },
                ).semantics {
                    if (state == ToggleableState.Indeterminate) {
                        stateDescription = partiallySelectedDescription
                    }
                },
        contentAlignment = Alignment.Center,
    ) {
        Box(
            modifier =
                Modifier
                    .size(CheckboxSize)
                    .clip(CaroTheme.shape.xs)
                    .background(backgroundColor)
                    .border(
                        width = BorderThickness,
                        color = borderColor,
                        shape = CaroTheme.shape.xs,
                    ),
            contentAlignment = Alignment.Center,
        ) {
            glyph?.let {
                Icon(
                    modifier = Modifier.size(CaroTheme.spacing.l),
                    painter = painterResource(it),
                    contentDescription = null,
                    tint = CaroTheme.color.icon.inverse,
                )
            }
        }
    }
}
