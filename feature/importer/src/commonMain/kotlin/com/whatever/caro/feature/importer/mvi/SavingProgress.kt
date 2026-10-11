package com.whatever.caro.feature.importer.mvi

/**
 * 묶음 목록, 키, 조정 객체는 ViewModel의 private 필드에 두고 화면에는 장수와 상태만 보여준다.
 */
data class SavingProgress(
    val deckName: String,
    val totalCards: Int,
    val savedCards: Int = 0,
    val status: SavingStatus = SavingStatus.Saving,
) {
    /** 첫 응답 전에는 저장한 카드가 없어 진행률을 알 수 없다. */
    val isIndeterminate: Boolean
        get() = savedCards == 0
}

enum class SavingStatus {
    Saving,

    /** 서버가 같은 묶음을 아직 처리 중이다. */
    WaitingForServer,

    /** 연결이 끊겨 자동으로 다시 보내는 중이다. */
    Retrying,

    /** 멈춤을 확인한 뒤 진행 중 묶음의 응답을 기다리는 중이다. */
    Stopping,
}
