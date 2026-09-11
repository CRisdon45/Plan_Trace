# Project and interaction contract

Target behavior, 2026-09-10. This is not a claim that these relationships or interactions already exist. Implement in small migration-safe slices, using [BENCHMARKS.md](BENCHMARKS.md).

## One physical project, multiple views

`Project + protected site base -> design alternatives -> domain objects and relationships -> working canvas / presentation / dimensions / quantities / output sheets`

Imported pages keep their own registration, calibration, provenance and page-local notes. Physical pool/deck/plant objects belong to the project or its deliberate design alternative, not separately to each PDF page. A presentation sheet and a landscape sheet reference the same design state. Existing per-page sketches must remain readable and editable during migration; do not reinterpret colors or layer names as semantic truth.

Separate object kind, geometry, relationships, material/style, and source confidence. Use stable object and relevant edge identities, explicit real-world units, versioned persistence and atomic edits. Keep ordinary freehand annotations lightweight. Proposed relations are deliberately limited; a universal constraint solver is not a prerequisite.

## Intent that survives editing

- **Attached:** coping follows the pool perimeter; shelves/steps retain their chosen attachment and valid footprint. Resizing a pool does not indiscriminately scale every feature.
- **Shared:** edit a paving/turf boundary once, updating both surfaces and their quantities. Calculate occupied regions/holes intentionally so overlapping display fills do not double-count quantities.
- **Fixed:** a patio edge aligned to the house stays fixed when the pool's inner boundary changes. Unrelated plants and furniture do not move automatically.
- **Distributed:** repeated bowls, pads or chosen plant groups can retain a deliberate spacing/alignment rule. Collections remain parameterized rather than becoming frozen stamps.
- **Associated:** generated dimensions refer to geometry. Source-stated dimensions, calculated quantities and manually entered notes remain distinguishable; a literal text label is not an associative measurement.

Preview dependent changes and expose relationship controls when needed. A completed operation commits once and one Undo restores all affected objects, annotations and relationships. Invalid offsets, disappearing edges, conflicting anchors or impossible clearances need an understandable warning and recoverable outcome, not silent distortion.

Store height/depth with its datum: beam, waterline, adjacent finished deck or another defined reference. Distinguish a raised edge from a flush border without starting 3D. Planting eligibility is separate from surface material: granite does not require a bed object to accept plants.

## Site setup and confidence

Support an imported plan/overhead image and measured drawing from scratch. Establish orientation and scale, verify another known distance where available, and draw house/patio/wall/access features in the same workspace used for design. Retain traced, assumed and field-verified status. Printed scale and a clean outline alone are not a survey. Source changes must not silently rescale a completed design.

## Pen-first operation

The owner rarely wants on-screen numeric typing. Direct pen gestures, dependable snapping and local context should carry routine design. Typed values are an occasional precision fallback, not the default path through every operation. The existing numeric fields do not settle the final interaction design, which the owner deferred.

The primary barrel-button action summons a compact radial menu near the pointer, offset from the hand and usable near screen edges. Common command directions remain stable; context changes relevant operations without rearranging learned positions unpredictably. A visible entry provides the same functionality when device events are unavailable. Do not depend on Bluetooth remote gestures or silently change system settings.

Use the wheel to choose intent, pen handles to manipulate geometry, and contextual precision controls for accurate changes. Keep feet/inches entry available as an occasional fallback, not a required step. Large plant/material collections belong in a temporary panel rather than nested rings of tiny choices. Specific sector layout, flick thresholds and handedness behavior are proposals to test, not fixed values invented by this document.

A deliberate completed gesture, radial choice or finished numeric entry is the commit. Avoid routine Apply/Accept/OK confirmations. Unfinished multistep paths still need clear Finish/Cancel. Destructive or data-risk operations may require safeguards. No essential action exists only as an undiscoverable gesture.

Pen contact, button transitions, cancellation, focus loss, and pen/finger routing must not change tools mid-stroke or create stray marks. Hover alone never edits. Locks protect every mutation path. Finger navigation and palm rejection coexist with drawing; a client pointing at the tablet must not accidentally author geometry. The exact physical event behavior must be tested on the target tablet, not inferred from synthetic input.

## Presentation without duplicate work

Show an alternative or enter Present without rebuilding a drawing. Hide interaction overlays while preserving the exact view and design. Temporary discussion marks are clearly separate from committed project changes. Save/reopen, Undo and export remain part of the same user outcome.

Technical/Graphic/Northstar modes use the same geometry and registration. Styles may simplify during movement, but object identity and material anchoring remain stable. See [NORTHSTAR_VISUAL_SPEC.md](NORTHSTAR_VISUAL_SPEC.md).
