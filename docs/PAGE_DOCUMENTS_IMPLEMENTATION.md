# Page-specific document state

## Behavior

Every source and PDF sheet owns its drawings, layers, active layer, calibration, underlay opacity and lock state. New sheets begin empty and uncalibrated. Returning to a sheet restores its own state. Undo and Redo stay with each sheet for the current open-project session; switching sheets cannot apply an edit from a different sheet. Session history is not persisted across app restarts.

Changing the imported source now opens that source's separate drawing state. Previous source states remain stored in the project and can be recovered by selecting the same sample or reimporting the same document URI. They are not discarded or silently overlaid on a new source. A full document browser, explicit copy-between-sheets command, source relinking and document revision comparison remain future work. If a provider assigns a new URI to a replacement file, it is treated as a different source.

The canvas resets unfinished gestures and its view when changing sheets. Calibration and selection are cleared on navigation. Background loading happens away from the UI thread and ignores a stale result for another sheet/project. Export waits for the active sheet to finish loading and uses that sheet's state; full multi-sheet export is not implemented yet.

## Existing projects and migration

Database version 2 adds a single non-null `pageDrawingsJson` column with an empty-object default. The version 1 migration uses ALTER TABLE; it does not rebuild or delete project records. Destructive fallback migration was removed.

The legacy flat fields remain the live active sheet, preserving compatibility with existing project records. On save, a complete current-sheet snapshot is included with the stored inactive sheets. Legacy drawings that previously appeared on every PDF page are retained on the last saved active page only. There is no evidence of their intended original page ownership, so the migration does not guess or duplicate them. Other pages begin empty. Existing geometry, styles, layer IDs and calibration are retained on that saved page.

Five focused regressions cover sheet state isolation, serialized reopening, legacy page assignment, separation of different sources and a real version 1-to-2 Room database upgrade. Existing integrity/model tests also run. Tablet migration and page-flow evidence is recorded below after verification.

## Verified on the connected tablet

Samsung Tab S10 FE, Android 16, isolated Plan Trace Dev. The version 1 database was snapshotted before installation. After the upgrade, its schema was version 2 and **every legacy field of every saved project compared equal** to the baseline.

- Imported the two-page QA PDF. Both new pages began empty and uncalibrated, while the prior image's eight objects remained archived.
- Page 1: rectangle, an independently drawn 10-foot calibration segment, unlocked underlay, and later the Notes & Dimensions active layer.
- Page 2: diagonal line, an independently drawn 5-metre calibration segment, locked underlay and Base Linework active layer. These deliberate QA calibration segments are distinct from the printed fixture's reference line.
- Switched back and forth. Drawings, layers, scale and underlay lock compared exactly to each saved baseline.
- Undo removed only the current sheet's object. After changing sheets, Redo restored each sheet's own object exactly.
- Force-stopped on page 2 and reopened. Both sheets' geometry, scale and active layer persisted exactly; visual inspection confirmed the page 2 underlay with only its diagonal line.
- Reimported the original image: all eight original objects and its layer/scale state returned exactly. Reimporting the PDF restored its previously edited pages.
- Exported page 2 through the actual PDF share chooser and inspected the rendered PDF for its page 2 underlay and isolated line.

Evidence is in the local sibling `Plan_Trace-Foundation-QA` folder, including `before-pages-migration`, `after-pages-migration`, `page-one-*`, `page-two-*`, `pages-reopened`, `image-archive-restored`, and `page-two-export.pdf/png`. No original production-app database was altered.

Final build: **23 tests passed, zero failures** (5 page/migration, 10 integrity, 8 existing model tests). APK SHA-256: `69D64BEE74F34DAFE03636AD81843570A0B7776AF699EACDBAF5C498BA17212D`.
