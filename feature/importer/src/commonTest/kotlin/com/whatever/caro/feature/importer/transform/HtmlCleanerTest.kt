package com.whatever.caro.feature.importer.transform

import com.whatever.caro.core.importparser.model.HtmlMode
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import kotlin.time.Duration.Companion.minutes

class HtmlCleanerTest : FunSpec() {
    init {
        listOf(
            Triple("<div>위</div>\n<div class=\"x\">아래</div>", "위\n아래", 0),
            Triple("a<div><br></div><div>b</div>", "a\n\nb", 0),
            Triple("줄1<br />줄2", "줄1\n줄2", 0),
            Triple("x<y and y>z", "x<y and y>z", 0),
            Triple("<foo>모르는 태그</foo> <b>굵게</b>", "<foo>모르는 태그</foo> 굵게", 0),
            Triple("<style>.x{}</style>앞<script>a = 1 < 2;</script>", "앞", 0),
            Triple("<video controls><source src=\"a.mp4\"><source src=\"a.webm\"></video>뒤", "뒤", 1),
            Triple("<img src=\"b.png\"><embed src=\"c.swf\"><iframe src=\"d.html\">대체 글</iframe>", "대체 글", 3),
            Triple("<ruby>漢字<rp>(</rp><rt>かんじ</rt><rp>)</rp></ruby>", "漢字(かんじ)", 0),
            Triple("<ruby><rb>漢</rb><rt>かん</rt><rb>字</rb><rt>じ</rt></ruby>", "漢(かん)字(じ)", 0),
            Triple("<ruby>漢字<rt></rt></ruby> 漢字[かんじ]", "漢字 漢字[かんじ]", 0),
        ).forEach { (raw, expected, media) ->
            test("HTML 원본 정리: $raw") {
                HtmlCleaner.clean(raw, HtmlMode.HTML) shouldBe CleanedField(expected, media)
            }
        }

        test("PLAIN 원본은 태그와 엔티티를 그대로 두고 [sound:]만 지워 센다") {
            HtmlCleaner.clean("<b>x</b>&nbsp;[sound:a.mp3]", HtmlMode.PLAIN) shouldBe CleanedField("<b>x</b>&nbsp;", 1)
        }

        test("태그를 지운 뒤 이름·10진·16진 엔티티를 풀고 nbsp는 공백으로 바꾼다") {
            val cleaned = HtmlCleaner.clean("&lt;b&gt; &amp; &#54620;&#xAE00; &eacute;&nbsp;끝", HtmlMode.HTML)

            cleaned.text shouldBe "<b> & 한글 é 끝"
        }

        test("닫히지 않은 주석·style·script·[sound: 여는 표기가 많아도 한 번 훑고 글자로 남긴다").config(timeout = 2.minutes) {
            val count = 20_000
            val raw = "<!--a".repeat(count) + "<style>b".repeat(count) + "<script>c".repeat(count) + "[sound:d".repeat(count)

            HtmlCleaner.clean(raw, HtmlMode.HTML) shouldBe CleanedField(raw, 0)
            HtmlCleaner.clean(raw, HtmlMode.PLAIN) shouldBe CleanedField(raw, 0)
        }

        test("주석·style·script는 가장 앞에서 시작하는 것부터 지우고 대소문자를 가리지 않는다") {
            HtmlCleaner.clean("<style> <!-- </style> -->", HtmlMode.HTML).text shouldBe "-->"
            HtmlCleaner.clean("<!-- <style> -->x</style>", HtmlMode.HTML).text shouldBe "x</style>"
            HtmlCleaner.clean("앞<STYLE type=\"a\">.x{}</Style >뒤<styles>남음", HtmlMode.HTML).text shouldBe "앞뒤<styles>남음"
            HtmlCleaner.clean("[sound:a.mp3][sound:b", HtmlMode.PLAIN) shouldBe CleanedField("[sound:b", 1)
        }

        test("원문 줄바꿈은 공백 하나가 되고 3개 이상 이어진 줄바꿈은 2개로 줄인다") {
            HtmlCleaner.clean("앞\n뒤<br><br><br><br>끝  ", HtmlMode.HTML).text shouldBe "앞 뒤\n\n끝"
        }
    }
}
