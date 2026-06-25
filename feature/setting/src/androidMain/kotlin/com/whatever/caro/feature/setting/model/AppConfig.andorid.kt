package com.whatever.caro.feature.setting.model

import com.whatever.caro.feature.setting.generated.BuildKonfig

actual object AppConfig {
    actual val appVersion: String
        get() = BuildKonfig.VERSION_NAME
}
