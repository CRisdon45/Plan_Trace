# Live target measurements and hand-placed proposed outlines

Recorded 2026-09-11. Cody requested the line distance beside the moving target so he can draw precisely without routinely typing dimensions. This slice also connects hand-placed proposed pool/deck boundaries to the existing site, command history, coping and save model.

## Implemented behavior

Draw -> Pool and Draw -> Paving now start a corner-based proposed outline where the pen indicates instead of inserting a preset at a fixed location. The Curved starter remains available. This is not continuous freehand recognition or direct arc construction. Pool closure validates and creates following coping; paving is currently a deck boundary, not a shared/cut-out filled surface. Neither requires typing. Back a corner, Cancel, optional right-angle assistance, explicit grid snapping and one complete Add/Undo are retained.

After the first corner, a floating readout sits beside the resolved endpoint while moving or hovering the pen. Guide, crosshair, numeric value and committed endpoint share one resolver. Grid/right-angle assistance operates before measurement. Returning near the first corner resolves to that exact point and labels the closing edge. There is no numeric line reading before a previous point exists.

The same readout serves house/property corner creation. Moving an existing vertex reports both incident edge lengths; changing a curve reports analytic arc length and radius; moving an object reports displacement as Move. An edit rejected by command validation is labeled as not placed instead of presenting a last-good number as the attempted geometry. This does not add universal geometric or site validation.

Feet/inches are rounded to the nearest eighth for display, with an approximate sign when rounded. Display rounding never changes stored coordinates or creates an eighth-inch snapping rule. The independently enabled grid remains one-foot snapping, not a finished configurable endpoint/alignment system. Display detail does not certify pen, source or survey accuracy.

## Input and storage

The badge is a noninteractive overlay measured inside the canvas, offset from the crosshair and clamped at its edges. It does not change drawing bounds. Box/background replaces a touch-blocking Surface for this feedback. Compose tests send a touch through the badge to the artwork. Readouts are temporary, disappear after completion/cancellation and are not saved/exported annotations. Generic stylus hover updates a temporary target, not the project.

Drafts and edits use the same ProjectDesign authority, exact line/arc geometry, locks, commands and atomic saves. Creating a pool/deck does not modify protected site objects. Bad closure/coping fails before adding an object and retains its recoverable draft. Format 5 is unchanged; no legacy Room migration or dependency change occurred. One local draft, session-only Undo and lack of portable project/asset backup remain limits. Restart tests wait for verified saving.

Current live-feedback coverage is project-workspace proposed/site corner creation and vertex/curve/body edits. The legacy page-sketch canvas and source-calibration/movement tools did not receive this overlay. The owner's broader feedback requirement applies to later tools without claiming every possible operation exists today.

## Verification

Production revision: c69a148a03f0041f33f239e12d75cef28b72961d. Refinement da3a2a1aba197b80d9fd4e0205209a1297ebbf7b changes one Android test only: select a pool restored by Redo before targeting its handles. Production and unit-test code are unchanged, confirmed by the exact comparison.

