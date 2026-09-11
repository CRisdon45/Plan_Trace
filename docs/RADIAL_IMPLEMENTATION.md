# Radial commands: verified implementation record

This report restores a missing durable description of the radial milestone. It does not claim a new application change. The previously reported documentation commit `786f644e1ec12f7f50bc28d738d13f36178d7e1c` never landed; links to that commit must not be used. The actual application evidence below remains valid.

## Implemented and bounded

Draw, Edit, View, Assist, History and Select occupy stable main-wheel positions. Each category exposes a small outer fan. Disabled actions retain their locations; Back/Close and outside dismissal do not modify the drawing. Edge placement shifts the menu inward without rotating the learned directions. Compact or large-text layouts use equivalent list actions with navigation kept outside the scroll area. A visible Commands control remains available.

The wheel operates on the same connected pool/coping command and save model as the canvas. Software stylus generic-event routing exists; actual Samsung S Pen delivery and feel are not certified. This is tap-to-select, not continuous hold-and-flick. Grid display and one-foot vertex/movement snapping are independent preferences, not a finished endpoint/alignment snap engine.

Exact numeric controls exist as an occasional fallback. Rectangles resize independently around corner 1 along their own axes. Other outlines scale uniformly by their exact overall span to preserve circular arcs. This is not independent freeform stretching. Coping retains its physical width. Canceled or untouched text does not create an edit. The owner's later clarification makes direct pen work, not keyboard input, the primary direction.

## Dated evidence

Application `27ebdb21c9027b29aa0c0104a2dca8e4a45094da` passed 137 unit tests and debug assembly in [run 34534238423](https://github.com/CRisdon45/Plan_Trace/actions/runs/34534238423). [Android run 34534239074](https://github.com/CRisdon45/Plan_Trace/actions/runs/34534239074) completed five separately invoked scenarios: editing/saving, process restart, radial safety, portrait and compact access. This session re-counted the existing XML archive and confirmed five successful scenario reports. It did not rerun that older revision.

The cases exercise actual snapped movement and Undo, canceled/untouched size input, retained coping and unrelated objects, edge access, and a 320dp compact configuration. Later source-intake runs retain these cases. Virtual checks do not establish full accessibility, handedness, physical pen/palm behavior, hardware performance, or Northstar approval.

The compact Back control had required a layout correction during the earlier review. The final application uses pinned navigation, with short-layout checks. Build-log exceptions and screenshot acceptance are separate from green tests. Current progress and next actions belong in CURRENT_STATE.md; this historical report should not become another competing roadmap.
