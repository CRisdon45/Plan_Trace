# Current state and next-session handoff

Updated 2026-09-10 after radial-menu and responsive interaction verification. **Main contains this routing context, not the unfinished application changes.** Independent development and GitHub virtual-device testing remain authorized; the physical tablet's interrupted session, app, signing key and data remain untouched. Verify live refs and concurrent work before writing.

## Where to continue

Active branch: **feat/project-geometry-seam**, PR #4 into feat/off-tablet-integrity. PR #3 targets feat/2d-foundation-northstar; PR #2 targets main. No application PR was merged in the radial review session.

Read the [active CURRENT_STATE](https://github.com/CRisdon45/Plan_Trace/blob/feat/project-geometry-seam/docs/CURRENT_STATE.md). Saved documentation checkpoint: **786f644e1ec12f7f50bc28d738d13f36178d7e1c**. Tested application revision: **27ebdb21c9027b29aa0c0104a2dca8e4a45094da**. Documentation commits do not create new application-validation claims. The active decision record includes revisable implementation choices D13-D16.

## Current workflow

Projects -> Design workspace · preview opens one separately saved draft with exact pool lines/arcs, connected coping, direct editing, Undo/Redo, feet-based perimeter and verified atomic saving. The existing renderer draws the canonical project's disposable projection, not another editable yard. Sampled coping validity is not construction approval or a complete takeoff engine.

A two-level radial menu now supplies stable Draw/Edit/View/Assist/History/Select categories and short outer fans. Disabled actions keep their slots. Commands remains visible in responsive headers. The wheel shifts inward at edges without rotating directions; small-space/large-text fallback uses a matching list with Back/Close pinned above scrolling actions. This is tap-to-select, not continuous hold-and-flick. Stylus generic-event routing has synthetic software coverage, not physical S Pen certification.

Edit -> Size accepts feet, marked inches and fractions. Rectangles resize independently along their own axes with corner 1 fixed; other outlines currently scale uniformly by an overall X/Y span to preserve circular arcs. Coping width remains unchanged. Keyboard Done commits once; canceled/untouched values do not mutate geometry. Visible grid and one-foot vertex/movement snapping are independent preferences, not the full alignment/endpoint snap system.

## Verified evidence

[Unit/build run 34534238423](https://github.com/CRisdon45/Plan_Trace/actions/runs/34534238423), application 27ebdb2: **137 tests passed, zero failures/errors/skips; debug assembly succeeded**. Downloaded XML totals checked.

[Android run 34534239074](https://github.com/CRisdon45/Plan_Trace/actions/runs/34534239074), same application: app/instrumentation builds and **five scenarios passed**. Coverage includes existing edit/restart flows, actual snapped drag and Undo, canceled/unchanged numeric entry, uniform freeform sizing, four-corner wheel access, disabled actions, portrait and 320 dp compact command access. Saved files were byte-identical across process restart at revision 12. Subsequent edit/Undo tests restored exact objects while correctly advancing the revision to 16.

Final radial, size-panel, portrait and compact screenshots were opened. Earlier passing tests missed a partly clipped compact Back control; the final fix pins it above the list and adds specific coverage. These are authentic app captures, not Northstar approval or pixel-golden synchronization. No cause-specific fix was made for the previously observed KSP/AWT tooling exception; service/deprecation warnings and that audit remain open.

Detailed scope, failures, artifacts and hashes: [RADIAL_IMPLEMENTATION](https://github.com/CRisdon45/Plan_Trace/blob/786f644e1ec12f7f50bc28d738d13f36178d7e1c/docs/RADIAL_IMPLEMENTATION.md). Earlier implementation reports retain dated evidence, not competing resume instructions.

## Next bounded outcome: SITE-01

Bring source image/plan intake, explicit registration and known-distance calibration into this same workspace. Retain traced/assumed/verified confidence, distinguish protected existing site information from proposed geometry, and prove save/reopen/output without a preparatory Pool Studio session. Public tests use original synthetic inputs. This does not select a maps provider, certify survey accuracy, introduce runtime AI or authorize automatic migration of legacy drawings.

Do not spend the next milestone merely expanding the wheel. Arbitrary outlines, attached steps/shelves, shared surfaces, alternatives, multi-project/portable recovery, full landscape scope and live Northstar presentation remain unfinished and should follow actual project workflows.

## Limits and safety

The current workspace is one local draft with session-only Undo. Format 2 is unchanged by radial work; it reads earlier format-1 outlines without implicit promotion, but format-1-only builds cannot read newer saves. Legacy Room plans are separate. Tests wait for verified Saved before force-stop; unfinished writes are not guaranteed to survive termination.

Physical S Pen/palm/barrel feel, continuous flick, full accessibility/handedness, pinch synthesis, hardware performance, new-screen export/share, final PDF/fixed-frame output and Northstar quality remain pending. Portrait/narrow software checks do not substitute for these.

No physical installation, original-data clearing, signing-key replacement, application merge, release/APK publication, permission change, framework restart or 3D work occurred. Private client plans/photos and unapproved reference images remain outside public Git, PRs, logs and artifacts. No runtime AI feature; inherited service cleanup and Estimator interchange remain separate work. Update this routing file and the active handoff at the next meaningful checkpoint.
