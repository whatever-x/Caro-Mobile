# Learning Completion Stats Design

## Goal

Match Figma node `55985:28901` in the learning completion screen and keep every statistic label readable without clipping the total label.

## Root Cause

The Figma card is `354 x 100 dp`. It centers four `70.5 x 69 dp` statistic items with `8 dp` gaps and `24 dp` horizontal outer padding. Each label is a single line; the Korean label widths are approximately `37 dp`, `49 dp`, and `61 dp`.

The current Compose layout applies `20 dp` vertical padding to the outer row and another `12 dp` padding inside each item. Only about `36 dp` of vertical content space remains. When `isTotal` selects the 24 sp display style, the count, gap, and label require more height than that. The item's `clip` modifier then removes the label. The same inner padding constrains the widest Korean labels and can make them wrap before clipping.

## Layout

- Keep the statistics card at `100 dp` high.
- Keep `24 dp` horizontal outer padding and `8 dp` gaps at the Figma target width.
- Give every statistic item a fixed `69 dp` height and equal width, centered vertically in the card.
- Remove the item-level content padding so the text can use the item's full width and height.
- Preserve the total count's display typography and the other counts' body2 semibold typography.
- Preserve the existing Figma colors and corner radii.

## Overflow Behavior

Labels remain single-line with `maxLines = 1` and `softWrap = false`. At the Figma target width, all localized Korean labels, including `모르겠어요`, must render in full. If a smaller viewport or larger font scale makes a label wider than its item, use `TextOverflow.Ellipsis` instead of wrapping or clipping vertically. Compose semantics retain the complete resource text for accessibility.

Font auto-sizing is intentionally excluded because it would weaken the typography token and scaled-text accessibility behavior. A two-line label is also excluded because it conflicts with the fixed 69 dp Figma item height.

## State and API

No ViewModel or MVI state changes are needed. `isTotal` continues to control only the total item's visual variant; it does not control label visibility.

## Testing

- Add or extend a source-level design contract test for the 100 dp card, 69 dp item height, and single-line ellipsis policy.
- Compile the learning module after the change.
- Run the learning module tests and Spotless checks.
- Compare the completion preview or rendered screen against Figma node `55985:28901`, including the total label and all three Korean evaluation labels.

