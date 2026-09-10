# Plan Trace — connected tablet 2D QA

Tested September 9, 2026, approximately 5:29–5:51 PM Arizona time.

**Assessment: useful for exploratory sketching, but the tested build is not ready for dependable editing or measured deliverables.** Basic creation and persistence work. Editing protections, undo, calibration, export delivery, and the PDF graphic scale need correction first. No 3D functions were tested.

> This is the initial pass. See [the extended premium workflow audit](premium-workflow-audit.md) for additional failures, controlled pinch testing, resize attempts and layer deletion recovery. Raw evidence filenames below identify the local QA bundle; selected published evidence is linked from the extended audit.

## Build and method

- Device: Samsung SM-X520 / Tab S10 FE, Android 16; 2304 × 1440 landscape, also tested in portrait.
- Installed package: `com.aistudio.plantrace.jzkrwq`, version 1.0 (1).
- APK: `PlanTrace_Tablet_Filament_Test_2026-09-09.apk`.
- SHA-256: `392874CF3938711AF31F8BCAE5CD7B84D750C733A03E13B1EA6D8AF9D7CCE6FF`.
- Used the actual tablet UI through Android input, screenshots, UI hierarchy dumps, app-specific logs, and read-only copies of the app's project database. Database checks distinguish a visible preview from an object that actually saved.
- Test projects: `QA_2D_0909` and `QA_Import_0909_Checked`. The existing non-QA project was retained. Fixtures were placed in the tablet's Downloads folder.
- No application source was edited during this QA. System rotation control was returned to its original `free` setting. The main QA project was left open in S Pen Only mode.
- The checked-out source is commit `ed8acb5`, but it is **not an exact behavioral match for this APK**: the installed build commits polylines and resumes the latest project, while the inspected source does not implement those behaviors the same way. Treat source locations below as investigation leads; obtain the exact APK source before making fixes.

## What worked

| Workflow | Verified result | Limits |
|---|---|---|
| Startup | Opened repeatedly; no app crash found in the session crash log | This was a functional pass, not a long-duration stability test |
| Project creation | Created two named sketches; CAD Deck template loaded | New projects inherit the previous canvas position |
| Project rename/switch | Renamed the import sketch and switched back to the main QA sketch | Project deletion was not tested |
| Autosave/reopen | Geometry, note, layers, scale, and imported PDF survived force-stop and relaunch | PDF page and input preferences reset |
| Line, rectangle, ellipse | Visible objects saved with geometry and styles | “Circle” supports an ellipse; exact-size entry was not tested |
| Freehand pen | Touch strokes and one synthetic stylus stroke saved | Physical pressure/tilt/hover not verified |
| Polyline | Double-tapping the endpoint committed a six-point polyline | Finishing and cancellation need clearer UI |
| Add-object undo/redo | Undo removed the ellipse; Redo restored the same object | Drag undo is broken; see below |
| Selection and movement | Selected and translated shapes; positions persisted | Floating action buttons and layer protection fail |
| Notes | Added and saved `QA_NOTE_20ft` | Editing existing note content not verified |
| Colors and line width | Palette color saved; stroke width changed from 1.5 to 7 | Palette and width choices lack accessible names |
| Watercolor fill | Wash changed the rectangle's fill from cyan to red and saved it | All blend modes were not verified |
| Eyedropper | Sampled `#38BDF8` from the ellipse; next line saved that color | Hidden-layer and bitmap sampling edge cases not verified |
| Layers | Added QA layer, assigned it as active, created geometry on it | Reorder, merge, and layer deletion not verified |
| Layer visibility | Hiding QA removed its rectangle from the canvas and PDF | Full hidden-object hit testing not exhaustively checked |
| Layer lock, partial | Blocked creating a new rectangle on a locked layer | Existing locked objects can still move |
| Scale/dimensions | Set a reference of approximately 401 px = 20 ft; dimension objects saved and appeared in exports | Unsafe with Erase selected; graphic scale wrong |
| Image import | PNG fixture rendered from Downloads | Import retains an unverified previous/default calibration |
| PDF import | Two-page PDF rendered, both navigation directions available, page 2 visible | Markups are shared between pages |
| PDF generation | Created a valid 17 × 11-inch page with title block, note, geometry and callouts | Share fails; graphic scale incorrect |
| PNG generation | Created a readable 2048 × 1536 PNG containing the drawing | Share fails; other output sizes/options not exhaustively tested |
| Finger/stylus routing | In S Pen Only mode, synthetic stylus added one object; finger drag panned without adding an object | Real S Pen palm rejection and multi-touch remain manual checks |

## Confirmed failures and required fixes

### 1. P1 — Scale calibration can erase existing work

**Reproduce:** In `QA_Import_0909_Checked`, draw a line, select Erase, tap Scale, and drag the reference along that line. The calibration dialog opens, but the line is deleted. The database changed from one Line object to zero. Canceling calibration does not restore it; Undo was used to recover the test line.

