package com.whatever.caro.core.designsystem.components

import android.graphics.Bitmap
import android.graphics.Canvas
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PixelMap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.toPixelMap
import androidx.compose.ui.platform.ViewRootForTest
import androidx.compose.ui.test.SemanticsNodeInteraction

// Robolectric에서 captureToImage는 draw 대기 중 timeout이 나므로 view를 직접 그려 픽셀을 읽는다.
internal fun SemanticsNodeInteraction.renderPixels(): RenderedNode {
    val node = fetchSemanticsNode()
    val view = (node.root as ViewRootForTest).view
    val bitmap = Bitmap.createBitmap(view.width, view.height, Bitmap.Config.ARGB_8888)
    view.draw(Canvas(bitmap))
    return RenderedNode(pixels = bitmap.asImageBitmap().toPixelMap(), bounds = node.boundsInRoot)
}

internal class RenderedNode(
    private val pixels: PixelMap,
    private val bounds: Rect,
) {
    // 왼쪽 10% 지점의 세로 중앙은 글자가 닿지 않는 면 색이다.
    fun surfaceColor(): Color = pixels[(bounds.left + bounds.width * SURFACE_SAMPLE_RATIO).toInt(), bounds.center.y.toInt()]

    fun countPixels(color: Color): Int =
        (bounds.top.toInt() until bounds.bottom.toInt()).sumOf { y ->
            (bounds.left.toInt() until bounds.right.toInt()).count { x -> pixels[x, y] == color }
        }

    // 노드 왼쪽 끝에서 leftWidthPx 안의 열에 color가 있는 행(루트 좌표)을 위에서 아래로 돌려준다.
    fun rowsWithColor(
        color: Color,
        leftWidthPx: Int,
    ): List<Int> =
        (bounds.top.toInt() until bounds.bottom.toInt()).filter { y ->
            (bounds.left.toInt() until bounds.left.toInt() + leftWidthPx).any { x -> pixels[x, y] == color }
        }

    fun sameImageAs(other: RenderedNode): Boolean {
        val width = bounds.width.toInt()
        val height = bounds.height.toInt()
        if (width != other.bounds.width.toInt() || height != other.bounds.height.toInt()) return false
        return (0 until height).all { dy ->
            (0 until width).all { dx ->
                pixels[bounds.left.toInt() + dx, bounds.top.toInt() + dy] ==
                    other.pixels[other.bounds.left.toInt() + dx, other.bounds.top.toInt() + dy]
            }
        }
    }

    private companion object {
        const val SURFACE_SAMPLE_RATIO = 0.1f
    }
}
