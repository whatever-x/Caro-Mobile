package com.whatever.caro.feature.importer.mvi

sealed interface ImportStep {
    data object Source : ImportStep

    data object Paste : ImportStep

    data object Preview : ImportStep

    data class Settings(
        val kind: SettingsKind,
        val entry: SettingsEntry,
    ) : ImportStep

    /** 덱을 만든 뒤 카드를 묶음으로 저장한다. 원본과 미리보기를 놓은 뒤라 앞 단계로 돌아갈 수 없다. */
    data object Saving : ImportStep
}

enum class SettingsKind {
    Subdeck,
    Sheet,
    Delimiter,
    CardLayout,
}

enum class SettingsEntry {
    /** 미리보기의 [변경]으로 열었다. 처리 중이 아닐 때 뒤로 가면 초안을 버리고 미리보기로 간다. */
    FromPreview,

    /** 미리보기 전에 먼저 고칠 것이 있어 열었다. 적용된 설정이 없으므로 뒤로 가면 처음으로 돌아갈지 묻는다. */
    Gate,
}
