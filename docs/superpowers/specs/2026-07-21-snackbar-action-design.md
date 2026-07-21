# Snackbar Action Design

## Goal

Match the Figma snackbar shown after deck creation. The snackbar displays a success message and a `바로가기` action. Selecting the action opens the detail screen for the deck that was just created.

## Scope

- Add an optional action label and callback to the app-wide snackbar contract.
- Render the action in `CaroSnackbar` using the Figma typography, color, spacing, and padding.
- Return the created deck from `DeckRepository.createDeck` so navigation targets the exact deck.
- Emit a deck-created side effect from `CreateDeckViewModel`.
- On success, return to the previous screen, show the snackbar, and navigate to the created deck detail only when the action is selected.
- When the current destination is Home, place the app-wide snackbar above Home's floating deck-create button.
- Add localized success and action strings.
- Update focused repository and ViewModel tests.

Out of scope:

- Changing the general deck-detail navigation contract to load by ID.
- Adding actions to existing error or account snackbars.
- Changing snackbar duration or animation.
- Moving the app-wide snackbar host into `HomeScreen` or moving Home's floating button into the root `Scaffold`.
- Adding a caller-controlled position to every `SnackBarMessage`.

## Data Flow

1. `CreateDeckViewModel` calls `DeckRepository.createDeck(name, description)`.
2. `DeckRepositoryImpl` maps `CreateDeckResponse` to a new `Deck` with zero card/progress counts and `DeckState.NOT_STARTED`.
3. The ViewModel emits `CreateDeckSideEffect.Created(deck)`.
4. `CreateDeckRoute` emits `NavCommand.Back`, then sends a `SnackBarMessage` with the localized success message, localized action label, and an action callback.
5. The app-level snackbar host displays the message. `CaroSnackbar` calls `SnackbarData.performAction()` when the action text is selected.
6. `SnackbarHostState.showSnackbar` returns `SnackbarResult.ActionPerformed`; only then does the app invoke the callback, which emits `NavCommand.To(DeckDetailEntry(deck))`.
7. Because the back command makes Home the current destination before the snackbar is shown, `CaroApp` applies Home's snackbar bottom offset while the snackbar is visible.

Timeouts, replacement by a newer snackbar, and ordinary dismissal do not invoke the action callback.

## Component Design

`SnackBarMessage` receives a nullable `actionLabel` and action callback. `CaroSnackbarVisuals` receives the label through Material's standard property, while the app-level display helper owns the callback until `showSnackbar` returns. No action space is reserved when the label is absent, preserving current snackbars.

`CaroSnackbar` uses a horizontal layout with:

- 8 dp between message and action.
- 16 dp horizontal and 12 dp vertical container padding.
- Existing style-dependent message and surface colors.
- `body2.medium` for the message.
- `label2.regular` and the brand text color for the action.
- Button semantics on the clickable action text.

The standard `SnackbarVisuals.actionLabel`/`SnackbarData.performAction()` contract is used instead of a custom composable slot because the action presentation is fixed by the design system.

## Placement Design

The snackbar host remains in the root `CaroApp` so messages emitted during a navigation transition are not lost. `CaroApp` derives whether the current back-stack entry is `HomeEntry` and applies a bottom padding to the snackbar host only for Home.

The Figma placement is reproduced from the visible vertical stack:

- Home floating button height: 48 dp.
- Gap between snackbar and floating button: 16 dp.
- Floating button bottom padding: 24 dp.
- Snackbar host bottom padding on Home: 88 dp total.

Material 3 `Scaffold` already places its snackbar above the bottom system-bar inset. The 88 dp padding is therefore added inside the safe content area and must not include a second navigation-bar inset. Non-Home destinations use zero additional padding and preserve the existing global snackbar position.

This destination-aware host offset is preferred over moving the host into `HomeScreen`: the deck-created snackbar is emitted from `CreateDeckRoute` immediately after `NavCommand.Back`, so the root host provides stable ownership across that transition. It is also preferred over a position field on `SnackBarMessage`, because the obstruction belongs to the destination layout rather than to the message itself.

## Error Handling

- A create response without an ID is invalid and must fail mapping rather than navigating to an unusable deck.
- Missing deck name or description falls back to the submitted request values.
- Existing create failure behavior remains: loading ends and the error snackbar is shown without an action.
- A null action label produces the existing message-only snackbar.
- If Home is no longer the current destination, the host immediately returns to its default bottom placement.

## Testing

- Repository test: a valid create response maps to the expected new `Deck`; a missing ID fails.
- ViewModel test: successful confirmation emits `Created(expectedDeck)` and still calls the repository with the submitted inputs.
- ViewModel failure test remains unchanged in behavior.
- Placement test: Home resolves to 88 dp additional bottom padding and non-Home destinations resolve to zero.
- Run focused module tests, `spotlessCheck`, and the Android build needed to compile the Compose integration.
