package com.whatever.caro.feature.card.delete

import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.performClick
import com.whatever.caro.core.designsystem.themes.CaroTheme
import com.whatever.caro.core.model.card.CardBadge
import com.whatever.caro.core.model.card.CardContent
import com.whatever.caro.core.model.card.DeckCard
import com.whatever.caro.feature.card.delete.components.DeleteCardListItem
import com.whatever.caro.feature.card.delete.model.DeleteCardItem
import io.kotest.matchers.shouldBe
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [35], qualifiers = "ko-rKR-w393dp-h852dp-xxhdpi")
class DeleteCardListItemSemanticsAndroidTest {
    @get:Rule
    val composeRule = createComposeRule()

    private fun showItem(
        isSelected: Boolean,
        onClick: () -> Unit = {},
    ) {
        composeRule.setContent {
            CaroTheme {
                DeleteCardListItem(item = DeleteCardItem(card = CARD, isSelected = isSelected), onClick = onClick)
            }
        }
        composeRule.waitForIdle()
    }

    @Test
    fun `선택한 카드 줄은 카드 선택 설명을 읽는다`() {
        showItem(isSelected = true)

        composeRule.onNode(hasClickAction() and hasContentDescription(SELECTED_DESCRIPTION)).assertExists()
    }

    @Test
    fun `선택하지 않은 카드 줄은 카드 선택 설명을 읽지 않는다`() {
        showItem(isSelected = false)

        composeRule.onNode(hasClickAction() and hasContentDescription(SELECTED_DESCRIPTION)).assertDoesNotExist()
    }

    @Test
    fun `카드 줄을 누르면 onClick을 한 번 호출한다`() {
        var clicks = 0
        showItem(isSelected = false, onClick = { clicks += 1 })

        composeRule.onNode(hasClickAction()).performClick()

        clicks shouldBe 1
    }

    private companion object {
        const val SELECTED_DESCRIPTION = "카드 선택"
        val CARD = DeckCard(id = 1L, content = CardContent(front = "Apple", back = "사과"), badge = CardBadge.NEW, reviewCount = 0)
    }
}
