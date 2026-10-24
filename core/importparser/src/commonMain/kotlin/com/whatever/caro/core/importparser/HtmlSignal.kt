package com.whatever.caro.core.importparser

/**
 * HTML 판정 신호. 같은 태그 목록을 기기의 HTML 정리가 지울 태그로도 쓰므로 두 곳이 어긋나지 않게 여기 한 번만 둔다.
 */
object HtmlSignal {
    const val MEDIA_TAG_NAMES = "img|audio|video|source|object|embed|iframe"

    /** table 계열은 table, thead, tbody, tr, td, th로 한정한다. tfoot, caption 같은 이름은 글자로 남는다. */
    const val KNOWN_TAG_NAMES =
        "b|i|u|s|em|strong|br|hr|div|p|span|ul|ol|li|sub|sup|font|a|h[1-6]|table|thead|tbody|tr|td|th|" +
            MEDIA_TAG_NAMES + "|ruby|rt|rb|rp"

    /** 속성이 있거나 스스로 닫아도 태그로 본다. 이름 뒤에 공백, `/`, `>`가 와야 해서 `x<y and y>z` 같은 본문은 신호가 아니다. */
    val KNOWN_TAG: Regex = Regex("</?(?:$KNOWN_TAG_NAMES)(?=[\\s/>])[^<>]*>", RegexOption.IGNORE_CASE)

    private const val NBSP_ENTITY = "&nbsp;"

    fun containsSignal(value: String): Boolean = value.contains(NBSP_ENTITY) || KNOWN_TAG.containsMatchIn(value)
}
