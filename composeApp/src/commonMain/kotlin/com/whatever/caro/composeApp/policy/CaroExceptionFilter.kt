package com.whatever.caro.composeApp.policy

import com.whatever.caro.core.model.exception.SilentlyHandledException
import com.whatever.caro.core.viewmodel.ExceptionFilter

internal class CaroExceptionFilter : ExceptionFilter {
    override fun shouldSuppress(throwable: Throwable): Boolean = throwable is SilentlyHandledException
}
