plugins {
    id("caro.kmp")
    id("caro.kmp.android")
    id("caro.kmp.ios")
    id("caro.cmp")
}

kotlin {
    android {
        namespace = "com.whatever.caro.core.designsystem"
    }
}

compose.resources {
    publicResClass = true
    generateResClass = auto
}
