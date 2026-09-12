# Plan Trace: premium 2D workflow audit

September 9, 2026. Hands-on testing on the connected Samsung Tab S10 FE, extending the initial functional QA. All 3D work was excluded.

**Verdict: the app supports useful concept sketching, but I would not yet depend on this build for precise revisions or client deliverables.** The strongest parts are immediate drawing, imported underlays, visible dimensions, layers, and local persistence. The blockers are trust: edits can affect protected objects, recovery can lose context, and exports can change the relationship between the plan and the drawing.

This review uses a pool/patio/landscape planning workflow inferred from the app and the owner's request. It is a product judgment, not a claim to know every personal preference. Confirmed failures, unavailable/discoverability gaps, proposed features, and untested capabilities are separated below.

## Read with the initial functional report

[Initial functional QA](functional-audit.md) covers the exact APK/device identity, ten earlier findings, reproduction steps, and acceptance criteria. Its coverage statements describe the first pass; this document extends them. The exact tested APK source is not established: the checked-out `ed8acb5` source differs from installed behavior. Source references are investigation leads, not proof of the installed implementation.

The second pass used a disposable `QA_Premium_Patio` project and a generated 1000 × 700 image with a 400-pixel reference labeled 20 ft. Existing non-QA projects were compared before and after testing and were unchanged. No app source was modified. The tablet was left in Select / S Pen Only mode on the QA project. QA sketches and fixtures remain available for reproduction.

## The job I tried to complete

1. Create a patio concept, import a plan, and establish a reference scale.
2. Draw an approximately 20 × 15 ft rectangle and revise its size and placement.
3. Add a note, separate work into a Patio layer, and recover from deleting that layer.
4. Zoom in and out without modifying the drawing.
5. Produce PNG and architectural PDF versions that preserve the on-screen design.

Creation and drawing were straightforward once the input mode was understood. Precision revision did not have an obvious numeric route. The deletion recovery and final deliverables failed in ways that would undermine confidence during a real project.

## Additional confirmed failures

### 11. P1 — Export changes underlay-to-drawing alignment in both PNG and PDF

**Reproduction:** Import the supplied 1000 × 700 image, calibrate its red 400-pixel reference to 20 ft, draw a 400 × 300 world-pixel rectangle, move it below the blue boundary, add `PATIO_OPTION_A`, and export with the background included.

**Observed:** On the tablet, the rectangle is below the blue boundary and the note is to the right of the image. In both exported files, the rectangle is inside the blue boundary and crosses the red reference. The note is also moved inside the apparent plan area relative to the underlay. The exported rectangle retains a roughly 20 × 15 ft label, while the underlay's 20-ft reference is about twice its width.

The PNG is 2048 × 1536. The 1000 × 700 underlay is stretched to that output, while vector coordinates retain their original scale. This also changes the underlay's aspect ratio. The local `ExportManager.renderProjectToCanvas()` stretches the bitmap into the supplied width and height before rendering elements in their original coordinates, consistent with the observed mismatch. Both output formats exhibit the failure in the installed APK.

**Impact:** A visually correct placement on the tablet can communicate the wrong location and size to a client or installer. This is separate from the earlier incorrect PDF graphic scale and broken share chooser.

**Required change:** Use one document/world coordinate system for image, annotations, dimensions and export. Apply a single aspect-preserving world-to-output transform to all content. Define the sheet/crop bounds explicitly; do not stretch the image independently to a fixed export size.

**Acceptance:** For square, portrait, landscape and nonstandard underlays, landmarks and annotation corners have the same relative positions on canvas, PNG and PDF. A calibrated reference agrees with drawn dimensions and the printed graphic scale. Check background-on/off, sheet sizes, orientation, and geometry outside the image bounds.

Evidence:

- [Canvas before export](evidence/premium-final-canvas.png)
- [Actual exported PNG](evidence/premium-export.png)
- [Rendered exported PDF](evidence/premium-export-render.png) and [actual PDF](evidence/premium-export.pdf)
- [Original image fixture](evidence/QA_image_plan.png)
- [Sanitized QA project snapshots](evidence/qa-observations.json), `premium-before-layer-delete` / `premium-after-layer-undo`

### 12. P1 — Deleting a layer cannot be correctly undone

**Reproduction:** Add a short-named Patio layer. Draw two lines on it. Delete that layer, close Layers, then tap Undo once.

**Observed:** Delete immediately removes the layer and both lines without confirmation. Undo restores only one line. Its `layerId` still points to the deleted layer; the layer itself is not restored. The recovered line is therefore orphaned and is absent from the visible/exported layer rendering.

| State | Layers | Total objects | Objects pointing to absent layers |
|---|---:|---:|---:|
| Before delete | 4 | 4 | 0 |
| After delete | 3 | 2 | 0 |
| After one Undo | 3 | 3 | 1 |

