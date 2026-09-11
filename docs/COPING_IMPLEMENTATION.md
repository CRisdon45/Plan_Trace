# Connected pool and following coping

Checkpoint: 2026-09-10. A first pool/coping relationship is implemented through the real workspace, command history, saved draft and existing renderer. This is a bounded design preview, not a complete pool object, exact analytic offset kernel, area takeoff or construction approval.

## Implemented behavior

New pool starters carry a default 12-inch CopingSpec. Changing the pool boundary regenerates the coping; changing coping width preserves the pool's waterline. The spec belongs to the pool, not a separate editable polygon. Stable pool/node/edge identities survive supported edits, unrelated objects stay unchanged, and one Undo restores the pool and its coping together.

The workspace displays flat water and stone using the existing material renderer. Selected pools have inline coping-width entry in inches, committed by keyboard Done without an Apply/Accept dialog. Current supported width range is 2-48 inches. Invalid numeric or geometric input leaves the committed value in place and reports the reason. These are initial implementation/UI limits, not industry recommendations or a final material/edge-treatment catalog.

Version 2 JSON stores width and generator version, not sampled outer vertices. Reopening reconstructs the derived footprint. Version 1 drafts decode unchanged as outlines without coping. Existing outlines have an explicit Add following coping action; failed attachment does not modify them. A later edited save uses format 2, which older format-1-only builds cannot read. Legacy Room plans and the original tablet app are not migrated or replaced.

## Geometry strategy and limits

Use pinned org.locationtech.jts:jts-core:1.20.0 for planar validation and derived buffering rather than introducing a bespoke Boolean engine. The EDL license notice is included in app assets. This is local computation, not an AI service. Exact line/circular-arc pool geometry remains authoritative.

The generator localizes the exact boundary BEFORE sampling arcs with a maximum 0.25 mm chord error. It checks a valid single source polygon, rejects near-degenerate details at a 2 mm clearance guard, and checks the derived outer polygon and single annular band. It does not repair crossing outlines with buffer(0), discard extra components or silently accept a bridged bay.

An outward offset must not consume an inside circular arc. Offset-then-inset boundaries must agree within a bidirectional 2 mm recovery guard, rejecting bridged recesses and clipped tight corners. Generator version 1 uses mitred joins with mitre limit 4 and no input simplification. Limits are 256 canonical source edges, 4,096 sampled source points, 8,192 outer points and 1,000 metres of local extent. These are explicit initial work/precision limits, not measured tablet capacity.

Validation is resolution-limited, not an analytic proof of every possible curved topology. Very small/ambiguous details fail closed. Site objects, setbacks, overlapping pools, decking exclusions, elevations, per-edge coping changes and construction clearances are not checked. No coping-area or pool-area takeoff is exposed. Water fill displays the accepted sampled shape; it does not certify the broader site.

Derived-footprint caching belongs to each immutable object. Validation occurs before the next document reaches history or saving. Renderer-only polylines never replace exact pool arcs. Output settings enforce compatible sampling/Float precision, and labels use analytic pool perimeter. Existing auto-fit still changes framing when labels are hidden; fixed output frames remain a follow-up.

## Completed verification

Tested application revision: f0f1892f89d6113710172b54213e09e306c32043.

