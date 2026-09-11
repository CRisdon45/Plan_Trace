# Current state and next-session handoff

Updated 2026-09-11. Active implementation: feat/project-geometry-seam, PR #4 into feat/off-tablet-integrity; PR #3 targets the 2D foundation and PR #2 main. Verify live refs and concurrent work. No application PR was merged; the interrupted physical-tablet session, installed app, data and signing key remain untouched.

## Owner direction

Cody wants the current line distance visible beside the moving target while drawing, so he can be precise without routinely typing. Target, guide, displayed length and eventual endpoint must agree after constraints/snapping. Pen manipulation remains primary; typed values are an occasional fallback. No runtime AI, 3D paused, Northstar is still the visual target.

## New working behavior

Draw -> Pool and Draw -> Paving now start hand-placed corner outlines, not fixed-position presets. Closure creates one object/Undo step; a pool includes following coping, while paving is a deck boundary only, without shared cutouts or filled-surface/quantity promises. The existing Curved starter remains available. Back a corner/Cancel, optional first-edge-relative right angles and explicit one-foot snapping operate on temporary drafts.

After the first corner, a floating readout follows the actual constrained endpoint. Returning near the start resolves to the exact first corner and shows the closing-edge length. The same feedback serves existing-house/property corner drawing. During boundary edits it reports both incident edges at a vertex, analytic arc length/radius for a curve, and displacement labeled Move for translation. Rejected edits do not retain misleading placed-length feedback.

Readings use feet and fractional inches rounded to eighths for display, marked approximate when appropriate. Display precision is not snapping or source/survey accuracy; canonical geometry is not rounded. The badge is a noninteractive, clamped canvas overlay and not a saved/exported annotation. Coverage does not yet include the legacy page-sketch canvas or source-calibration/movement tools.

## Verified checkpoint

Application/test revision da3a2a1aba197b80d9fd4e0205209a1297ebbf7b contains production implementation c69a148 plus a test-only navigation correction. Unit/build run 34622090828 passed 195 tests, zero failures/errors/skips, and debug assembly. Android run 34622090815 built app/instrumentation and passed eleven scenarios. Downloaded XML, scenario results, archive hashes and 26 capture-window records were checked. Actual live line, vertex edit, site-line, reopened workspace and PNG output were opened.

The held-pointer test observed 10-to-12-foot feedback, matched the committed endpoint to its target, and closed a 12-by-9-foot pool with a 9-foot final edge and following coping. It checked transient vertex/site feedback and canceled edits, then created a concave deck. Existing objects remained unchanged. The format-5, six-object document was byte-identical across completed-save force-stop/reopen at revision 45. No live label or unfinished draft is stored in it.

The first replay stopped because the test attempted to use handles without selecting the pool restored by Redo. The correction explicitly selects that object through its chip. Production behavior and all assertions were retained. LIVE_DRAWING_IMPLEMENTATION.md records actual results and limitations. The successful initial build still logged the known KSP/AWT exception; no tooling repair is claimed.

## Remaining work

The readout is legible and leaves the target visible in reviewed captures. Existing automatic export labels still overlap in the deliberately overlapping test scene; source/house notice placement remains a presentation gap. These are technical captures, not Northstar acceptance.

Protected site objects, source calibration/checks, radial commands and verified atomic saving remain. Format 5 is unchanged; legacy Room projects are separate. One draft, session-only Undo and owned images are not portable backup or multi-project management. Tests stop the app only after verified Saved.

New construction is a straight-edged corner workflow, not freehand recognition or direct curved authoring. Deck exclusions, shared surfaces, attached shelves/steps, broader snapping, source PDF/rotation, alternatives and landscape scope remain unfinished. Physical S Pen/palm/barrel and hover feel, hand occlusion, latency and full drafting across every window size are unverified.

Next: improve pen-guided alignment and edge manipulation against existing house/property/pool geometry using this same resolved target and live measurements, not more mandatory numeric forms. Preserve site locks, one-operation Undo, recovery and real-renderer visual review. Shared decking and direct curved construction are follow-ons, not accomplished by a label.

No physical app/data clearing, signing workaround, release/APK publication, private client/reference publication, permission change, runtime AI, framework/repository merger or 3D work is included. Keep routing and evidence tied to successful read-back-verified writes.