**Fix:** Give calibration an exclusive input state ahead of every drawing/editing tool. No erase, fill, selection, move, note creation or geometry mutation should occur while choosing reference points.

**Acceptance:** Start calibration from every tool. Cancel and Apply must leave the object collection identical; only calibration data may change on Apply.

Evidence: `before-eraser-calibration/projects.json`, `after-eraser-calibration/projects.json`, `eraser-calibration.xml`, `eraser-calibration.png`.

### 2. P1 — Floating Delete and Duplicate controls do not execute

**Reproduce:** Select the rectangle, then tap Duplicate in its floating toolbar. The ellipse behind that button becomes selected and the toolbar jumps to the ellipse. No new object is saved. Repeating Duplicate on the ellipse and tapping its Delete button also leaves the count unchanged at nine objects.

**Likely cause:** Canvas hit testing is receiving taps intended for the overlay. This is inferred from the selection change, not a debugger-confirmed diagnosis.

**Fix:** Put the object action toolbar outside the canvas gesture interception region, or explicitly consume toolbar input before canvas dispatch. Check Fill and Use current color in the same toolbar as part of the fix; their individual actions were not separately verified.

**Acceptance:** Duplicate adds exactly one copy and selects it; Delete removes exactly the selected object. No underlying object receives the toolbar tap. Both operations undo in one step.

Evidence: `duplicate-rect-controls.xml`, `duplicate-rect-after.xml`, `duplicate-shape/projects.json`, `duplicate-rect-after/projects.json`, `object-delete-after/projects.json`.

### 3. P1 — Undo records pieces of a drag instead of the whole move

**Reproduce:** Drag a rectangle approximately 100 px right and 200 px down. Its top-left moves from `(1000,250)` to `(1099.52,449.05)`. One Undo only moves it to `(1097.68,445.36)`.

**Fix:** Begin one undo transaction at drag start and commit at drag end. Do not push history for every movement event. The inspected `MainViewModel.updateElement()` pushes an undo entry on each update, consistent with the observed behavior.

**Acceptance:** One Undo restores `(1000,250)` and one Redo restores the completed drag, regardless of stroke duration or event rate.

Evidence: `before-move/projects.json`, `after-move/projects.json`, `undo-move/projects.json`.

### 4. P1 — Locked layers do not protect existing geometry

**Reproduce:** Create a rectangle on QA, lock QA, choose Select and drag the rectangle. With `isLocked=true`, its top-left moves from `(350,950)` to approximately `(447.94,812.89)`. New-shape creation is correctly blocked, so protection is inconsistent.

**Fix:** Enforce layer lock in the shared mutation path as well as canvas hit testing, including move, resize, delete, fill, recolor, duplicate and reassignment.

**Acceptance:** A locked layer's objects and properties remain byte-for-byte unchanged after attempted edits. Provide clear feedback explaining the lock.

Evidence: `qa-layer-shape/projects.json`, `locked-draw/projects.json`, `locked-move/projects.json`, `locked-layer.png`.

### 5. P1 — PDF and PNG sharing fails while reporting success

**Reproduce:** Export & Share PDF, then Export & Share PNG. Each file is generated, but the tablet remains on the canvas with an “Exported…” message. No Android share chooser opens.

The app log identifies the error: starting an activity from a non-Activity context requires `FLAG_ACTIVITY_NEW_TASK`. Both failures point to `ExportManager.shareExportedFile`.

**Fix:** Launch the chooser from an Activity, or correctly configure the chooser for the application context. Propagate sharing failures rather than swallowing them and showing unconditional success. Add a reliable Save to Files route.

**Acceptance:** A user can open the chooser and save each format to a chosen destination, then reopen it. Failed delivery produces an actionable error. No external message was sent during this test.

Evidence: `export-log.txt` (PDF at 17:38:19, PNG at 17:39:27), `QA_export.pdf`, `QA_export.png`.

### 6. P1 — The exported PDF graphic scale is numerically wrong

The generated PDF states a calibration of 20 ft = approximately 401.288 world pixels. The plan's PDF transform is 0.42 points per world pixel, so a 20-ft span must be approximately **168.54 PDF points**. The graphic scale labels **100 PDF points as 20 ft**. It therefore does not measure the drawing it accompanies.

**Fix:** Derive scale-bar lengths and labels from the actual world-to-page transform and calibration. The inspected implementation instead gives segments a fixed layout width and derives labels from the reference units.

**Acceptance:** A drawn 20-ft line and the graphic scale's 0–20-ft span have identical length on the exported sheet, for every sheet size/orientation and import size.

Evidence: `QA_export.pdf`, `export-content.txt` (0.42 transform and 50-point scale segments), `export-render-1.png`, `calibrated-dimension/projects.json`.

