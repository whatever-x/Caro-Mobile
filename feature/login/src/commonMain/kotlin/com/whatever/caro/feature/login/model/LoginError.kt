package com.whatever.caro.feature.login.model

import androidx.compose.runtime.Composable
import caromobile.feature.login.generated.resources.Res
import caromobile.feature.login.generated.resources.login_cancelled
import caromobile.feature.login.generated.resources.login_error
import org.jetbrains.compose.resources.stringResource

enum class LoginError {
    UNKNOWN,
    USER_CANCELLED,
}