**Impact:** Undo appears available but does not recover the user's work. History restores an earlier element list without its corresponding layer structure.

**Required change:** Record layer deletion as a whole-project transaction containing layer metadata, ordering, active layer and affected objects. Restore them together. Add an immediate Undo affordance or explicit confirmation for destructive layer removal, and enforce valid layer references during history operations.

**Acceptance:** Delete a layer with two differently styled objects, then Undo once: the same layer, order, properties and both objects return. Redo removes them together. Reopen the project and verify no orphaned object references. Repeat with active, hidden and locked layers.

Evidence: [qa-observations.json](evidence/qa-observations.json), `premium-before-layer-delete`, `premium-after-layer-delete`, `premium-after-layer-undo`. Original UI records are retained locally as `premium-delete-ready.xml` and `premium-layer-deleted.xml`.

### 13. P2 — Rectangle corner handles suggest resizing but drag translates the object

**Reproduction:** Select the rectangle and drag from its bottom-right corner. A second attempt used the padded corner-handle position after selection, calculated from saved geometry and the canvas transform.

**Observed:** Both attempts moved the whole rectangle. Width and height remained exactly 400 × 300 world pixels. The second drag changed bounds from `(498.503,648.878,898.503,948.878)` to `(597.968,748.343,997.968,1048.343)`. The selection display includes corner handles, making resize the expected behavior.

**Required change:** Give resize handles distinct hit testing and a clear cursor/interaction state. Preserve the opposite anchor. Add exact width, height, position and rotation fields, with aspect-lock and snapping options. If handles are decorative, replace their appearance until resizing exists.

**Acceptance:** A corner drag changes dimensions while the opposite corner stays fixed, numeric entry can make an exact 20 × 15 ft rectangle, dimensions update, and one Undo restores the original shape.

Evidence: [selected rectangle](evidence/premium-selected.png); [qa-observations.json](evidence/qa-observations.json), `premium-selected`, `premium-corner-drag`, `premium-handle-drag`.

## Additional passes and discoverability gaps

| Check | Result | Interpretation |
|---|---|---|
| Two-finger zoom | A controlled two-pointer gesture expanded span from 400 to 800 px, then reversed it. The view zoomed about 2× and returned; saved rectangle geometry stayed unchanged. | Software pinch path passes. This is injected touch input, not physical finger/palm validation. |
| New named layer | Patio was created, selected and received both new lines. Scrolling revealed its controls. | Basic layer assignment works. Long default names still clip actions, as in the initial report. |
| Text creation | `PATIO_OPTION_A` saved and appeared in PNG/PDF. | Creation works. |
| Revise existing text | Select/double-tap did not expose a text editor. Choosing Note and tapping the existing note opened a blank Add Plan Note dialog. | No usable revision route found in these paths; do not treat this as proof that every possible editing route is absent. |
| Long-press selected rectangle | No additional size, angle or assignment inspector appeared. | Exact editing remains a visible capability/discoverability gap. |
| Existing project preservation | Full non-QA project records matched the earlier snapshot. | Existing work was unchanged by this audit. |

## What I would expect from a premium version

These are proposed product requirements, not assertions that every item was promised or exhaustively proven absent. Priorities are based on completing the workflow above. Repair confirmed defects before investing in optional enhancements.

