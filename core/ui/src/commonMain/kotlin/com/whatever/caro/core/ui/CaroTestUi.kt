package com.whatever.caro.core.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.whatever.caro.core.designsystem.components.CaroButton

@Composable
fun CaroTestUi(
    text: String,
    onClick: () -> Unit,
) {
    CaroButton(
        text = text,
        onClick = onClick,
    )
}

@Composable
@Preview
private fun CaroTestUiPreview() {
    CaroTestUi(
        text = "",
        onClick = {},
    )
}
