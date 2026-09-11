# Geometric snapping for the pen-first workspace

Recorded 2026-09-11. This slice makes the existing measured drawing target refer to actual project geometry. It does not redesign the live readout or create automatic persistent relationships.

## User direction

Cody wants an expert-facing, clean interface. For ordinary line drawing, the eventual readout should be just the dimension beside the target: no bubble, routine "Line" label or repeated grid-state narration. Meaningful warnings and cues remain useful. He explicitly allowed that presentation cleanup to wait rather than interrupt functional development. AGENTS.md, decision D20 and the interaction contract preserve that distinction. The current readout is not visually approved by this milestone.

## What is implemented

Assist -> Object snap is an explicit device preference, independent of grid visibility and the existing one-foot grid snap. It starts off rather than unexpectedly changing a saved user's drawing behavior. The existing Assist action directions are retained when adding the new option.

The target resolver considers canonical object corners, exact line/arc midpoints, nearest points on finite lines/arcs, and alignment lines through existing corners. Source pixels and derived coping polygons are not indexed. Locked house/property objects can be used as references without being unlocked or changed. During vertex editing, the edited object is excluded from attraction to avoid snapping back to itself.

Drafting uses the same resolved endpoint for the guide, measurement and eventual placement. A geometric reference takes precedence over grid rounding, so an existing off-grid house corner remains exact. A deliberate right-angle construction axis is retained; nearby incompatible corners do not bend it. Alignment uses the first drawn edge's orientation, or project axes before one is established. It is not a general house-axis inference engine.

The initial acquisition radius is 12 dp converted through the current viewport, with a 1.6-times release radius for an acquired reference. Equally ranked neighbours do not alternate while a target remains captured; a higher-priority corner can replace an edge capture. Deterministic tie-breaking does not depend on object list ordering. These are initial behavior settings, not proven hardware/ergonomic thresholds.

A small noninteractive diamond identifies the resolved attraction. Corner alignment adds a thin guide back to the reference. No new textual snap narration is placed beside the drawing target. Existing live measurement presentation remains unchanged for the later UI pass.

## Geometry, limits and authority

Nearest arc points use the actual circular definition in a local chord basis, not a sampled display polyline or the full unbounded circle. Constraint-to-straight-edge intersections respect finite edges. Interior arc intersections with a constrained drawing axis are intentionally not implemented; canonical arc endpoints and midpoints can still be used where compatible. Snapping does not add tangent solving or arbitrary edge/edge intersections.

The immutable reference index is cached for committed geometry. An 8,192-edge budget is all-or-nothing; over-budget geometry causes an explicit paused-snapping message, not an arbitrary partial reference set. There is no tablet performance acceptance implied by this budget.

Creation and vertex edits still use the existing document validation, lock, preview, command, Undo and atomic-save path. Invalid edits do not keep a misleading accepted snap. This is one-time placement, not an attachment: later source-object edits do not automatically drag geometry that once snapped to them. No new saved format, dependency, legacy Room migration or persistent constraint data is introduced.

Current object-snapping coverage is corner creation and vertex movement. Whole-object moves and curve-handle reshaping retain their previous behavior; direct straight-edge manipulation, snap-to-derived-coping, general intersections and richer persistent relationships remain separate work. Snapping to traced coordinates does not upgrade their source confidence or certify site accuracy.

## Verification

Application revision: 3579e10a59da869a7cedb5719e7fd003ed9c32c8.

