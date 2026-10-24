package com.whatever.caro.core.designsystem

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import java.io.File

class CardLimitStringsTest :
    FunSpec({
        test("ko 덱 만들기 팁은 카드 생성 요청 1회 200장 기준으로 안내한다") {
            koString("deck_tip_max_cards") shouldBe "카드는 한 번에 200장까지 추가할 수 있어요."
        }

        test("en 덱 만들기 팁은 카드 생성 요청 1회 200장 기준으로 안내한다") {
            enString("deck_tip_max_cards") shouldBe "You can add up to 200 cards at a time."
        }

        test("ko 카드 추가 팁은 카드 생성 요청 1회 200장 기준으로 안내한다") {
            koString("card_tip_max_cards") shouldBe "카드는 한 번에 200장까지 추가할 수 있어요."
        }

        test("en 카드 추가 팁은 카드 생성 요청 1회 200장 기준으로 안내한다") {
            enString("card_tip_max_cards") shouldBe "You can add up to 200 cards at a time."
        }

        test("ko 상한 도달 안내는 저장한 뒤 이어서 추가하도록 알려준다") {
            koString("card_max_cards_reached") shouldBe
                "한 번에 200장까지 추가할 수 있어요. 저장한 뒤 이어서 추가해 주세요."
        }

        test("en 상한 도달 안내는 저장한 뒤 이어서 추가하도록 알려준다") {
            enString("card_max_cards_reached") shouldBe
                "You can add up to 200 cards at a time. Save them, then keep adding."
        }
    })

private const val RESOURCES_DIR = "core/designsystem/src/commonMain/composeResources"

private fun enString(name: String): String = stringValue("$RESOURCES_DIR/values/strings.xml", name)

private fun koString(name: String): String = stringValue("$RESOURCES_DIR/values-ko/strings.xml", name)

private fun stringValue(
    relativePath: String,
    name: String,
): String {
    val source = findProjectFile(relativePath).readText()
    val match = Regex("""<string name="${Regex.escape(name)}">(.*?)</string>""").find(source)
    return requireNotNull(match) { "$relativePath 에 $name 문구가 없다" }.groupValues[1]
}

private fun findProjectFile(relativePath: String): File =
    generateSequence(File("").absoluteFile) { it.parentFile }
        .map { File(it, relativePath) }
        .first { it.exists() }
