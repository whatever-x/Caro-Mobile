package com.whatever.caro.feature.importer.components

import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.platform.LocalDensity

// 큰 글자 시안이 글자 1.3배로 그려져 있어 같은 배율부터 큰 글자 배치를 쓴다.
private const val LARGE_FONT_SCALE = 1.3f

@Composable
@ReadOnlyComposable
internal fun isLargeText(): Boolean = LocalDensity.current.fontScale >= LARGE_FONT_SCALE
