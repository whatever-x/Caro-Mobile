package com.whatever.caro.feature.importer.platform

import okio.Source

sealed interface PickResult {
    data class Picked(
        val file: PickedImportFile,
    ) : PickResult

    data object Cancelled : PickResult

    data class TooLarge(
        val displayName: String,
        val sizeBytes: Long,
    ) : PickResult

    data class Failed(
        val cause: Throwable,
    ) : PickResult
}

/**
 * 선택기가 고른 원본의 핸들. 경로를 주지 않는 것은 사본을 만들고 크기를 세는 일을 ViewModel이 io에서 하기 때문이다.
 * 모든 메서드가 블로킹 호출이므로 io 디스패처에서 부른다.
 */
interface PickedImportFile {
    fun displayName(): String

    /** 읽기 전에 알 수 없으면 null이다. */
    fun sizeBytes(): Long?

    /** 호출할 때마다 새 [Source]를 열고 닫는 책임은 호출자에게 있다. */
    fun openSource(): Source

    /** 선택기가 남긴 임시 사본 같은 자원을 지운다. */
    fun discard()
}
