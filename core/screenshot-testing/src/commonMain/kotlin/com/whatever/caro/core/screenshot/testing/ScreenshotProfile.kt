package com.whatever.caro.core.screenshot.testing

enum class ScreenshotVariant {
    DEFAULT,
    NARROW,
    SMALL_LARGE_FONT,
}

data class ScreenshotProfile(
    val platform: String,
    val variant: ScreenshotVariant,
    val id: String,
    val widthPx: Int,
    val heightPx: Int,
    val density: Float,
    val fontScale: Float,
)

object ScreenshotConfiguration {
    val languages = listOf("ko", "en")
    val profiles =
        listOf(
            ScreenshotProfile("android", ScreenshotVariant.DEFAULT, "pixel10-pro", 1280, 2856, 3f, 1f),
            ScreenshotProfile("android", ScreenshotVariant.NARROW, "pixel10-pro-narrow", 960, 2100, 3f, 1f),
            ScreenshotProfile("android", ScreenshotVariant.SMALL_LARGE_FONT, "pixel10-pro-small-large-font", 960, 1440, 3f, 1.5f),
            ScreenshotProfile("ios", ScreenshotVariant.DEFAULT, "iphone18-pro", 1206, 2622, 3f, 1f),
            ScreenshotProfile("ios", ScreenshotVariant.NARROW, "iphone18-pro-narrow", 960, 2100, 3f, 1f),
            ScreenshotProfile("ios", ScreenshotVariant.SMALL_LARGE_FONT, "iphone18-pro-small-large-font", 960, 1440, 3f, 1.5f),
        )
}