| User need | Current experience / gap | Desired behavior and completion test |
|---|---|---|
| Trust the plan scale | Imports retain unrelated/default calibration; exported scale and alignment fail. | Guided two-point calibration, clear Uncalibrated state, feet/inches and metric entry, editable reference, per-page calibration, and a second-reference verification. A known 20-ft feature must agree everywhere. |
| Draw a design to a requested size | Dragging produces approximate dimensions; no numeric inspector found. | Exact length, width, radius/diameter, angle and position entry; decimal and feet/inches parsing; live dimensions; constrained rectangles/circles. Enter 20 × 15 ft directly without repeated dragging. |
| Revise without redrawing | Handles translate; toolbar delete/duplicate and drag history fail. | Functional resize/rotate/vertex editing, keyboard/nudge controls where appropriate, explicit snapping and orthogonal constraints, offset/parallel lines, fillets and closed-path editing. Every completed edit undoes in one step. |
| Try Option B safely | Layers help separate work, but recovery is unreliable. | Duplicate project/option, named versions and compare/visibility controls, group/multiselect, copy/paste across layers and projects. Preserve Option A while developing Option B. Project duplication and version recovery were not verified in this audit. |
| Keep a complex job organized | Long names hide controls; deletion breaks history. | Stable layer rows, rename/reorder/group, clear active layer, object reassignment, isolation, visible lock status and confirmation/Undo. Add an object list for finding off-screen or tiny items. Merge/reorder need separate testing. |
| Edit communication, not just geometry | Notes can be added; revision route was not found. | Edit existing text, wrap/resize text boxes, typography/alignment, leader notes, numbered callouts and reusable labels. Change a patio note without deleting and recreating it. |
| Manage real plan sets | Page navigation works, but annotations are shared across pages. | Page thumbnails, page-specific layers/scale/view, page reorder/rotation, underlay replacement with alignment preview, crop and registration controls. Markup on one page must never leak onto another. |
| Navigate comfortably on a tablet | Finger pan and controlled pinch work; new views can inherit old offsets; portrait controls disappear. | Fit Page/Fit Selection, reset rotation/zoom, saved per-project view, responsive portrait/landscape layout, collapsible docks and handedness options. Keep drawing area useful with the keyboard and palettes open. |
| Know what was saved and undo mistakes | Basic autosave/reopen works; unsaved polyline preview is ambiguous. | Visible saved/saving/error state, explicit Finish/Cancel for paths, coherent undo/redo history, recoverable deletion, autosave recovery, and portable project backup/restore. A force-stop after a completed edit must preserve it. |
| Deliver exactly what was designed | Files generate, but sharing fails and output can misrepresent geometry. | Export preview matching the final file, Save to Files plus Share, selectable sheet scale/resolution, aspect-preserving fit, crop bounds, transparent PNG option, editable title-block fields and persistent export preferences. Batch PDF pages after page isolation works. SVG/DXF or other editable interchange is a later requirement decision. |
| Present a polished proposal | Architectural borders, dimensions and notes already exist, but correctness needs repair. | Legible print line weights, controllable hatches/fills, consistent dimension placement, legends, north-arrow orientation, project/client/revision details, logo support and reusable sheet templates. Confirm readability at actual print size. |
| Estimate the design's quantities | No area/perimeter or takeoff route found in the tested workflow. | Closed-shape area and perimeter, pool/patio material schedules, configurable waste factors and editable units. Quantities should link to geometry and update on edits; do not imply engineered accuracy before measurement validation. |
| Work quickly with repeated objects | Basic primitives are available. | A curated 2D symbol library for pools, spas, steps, equipment, planting and furniture; reusable groups, favorites and searchable styles. This adds value after precision editing and reliable copy operations. |
| Feel confident using the S Pen | Synthetic stylus routing passes; real hardware feel is unverified. | Physical pen/palm testing, clear touch mode, persistent preferences, optional barrel-button eraser/selection, adjustable smoothing and pressure behavior, predictable hover feedback. Avoid requiring hidden gestures for essential actions. |

The product should make the common path obvious: **Import → Calibrate → Draw precisely → Revise safely → Present faithfully.** Contextual help should explain the next action, especially calibration, touch mode, layer locks and polyline completion. Accessible labels for colors and line widths are also necessary for discoverability and automation.

## Delivery order and release gates

### Gate 1 — Protect work and establish a reproducible build

Recover the exact APK source, restore a repeatable local build and stamp APKs with commit/build IDs. Fix calibration input ownership, floating toolbar routing, locked-object mutation, gesture-level Undo, and complete layer transactions. Add model invariants preventing orphaned objects. Pass each failure's reproduction and acceptance checks before more editing features.

### Gate 2 — Make measurements and deliverables trustworthy

Fix shared world coordinates for import/canvas/export, scale-bar math, uncalibrated imports, page-specific markup, and share/save error handling. Use the published fixture for export alignment regression and additional differently sized fixtures. Validate saved output by reopening it and comparing landmarks and measured spans.

### Gate 3 — Complete the daily editing workflow

Implement exact shape dimensions and working resize, existing-note editing, discoverable path completion, responsive layer/toolbar layouts, fit/reset view and persistent input preferences. Add safe option duplication and project recovery. Run a timed real task: create and revise two patio options, close/reopen, export both without a workaround.

### Gate 4 — Add premium productivity

Prioritize associative area/perimeter takeoffs, reusable 2D symbols, grouped editing, reusable sheet templates and document/option management. Decide editable interchange and backup/sync requirements from actual job handoff needs. These are proposed enhancements; cloud collaboration, AI and 3D are not prerequisites for making this 2D workflow dependable.

## Remaining validation and evidence limits

This is a focused hands-on functional and usability audit, not exhaustive certification. Physical S Pen pressure/tilt/hover, palm rejection, real finger ergonomics, latency, battery/thermal behavior, large production PDFs, low memory/storage, extended sessions, provider-specific imports, accessibility navigation, project deletion/trash, layer reorder/merge, invalid numeric input and backup restoration still need dedicated passes. A successful injected pinch does not validate physical palm rejection.

Published evidence contains generated fixtures, QA-only screenshots/exports and sanitized QA project records. Raw databases, unrelated user projects and general device logs are retained locally rather than published. The first report's raw evidence filenames refer to that local audit bundle; the linked evidence here is included in the repository.