| Check | Completed result |
| --- | --- |
| [Unit/build run 34523944344](https://github.com/CRisdon45/Plan_Trace/actions/runs/34523944344) | 111 tests, zero failures/errors/skips; debug assembly succeeded. Downloaded XML counts matched the job summary. |
| [Actual Android run 34523944339](https://github.com/CRisdon45/Plan_Trace/actions/runs/34523944339) | Application and instrumentation builds succeeded; both separately invoked scenario tests passed. |

The 21 new unit tests cover rectangle width, circular and concave/convex cases, winding/translation, stable identities, coupled preview/Undo/Redo, width changes, locks, crossing geometry, consumed inside curves, narrow bays, near touches, explicit legacy attachment, JSON, atomic draft preservation and actual native PNG rendering. The existing 90 tests remain in the full suite. The two emulator scenarios are separate from the 111-test count.

The virtual device ran API 35, x86_64, with a 1600 x 1000 / density 240 override. It created coupled pools through normal app navigation, moved vertices/curves, checked derived coping and the unrelated pool, canceled an edit, and exercised Delete/Undo. It entered 16 inches, rejected a 48-inch result that would consume the tight inside curve, and rejected a crossing vertex drag without changing the saved design or revision. A separate force-stop/relaunch and activity recreation retained the design after waiting for verified Saved.

Scenario durations were 17.543 and 5.155 seconds, not latency or meeting-speed benchmarks. Process restart preserved byte-identical 1,358-byte canonical files at revision 9, SHA-256 b46d092a01dabe363b96233dfaaebbed8b7c71f41f706f699c0e112616ed6077. Physical S Pen/palm/button feel and edits killed before saving completes are not certified by these scenarios.

Downloaded unit artifact 10170956268 verified against ZIP SHA-256 d48f251982bc9cc45501a726a811b23ab5aba371adfea79625377522111339ad. Emulator artifact 10170988091 verified against 04113dc0ea272291f3e100e592e1c1de87c7bb6722bf5899fdbec14af79191ee. Both have seven-day retention, not permanent availability.

## Failures, corrections and visual review

The initial full run at fffde667 built the app but failed 2 of 111 tests. Initial ring comparisons were too literal about floating-point equality and ring starting position. Allowing cyclic starts, either winding and a 1e-8 metre point tolerance fixed winding, but translation still failed at f319783. The comparison-only explanation was incomplete: there was a production stability problem. The correction at f0f1892 localizes the exact boundary before arc interpolation rather than subtracting an origin after world-space sampling. The strict translation regression and geometry guards were retained, and the full final suite passed. No failed test was removed.

An emulator attempt at f319783 stopped at the immediate KVM permission check before SDK setup or app testing. The workflow now waits for udev and prints permissions before failing. That failed run is infrastructure evidence, not a passed app test.

Earlier successful emulator run 34523191828 passed semantic interaction and saved-state assertions, but its downloaded screenshots exposed a system dialog reading "Pixel Launcher isn't responding". It is not accepted as unobstructed visual evidence. An empty crash buffer did not establish a clear screen. The final run at f0f1892 passed both scenarios again; its recreated-workspace and rejection captures were inspected without that system dialog. No cause-specific launcher fix was made. Future visual checks must inspect actual captures rather than equating semantic-test success with an unobstructed display.

The final recreated screenshot shows both edited pool/coping assemblies, selected handles, 16.00-inch coping and Saved on device. SHA-256: 86cd038409c75f320f9fdc4b1afd236fda5460264476697f5243bafeef0e91da. The rejected-width capture reports that coping is too wide for the inside curve while retaining 16.00 inches. Actual native PNG output was opened and reviewed for a continuous band and labels. These are flat Graphic/technical checks, not Northstar approval. System-bar contrast after recreation remains a follow-up.

The final unit/build log still contains KSP/AWT background NullPointerExceptions despite successful tasks. Existing Google-services/deprecation warnings remain. No cause-specific resolution, warning-free build, local Android build, physical stylus certification, new-screen export/share scenario or PDF visual acceptance is claimed.

## Next bounded outcome

Bring primary radial commands and exact pool-dimension controls into this connected workspace. Keep sectors predictable, visible alternatives available, and edits on the same preview/Undo/save path. Test software interaction virtually while physical barrel-button feel remains pending. Do not expand the generic toolbar or restart frameworks. Connected shelves/steps and shared-deck behavior should reuse this authority rather than independent artwork.

Northstar remains the live-design visual target; current flat fills are not that finished style. Source/site registration, arbitrary outlines, tangent-editing assistance, multiple projects, portable recovery, full material scope and dependent quantities remain unfinished. This is still a single opt-in local draft with session-only Undo.

No physical tablet was accessed, no application PR was merged, and no client plans/photos, source identities or unapproved artwork were published. All test imagery is original synthetic work. Repository visibility and permissions are unchanged. No release, installable APK publication or 3D work occurred. The inherited runtime-service dependency audit remains separate from this geometry feature.
