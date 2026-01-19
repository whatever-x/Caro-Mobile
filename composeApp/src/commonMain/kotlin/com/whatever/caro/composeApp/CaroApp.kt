package com.whatever.caro.composeApp

import androidx.compose.runtime.Composable
import caromobile.core.designsystem.generated.resources.Res
import caromobile.core.designsystem.generated.resources.test
import com.whatever.caro.designsystem.CaroButton
import org.jetbrains.compose.resources.stringResource

@Composable
fun CaroApp(

) {
    CaroButton(
        text = stringResource(resource = Res.string.test),
        onClick = {}
    )
}