package com.whatever.caro.core.ui.snackbar

import androidx.compose.material3.SnackbarDuration
import com.whatever.caro.core.designsystem.components.CaroSnackbarStyle

/**
 * 이미 해석된 토스트 문자열을 운반하는 계약 타입. 문자열 지역화는 호출부(Route)에서 처리한다.
 * 액션이 있으면 [actionLabel]을 노출하고, 사용자가 액션을 선택했을 때만 [onAction]을 실행한다.
 */
data class SnackBarMessage(
    val message: String,
    val style: CaroSnackbarStyle = CaroSnackbarStyle.Normal,
    val duration: SnackbarDuration = SnackbarDuration.Short,
    val actionLabel: String? = null,
    val onAction: (suspend () -> Unit)? = null,
)
