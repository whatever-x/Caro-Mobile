package com.whatever.caro.core.designsystem.components

import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview

@Composable
fun CaroButton(
    text: String,
    onClick: () -> Unit,
) {
    Button(
        onClick = onClick,
    ) {
        Text(
            text = text,
        )
    }
}

@Preview
@Preview(locale = "en")
@Preview(locale = "ko")
@Composable
private fun CaroButtonPreview() {
    CaroButton(
        text = "",
        onClick = {},
    )
}
