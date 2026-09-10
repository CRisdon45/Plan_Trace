# Current state and next-session handoff

Updated 2026-09-10. **Main carries this routing checkpoint, not the unfinished application changes.** The owner authorized continued implementation without physical tablet access and virtual testing through GitHub. Check live refs, PRs and local work before changing anything.

## Where to continue

Active implementation is **`feat/project-geometry-seam`, PR #4 into `feat/off-tablet-integrity`**. PR #3 targets `feat/2d-foundation-northstar`, and PR #2 targets main. None of these application PRs was merged in the workspace session. The original tablet/foundation application, its data/signing key and interrupted test were not changed.

Read the [active checkpoint](https://github.com/CRisdon45/Plan_Trace/blob/feat/project-geometry-seam/docs/CURRENT_STATE.md). Saved context is `2a8ff72e3a046aedc3bdeb386d80e5d84e5f8c30`. Application refinement `b4fb724cba3f48d18c394df0e54fbec92a6e8a66` passed the unit/build suite. Revision `b01cec4698262086a07fecc11b2696dc4cdfed20` has identical app/test sources with a repaired emulator script and passed actual Android scenarios. The context commit changes documentation only.

## Latest outcome

**Projects -> Design workspace · preview** now opens one independently saved local draft inside the real app. Add straight or concave/convex pool outlines and paving outlines, select and drag vertices/curve handles/boundaries, Undo/Redo, Delete/Undo, Fit and see perimeter in feet. It edits canonical ProjectDesign geometry and uses the existing renderer, not a separate illustration implementation. Explicit Touch edit permits touch authoring; fingers otherwise navigate.

Serial atomic file writes save exact canonical JSON in a location separate from the old Room projects. Saved means the current revision was written and read back successfully. Stale/conflicting writes and corrupt input do not silently overwrite/reset the draft. Legacy projects were not migrated. One draft is not project-gallery integration or portable backup; Undo history is session-local, and restart evidence covers completed saves, not a process killed before saving finishes.

## Verified evidence

[Unit/build run 34518645007](https://github.com/CRisdon45/Plan_Trace/actions/runs/34518645007): **90 tests passed, zero failures/errors/skips, debug assembly succeeded**. Downloaded XML totals were checked.

[Virtual Android run 34518920691](https://github.com/CRisdon45/Plan_Trace/actions/runs/34518920691): app/instrumentation builds and **two separately invoked scenario tests passed**. API35 x86_64 at 1600x1000 / density240 exercised creation, finger navigation, vertex/curve edits, cancellation, Undo/Redo/Delete recovery, saved-screen reentry, force-stop/relaunch and activity recreation. Canonical files after verified Saved were byte-identical before/after process restart.

Actual screenshots were opened for screen/outline integrity. They are not settled-frame pixel goldens or Northstar acceptance. System-bar icon contrast after recreation and screenshot synchronization remain polish follow-ups. The first AVD startup failed before app tests; explicit device paths and bounded diagnostics repaired the harness. KSP/AWT background exceptions still appeared during the successful emulator build; root cause and inherited service/deprecation warnings remain tracked.

Detailed evidence, artifact hashes and scope: [workspace implementation report](https://github.com/CRisdon45/Plan_Trace/blob/2a8ff72e3a046aedc3bdeb386d80e5d84e5f8c30/docs/WORKSPACE_IMPLEMENTATION.md). Earlier geometry/export reports remain historical evidence, not current capability summaries.

## Next bounded outcome and remaining limits

Use this actual editing/save flow to establish a valid pool-boundary and following-coping operation with straight and concave cases, explicit invalid-result handling, one Undo and preserved intent on reopen. Keep exact numeric manipulation and primary radial commands close to that workflow instead of expanding a generic toolbar or isolated geometry laboratory.

Topology/offset validity must precede filled surfaces and takeoff area. Current signed area is algebraic only. Source/site registration, arbitrary outline creation, attached shelves/steps, shared surfaces, associative dimensions, multi-project recovery, primary radial menus and live Northstar appearance remain unfinished. The new-screen export/share action is wired but was not included in the emulator scenarios. Physical S Pen/palm/barrel behavior, pinch synthesis, portrait/handedness, hardware performance and final output visual acceptance remain pending.

Finish the interrupted physical tablet test on its known build before deliberately changing versions. No uninstall, data clearing or signing-key replacement. CI creates only disposable emulator installs and does not publish APK replacements. Private client plans/photos and unapproved Northstar pixels stay out of public Git, PRs, logs and artifacts. The fixtures/screens are original synthetic work. No runtime AI or 3D work was introduced; 3D stays paused, and the inherited runtime-service audit remains separate work. Repository visibility and permissions are unchanged.

Keep this routing file and the active checkpoint current when branches or verified outcomes change. Preserve concurrent work and the personal client-design goal.
