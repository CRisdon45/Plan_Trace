# Foundation implementation — September 9, 2026

## Implemented

- Restored Gradle wrapper and compiled the available checkout. Two pre-existing compile defects were repaired: Offset coordinate property names in the dormant 3D source and an incomplete tool switch in the alternate canvas. No 3D feature development was performed.
- Added an isolated **Plan Trace Dev** package for safe device verification beside the installed APK.
- Unified image/vector export coordinates with aspect-preserving content fitting; visible geometry outside the underlay is retained. Text extents are measured rather than assumed to be 120 px wide.
- Derived graphic scale lengths from calibration and actual world-to-page scale. Uncalibrated output no longer receives a fictitious numeric scale bar.
- Corrected share chooser launch flags and attachment permissions. The UI distinguishes a generated file from a successful chooser launch.
- Replaced element-only Undo with layer/element/active-layer snapshots and gesture transactions. Cancelled gestures recover their pre-gesture state.
- Guarded existing-object edits, erase and assignment against locked or hidden layers; layer deletion and merge recover complete ownership through history.
- Routed calibration ahead of the drawing/eraser tool and prevented drawing mutations during calibration.
- Moved canvas input off the parent of selection controls, allowing Delete/Duplicate to receive their taps.
- Added native finger pan and centroid-anchored pinch handling, with drawing cancellation when navigation begins. Reset the view on project/underlay changes.
- Marked new/imported plans uncalibrated, persisted PDF page navigation, and repaired repository reopen selection so it loads an existing recent project instead of creating another new sketch.
- Suppressed persistence during unfinished gestures and queued completed saves in order so asynchronous writes cannot overwrite a newer edit with an older snapshot. Save errors are surfaced.

## Evidence established on the connected tablet

Samsung Tab S10 FE, Android 16, using `com.aistudio.plantrace.jzkrwq.dev` and disposable `QA_Foundation`.

| Check | Observation |
|---|---|
| Floating actions | Rectangle count 1 → Duplicate 2 → Delete 1 → Undo 2. |
| Drag Undo | Copy moved from `(630,429)` to approximately `(930.418,679.348)`; one Undo returned exactly to `(630,429)`. |
| Calibration with Erase | Cancelled calibration retained both rectangles; no erase occurred. |
| Layer deletion recovery | Undo restored all layer records, every object and active-layer ID exactly. The test layer had three lines by the deletion baseline; total project objects returned to five. |
| Locked object | Attempted drag left saved geometry unchanged. |
| Image import | The generated 1000 × 700 fixture rendered with a Set Scale state. |
| PNG | Android image share chooser opened; actual file preserved relative positions and the underlay's aspect ratio. |
| PDF | Android document share chooser opened. Rendered PDF preserved alignment, and the 20-ft scale span visually matched the calibrated reference. Mathematical scale tests also passed. |
| Reopen | Same QA project and calibrated scale resumed after force-stop; saved geometry persisted. |

Raw local evidence is in the sibling `Plan_Trace-Foundation-QA` folder: UI trees, QA-only model snapshots, `fixed-export.png`, `fixed-export.pdf`, `fixed-export-render.png`, `export-canvas.png` and chooser captures. These are real app outputs, not the supplied Northstar inspiration images.

The first package expanded run passed **15 tests with zero failures**: seven integrity tests and eight existing model/context tests. Debug assembly succeeded. A new line was saved on the final installed build and all six project objects survived force-stop/reopen unchanged. APK SHA-256: `EEEA0A600CC941537EC72185AA9CF66CE095D0FFE6D45C6B6AE676A58C1B3593`. Host tests do not replace physical pen or full runtime regression.

## Remaining work and limits

This checkout still differs from the original APK. Keep both installed during development. Source polyline completion is now implemented and checked as recorded below; other APK/source differences still need reconciliation before promotion.

Per-page PDF object ownership is not yet implemented; saving the active page alone does not solve that defect. Exact editing beyond rectangles, long layer-name/portrait layout, complete pen-button behavior, Fit View, save recovery UI and project backup remain on the execution plan. The Northstar specification is recorded, but semantic materials and presentation modes are not yet implemented.

History presently covers layer/element edits and active layer, not underlay replacement or calibration. Opacity sliders still record individual changes. Export annotation clearance uses padding in addition to measured note bounds; crowded and unusually long dimension labels need dedicated layout work. Failed saves are reported but durable recovery/retry storage is not implemented. These are explicit follow-up requirements, not completed claims.


## Second package: everyday editing

- Rectangle corner handles resize while holding the opposite corner fixed. The pointer offset prevents a jump when grabbing the padded handle. An entire drag remains one Undo action.
- Edit size accepts positive decimal width and height in the calibrated unit, or pixels before calibration; it preserves the top-left corner. Other shapes and mixed feet/inches entry remain future work.
- Polylines have a visible unfinished preview, Finish, Close shape and Cancel. Changing tool, page or layer cancels the draft with feedback. Two points create an open path; three can close. Double-tapping the endpoint and tapping the starting vertex provide shortcuts, but those shortcuts still need separate hardware validation.
- Edit note revises an existing note without replacing its identity, position, layer or styling. Text hit testing and export bounds include the full multiline extent, and rendering draws each line.

Tablet evidence: entering 20 by 15 feet produced rectangle bounds (630,429) to (1029.298828,728.474121), matching the measured 399.298828-pixel / 20-foot calibration. A corner drag added exactly 100 world pixels in each direction while retaining the top-left corner. One Undo restored every element exactly. PATIO_OPTION_A changed to PATIO_OPTION_B with the same note ID and attributes; Undo restored the original project geometry and text exactly. Close shape saved a three-vertex closed polyline. Finish added an open polyline (8 to 9 objects), Undo returned to 8, Cancel retained 8, and force-stop/reopen retained all eight records exactly.

A visual reopen check then exposed a separate defect that geometry snapshots did not catch: imported image access was temporary and the underlay disappeared. The import path now retains the document read permission, identifies PDFs by provider MIME type as well as extension, validates a readable preview before replacing the current underlay, and reports inaccessible existing sources instead of silently displaying a grid. This retains access, not a private backup: a source deleted or moved outside the app may still need reimport. Earlier geometry-only reopen evidence must not be interpreted as proof of underlay recovery.

Final second-package validation: **18 tests passed, zero failures** (10 integrity and 8 existing tests), debug assembly succeeded. On the installed build, both QA image and two-page PDF were imported through Android Files, force-stopped, reopened and visually confirmed with their underlays intact. The PDF's provider URI did not require a filename extension for correct classification. Evidence: `durable-image-reopened.png`, `durable-pdf-reopened.png`, `pdf-durable/projects.json` in the local QA folder. Tablet left in Select / S Pen Only with the image restored; reimport intentionally resets calibration, so Set Scale is shown. APK SHA-256: `4200195D1BF007F165EF83AF16123E84F9C0DE123C573E223EDB8D53B7A32A4B`.
