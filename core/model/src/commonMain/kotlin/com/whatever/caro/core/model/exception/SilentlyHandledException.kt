package com.whatever.caro.core.model.exception

/**
 * ViewModel에서 처리하는 예외 로직으로 빠지면 안되는 Exception입니다.
 * App 모듈에서의 CaroExceptionFilter에서 필터링을 하게 됩니다.
 * EventBus나 별도의 처리가 필요한 경우 사용합니다.
 */
interface SilentlyHandledException
