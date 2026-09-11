# Pen-created, protected existing-site outlines

Recorded 2026-09-11. This slice turns a calibrated source image into editable existing-house and property reference objects in the same workspace as the proposed design. It is not automatic image recognition, a survey or the completed SITE-01 workflow.

## Reachable behavior

View -> Site offers House and Property. Choose one, mark corners by tapping or dragging the pen to each intended endpoint, and deliberately close by returning to the first corner or choosing Close outline. The temporary controls include Back a corner, Cancel and optional Right angles. No dimension typing is required to author these outlines.

House mode starts with right-angle assistance relative to the first segment, not an assumed horizontal screen axis. Property mode starts unrestricted. Both use straight edges in this first slice; the last closing edge is not automatically solved into an orthogonal constraint system. Existing explicit grid snapping can assist placement. This is corner-based authoring, not continuous freehand recognition, curved site boundaries or an open wall-line tool.

A draft is transient. Preview and unfinished corners do not change the saved design; Back/Undo while constructing removes a draft corner rather than an existing pool. Closing validates the outline, assigns stable object/vertex/edge IDs and commits one complete object and one Undo step. Cancel, workspace exit or lifecycle interruption discards an unfinished outline. Completed saved geometry is separate from that temporary state.

## Protection and provenance

Completed outlines are typed SITE_OUTLINE objects with House or Property roles, marked TRACED and locked by default. Existing pool and landscape objects are unchanged. Locked site outlines have no editing handles and cannot be translated, resized, reshaped or deleted through ordinary mutation commands. Select their chip and use Site controls to deliberately unlock, revise and relock them. A real boundary edit retains IDs, records that the trace was adjusted, and is reversible through the same command/history/save path. Lock changes themselves are reversible.

The source asset identity and original calibrated registration are retained as authored provenance, not a live transform. Moving or rescaling the image never silently moves traced geometry. The app reports that alignment needs review; removing or replacing the source leaves an unavailable-reference notice. Source visibility and distance-check evidence do not change registration. Reversing a source move restores its former registration status. No operation here silently upgrades a trace to FIELD_VERIFIED.

Straight polygon validation uses the existing pinned local JTS dependency, with 3–128 corners, at least 1 cm between consecutive corners, bounded local extent and rejection of crossings, self-touches and degenerate shapes. These are implementation limits, not suggested measurement practices. No setbacks, easements, site collision, construction clearance or area-takeoff certification is added.

## Rendering and storage

The existing canvas/export renderer draws the actual site vectors, ordered before proposed objects. Hiding the raster leaves the house and property outlines visible. Output retains traced/unverified and changed-source notices even when the source image is omitted. Labels and current muted line styling are technical previews, not Northstar approval or finished presentation typography.

Format 5 reads formats 1–4 without inventing site metadata. SiteTrace includes role, original source registration and an adjusted flag. Corrupt or falsely field-verified metadata fails the whole read rather than silently stripping evidence. Later saves cannot be read by older format-limited builds. No legacy Room project was migrated and no physical tablet was updated. The single local workspace still lacks portable asset backup, multiple-project management and persistent Undo history.

## Verification

