package com.whatever.caro.feature.importer.mvi

import com.whatever.caro.core.importparser.model.ParsedTable
import com.whatever.caro.core.model.importing.FieldMapping
import com.whatever.caro.core.viewmodel.contract.UiState
import com.whatever.caro.feature.importer.logic.needsDelimiterCheck
import com.whatever.caro.feature.importer.transform.ImportCard
import com.whatever.caro.feature.importer.transform.ImportPreview
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.ImmutableMap
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.persistentMapOf

/**
 * [table]은 큰 파싱 결과라 rememberSaveable에 넣지 않는다.
 * [isPasteOverflowed]는 길이 초과 오류 상태다. 상한을 넘겨 붙여 넣으면 [pasteText]의 뒤가 이미 잘려 있어, 일부를 지우거나 바꿔 상한 이하로 돌아와도 풀리지 않는다.
 * [emptySideText]는 첫 읽기 입력이 넘긴 빈 면 문구다. 설정 화면의 첫 카드 미리보기와 다시 만든 미리보기도 처음 미리보기와 같은 문구를 써야 한다.
 * [mappings]는 미리보기 계산이 받는 모양대로 noteTypeId를 키로 둔다.
 * [filterCounts]가 0인 칩은 화면이 숨긴다.
 *
 * `draft`로 시작하는 값은 열린 설정 화면의 초안이고, [적용]이 성공해야 [table], [selection], [mappings]로 옮겨진다.
 * 다시 읽어야 하는 선택(시트, 구분 기준, 첫 줄)은 읽는 동안 [pendingDraftSelection]에 두고 읽기가 성공해야 [draftSelection]으로 옮긴다.
 * 요약 행 수로 막히는 시트는 읽지 않으므로 그 시트를 고른 동안에는 [draftTable]이 직전에 읽은 시트다.
 * [draftFirstCards]는 첫 카드 미리보기가 있는 구분 기준 확인과 카드 구성 화면에서만 채운다.
 * [settingsReason]은 설정을 바꾸면 다시 계산하므로, [적용] 뒤 판정에 걸린 사유도 설정을 바꾸기 전까지 남는다.
 *
 * [deckName]은 미리보기를 만들 때마다 설정에 맞춰 다시 채우지만, [isDeckInfoEdited]면 사용자가 정한 이름이라 덮어쓰지 않는다.
 * 미리 채운 글자와 같은 이름을 [적용]해도 사용자가 정한 이름이므로 글자 비교로 가리면 안 된다.
 * [deckInfoDraft]가 null이 아니면 미리보기 위에 덱 정보 시트가 열려 있다.
 */
data class ImportDeckState(
    val step: ImportStep = ImportStep.Source,
    val loading: ImportLoading? = null,
    val isPickerOpen: Boolean = false,
    val isPaste: Boolean = false,
    val pasteText: String = "",
    val isPasteOverflowed: Boolean = false,
    val emptySideText: String = "",
    val table: ParsedTable? = null,
    val selection: ImportSelection = ImportSelection(),
    val mappings: ImmutableMap<String, FieldMapping> = persistentMapOf(),
    val preview: ImportPreview? = null,
    val previewFilter: ImportPreviewFilter = ImportPreviewFilter.All,
    val filterCounts: ImmutableMap<ImportPreviewFilter, Int> = persistentMapOf(),
    val filteredCards: ImmutableList<ImportCard> = persistentListOf(),
    val detailCard: ImportCard? = null,
    val isDelimiterBannerDismissed: Boolean = false,
    val deckName: String = "",
    val deckDescription: String = "",
    val isDeckInfoEdited: Boolean = false,
    val deckInfoDraft: DeckInfoDraft? = null,
    val saving: SavingProgress? = null,
    val dialog: ImportDialog? = null,
    val draftTable: ParsedTable? = null,
    val draftSelection: ImportSelection = ImportSelection(),
    val pendingDraftSelection: ImportSelection? = null,
    val draftMappings: ImmutableMap<String, FieldMapping> = persistentMapOf(),
    val draftFirstCards: ImmutableMap<String, ImportCard> = persistentMapOf(),
    val settingsReason: SettingsReason? = null,
) : UiState {
    val isBusy: Boolean
        get() = loading != null || isPickerOpen || dialog != null || detailCard != null

    val isPasteNextEnabled: Boolean
        get() = pasteText.isNotEmpty() && !isPasteOverflowed

    /** 구분 기준이나 첫 줄을 바꿔 다시 나누는 중이다. 덮개 대신 결과 표 자리에 보이고, 그동안 구분 기준과 첫 줄은 이어서 바꿀 수 있다. */
    val isResplitting: Boolean
        get() = pendingDraftSelection != null && loading == null

    /** 다시 읽는 동안의 [settingsReason]은 아직 읽지 않은 선택이 아니라 지난 초안의 판정이라 숨긴다. 읽기를 멈추거나 실패하면 초안이 그대로 남아 같은 사유가 다시 보인다. */
    val visibleSettingsReason: SettingsReason?
        get() = settingsReason.takeIf { pendingDraftSelection == null }

    /** 하위 덱을 하나도 고르지 않으면 사유 없이 끈다. */
    val isSettingsApplyEnabled: Boolean
        get() {
            val settings = step as? ImportStep.Settings ?: return false
            val hasSubdeck = settings.kind != SettingsKind.Subdeck || draftSelection.subdeckIds.isNotEmpty()
            return loading == null && pendingDraftSelection == null && settingsReason == null && hasSubdeck
        }

    /** 설정 확인 띠에 넣을 칸으로 나뉘지 않은 줄 수다. 띠를 보이지 않으면 null이다. */
    val delimiterBannerRowCount: Int?
        get() =
            table
                ?.takeIf { source -> step == ImportStep.Preview && !isDelimiterBannerDismissed && needsDelimiterCheck(source) }
                ?.layout
                ?.singleFieldRowCount
}

/** 선언 순서가 칩이 놓이는 순서다. */
enum class ImportPreviewFilter {
    All,
    EmptyReplaced,
    Truncated,
    MediaRemoved,
}