[Unit/build run 34626951272](https://github.com/CRisdon45/Plan_Trace/actions/runs/34626951272) completed 213 tests, zero failures/errors/skips, and debug assembly. The downloaded XML independently totals 30 suites, including 13 new geometric-reference tests and five new integration tests above the previous 195. Unit artifact 10275340787 ZIP SHA-256 matches da0fa16cca6fc20897a3fd839147a1b4447ea4a5eb305780628c94e61b5f28d7. The successful log still contains the inherited KSP/AWT background exception and service/deprecation warnings; no tooling fix is claimed.

A local exploratory harness executed the actual production geometry code through 792 arc-projection cases plus midpoint and constrained-line checks. This is supplementary math evidence, not part of the 213-test count and not a local Android build.

[Android run 34626951182](https://github.com/CRisdon45/Plan_Trace/actions/runs/34626951182) attempt 1 built the actual app/instrumentation, then the first scenario stopped because the screenshot guard detected a Pixel Launcher ANR dialog over the app. The raw screenshot and active-window tree were inspected. This is not an accepted geometry-snapping scenario or a clear visual capture. Failed artifact 10275575262 ZIP SHA-256 matches 40e7d1851af4d6f1406a39ff94aa563d708fc339f084e153737031badd4c7d26. The same job was retried on the unchanged application revision without dismissing the error or weakening assertions. No Pixel Launcher root-cause repair is claimed.

The unchanged application completed [Android run 34626951182 attempt 2](https://github.com/CRisdon45/Plan_Trace/actions/runs/34626951182/attempts/2), job 103357251355: app/instrumentation builds and all 13 scenario tests passed. The two new scenarios add actual corner/vertex snapping and a separate process-restart check to the previous eleven. Downloaded result files each report OK (1 test); all 29 active-window capture records identify the application. The new snapped-line, snapped-vertex and reopened screenshots were opened and were unobstructed. The crash buffer was empty, not proof of all-flow crash freedom.

The test places a deck boundary at exact protected house corners while grid snap is also enabled, confirms small pointer movements retain the same corner and live dimension without resizing the canvas, commits the indicated point, and checks Undo/Redo. It then moves one of the new deck's vertices onto a different house corner, cancels and repeats the edit, and verifies the original six objects, source registration and house lock remain unchanged. This is deliberately overlapping synthetic geometry, not a client layout. The new scenario directly checks corner attraction and vertex edits; edge/arc/midpoint/alignment variants have unit coverage rather than their own separate physical-device acceptance.

The final seven-object document is format 5, revision 51 and 7,747 bytes. Its completed-save before/after process-restart files are byte-identical, SHA-256 37f33f9f846bcd19a17605207e4398e9700e45eb9039c4f96344d4883ab7ca1d. The prior six objects and image registration are exactly preserved. The new triangle's corners match the intended house corners; no snap constraint or live cue is serialized. The explicit device preference survives reopening, while the temporary target/guide does not. This does not prove recovery of work killed before saving completes.

Verified replay artifact 10274873579 ZIP SHA-256: ef9f3ca5ea481442767f216688a3bcc7dc96286da3b1f1901b644b4875585ba1. This is the later artifact from attempt 2, not the identically named failed-attempt archive. Snapped-line PNG: 705a41b9896d6149e8ad9c906fdddcfbc2ae69a163afe6ec177831a46efb609b. Snapped-vertex PNG: e30d2bd44c8d5e0db6c23f338c16691896de3b0c63cc2dfc22c09af78a6f077c. Reopened PNG: fc85b4e78674ad11150d568088454bbf221079be76ec4a91698cc1184b254d35. Evidence retention is seven days, not permanent storage.

The Android environment is API 35 x86_64; new snapping scenarios use landscape 1600x1000/density240 and explicit Touch edit. Retained command-layout cases also run portrait and compact configurations. The current bubble and verbose live labels are still visible in captures: they were not polished in this functional pass. No separate snapped-seven-object export or physical stylus scenario was added; earlier direct-output checks remain in the full replay.

## Next outcome and safety

Next bounded outcome: manipulate an entire straight pool side with the pen, preserving the intended adjoining geometry, following coping, useful alignment, live measurements and one-operation Undo. This is not implemented by the snap resolver alone. Keep UI cleanup as a coherent pass over usable draw/snap/reshape behavior, rather than polishing isolated controls or postponing the visual bar until every landscape feature exists.

Physical S Pen/palm/hover delivery, hand occlusion, latency and full drafting across all screen sizes remain unverified. Current flat rendering, temporary explanatory controls and export-label collisions remain presentation work. No shared-deck topology, setbacks, quantities, full-yard readiness or Northstar acceptance is claimed.

Only original synthetic data is used. No physical installation, original-app/data clearing, signing workaround, application PR merge, release/APK publication, private client/reference publication, visibility/permissions change, runtime AI, 3D or framework/repository merger occurred. Older checkpoints remain historical evidence; CURRENT_STATE owns the next-session route.
