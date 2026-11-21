package com.whatever.caro.feature.importer.logic

/**
 * 상한을 넘겨 붙인 글은 입력칸에 보관할 때 이미 뒤가 잘려 있다.
 * 일부를 지우거나 바꿔 상한 이하로 돌아와도 잘린 뒷부분은 돌아오지 않으므로, 비우거나 통째로 다른 글을 넣기 전까지 넘친 것으로 둔다.
 * [edited]의 절반 이상이 [previous]와 앞뒤로 같으면 보관된 글을 고친 것으로 본다.
 */
internal fun nextPasteOverflowed(
    previous: String,
    wasOverflowed: Boolean,
    edited: String,
    maxPasteChars: Int,
): Boolean =
    when {
        edited.length > maxPasteChars -> true
        !wasOverflowed || edited.isEmpty() -> false
        else -> sharedEndsLength(previous, edited) * 2 >= edited.length
    }

/** 같은 글자를 앞뒤에서 두 번 세지 않게 접미는 접두가 끝난 곳까지만 센다. 서로게이트 쌍은 반쪽만 같으면 다른 글자라 세지 않는다. */
private fun sharedEndsLength(
    original: String,
    edited: String,
): Int {
    val shorter = minOf(original.length, edited.length)
    var prefix = 0
    while (prefix < shorter && edited[prefix] == original[prefix]) prefix++
    if (prefix > 0 && edited[prefix - 1].isHighSurrogate()) prefix--
    var suffix = 0
    while (suffix < shorter - prefix && edited[edited.length - 1 - suffix] == original[original.length - 1 - suffix]) suffix++
    if (suffix > 0 && edited[edited.length - suffix].isLowSurrogate()) suffix--
    return prefix + suffix
}
