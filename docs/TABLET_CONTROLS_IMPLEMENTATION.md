# Tablet controls and saved preferences

The top bar uses a single row on wide tablets and reflows into separate rows in portrait or narrower windows. Project titles yield space to page navigation and actions instead of pushing them offscreen. Very narrow windows have a third action row; those phone/split-window sizes still need dedicated testing.

Layer names wrap to two lines and expose their full name to accessibility services. Visibility and lock buttons keep normal touch targets. Reorder, duplicate, merge, clear and delete actions are available from a per-layer menu. Opacity and blend controls expand for the active layer. The dialog adapts its width and can scroll when the keyboard reduces available space. Colour, stroke width and style selectors now have accessible names; selected tools and swatches expose selection state.

The app saves the chosen drawing tool, stylus-only mode, dimension visibility, colour, width and stroke style. Temporary calibration mode is not restored as a drawing tool. Invalid stored tool/style names fall back to defaults; invalid stroke widths are rejected.

More Options > Fit drawing frames the current underlay and visible objects together, leaving room for the tool dock. It changes only the view, not drawing coordinates or calibration. Fit uses the shared tested geometry transform; navigation remains available afterward. It does not implement arbitrary canvas rotation.

## Tablet evidence

On Samsung Tab S10 FE / Android 16:

- Layer lock toggled and persisted. The layer menu's Duplicate changed 3 layers / 1 object to 4 layers / 2 objects; Undo restored 3 / 1.
- Added QA_Long_Landscape_Notes_and_Construction_Details in portrait. The full name wrapped within its row and its lock/action controls remained reachable. Locking that layer succeeded.
- Selected Select, S Pen Only, dimensions off, Blueprint Blue, 7-pixel width and CAD stroke style. Force-stop/reopen retained all six settings. UI and saved preferences were checked; actual physical pen-button behavior remains separate work.
- Visually inspected portrait and landscape. Page navigation, Layers, Export, scale and dimensions remained accessible. Restored the device's original auto-rotation settings after testing.
- Fit drawing showed the complete page in both orientations. Every project field compared equal before and after Fit.

Final installed APK SHA-256: `C9C1EC9E7988ED79502A19ACC49FB6F21D0AAC10012A81A3BB56CCEF69DC17B3`. **23 tests passed, zero failures**, and debug assembly succeeded. UI evidence is in the local sibling `Plan_Trace-Foundation-QA` folder (`layers-new-layout`, `long-layer-portrait`, `preferences-reopened`, `responsive-portrait`, `fit-portrait`, `fit-landscape`, and layer-control snapshots). Early layer screenshots contain an active-label replacement glyph; that label was corrected in the final installed build.

This improves usability within the existing visual design. It is not the complete premium dock/inspector redesign or physical S Pen certification. Popout brush controls still require scrolling in the narrow tool rail; their eventual dock redesign remains planned. Sliders still create multiple Undo entries while changing values.
