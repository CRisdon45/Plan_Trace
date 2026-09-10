# Current state and next-session handoff

Updated 2026-09-10 after the first editable workspace and successful virtual Android testing. Verify live refs, PRs and local changes before working. Implementation is authorized without physical tablet access. The interrupted physical-device test is still pending, not a global development block or a passed acceptance gate.

## Active branch and preserved baseline

**Work on `feat/project-geometry-seam`, PR #4 into `feat/off-tablet-integrity`.** PR #3 targets the 2D foundation branch; PR #2 targets main. No application PR was merged in this session. The foundation/tablet app, its signing key, data and ongoing test were not changed. Main receives a routing checkpoint, not these application changes.

Tested application refinement: `b4fb724cba3f48d18c394df0e54fbec92a6e8a66`. The emulator-verified revision `b01cec4698262086a07fecc11b2696dc4cdfed20` contains the same application/tests with an AVD-script repair. Later documentation commits do not create new application-validation claims.

## What is now usable in the development branch

**Projects -> Design workspace · preview** opens one separately saved local design draft. Add original straight or concave/convex pool outlines and a paving outline; select objects, drag vertices/curve handles/boundaries, Undo/Redo, Delete/Undo, Fit, and see analytic perimeter in feet. Preview remains transient; completed edits use the canonical ProjectDesign command path. The existing WatercolorRenderer draws its disposable projection, not a second editable document or demo renderer.

Serial background AtomicFile writes preserve exact canonical JSON and verify readback before Saved is shown. Stale/conflicting writes and corrupt existing documents are rejected rather than silently overwriting/resetting. This opt-in draft is separate from legacy Room projects; no migration occurred. Undo history is session-only. One local draft is not multi-project management, portable backup or a guarantee for edits killed before the save completes.

The workspace has an explicit Touch edit mode; fingers otherwise navigate. Physical pen, palm, hover and barrel events remain unverified, and the primary radial menu has not yet been built. Current controls and outline-only appearance are an integration preview, not the approved final UI/Northstar.

## Evidence actually obtained

- Final unit/build run 34518645007 at b4fb724: **90 tests passed, zero failures/errors/skips; debug assembly succeeded**. Downloaded XML totals were checked. These include twelve new editing/storage tests above the previous 78.
- Actual Android run 34518920691 at b01cec4: **two instrumentation scenarios passed** on API35 x86_64 with 1600x1000 / density240 override. They exercise creation, finger navigation, vertex and curve drags, cancellation, Undo/Redo/Delete recovery, local save, screen reentry, force-stop/relaunch and activity recreation. Canonical files before/after process restart were byte-identical after waiting for verified Saved.
- Actual screenshots were inspected for screen/outline integrity. They are not synchronized pixel goldens or Northstar acceptance. Low-contrast system-bar icons after recreation and capture synchronization remain polish follow-ups.
- The first AVD startup failed before app tests and was superseded. Explicit AVD paths and bounded diagnostics repaired the test harness. KSP/AWT background exceptions still appeared during the successful emulator build; the toolchain follow-up and service/deprecation warnings remain.

[WORKSPACE_IMPLEMENTATION.md](WORKSPACE_IMPLEMENTATION.md) owns the pinned runs, hashes, exact scenario coverage and limits. PROJECT_GEOMETRY_IMPLEMENTATION.md and OFF_TABLET_IMPLEMENTATION.md remain dated earlier evidence, not today's capability summary. No local Android build or physical device acceptance is claimed.

## Next bounded outcome

Advance one real pool-boundary/coping edit through this workspace: validate the boundary and offset result, follow the selected pool perimeter, handle concave/invalid cases clearly, and preserve related intent through preview, one Undo and save/reopen. Use the same authority and real renderer. Keep exact numeric manipulation and primary radial access near this workflow; do not grow a detached geometry lab or a generic drawing-toolbar product.

Before filled surfaces or takeoff quantities, establish topology/offset validity. The current core reports algebraic area only, not a certified surface. No source-page registration, arbitrary drawing path, automatic legacy promotion or shared-boundary model exists yet. Do not save the disposable TraceProject projection as the canonical yard. Fixed export frames remain needed because existing auto-fit changes framing when labels toggle.

## Pending checks and constraints

The new-screen export/share action is wired but not exercised by the emulator scenarios. Physical S Pen/palm/button feel, pinch synthesis, portrait/handedness, hardware performance, latest courtyard acceptance and final PNG/PDF visual checks remain pending. Finish the interrupted physical test on its known build before deliberately switching versions. No uninstall or data clearing to work around signing differences; CI uses ephemeral keys and does not publish replacement APKs.

Connected steps/shelves, shared surfaces, full site setup, options, multi-project recovery, associative dimensions, primary radial menus and Northstar appearance remain unfinished. Exact Estimator interchange and inherited runtime-service cleanup are separate work. No runtime AI, no framework restart or repository merger; 3D remains paused. Client originals and unapproved Northstar pixels stay outside public Git/PRs/logs/artifacts.

Replace this checkpoint at meaningful milestones with the actual application revision, checks, limitations and next outcome. Update the main routing file when the active branch/checkpoint changes. Preserve concurrent work and user data; review every publication for private material.
