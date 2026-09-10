# Current state and next-session handoff

Updated: 2026-09-10, off-tablet implementation checkpoint. This is the current handoff, not an append-only chronology. Verify live branch heads and PRs before changing anything.

## Scope and where to work

**Implementation has resumed at the owner's explicit request while tablet testing is unavailable and was already in progress elsewhere.** The pending physical-device gate is not a blanket prohibition on independently verifiable development. It is also not a passed gate. Preserve the interrupted tablet session, its installed build and all project data.

Repository: `CRisdon45/Plan_Trace`, intentionally public. `main` carries current direction/handoff documents without the unfinished application code. The integrated 2D foundation is `feat/2d-foundation-northstar` (PR #2), with application code still based on `3fc0491`; the context checkpoint before this session was `5367dbe`.

**Current implementation work: `feat/off-tablet-integrity`, PR #3 into `feat/2d-foundation-northstar`.** It was branched from `5367dbe` to avoid mixing changes with the tablet baseline. PR #3 is not an installation or a release. Main/foundation updates in this session are documentation-only; new application code stays on the parallel branch until deliberately integrated. Old perspective/Filament and audit branches are not the next implementation target.

## What was completed off-tablet

Shared PDF/PNG measurement and source-underlay controls now use labeled whole-row switches and saved state. The choices survive format switching and saved-state recreation. Closed freehand/polyline measurements now include the implicit final edge, without modifying vertices, IDs, stored data or schema. This fixes chain-length labels; it is not a freeform curve engine or complete takeoff system.

A GitHub workflow runs the full unit-test suite and debug assembly using synthetic test data, read-only repository permission and an ephemeral CI debug key. It publishes test reports only, not an APK or a deployment. Never use its disposable signing key to replace the existing tablet application's key.

## Evidence and limitations

| Revision / scope | Actual result |
| --- | --- |
| `d9ef977f1b08c957240061bbb1022d690d7e8b98`, unchanged application baseline plus CI | Run 34496770033: 37 tests, zero failures/errors/skips; debug assembly succeeded. This full suite includes two tests absent from the prior focused 35-test report. |
| `e7e75e895ecf1c2863945088c5502ee58b04cdff`, implementation and 12 new regressions | Run 34497580131: 49 tests, zero failures/errors/skips; debug assembly succeeded. Seven path tests, four Compose dialog tests and one actual PNG file/renderer test were added. |
| Physical tablet / visual acceptance | Not run in this session. The latest courtyard example, real pen/palm/button behavior, physical reachability and final PNG/PDF appearance remain pending. |

The implementation run also logged a KSP/AWT background-thread NullPointerException despite successful tasks/tests. It is recorded for toolchain follow-up, not certified harmless or hidden behind a clean-log claim. Existing Google-services/deprecation warnings remain. Read [OFF_TABLET_IMPLEMENTATION.md](OFF_TABLET_IMPLEMENTATION.md) for pinned run links, exact checks and unresolved limits. Later documentation-only commits do not create a new application-validation claim.

## Next outcome

Keep PR #3 reviewable and reconcile any concurrent work before integration. The next independent implementation slice can begin Q1: establish a small project-owned geometry/relationship seam and original synthetic straight/concave-curved cases, without migrating live user data or creating a second demonstration-only renderer. Keep the scope small enough to exercise the actual editing, persistence and rendering paths. Model the project once, while preserving existing page-local sketches and their calibration.

Q0's shared measurement-control defect has automated coverage now. Its remaining tablet and visual checks stay explicit acceptance debt, not a reason to halt unrelated geometry tests. When the tablet is available, finish the interrupted session on its known build first, then deliberately test the newer version and compare its actual PNG/PDF outputs with automatic measurements on/off. No uninstall or data clearing.

## Remaining product gaps

The example still uses separate pool/coping/steps/labels; the new contract's connected project authority is not implemented. Per-page drawing storage remains the current model. Calibration/underlay history, source relinking, portable backups, associative dimensions, exact editing beyond rectangles, lasso, full-project handoff, primary radial interaction and Northstar rendering remain unfinished.

The inherited runtime AI/service dependency audit is separate from the prior root metadata cleanup and was not done in this patch. No runtime AI is an accepted requirement; do not treat leftover scaffolding as approval to use it. 3D remains paused. Exact interchange with Estimator/Design-Platform is still a technical decision, not a repository merger.

Private project originals and Northstar image pixels remain outside this public repo. Public benchmark descriptions are still specifications, not recreated client projects. See REFERENCES.md and its manifest. Do not infer access to absent private files or claim visual parity without reference review.

## Maintain the checkpoint

At each meaningful checkpoint replace the current status/next outcome, record the application revision and checks that actually ran, and link detailed evidence. Keep baseline and parallel-work branches distinguishable; documentation presence is not application parity. Preserve concurrent edits, original user data and the public/private boundary. Historical implementation reports are indexed in HISTORY.md and do not override current scope.
