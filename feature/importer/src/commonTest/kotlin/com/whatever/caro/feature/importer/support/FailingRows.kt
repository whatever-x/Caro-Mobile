package com.whatever.caro.feature.importer.support

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Runnable
import kotlin.coroutines.CoroutineContext

/** [failWhen]이 참일 때 원소를 꺼내면 [ForcedError]를 던진다. 표를 받은 뒤의 계산을 실패시키는 데 쓴다. */
internal class FailingList<T>(
    private val items: List<T>,
    private val failWhen: () -> Boolean,
) : AbstractList<T>() {
    override val size: Int get() = items.size

    override fun get(index: Int): T = if (failWhen()) throw ForcedError() else items[index]
}

/**
 * [arm]한 뒤 이 디스패처에서 도는 작업 안에서만 [isRunning]이 참이다.
 * Main의 계산과 default의 미리보기 계산이 같은 표를 읽으므로, default 쪽 계산만 골라 실패시키려면 실행 위치를 가려야 한다.
 */
internal class MarkingDispatcher(
    private val delegate: CoroutineDispatcher,
) : CoroutineDispatcher() {
    private var armed = false

    var isRunning: Boolean = false
        private set

    fun arm() {
        armed = true
    }

    override fun dispatch(
        context: CoroutineContext,
        block: Runnable,
    ) {
        delegate.dispatch(
            context,
            Runnable {
                isRunning = armed
                try {
                    block.run()
                } finally {
                    isRunning = false
                }
            },
        )
    }
}
