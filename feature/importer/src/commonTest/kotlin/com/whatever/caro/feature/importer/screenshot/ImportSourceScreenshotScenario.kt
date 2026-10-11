package com.whatever.caro.feature.importer.screenshot

import androidx.compose.runtime.Composable
import com.whatever.caro.feature.importer.mvi.ImportDeckState
import com.whatever.caro.feature.importer.mvi.ImportStep

internal object ImportSourceScreenshotScenario {
    const val INITIAL_ID = "importer.source.initial"
    const val PASTE_FILLED_ID = "importer.paste.filled"

    // 탭은 입력칸에서 폭 없이 그려져 낱말이 붙어 보이므로 쉼표로 나눈 줄을 쓴다.
    private const val PASTE_TEXT =
        "学校,학교,がっこう,学校に行きます。\n" +
            "先生,선생님,せんせい,先生に聞きます。\n" +
            "読む,읽다,よむ,本を読みます。\n" +
            "毎日,매일,まいにち,毎日勉強します。\n" +
            "日本,일본,にほん,日本に住んでいます。\n" +
            "友だち,친구,ともだち,友だちと遊びます。\n" +
            "電話,전화,でんわ,電話をかけます。\n" +
            "図書館,도서관,としょかん,図書館で本を読みます。\n" +
            "駅,역,えき,駅で会いましょう。\n" +
            "時間,시간,じかん,時間がありません。"

    @Composable
    fun RenderInitial() {
        ImportScreenshotScenario.Render(ImportDeckState())
    }

    @Composable
    fun RenderPasteFilled() {
        ImportScreenshotScenario.Render(ImportDeckState(step = ImportStep.Paste, isPaste = true, pasteText = PASTE_TEXT))
    }
}
