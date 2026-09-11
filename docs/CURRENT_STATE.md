# Current state and next-session handoff

Source-image and distance-check checkpoint. Work is authorized on **feat/project-geometry-seam**, PR #4 into feat/off-tablet-integrity. PR #3 targets the 2D foundation and PR #2 main. No application PR was merged this session. Verify live refs and concurrent work; preserve the interrupted physical tablet test, original app/data/signing key.

## Owner direction

Cody rarely, if ever, wants to type dimensions on screen. Direct pen manipulation, useful snapping and contextual controls carry the intended workflow. Typed input is an occasional fallback. The owner deferred redesigning precision interaction; do not turn the next site step into a keyboard-entry milestone. This is also recorded in AGENTS.md, decision D16 and the interaction contract.

## Verified application

Production feature revision **765ef687c91ec4482213f88c3fadc647055d978d** adds optional second-distance evidence to the site-image workflow. **884f0ef0d23b04751dc9b9b84f99b08564f12503** preserves that production code and unit tests while strengthening actual Android capture checks and display-test setup. Later documentation commits do not create new feature/build claims.

At 884f0ef: unit/build run **34545769738** passed **165 tests with zero failures/errors/skips** and debug assembly. Android run **34545769754** built app/instrumentation and passed **seven scenarios**. Downloaded XML, result files and all 15 capture-window records were checked. The source-backed saved file was byte-identical across completed-save force-stop/reopen at format 4, revision 29. Actual source-check, reopened-workspace, compact, PNG and rendered PDF captures were opened.

The preceding 765ef687 replay had seven green scenarios but a Pixel Launcher error dialog over its screenshots. That is not visual acceptance. The new capture helper records the actual active-window tree and raw screenshot, then fails if another package obscures the app; it never dismisses error dialogs. Final reviewed captures were unobstructed. This is a tested harness improvement, not a Pixel Launcher root-cause fix or replacement for manual visual review.

See **SITE_IMAGE_IMPLEMENTATION.md** for exact runs, hashes, budgets, limitations and failures. RADIAL_IMPLEMENTATION.md restores the real earlier 137-test/five-scenario record. The formerly claimed 786f644 documentation commit did not land; obsolete main/PR links to it are replaced in this checkpoint, not treated as existing evidence.

## Current working scope

Projects -> Design workspace has one separately saved canonical draft, exact line/circular-arc objects, connected coping, direct edits, Undo/Redo/Delete, radial commands, grid/snap preferences and optional exact numeric controls. The existing renderer displays a disposable projection of that same authority. Coping uses sampled planar validation, not construction approval or a full takeoff engine.

**View -> Site** imports bounded PNG/JPEG imagery into app-owned storage, with JPEG orientation normalization. Source registration is explicit upright uniform scale/translation. Two-point known-distance calibration and source-only movement never resize proposed geometry. Presets permit source setup without mandatory typing. Source visibility, remove/Undo, cancellation, saving and direct registered PNG/PDF output have automated/virtual coverage.

**Check another distance** compares a separate segment against the existing scale. It records agreement or disagreement without rescaling, moving objects or promoting the site to field-verified. Move/hide retains the check; recalibration clears stale evidence. The comparison remains in the saved draft and source-present output notice. It is optional and not a survey or full-image distortion test.

Format 4 reads formats 1-3 without inventing source/check information. Older builds reject newer saves. Legacy Room projects are separate and unchanged. App-owned image assets are not portable backup. One draft and session-local Undo remain limitations; tests wait for verified Saved before termination.

## Next bounded outcome

Advance SITE-01 to **pen-created editable existing-house and property outlines over the calibrated source**, with deliberate closure, stable identities, traced provenance, protection from incidental edits and save/reopen/output through the same authority. Source-image intake alone is not complete site preparation. Establish how later source re-registration affects confidence/relationships rather than silently moving traced or proposed objects. Use original synthetic sources, not client material.

Do not spend another milestone merely enlarging the wheel or adding numeric forms. Keep Northstar visual quality alongside useful editing, but current flat fills, technical notices and synthetic overlapping test pools are not approved presentation design.

## Remaining limits and safety

PDF intake, arbitrary site-outline authoring, source rotation/georeferencing, tangent assistance, attached steps/shelves, shared decking, associative dimensions, alternatives, multi-project/portable recovery and full landscape scope remain unfinished. System-picker/provider and share-chooser UI, physical S Pen/palm/barrel feel, continuous flick, hardware performance, fixed output frames and final Northstar acceptance are unverified. Prior KSP/AWT tooling exceptions and service/deprecation warnings remain open absent a tested cause-specific repair.

Only disposable virtual devices were installed/force-stopped. No physical app/data clearing, signing-key replacement, release/APK publication, permissions/visibility change, private client-reference publication, runtime AI, framework/repo merger or 3D work occurred. Keep tests and screenshots truthful; update this handoff and main routing using only successful, read-back-verified commits.