The refinement at **571a28c97c9ad8be7666f25eb5bbba5d83fc94a9** passed [unit/build run 34616514170](https://github.com/CRisdon45/Plan_Trace/actions/runs/34616514170): **182 tests, zero failures/errors/skips, successful debug assembly**. Downloaded XML independently confirmed the counts. This is the previous 165 plus 15 site-model/document tests and two real Compose status-layout tests. Artifact 10271376996 ZIP SHA-256 verified as 8cd2c5f77c4db09d7ac0598e9c99a01c2127490d4f7481db39192ea22f0f2ef2.

At the same revision, [Android run 34616514212](https://github.com/CRisdon45/Plan_Trace/actions/runs/34616514212) built the actual app/instrumentation and completed **all nine scenario tests**. The two new scenarios create a six-corner house and a four-corner property outline over the existing synthetic raster, cancel/undo draft corners, close through both supported routes, test a contact on a protected house edge, deliberately unlock/edit/Undo/relock, move the raster without moving site objects, review its warning, hide the source, and reopen/export the resulting vectors. The earlier seven scenarios remain, including radial safety, source distance checks, and portrait/compact command access.

Downloaded Android artifact 10270406738 verified against ZIP SHA-256 6edca56f03f7cb1154e32ae1e010d419ec24aa80f92cfb743a858940bd568299. Its nine result files each report OK (1 test). All 20 active-window capture records identify the application, not a system dialog. The saved four-object document is format 5, revision 41, 4,955 bytes and byte-identical across completed-save force-stop/reopen: SHA-256 71ce2acded027c80095607a9691297b29bc85c4b466a56418e08c88e3dfe25af. The crash buffer was empty, which is not a claim about every possible flow. Report artifacts have seven-day retention.

The actual reopened-workspace, source-hidden-workspace, registration-warning and vector-only PNG output were opened. They show the authored house/property vectors independent of the raster, retained locks, and the deliberate mismatch warning. Reopened screenshot SHA-256: 0b66163163c1ec06e79ea453e595ea3414b811aba5c2c915601716df1f4e1856. Source-hidden screenshot: 9785a1281a9fc6da4deb46108e0e51894f1d0707426d4fad7a42f05a141cbbcb. The direct PNG has hash c0bc3239870b1b55d52ac2b186ee55050fb48308bded8320b56370816a3c6d40. That export preserves geometry, but its existing-house notice touches the property outline; label collision/layout remains a real presentation follow-up. It is not a fully accepted presentation sheet.

The synthetic scene intentionally retains two overlapping test pools and a simple raster house. No collision-aware layout or client-ready aesthetic is claimed. The Android device is API 35 x86_64, with landscape 1600x1000 at density 240 for the outline workflow; earlier command tests additionally run portrait and 320dp compact configurations. There is no new local Android build or physical-tablet acceptance claim.

The initial application commit 54f168886a162c1394981a5fbacf4009e6d00daf passed 180 unit tests and debug assembly in run 34615323718. Its unit ZIP 10269329589 verified against c9ba9e369df65603c980ebeeddce690e6b6666bf458e7e1430aa1254e7f149de. The Android run 34615323569 passed the five preceding scenarios, then the new outline scenario failed during source movement. Its diagnostic ZIP 10270920396 verified against 4be918bcbe04013ab6ea6e20abc54c6c1939f4ec2f7db26253db6a757b2ca9b7. This run did not complete all nine scenarios and is not accepted as complete evidence.

The mismatch notice was reading the transient preview. Inserting that footer changed the canvas bounds during a drag, triggering its resize cancellation. The correction renders provenance status from committed state, so live geometry can preview without an extra footer row shifting its input surface. Two Compose regressions check stable preview bounds and commit-time status changes. The Android scenario also asserts actual source movement, touches an actual locked house edge, and cancels an in-progress corner contact. No failed assertion was dropped.

## Remaining limits and next outcome

Virtual corner creation is exercised using the explicit Touch edit path; physical S Pen delivery, palm rejection, hand feel and performance remain pending. The main pen branch shares the same drafting commands, but an emulator is not Samsung hardware. Full draft creation in portrait/compact layouts, advanced accessibility, source rotation/PDF intake, curved/open site boundaries and automatic closure assistance are not certified by this slice.

The next bounded outcome should use this editable site to author proposed pool/deck geometry directly where the pen indicates, rather than always inserting a preset starter. Preserve following coping, stable IDs, dependable snapping, one-operation Undo and the protected site base. Reuse the shared model and renderer instead of creating another isolated drawing mode or demanding numeric typing. Broader source-registration tools and final Northstar quality remain important follow-ons; this milestone does not complete the whole client-meeting product.

Only original synthetic fixtures are published. No client plans/photos, names, reference artwork, release keys or installable APKs are added. No application PR merge, original-app/data clearing, permission/visibility change, runtime AI, framework/repository merger or 3D work occurs. Earlier KSP/AWT and service/deprecation warnings remain open unless separately fixed; a passing build is not a warning-free-toolchain claim.
