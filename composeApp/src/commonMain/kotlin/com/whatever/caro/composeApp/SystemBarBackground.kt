package com.whatever.caro.composeApp

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsTopHeight
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.navigation3.runtime.NavKey
import com.whatever.caro.core.designsystem.themes.CaroTheme
import com.whatever.caro.core.navigator.entries.HomeEntry
import com.whatever.caro.core.navigator.entries.LoginEntry
import com.whatever.caro.core.navigator.entries.SplashEntry

internal enum class SystemBarBackgroundRole {
    Brand,
    Primary,
}

internal data class SystemBarBackgroundRoles(
    val statusBar: SystemBarBackgroundRole,
    val navigationBar: SystemBarBackgroundRole,
)

internal fun systemBarBackgroundRoles(destination: NavKey?): SystemBarBackgroundRoles =
    when (destination) {
        SplashEntry, LoginEntry -> {
            SystemBarBackgroundRoles(
                statusBar = SystemBarBackgroundRole.Brand,
                navigationBar = SystemBarBackgroundRole.Brand,
            )
        }

        HomeEntry -> {
            SystemBarBackgroundRoles(
                statusBar = SystemBarBackgroundRole.Brand,
                navigationBar = SystemBarBackgroundRole.Primary,
            )
        }

        else -> {
            SystemBarBackgroundRoles(
                statusBar = SystemBarBackgroundRole.Primary,
                navigationBar = SystemBarBackgroundRole.Primary,
            )
        }
    }

@Composable
internal fun CaroSystemBarBackground(
    roles: SystemBarBackgroundRoles,
    content: @Composable () -> Unit,
) {
    val statusBarColor = roles.statusBar.color()
    val navigationBarColor = roles.navigationBar.color()

    Box(
        modifier =
            Modifier
                .fillMaxSize()
                .background(navigationBarColor),
    ) {
        Box(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .windowInsetsTopHeight(WindowInsets.statusBars)
                    .background(statusBarColor),
        )
        content()
    }
}

@Composable
private fun SystemBarBackgroundRole.color(): Color =
    when (this) {
        SystemBarBackgroundRole.Brand -> CaroTheme.color.background.brand
        SystemBarBackgroundRole.Primary -> CaroTheme.color.background.primary
    }
