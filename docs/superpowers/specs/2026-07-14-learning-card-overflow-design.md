# Learning Card Overflow Design

## Goal

Match Figma nodes `55985:27937` and `55985:28069` in the existing learning screen. When the currently visible primary card text exceeds 11 rendered lines, truncate it, show a `더보기` action, and let the learner inspect the complete text in a modal card.

## Scope

- Apply the behavior independently to the front and back primary text.
- Preserve the existing card flip, swipe evaluation, and progress behavior.
- Reuse the existing Caro theme tokens and learning resources.
- Do not move layout-dependent overflow state into `LearningState` or `LearningViewModel`.

## Card Behavior

The primary text uses `maxLines = 11` and `TextOverflow.Ellipsis`. Its `onTextLayout` callback records `TextLayoutResult.hasVisualOverflow`. The `더보기` button is rendered only when the current text actually overflows at the device's current width, font scale, and typography.

The front and back maintain independent overflow measurements. Switching sides displays the measurement and action for the newly visible primary text. A new card starts with no open modal.

Clicking the card outside the action keeps the existing flip behavior. Clicking `더보기` opens the modal without also flipping the card.

## Full-text Modal

Use Compose `Dialog` so `onDismissRequest` handles both a tap on the dimmed area and the platform back action. The dialog content follows the Figma design:

- dim overlay supplied by the dialog window;
- white surface with secondary border, 16 dp corner radius, and the existing dialog elevation treatment;
- width constrained by 16 dp screen margins, with the Figma target width of 362 dp;
- height constrained to the available viewport, with the Figma target height of 642 dp;
- 8 dp horizontal and 40 dp vertical surface padding;
- full text in the body1 semibold style with 24 dp line height and primary text color.

The full text area scrolls vertically when the content is taller than the available modal content height. The dialog has no separate close button because the approved dismissal paths are the dimmed area and system back action.

## State Ownership

Overflow measurements and modal visibility are ephemeral layout/UI state, so they remain local to `LearningCard` using remembered Compose state. The open modal stores the currently requested text value, which also makes dismissal a simple reset to `null`.

State is keyed or reset by the displayed text values so stale overflow and modal state do not leak when the current study card changes.

## Accessibility and Resources

- Add localized `learning_more` resources for `더보기` and `More`.
- Render the action as an actual clickable element with the localized label.
- Keep modal text selectable only if the existing learning UX already supports selection; otherwise preserve normal read-only text behavior.

## Testing

Follow TDD for extracted behavior:

1. Add a small pure decision helper test proving that the action is hidden without visual overflow and shown with visual overflow.
2. Add coverage for front/back selection so the requested modal text matches the currently visible primary text.
3. Run the learning module tests, Spotless checks, and an Android compilation/build task that covers the changed Compose source.

Visual details are verified against the two supplied Figma nodes after compilation. Existing staged and unstaged user changes remain intact.
