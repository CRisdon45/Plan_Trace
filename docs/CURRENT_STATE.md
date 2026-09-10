# Current state and next-session handoff

Updated 2026-09-10 at the start of radial interaction review. Work is authorized without physical tablet access. Keep its interrupted test, original app/data/signing key untouched. Verify live branches, PRs and concurrent edits before changing anything.

## Active branch and tested baseline

Work on `feat/project-geometry-seam`, PR #4 into `feat/off-tablet-integrity`. PR #3 targets the 2D foundation; PR #2 targets main. No application PR is merged by this checkpoint. Main's routing may still point to the preceding coping milestone until this review finishes.

Application baseline: `571af40330c48116573703c127e08f1b872b77e7`. This documentation update does not change it. It includes two-level radial commands and exact-size editing in addition to the connected pool/coping workspace. The prior session reported 126 passing unit tests and successful debug assembly in run 34530450794. Those tests have not been rerun in this new session.

## What exists

Projects -> Design workspace opens one separately saved draft, not a replacement for legacy Room projects. Exact line/circular-arc authority drives actual canvas vertex/curve/body edits, coping, command Undo/Redo and verified atomic saving. Format 2 reads version-1 outline drafts; old format-1-only builds cannot open later format-2 saves. Undo history remains session-local; one draft is not portable backup or full project management.

The new Commands wheel has stable Draw/Edit/View/Assist/History/Select positions and a short outer fan. It replaces the permanent secondary tool row. Commands operate on the same document; disabled commands retain their positions. Center steps back/closes, outside dismissal does not commit, edge placement shifts without rotating, and limited-space/large-text layouts have a list fallback. Selection is tap-based, not continuous hold-and-flick. A visible Commands button and software stylus-button invocation coexist; physical S Pen feel remains pending.

Grid display and one-foot snapping are distinct saved view preferences. Snapping affects vertex positions and movement deltas, not existing geometry, curves or exact numeric entry. Edit -> Size opens temporary feet/inches/fraction fields. Rectangles resize independently about corner 1 along their own axes. Other outlines scale uniformly about vertex 1; independent freeform stretching is not implemented. Coping width stays unchanged and invalid geometry is rejected.

## Review completed in this session so far

Downloaded emulator artifact 10173523961 from run 34530450760 at 571af403. Verified ZIP SHA-256 `bc454ffdb07fdae7e95a395a3260c011526bcfbb04b758f0fb319e23f12e94d6`. Both scenario result files report OK (1 test); saved-before/after process files are byte-identical, SHA-256 `b978831af7052abbfb73e85783fa9040e5e2596ce2858f6b4ccae2e2d6922e30`.

Opened actual Edit-fan and Assist-fan captures: the categories, child actions and on/off state are visible without a system-dialog obstruction. These are an early flat graphic UI, not final Northstar acceptance or usability/performance certification. No new emulator run, physical-device test or feature-completion claim is implied by this review.

## Next bounded outcome

Harden the existing radial/exact-edit workflow under touch/stylus invocation, cancellation, grid snapping and restricted screen space before expanding features. Use the real application with original synthetic geometry. Add focused checks where software acceptance is missing and inspect resulting captures. Finish a concise RADIAL_IMPLEMENTATION report and align this checkpoint, main routing and PR metadata with verified results.

The current code still has limited-space, physical pen, portrait/handedness and new-screen export coverage gaps. Source/site registration, arbitrary outlines, tangent assistance, attached steps/shelves, shared surfaces, associative dimensions, alternatives, full-project recovery and live Northstar remain unfinished. Do not convert current uniform freeform scaling into a claim of intelligent reshaping. Existing output auto-fit changes framing with labels; fixed export frames remain work.

## Preserved limits

Coping uses pinned local JTS 1.20.0 and resolution-guarded derived geometry, not construction certification or area takeoff. Earlier reports preserve geometry budgets and failures. The KSP/AWT background exception and service/deprecation warnings are not resolved merely because builds pass. Test captures must be reviewed for launcher/system overlays, not inferred from a green badge.

No runtime AI feature, 3D work, framework restart, repository merger, physical installation, release, APK publication or permission change. Client originals and unapproved reference artwork stay out of public source, PRs, logs and artifacts. Keep actual revision, evidence and next outcome current rather than accumulating stale next-step lists.
