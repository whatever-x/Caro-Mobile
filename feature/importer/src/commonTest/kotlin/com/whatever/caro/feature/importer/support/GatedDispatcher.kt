package com.whatever.caro.feature.importer.support

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Runnable
import kotlin.coroutines.CoroutineContext

/**
 * [hold] 중에 넘어온 작업은 쌓아 두고 [release]에서 [delegate]로 보낸다.
 * 한 테스트 디스패처로는 advanceUntilIdle 한 번에 계산이 끝나, 계산 도중의 상태와 취소 시점을 볼 수 없다.
 */
internal class GatedDispatcher(
    private val delegate: CoroutineDispatcher,
) : CoroutineDispatcher() {
    private val held = ArrayDeque<Pair<CoroutineContext, Runnable>>()
    private var holding = false

    fun hold() {
        holding = true
    }

    /** 이미 잡아 둔 작업은 [release]까지 계속 잡아 둔다. 같은 디스패처를 쓰는 작업 중 앞서 잡은 작업만 늦출 때 쓴다. */
    fun passNewKeepingHeld() {
        holding = false
    }

    fun release() {
        holding = false
        while (held.isNotEmpty()) {
            val (context, block) = held.removeFirst()
            delegate.dispatch(context, block)
        }
    }

    override fun dispatch(
        context: CoroutineContext,
        block: Runnable,
    ) {
        if (holding) held.addLast(context to block) else delegate.dispatch(context, block)
    }
}
