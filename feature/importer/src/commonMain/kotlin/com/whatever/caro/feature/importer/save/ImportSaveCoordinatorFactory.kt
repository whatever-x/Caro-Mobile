package com.whatever.caro.feature.importer.save

/** 조정 객체는 키별 첫 전송 시각을 기억하므로 저장 흐름마다 새로 만들어야 한다. */
internal fun interface ImportSaveCoordinatorFactory {
    fun create(): ImportSaveCoordinator
}
