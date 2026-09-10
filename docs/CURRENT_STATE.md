# Current state and next-session handoff

Updated 2026-09-10 after the first connected pool/coping milestone. **Main contains this routing context, not the unfinished application changes.** Independent implementation and GitHub virtual-device testing are authorized while the physical tablet is unavailable. Preserve its interrupted test, app/data and signing key. Verify live refs and concurrent changes before working.

## Where to continue

Active branch: **feat/project-geometry-seam**, PR #4 into feat/off-tablet-integrity. PR #3 targets feat/2d-foundation-northstar, and PR #2 targets main. No application PR was merged in this session.

Read the [active checkpoint](https://github.com/CRisdon45/Plan_Trace/blob/feat/project-geometry-seam/docs/CURRENT_STATE.md). Saved documentation checkpoint: **f8e7bde4692a7dbeb04200154e4a3b39143af03f**. Tested application revision: **f0f1892f89d6113710172b54213e09e306c32043**. Later documentation changes are not new build/test claims. The active branch's decision record contains initial geometry/coping choices D13-D14; they are revisable technical choices, not new user requirements.

## Latest implemented outcome

Projects -> Design workspace · preview has one separately saved local draft with exact pool lines/arcs, direct vertex/curve/body editing, Undo/Redo, Delete/Undo, Fit and perimeter in feet. **New pools now include following coping.** Reshaping regenerates the band from the pool's saved width/generator intent; it is not a separate editable polygon. Width entry in inches commits on keyboard Done, without Apply/Accept. Unsupported crossing geometry, consumed inside arcs and bridged narrow recesses are rejected without replacing the saved design.

The local JTS-backed generator uses an explicit sampled approximation and guards, not an exact analytic topology proof or construction approval. No area takeoff, other-object clearance/deck exclusion or per-edge material treatment is claimed. The existing renderer displays flat water/stone; it is not the finished Northstar style.

Format 2 saves coping intent and reads version-1 outlines unchanged. Old outlines require explicit attachment; no silent promotion. Later format-2 saves cannot be read by old format-1-only builds. Legacy Room projects are separate and unchanged. The opt-in draft uses serial verified atomic saves, but is not portable backup or multiple-project management. Undo history is session-local.

## Verified evidence

[Unit/build run 34523944344](https://github.com/CRisdon45/Plan_Trace/actions/runs/34523944344) at f0f1892: **111 tests, zero failures/errors/skips; debug assembly succeeded**. Downloaded XML totals matched the log. This is the previous 90 plus 21 new coping/document/output tests.

[Actual Android run 34523944339](https://github.com/CRisdon45/Plan_Trace/actions/runs/34523944339) at the same revision: application/instrumentation builds and **two emulator scenarios passed**. They checked straight/curved pool reshaping with coping, unrelated objects, navigation, cancellation, Undo/Redo/Delete recovery, 16-inch width, rejection of a 48-inch band and crossing edit, saved reentry, force-stop/relaunch and activity recreation. Canonical files were byte-identical after restart, after waiting for verified Saved.

Native PNG and final recreated/rejection captures were inspected. An earlier green emulator run had a Pixel Launcher ANR overlay; the final reviewed captures were unobstructed, but no launcher root-cause fix was made. A real translation-stability defect was found and corrected by localizing geometry before sampling, with the strict test retained. KSP/AWT background exceptions and inherited service/deprecation warnings still exist despite successful builds. Do not equate green checks with an exception-free toolchain, clear screenshots or physical pen acceptance.

Scope, budgets, failures and artifact hashes: [coping implementation report](https://github.com/CRisdon45/Plan_Trace/blob/f8e7bde4692a7dbeb04200154e4a3b39143af03f/docs/COPING_IMPLEMENTATION.md). Earlier reports remain historical evidence.

## Next outcome and boundaries

Bring primary radial commands and exact pool-dimension editing into this connected workspace. Keep stable command directions, visible fallback access, deliberate commit, and the same command/Undo/save authority. Test software behavior virtually; physical S Pen/palm/hover/barrel-button feel remains pending. Connected shelves/steps and shared decking should reuse this model rather than independent marks.

Source/site registration, arbitrary outline creation, tangent assistance, associative dimensions, alternatives, portable recovery, full landscape scope and Northstar appearance remain unfinished. New-screen export/share was not included in the emulator scenarios; final PDF, portrait/handedness, pinch synthesis and hardware performance remain unverified. Fixed output frames and system-bar contrast are follow-ups.

No tablet installation, original-app replacement, data clearing, release, installable APK publication, permissions/visibility change or 3D work occurred. CI uses disposable virtual devices. Private client originals and unapproved Northstar images remain outside public Git, issues, logs and artifacts. No runtime AI feature is introduced; the inherited runtime-service audit and exact Estimator interchange remain separate work. Update this routing file and the active checkpoint with real evidence at the next meaningful outcome.
