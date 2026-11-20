package com.whatever.caro.feature.importer.logic

import com.whatever.caro.core.model.deck.DeckInputLimits
import com.whatever.caro.feature.importer.mvi.DeckInfoDraft
import com.whatever.caro.feature.importer.mvi.ImportDeckState
import com.whatever.caro.feature.importer.mvi.ImportStep

internal fun ImportDeckState.openingDeckInfo(): ImportDeckState =
    if (step != ImportStep.Preview || deckInfoDraft != null) this else copy(deckInfoDraft = DeckInfoDraft(deckName, deckDescription))

internal fun ImportDeckState.withDeckInfoName(name: String): ImportDeckState {
    val draft = deckInfoDraft ?: return this
    return copy(deckInfoDraft = draft.copy(name = name.takeWithoutSplittingSurrogate(DeckInputLimits.NAME_MAX)))
}

internal fun ImportDeckState.withDeckInfoDescription(description: String): ImportDeckState {
    val draft = deckInfoDraft ?: return this
    return copy(deckInfoDraft = draft.copy(description = description.takeWithoutSplittingSurrogate(DeckInputLimits.DESC_MAX)))
}

internal fun ImportDeckState.applyingDeckInfo(): ImportDeckState {
    val draft = deckInfoDraft?.takeIf { draft -> draft.isApplyEnabled } ?: return this
    return copy(deckName = draft.name, deckDescription = draft.description, isDeckInfoEdited = true, deckInfoDraft = null)
}
