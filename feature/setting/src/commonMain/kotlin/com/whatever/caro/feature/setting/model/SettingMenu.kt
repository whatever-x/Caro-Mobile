package com.whatever.caro.feature.setting.model

import com.whatever.caro.feature.setting.mvi.SettingIntent
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.StringResource

sealed interface SettingMenu {
    data class Menu(
        val content: StringResource,
        val leadingIcon: DrawableResource,
        val action: SettingIntent,
        val highlight: Boolean = false,
    ) : SettingMenu

    data object Divider : SettingMenu
}
