package com.whatever.caro.feature.importer.platform

import androidx.compose.runtime.Composable

/** 시스템의 동작 줄이기 설정이 켜져 있는지 돌려준다. 앱이 켜진 채 설정이 바뀌어도 따라간다. */
@Composable
internal expect fun rememberReduceMotionEnabled(): Boolean