### 7. P1 — Multi-page PDF markup is not page-specific

**Reproduce:** Import the supplied two-page fixture. Navigate from page 1 to page 2. The same rectangles, ellipse, note and dimension appear over both different pages. The saved objects have no page association. A separate problem is that reopening returns to page 1 after leaving the document on page 2.

**Fix:** Store annotations, calibration and view state per page. Persist the active page. If intentionally supporting only one marked-up page, say so and separate that page into its own project instead of silently sharing annotations across the PDF.

**Acceptance:** A page-1 note never appears or exports on page 2; page-specific scale is retained; reopening restores the active page.

Evidence: `pdf-import.png`, `pdf-page2.png`, `pdf-import/projects.json`, `before-restart/projects.json`, `restarted.xml`.

### 8. P1 — Imports appear calibrated without a valid reference

**Reproduce:** A new sketch starts with 240 px = 20 ft. Import the 1000 × 700 image fixture, whose 20-ft reference is 400 pixels. The app still displays the default calibrated scale. Replacing the earlier CAD underlay with a PDF similarly retained its previous 401-pixel calibration.

**Fix:** Mark imported/replaced plans uncalibrated unless a verified scale is supplied. Require or prominently request a new reference before measurements. If preserving calibration is a supported option, make it explicit.

**Acceptance:** Imported plans cannot silently present measurements based on an unrelated template or prior underlay.

Evidence: `image-confirmed.png`, `image-import/projects.json`, `pdf-import/projects.json`.

### 9. P2 — Layers and portrait layouts hide essential controls

In landscape, the default long layer names and object counts push visibility, lock and delete controls beyond the layer card. Short-named QA exposes controls that are missing on longer rows. In portrait, the top toolbar runs off the right side: Export and More Options are unavailable in the visible UI. Rotation also reset the input mode to S Pen Only.

**Fix:** Allocate a bounded, ellipsized area for layer names; move secondary actions into a menu. Make the main toolbar responsive with compact icons or intentional overflow. Preserve input preferences across activity recreation.

**Acceptance:** All layer actions and export/import controls remain reachable in both orientations with long project/layer titles. Labels and actions must not overlap.

Evidence: `layers.png`, `layers.xml`, `qa-layer-added.xml`, `portrait.png`, `portrait.xml`.

### 10. P2 — Canvas and polyline state need explicit boundaries

- After panning one project, a new project and its imported image retained that pan offset, clipping the image at the top and left. No Fit/Reset View action was found in the tested toolbar or More Options menu.
- Polyline taps remained an unsaved preview until a double-tap committed them. Switching tools left the preview visible; a restart removed it. Tapping back at the first vertex did not finish the polygon in this test.
- Palette swatches and width choices have no descriptive accessibility labels, and expanding them pushes commonly used tools off the visible toolbar.

**Fix:** Reset/fit view on new project or import, optionally restore per-project views, and provide Fit/Reset View. Add visible Finish/Cancel for polylines, explain double-tap, and define what tool changes do to unfinished geometry. Label color/width choices.

**Acceptance:** New/imported plans open in a usable view. Users can tell whether a polyline is saved and finish/cancel without guessing. Toolbar options have identifiable labels.

Evidence: `after-finger-pan.png`, `image-confirmed.png`, `poly-taps/projects.json`, `poly-close-attempt/projects.json`, `poly-doubletap/projects.json`, `palette-scrolled.xml`, `widths-scroll.xml`.

## Recommended implementation order

1. Recover the exact tested APK source and make its local build reproducible. The supplied checkout lacks the Gradle wrapper launcher/JAR; the earlier local build also failed resolving a build plugin. Preserve the signing key and stamp future APKs with commit/build identifiers.
2. Fix input ownership: calibration, floating action toolbar, locked-object mutations, and one undo transaction per gesture.
3. Fix share/save delivery and verify PDF/PNG round trips.
4. Correct measurement integrity: import calibration, physical graphic scale, and page-specific PDF annotations.
5. Repair adaptive layouts, project view restoration, and polyline completion UX.
6. Repeat these regression scenarios on the same tablet, then perform physical S Pen and two-finger testing.

## Remaining manual/extended checks

Physical S Pen pressure, tilt, hover, barrel-button shortcuts, sustained palm contact, simultaneous palm/pen input, real pinch zoom, hold-to-straighten, precision resizing/snapping, layer merge/reorder, project deletion/recovery, large production PDFs, low-memory behavior and long sessions remain unverified. Synthetic stylus input only establishes software routing; it is not proof of physical S Pen quality. Import from providers that return opaque document IDs also needs testing. No 3D was exercised.

All UI dumps, screenshots, database snapshots, fixtures and export evidence are retained alongside this report. The recorded failures are observations from the installed tablet build; suspected implementation causes are labeled separately.
