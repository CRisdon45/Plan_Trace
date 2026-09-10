# Current state and next-session handoff

Updated 2026-09-10 at the start of source/site intake. Verify live refs and concurrent work. Work on `feat/project-geometry-seam`, PR #4 into `feat/off-tablet-integrity`; PR #3 targets the 2D foundation, PR #2 main. Physical tablet testing remains interrupted and must not be disturbed. No application PR is merged by this update.

## Owner clarification

Cody rarely, if ever, wants to type dimensions on screen. Typed values are an occasional fallback, NOT the main authoring workflow. Preserve precision while prioritizing direct pen manipulation, snapping and context-sensitive controls. The owner explicitly deferred redesigning that interaction; do not spend this site-intake milestone on another numeric-entry feature or remove the existing fallback without reason.

## Verified application checkpoint

Actual application head before this documentation update: `27ebdb21c9027b29aa0c0104a2dca8e4a45094da`. It contains the connected pool/coping workspace, two-level radial commands, independent grid/snap preferences, exact sizing and responsive/compact navigation.

Prior completed evidence at that revision: unit/build run 34534238423 (137 tests, zero failures/errors/skips, successful debug assembly) and emulator run 34534239074 (five scenarios, including edit/restart, radial safety, portrait and compact access). Evidence ZIPs and captures were downloaded in the prior session. These are not tests rerun for this documentation-only change. Physical S Pen/palm/button behavior and finished Northstar quality remain unverified.

Repository re-entry exposed an incomplete prior documentation write: the reported `786f644e1ec12f7f50bc28d738d13f36178d7e1c` checkpoint did NOT land. The branch stayed at the tested application revision while main/PR text referenced the missing commit. Do not use that nonexistent checkpoint or its report links. This update repairs the active handoff; main and PR references must be reconciled to actual returned commit IDs before finishing this session.

## What works and what remains

Projects -> Design workspace opens one separately saved draft. Exact line/circular-arc geometry drives the real canvas, stable IDs, following coping, preview/command/Undo, Delete/Undo and verified serial atomic saves. Coping is width/generator intent, not independent artwork. Its sampled JTS validation is not construction approval or area takeoff. Legacy Room plans are unchanged; format 2 reads format-1 outlines, but old format-1-only builds cannot read newer saved drafts. Undo history is session-local, not portable recovery.

The stable Draw/Edit/View/Assist/History/Select wheel opens short outer fans, retains disabled slots and clamps inward at edges. Tap-to-select and visible Commands are implemented; continuous flick is not. Compact list navigation stays above the scrolling actions. Rectangles resize along their own axes; freeform sizing is explicitly uniform, not arbitrary stretching. Typed sizing is a fallback, not the design direction. Grid display and one-foot vertex/movement snapping are separate preferences.

## Next bounded outcome: starting site

Implement source-image intake and explicit registration/known-distance calibration in the same workspace. Keep source pixels and their placement distinct from editable design geometry; source changes must not silently rescale existing objects. Use application-owned image data, truthful scale/confidence state, and verified save/reopen/output. Work initially with original synthetic raster images rather than client sources or a selected maps provider. PDF intake and complete arbitrary site tracing can follow; do not claim SITE-01 complete from a source-image milestone alone.

Do not grow the wheel further for its own sake. Protect existing designs and the interrupted tablet build. Do not auto-promote legacy strokes, reset corrupt drafts or store output polylines as the authoritative project.

## Remaining limits

Source/site registration, arbitrary outline creation, tangent assistance, attached shelves/steps, shared surfaces, project alternatives, multi-project/portable recovery and live Northstar rendering remain unfinished. New-screen export/share, fixed-frame output, physical stylus/palm, full accessibility/handedness, pinch synthesis and hardware performance remain pending. Tooling KSP/AWT exceptions and inherited service/deprecation warnings are unresolved, not excused by green builds.

No runtime AI features, 3D, framework/repo merger, physical installation, signing-key workaround, user-data clearing, release, APK publication or permissions change. Client originals and unapproved reference images remain out of public Git, PRs, logs and artifacts. Update this handoff and main routing using actual checked commits and evidence at the next meaningful outcome.
