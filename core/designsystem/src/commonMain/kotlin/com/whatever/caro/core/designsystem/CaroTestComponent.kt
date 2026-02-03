package com.whatever.caro.core.designsystem

import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import caromobile.core.designsystem.generated.resources.Res
import caromobile.core.designsystem.generated.resources.test
import org.jetbrains.compose.resources.stringResource

@Composable
fun CaroButton(
    text: String,
    onClick: () -> Unit,
) {
    Button(
        onClick = onClick
    ) {
        Text(
            text = text
        )
    }
}

@Preview
@Preview(locale = "en")
@Preview(locale = "ko")
@Composable
private fun CaroButtonPreview() {
    CaroButton(
        text = stringResource(resource = Res.string.test),
        onClick = {}
    )
}