# Current state and next-session handoff

Updated 2026-09-10 after connected pool/coping verification. The owner authorizes independent development and GitHub virtual-device testing while the physical tablet is unavailable. Keep that interrupted tablet session and its data/build/signing key untouched. Verify live refs and local work before editing or merging.

## Active work

**feat/project-geometry-seam, PR #4 into feat/off-tablet-integrity.** PR #3 targets feat/2d-foundation-northstar; PR #2 targets main. None of these application PRs was merged this session. Main carries a routing checkpoint, not the new app. Tested application revision: **f0f1892f89d6113710172b54213e09e306c32043**. Later documentation-only commits are not new validation claims.

## What now works

Projects -> Design workspace · preview opens the separately saved local draft. It has exact straight/circular-arc pool geometry, direct vertex/curve/body editing, whole-command Undo/Redo, Delete/Undo, Fit and perimeter in feet through the existing renderer.

**New pool starters include following coping.** Width and generator version belong to the pool; the outer footprint is regenerated rather than independently edited/saved. Reshaping retains that relationship. Inline inches entry commits on keyboard Done. Crossing outlines, consumed inside arcs, ambiguous near touches and coping that bridges a narrow recess are rejected without replacing the saved design. Existing outline-only drafts are not silently promoted; attachment is explicit.

The first implementation uses pinned local JTS 1.20.0 with sampled topology/offset checks and explicit guards. Exact pool arcs remain authoritative. This is resolution-limited planar validation, not construction approval or a universal analytic offset engine. No area takeoff, cross-object site/deck exclusion or per-edge coping treatment is claimed. See COPING_IMPLEMENTATION.md and decision D14.

Canonical JSON is now format 2, reading version 1 without changing its outlines. Subsequent saves use version 2, which old format-1-only builds cannot read. Legacy Room plans are separate and untouched. The draft uses serial AtomicFile writes and verified Saved status; it is not portable backup, multi-project management or persisted Undo history. Tests wait for completed saving before force-stop.

## Evidence and limitations

- Unit/build run **34523944344** at f0f1892: **111 tests passed, zero failures/errors/skips; debug assembly succeeded**. Downloaded XML totals matched the job summary. This is the previous 90 plus 21 new coping/serialization/output tests.
- Emulator run **34523944339** at the same revision: app/instrumentation builds and **two actual Android scenario tests passed**. Covered straight/curved reshape with following coping, navigation, cancellation, Undo/Redo/Delete recovery, 16-inch width, rejected 48-inch width and crossing drag, save/reentry, force-stop/relaunch and activity recreation.
- Canonical files were byte-identical before/after process restart at revision 9. Final native PNG and recreated-workspace/rejection screenshots were opened. Water/coping rendering and retained width were visible. This is flat Graphic evidence, not Northstar or physical-tablet acceptance.
- Initial comparison fixes were insufficient: a real translation-stability defect required localizing the source boundary BEFORE sampling. The strict regression now passes. Earlier failed runs are recorded, not relabeled as successes.
- A prior green emulator run had a Pixel Launcher ANR overlay in its captures; final reviewed captures were unobstructed. No launcher root-cause fix was made. Do not equate green semantic tests or empty crash buffers with visual acceptance. KSP/AWT exceptions persist in the successful unit/build log; inherited service/deprecation warnings and system-bar contrast remain open.

[COPING_IMPLEMENTATION.md](COPING_IMPLEMENTATION.md) contains pinned runs, artifact hashes, geometry budgets, exact scope and failures. Earlier workspace/geometry/export reports are dated evidence, not current instructions. No new-screen export/share emulator scenario, PDF visual acceptance, physical S Pen/palm/hover/button validation, pinch synthesis, portrait/handedness or hardware performance claim is made.

## Next bounded outcome

Bring **primary radial commands and exact pool-dimension editing** into this connected workspace, not another large generic toolbar. Keep common directions stable, visible fallback access, intentional commit and the same command/history/save authority. Verify virtual software behavior and leave actual barrel-button feel explicitly pending. Connected shelf/step and shared-surface operations should follow this model rather than become unrelated marks.

Maintain visual quality alongside useful editing, but do not call current flat fills Northstar. Source registration/site creation, arbitrary outline drawing, tangent assistance, shelves/steps, shared decking, associative dimensions, project alternatives, portable recovery and full landscape scope remain unfinished. Fixed export frames are needed because existing auto-fit changes framing with labels.

## Boundaries for resumption

Finish the interrupted physical test on its known build before deliberately switching versions. No uninstall, storage clearing or signing-key workaround; CI uses disposable emulator installs and does not publish replacement APKs. Check format compatibility and preserve drafts before any future downgrade.

No runtime AI feature, 3D development, framework restart, repository merger or permission change is included. Client/company originals and unapproved Northstar images remain private. Runtime-service dependency cleanup and exact Estimator interchange are separate work. Update this checkpoint and main routing at meaningful outcomes with actual revisions/results; retain uncertainty and protect concurrent work.
