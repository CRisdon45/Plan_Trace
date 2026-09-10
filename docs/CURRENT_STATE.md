# Current state and next-session handoff

Updated 2026-09-10 after the first project-owned geometry slice. Verify live refs and local work before changing branches or merging. The physical-tablet test remains interrupted/unavailable; independent implementation is authorized, but device acceptance is not passed.

## Branch routing

**Current implementation: `feat/project-geometry-seam`, PR #4 into `feat/off-tablet-integrity`.** It is stacked above PR #3's export/measurement fixes, which target `feat/2d-foundation-northstar` (PR #2 into main). The working application revision for the geometry slice is `0d18c20e6dbef82948d27400dc3803fb3a53b9dd`; later documentation commits do not change those results. None of these application PRs was merged in this session.

`main` carries product/context documents and a routing checkpoint, not the unfinished application. The tablet/foundation code has not been changed by this geometry work. Do not install anything, replace a signing key, uninstall the app, clear storage or disrupt the ongoing device-test session to simplify development.

## What exists now

A storage-neutral ProjectDesign core owns typed objects, exact line/circular-arc boundaries and stable object/vertex/edge IDs independently of PDF sheets. Commands support preview, add/remove/translate, vertex/arc/boundary changes and whole-command Undo/Redo, preserving locks and unrelated objects. Versioned JSON preserves the new authority and rejects unsupported/corrupt input without partial recovery masquerading as a complete yard.

A read-only adapter feeds the EXISTING renderer/PNG export path with bounded curve sampling and Float conversion, keeping analytic perimeter labels independent of sampling. Internal metric/y-up coordinates do not change the feet/inches product requirement; labels default to imperial. These APIs are compiled/tested but are NOT yet wired into the ordinary drawing UI or the Room/automatic-save repository. Legacy projects and their per-page storage remain unchanged.

## Evidence

- Geometry refinement `0d18c20` / run 34512772848: **78 tests passed, zero failures/errors/skips; Android debug build succeeded**. The 29 new tests cover exact curves/IDs/numerics, session history/locks, JSON and real PNG output. Downloaded XML totals independently matched the job log.
- Initial geometry `3bf9200` / run 34512053545: 76 tests passed and debug assembly succeeded.
- Earlier off-tablet patch `e7e75e8` / run 34497580131: 49 tests passed and debug assembly succeeded; see OFF_TABLET_IMPLEMENTATION.md.
- Two authentic synthetic PNGs were inspected for technical outline integrity, not style approval. Label toggling currently changes legacy auto-fit framing. Fixed export frames are still needed for pixel-stable comparisons.
- The earlier KSP/AWT exception recurred in the initial geometry run, but was absent from the final retrieved log. It was not deliberately fixed; keep the toolchain follow-up. Existing service/deprecation warnings persist.

See [PROJECT_GEOMETRY_IMPLEMENTATION.md](PROJECT_GEOMETRY_IMPLEMENTATION.md) on this branch for pinned revisions/runs, artifact hashes and limits. A separate exploratory local script exercised the actual geometry on randomized arcs; it is not part of the 78-test CI count. Physical pen/button/palm checks, tablet usability, Northstar quality, new-path PDF visual review and integrated project recovery were not performed.

## Next bounded outcome

Connect this authority to one small **actual on-canvas editing and safe persistence flow**, exercised with both straight and concave/convex synthetic shapes. Do not grow a disconnected geometry laboratory or build a parallel demo renderer. Use stable node/edge identity for hit targets and commands, with explicit source/view registration. Preview, commit, Undo, save/reopen and export must refer to the same authority.

Before introducing filled surfaces, area takeoffs or following coping, establish topology validation and the required offset behavior, including concave failure cases. Current signed area is algebraic only, and the adapter deliberately draws outlines rather than implying valid pool/deck regions. Tangency is diagnosed, not automatically constrained. Keep these dependencies scoped to the first useful workflow instead of building a universal CAD solver.

No automatic migration or reinterpretation of legacy strokes/material colors is authorized by the new types. Preserve old drawings and establish a reviewed opt-in transition before connecting stored documents. Do not save the disposable TraceProject output projection as the canonical yard. Consider fixed output frames before using label-on/off pixels as geometry comparisons.

## Remaining acceptance and boundaries

Finish the interrupted tablet session on its known build when the device becomes available, then deliberately test a newer build. Q0 still needs physical export-control usability, latest courtyard composition, real source-file/reopen behavior and PNG/PDF visual checks. These checks remain pending, not blockers for all independent implementation.

Connected coping/shelves/steps, shared surfaces, true associative dimensions, primary radial interaction, source calibration/registration, complete project options, integrated recovery and Northstar rendering remain unfinished. Q1 is partial, not complete. Private references stay outside the public repo; use original synthetic cases. No runtime AI, no framework restart or repository merger; 3D stays paused. The inherited runtime-service dependency audit and exact Estimator/Design-Platform interchange are still separate work.

Keep this handoff concise and replace stale status at the next meaningful checkpoint. Record actual evidence and source revisions, not aspirations. Preserve concurrent changes and original user data; review every published diff, log and artifact for private content.
