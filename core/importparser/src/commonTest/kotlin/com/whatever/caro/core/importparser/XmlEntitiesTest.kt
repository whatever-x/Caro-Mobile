package com.whatever.caro.core.importparser

import com.whatever.caro.core.importparser.xlsx.XmlEntities
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.comparables.shouldBeLessThan
import io.kotest.matchers.shouldBe
import kotlin.time.Duration.Companion.seconds
import kotlin.time.measureTime

class XmlEntitiesTest : FunSpec() {
    init {
        context("해석") {
            test("미리 정의된 엔티티와 숫자 문자 참조를 해석한다") {
                XmlEntities.decode("&lt;a&gt;&amp;&quot;&apos;&#65;&#x42;&#X43;") shouldBe "<a>&\"'ABC"
            }

            test("종결자가 없거나 정의되지 않았거나 숫자가 아닌 참조는 글자 그대로 둔다") {
                XmlEntities.decode("a & b &unknown; &#zz; &lt") shouldBe "a & b &unknown; &#zz; &lt"
            }

            test("엔티티 이름이 11글자까지는 해석하고 12글자부터는 글자 그대로 둔다") {
                XmlEntities.decode("&#" + "0".repeat(8) + "65;") shouldBe "A"
                XmlEntities.decode("&#" + "0".repeat(9) + "65;") shouldBe "&#" + "0".repeat(9) + "65;"
            }
        }

        context("선형 시간") {
            test("종결자 없는 &가 이어져도 &마다 정해진 길이까지만 찾아 입력 길이에 비례해 끝난다") {
                val raw = "&".repeat(500_000)

                val elapsed = measureTime { XmlEntities.decode(raw) shouldBe raw }

                elapsed shouldBeLessThan 1.seconds
            }

            test("먼 곳에 종결자가 하나 있어도 앞의 &는 해석하지 않고 글자 그대로 둔다") {
                val raw = "&".repeat(1_000) + ";"

                XmlEntities.decode(raw) shouldBe raw
            }
        }
    }
}
