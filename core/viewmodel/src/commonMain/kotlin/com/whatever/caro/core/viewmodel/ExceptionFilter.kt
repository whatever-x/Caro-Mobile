package com.whatever.caro.core.viewmodel

fun interface ExceptionFilter {
    fun shouldSuppress(throwable: Throwable): Boolean
}
