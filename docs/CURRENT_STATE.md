# Current state and next-session handoff

Updated 2026-09-11 after protected existing-site outline authoring. Active work: **feat/project-geometry-seam**, PR #4 into feat/off-tablet-integrity; PR #3 targets the 2D foundation, PR #2 main. Verify live refs and concurrent changes. No application PR was merged and the interrupted physical tablet test, installed app, data and signing key were not changed.

## Owner direction

Pen manipulation and dependable snapping are the primary workflow. Cody rarely wants to type dimensions; numeric text remains an occasional fallback. Do not turn new drawing features into forms. Current flat graphics and temporary controls are not the final Northstar presentation or interaction design. No runtime AI; 3D stays paused.

## Latest verified outcome

Tested application: **571a28c97c9ad8be7666f25eb5bbba5d83fc94a9**. Unit/build run **34616514170** passed **182 tests, zero failures/errors/skips**, and debug assembly. Android run **34616514212** built the real app/instrumentation and passed **nine scenarios**. Downloaded XML and scenario results were checked, along with all 20 active-window capture records. Actual reopened, raster-hidden, registration-warning and vector-export captures were opened.

The new four-object design is format 5, revision 41 and 4,955 bytes. It was byte-identical across completed-save force-stop/reopen. The new 17 unit tests cover site geometry/provenance/history/storage and stable notice layout; two new virtual-device scenarios extend the previous seven. SITE_OUTLINE_IMPLEMENTATION.md contains run links, hashes, exact coverage and limitations.

## What this adds

View -> Site -> House or Property starts a transient corner sequence over a visible calibrated source. Tap or drag to each corner, optionally use right-angle assistance relative to the first segment, then close by tapping the first corner or choosing Close outline. No typed dimensions are required. Back a corner/Undo modifies only the unfinished sequence; Cancel or workspace/lifecycle exit discards it. Closure commits one validated straight-edged object and one document Undo step.

Closed site objects are TRACED, locked by default, and distinct from proposed pool/landscape objects. Select their chip and use Site controls to deliberately unlock, edit and relock. Stable IDs, original image registration and adjusted-after-tracing evidence survive saving. Moving/rescaling/removing a source never silently moves these objects; it reports an alignment-review or unavailable-source notice. Hiding the source leaves actual house/property vectors visible through the same renderer and output model.

The first Android replay exposed a real interaction defect: a preview-driven mismatch warning resized the canvas and canceled source movement. Status now uses committed facts, with two layout regressions and a successful full replay. No failed assertion was dropped. The export review also found the house notice touching the property outline; label collision handling remains an explicit presentation gap, not accepted polish.

## Existing scope and compatibility

The same workspace retains connected pool/coping edits, radial commands, source-image intake/calibration, independent distance comparisons and serial verified atomic saves. Format 5 reads 1–4 without inventing site identities; older builds cannot read later saves. Legacy Room projects remain separate. One draft, session-only Undo and app-owned images are not multi-project or portable recovery. Tests terminate only after verified Saved.

## Next bounded outcome

Use this editable site to create proposed pool/deck geometry **where the pen indicates**, instead of always inserting a predefined starter. Preserve following coping, stable IDs, the protected existing site, dependable snapping, one-operation Undo and save/reopen/output. Reuse this authority and renderer; do not grow another disconnected tracing editor, a generic toolbar or a keyboard-first workflow. Keep the Northstar bar visible alongside the next useful authoring slice.

## Remaining limits and safety

Site outlines currently have 3–128 straight-edged corners, not curved/open wall paths, automatic image recognition or a survey. Final-closing-edge orthogonality is not solved automatically. PDF source intake, rotation, broader snapping, attached shelves/steps, shared surfaces, alternatives, multiple-project recovery and full landscape design are unfinished. Physical S Pen/palm/barrel delivery, draft authoring in portrait/compact views, full accessibility/handedness, hardware performance, fixed output frames and Northstar quality remain unverified. Existing portrait/compact command tests are not full drafting acceptance.

Only disposable emulator data was installed/force-stopped. No user-data clearing, signing workaround, release/APK publication, private client/reference publication, permission change, runtime AI, framework/repository merger or 3D work occurred. Prior tooling/service warnings remain open absent a cause-specific fix. Keep main's router and this checkpoint tied to successful read-back-verified commits, with historical reports as evidence rather than competing directions.