[Initial unit/build run 34620935116](https://github.com/CRisdon45/Plan_Trace/actions/runs/34620935116) passed 195 tests, zero failures/errors/skips, and debug assembly. Downloaded XML confirms the previous 182 plus eleven model tests and two native Compose tests. Artifact 10273000348 ZIP SHA-256: 72075f1479ba2423605dc134f8b11e23e327b6b30a3a04c690131bb50a78f565.

[Initial Android run 34620935220](https://github.com/CRisdon45/Plan_Trace/actions/runs/34620935220) passed seven preceding scenarios. The new scenario verified held-pointer 10/12-foot readings, the indicated endpoint, a 9-foot closing edge and pool Add/Undo/Redo. It then failed by targeting a handle without selecting the restored pool. That missing test navigation step was corrected without changing production selection behavior or removing assertions. The partial run is not a full eleven-scenario pass. Failure artifact 10272477443 ZIP hash: b175c550f366da301dd898914c25f2cb902e7da5772825a5a43bc221ea435726.

The final replay at da3a2a1aba197b80d9fd4e0205209a1297ebbf7b completed [unit/build run 34622090828](https://github.com/CRisdon45/Plan_Trace/actions/runs/34622090828) with **195 tests, zero failures/errors/skips and successful debug assembly**. [Android run 34622090815](https://github.com/CRisdon45/Plan_Trace/actions/runs/34622090815) built app/instrumentation and **all eleven scenarios passed**. Both archives were downloaded and verified; XML was counted independently and each device result reports OK (1 test).

The held-pointer scenario checks the number changing from 10 to 12 feet, then that the indicated endpoint is placed. It closes a 12-by-9-foot pool with a displayed 9-foot final edge, following coping and one Add/Undo. It then reselects the pool restored by Redo, observes both actual incident lengths during a vertex edit and cancels without changing stored geometry. It observes live site-line feedback and cancels that draft, then authors a six-corner concave deck. The separate restart scenario verifies the completed result, absence of draft/readout state and actual PNG output. All earlier site, source, radial, portrait and compact-command scenarios remain.

The final saved document has six objects, format 5, revision 45 and 7,130 bytes. Completed-save force-stop/reopen files are byte-identical: SHA-256 54207175c1a73d3a4ace29d39ef9ce366931155e891da0d1c9101e0d0621c09f. Final unit artifact 10272957396 ZIP hash: 00f4858b6c482f2a2526a7a1477ac20da6a0dfa89cc0877036140c6d19727b3e. Android artifact 10273177887 ZIP hash: b9a1d083d7f49559040bfeafb5c2ebad57355fc5685c6ba033cd37e7196e33d6. Seven-day retention is not permanent backup.

All 26 active-window records identify the application; the crash buffer is empty. Those checks are not exhaustive overlay detection or all-flow crash certification. Actual final line, vertex-edit, site-line and reopened screenshots were opened, along with direct PNG output. The readouts are legible near their crosshairs and leave the actual targets visible. The export still has overlapping pool/deck labels and a house notice touching the property boundary. It is not an accepted presentation sheet or collision-aware site layout.

Final screenshot SHA-256 hashes: line 721b48b9105b89c01dc7129047bfa8f55390c52bfdb9d14d277928710b1ea7d0; vertex edit 5a467c7185d632b06b8e69eea15a34c8835716b665774c3b59184ec25b18fd2c; site line 4d00b75e962508804d7c9c666b49e8a4445c7e580bb956bf28b789a5d4abe440; reopened 93714eadf17e86efe88ec89a66220514efee1c34d513bf429aff8024450c6072. Direct PNG: 8f85314dcffb277d24ae44aa7b860c113887b67b3419845f2e282d19f4d4670d.

API35 x86_64 runs held-pointer authoring at 1600x1000/density240. Existing command cases additionally run 1000x1600/240 portrait and 800x1400/400 compact layouts. The two new Android scenarios are separate from the 195 unit-test count. No local Android build is claimed. The successful initial unit run repeated the known KSP/AWT background exception; the test-only refinement makes no cause-specific tooling repair or warning-free-build claim.

## Limits and next outcome

Virtual authoring uses actual artwork contacts with explicit Touch edit. Physical S Pen delivery, hover feel, hand occlusion, palm rejection and latency still need the tablet. Arc/radius and movement readings have model coverage; the new held-pointer scenario concentrates on line/closure and vertex feedback. Command-layout and badge edge tests are not full ergonomic/drafting acceptance on every layout.

Flat rendering and temporary construction controls are not Northstar acceptance. The synthetic scene intentionally overlaps pools and a deck boundary; no shared-deck topology, exclusions, setbacks or quantities are certified. Automatic label collision/layout and fixed-frame export remain presentation work.

Next, improve pen-based alignment and edge manipulation against actual site/pool geometry using this shared target/measurement path, not mandatory numeric forms. Preserve site locks, one-operation Undo, recovery and real-renderer visual review. Direct curved construction, attached features, shared decking, source PDF/rotation, landscape scope and portable recovery remain unfinished.

Only original synthetic fixtures are published. No application PR merge, physical tablet/app/data/signing-key change, APK publication, private client/reference publication, permission change, runtime AI, 3D or framework/repository merger occurred. Prior tooling/service warnings remain open.
