# Reuse decisions and the local-runtime boundary

Reviewed 2026-09-11 against Plan Trace at 915017a. Reuse general-purpose machinery, not a foreign application's definition of a project. Pool/site geometry, connected behavior, one-action Undo, source confidence and Cody's expert pen workflow remain ours. This record is selective, not a claim to have audited every Google repository.

## Adopted now

**Wash-structure grass pigment (2026-09-12).** After stamp-count finishing
still read as digital grass in close-up, the production Java core keeps
Hobbs related glazes, mass-conserving `dry()` and RGB Kubelka-Munk, but
stops covering those washes with thousands of marks. Large stratified
sheets, reserve tongues, broken front sediment and sparse clustered
deposits replace the 2,800+12,000 stamp loop. Silhouette accents on the
Android path stay in a thin inward band. No third-party source, art,
runtime service or dependency was added. Ground cache bounds, the 1024-pixel
cap and off-thread preparation remain. See [the grass study](GRASS_PAINT_STUDY.md).
The older grass descriptions below are historical, not the current model.

**Revised grass pigment model (2026-09-12).** After the owner rejected the earlier
material pass, independently implement deeper related polygon glazes, finite
pigment redistribution to each wet front, shared paper deposition and ordered
RGB Kubelka-Munk compositing. The production Java core is shared with a desktop
PNG inspection adapter. See [the grass study](GRASS_PAINT_STUDY.md) for primary
sources, the limits of the mathematical approximation and the visual gate.
No third-party source, art, runtime service or dependency was added. Ground
cache bounds remain; expensive grass preparation moves off the hardware drawing
thread. The older grass description below is historical, not the current model.


**Grass and travertine paint (2026-09-12).** Reuse the already implemented polygon
edge displacement from `NorthstarPolygonWash` in `NorthstarGroundMaterials`, with
separate material palettes, glaze density, selective dry rims, lifted marks and
clustered grass/mineral detail. Only helper visibility changes in the water wash;
its generation is unchanged. No additional library or reference pixels are used.
Ground paint accumulates in a temporary floating-point bitmap before conversion
to ordinary ARGB. This avoids hue drift from repeatedly quantizing very faint
premultiplied deposits. Generation uses a 1024-pixel longest side with explicit
prefiltered levels, all included in a 24 MiB cache. Warm draws select a cached level from the
actual canvas transform. Exact silhouette clipping and final outline ink remain
in the shared renderer. Extreme magnification still exposes the texture cap.

The first travertine joint default is an illustrative 12x24-inch running bond.
Calibrated metres/feet/inches control its spacing; uncalibrated legacy surfaces
use a proportionate preview. Grid work is bounded for very large extents. It is
not persisted as an assigned product, counted as a cut layout or applied across
the coping's enclosing polygon. A future material/layout model must own those
choices explicitly. Per-object movement retains the paint and joint arrangement.
Verification uses actual renderer details, concave masks, repeatability after
cache eviction, physical calibration equivalence and a disposable two-material
scene in the real Android workspace. No tablet performance claim follows from
bounded work or emulator screenshots.

**Northstar polygon glazes (2026-09-12).** Adapt the published method, not source
code or assets, from Tyler Hobbs's 2017
[A Guide to Simulating Watercolor Paint with Generative Art](https://www.tylerxhobbs.com/words/a-guide-to-simulating-watercolor-paint-with-generative-art)
(primary essay inspected 2026-09-12). `NorthstarPolygonWash.kt` is an independent
Kotlin implementation: recursively displaced polygon edges with inherited local
variance, interleaved translucent blue layers and gaps in individual deposits.
Four spatial scales provide broad washes through small pigment blooms. Correlated
paper-tooth variation modulates existing pigment coverage at the final raster
resolution, retaining fine granulation without painting outside a glaze. The
existing bounded deposition field and caustics remain separate complementary
layers. No external code license or art redistribution grant is assumed; no
third-party code, image, dependency or runtime service is added.

This is an illustrative model, not physically accurate spectral pigment mixing.
The cache is byte-bounded to 12 MiB, with a 768-pixel longest-side texture and
fixed layer/polygon counts; exact object clipping stays in the shared renderer.
It is independent of translation and view scale. Changing aspect ratio changes
the generated wash; changing the silhouette within the same aspect ratio only
changes the caller's clip. Subpixel detail eventually softens at extreme zoom.
Proof gate: actual rectangular/organic app output, broad and fine variation
without caustics, repeatability after cache eviction, neighbor variation, concave
containment and unchanged saved geometry. Tablet cold-generation and frame-time
cost require measurement; bounded work alone is not a performance claim.

**JTS 1.20.0 spatial lookup.** The app already used this pinned local library for coping and site validity, with its EDL notice in assets. GeometrySnapIndex now also uses [STRtree](https://locationtech.github.io/jts/javadoc/org/locationtech/jts/index/strtree/STRtree.html) to shortlist finite corners, midpoints and edges. No new dependency, native bridge or copied third-party source was required.

The tree is a broad-phase lookup only. Exact line/arc projection, reference priority, deterministic tie order, acquisition/release behavior and explicit construction axes remain in the existing resolver. Conservative arc boxes include their sagitta, not just the chord or a sampled path. Queries cover the wider release radius. Distant corner-alignment guides remain global because a nearby guide can originate from a far-away corner. This is not an overall logarithmic-time guarantee or elimination of every full-scene operation. A query too large for finite bounds falls back to the exhaustive candidate set.

An internal exhaustive-candidate mode exists for equivalence testing through the same exact resolver; the public UI constructor always uses the index. Existing independent geometry tests remain essential, since comparing these two modes tests the broad phase, not the mathematical correctness of their shared resolver. The immutable lookup belongs to one source snapshot and does not become stored project authority.

**Remove unused inherited services.** Source inspection of the complete archived production tree found no callers of Firebase AI/App Check, Coil, Retrofit/OkHttp, Moshi, or their initialization/configuration APIs. Removed their direct dependencies, obsolete optional catalog entries, Moshi code-generation processor, Google Services/Secrets plugins and the unused tracked credential template. Kept actual AndroidX/Room/coroutines/JTS dependencies, their versions, application IDs, signing configuration and all project/legacy storage code unchanged. This is not a claim that those inherited SDKs had previously processed any client data.

The build now reports resolved debug and release runtime coordinates, including transitives. A CI guard inspects those reports and generated merged app manifests for the removed service families and network permissions. Missing or unresolved evidence fails; it is not treated as an empty, safe result. Nine Python guard regressions test positive and negative cases. The policy is specific to the current local-only app and is not a complete security or SDK-behavior audit. Android's external document providers and share targets are separate processes; the check does not claim to prevent their network activity.

## Keep as targeted references, not imports of entire apps

| Candidate | Current decision | Boundary and next adoption gate |
| --- | --- | --- |
| [AndroidX Ink](https://developer.android.com/jetpack/androidx/releases/ink) | Review-only; no dependency added. Official release table checked 2026-09-11 lists 1.0.0 stable and 1.1.0-alpha08. | First evaluate published stable authoring/rendering modules when implementing loose sketch/discussion marks. Confirm project-to-screen transforms, cancellation, output consistency, storage and target-device latency. Predicted/brush-smoothed input must never silently replace exact pool geometry. No Ink integration or performance gain was proved this session. |
| [Cahier drawing surface](https://github.com/android/cahier/blob/14a2c6a91228121621ce338a7333632540471ded/app/src/main/java/com/example/cahier/core/ui/DrawingSurface.kt) | Reference-only. Inspected sample uses Ink and gesture-sharing techniques; its catalog pins an alpha. | Adapt small patterns only after testing our pen/finger/overlay routing. Do not copy its ContentScale.Crop background into calibrated source registration or adopt a notebook data model. Keep notices for any later copied Apache-licensed code; none was copied now. |
| [Artemis](https://github.com/google/artemis) | Optional development QA candidate; not installed, run or connected. | It launches its own configured automation, not automatically this chat's model. Verify an Astra-only configuration, explicit disposable-device targeting, data handling and controlled setup before a trial. Do not install global agent rules or an accessibility helper on the interrupted personal tablet. Keep deterministic assertions as acceptance; model-reported success is not proof of geometry or stylus timing. |
| [Cavalier Contours](https://github.com/jbuckmccready/cavalier_contours) | Specialist research candidate, not a JTS replacement. | Its arc-aware operations are relevant, but documented rounded-only offset joins differ from our current mitred coping. Native integration, degeneracies, licensing notices and our own difficult cases must be evaluated before adoption. No benchmark or integration was performed here. |
| [Google style guide](https://github.com/google/styleguide) / [Kotlin conventions](https://developer.android.com/kotlin/style-guide) | Code-convention reference, not UI guidance or a pasted agent instruction set. | Keep formatting changes separate from geometry work. Choose and pin one formatter with a focused check before a whole-codebase format pass. No formatter or global rules were installed now. |

Upstream sample descriptions and README claims are not tested compatibility promises. These references do not change the no-runtime-AI rule or reopen a framework contest. Existing Compose/Android test infrastructure stays in place. Performance tools such as Macrobenchmark/Perfetto belong in a physical-device profiling step, not an invented emulator speed claim.

## Rules for the next reusable component

Name the concrete work being replaced and use an existing dependency first when it fits. Prefer a pinned published module over copying its repository. Keep adapters at the rendering/input/geometry boundary, preserve source and license notices, inspect runtime permissions and transitives, and prove one actual application workflow. Avoid two editable representations of the yard. Test an alternative against saved fixtures before changing generator behavior or file compatibility.

Do not install tools speculatively. A promising sample is not a reason to postpone the next user-visible operation indefinitely. After this bounded pass, resume whole-straight-side manipulation through the existing snapping/measurement/Undo path, then the coherent expert UI pass. Dimension-only/no-bubble direction is unchanged.

## Verification checkpoint

Application **0ecdb88fd6d6413615323130addcfe064043e39d** contains dependency cleanup 92d1788b510c1b12c14bca4db2d5607d34031cdd followed by the indexed lookup. The later documentation checkpoint leaves tested source, dependencies, tests and workflows unchanged.

[Unit/build run 34634307414](https://github.com/CRisdon45/Plan_Trace/actions/runs/34634307414) completed **223 app unit/Compose tests with zero failures, errors or skipped tests**, plus **nine separate Python policy tests**, and debug assembly. Downloaded XML from 31 classes independently confirmed the 223 total. Ten new snapping tests include 1,200 seeded indexed-versus-exhaustive queries covering rotated axes, exclusions and prior captures; these are comparisons inside a test, not 1,200 additional test cases in the count. Curved extrema, distant alignment guides, hysteresis, stable ties and finite-bound fallbacks have focused checks.

The actual sparse-scene report contains **512 objects, 2,048 edges and 4,096 finite point references**. At its selected query, the index shortlists **one edge and zero points**, versus the exhaustive set, with the same selected snap. Global corner-guide work remains. This establishes reduced finite candidate work, not lower total latency, faster frames or a 2,048-times speedup.

The resolved runtime reports contain **104 debug and 97 release modules**. Both policy results passed; neither merged manifest includes Internet/network-state permissions or the blocked services. The only listed permission is the AndroidX-generated app-specific non-exported receiver permission. This was checked on generated files, not inferred from source declarations. Release resolution and manifest processing passed, but a release APK was not assembled, signed or installed.

[Android run 34634307450](https://github.com/CRisdon45/Plan_Trace/actions/runs/34634307450) at the same revision completed **all 13 existing scenarios on the first attempt**, with successful app/instrumentation builds. This is a regression replay, not 13 newly written tests or a benchmark of the 512-object scene. It covers hand-placed pool/deck authoring, following coping, protected site geometry, registered-image intake callback/calibration/checks, radial access, live dimensions, off-grid snapping, cancellation, Undo/Redo, save/reopen and portrait/compact command cases. External system picker/share chooser UI and physical S Pen behavior remain unverified.

Final unit artifact **10277204347**, ZIP SHA-256 `f740621670ea8b81e4929ddd49d9299e2fb0f62573f68488d42e0c5c40186a06`. Android artifact **10277594464**, ZIP SHA-256 `6fa2e6178227baad65e12849f6f5e619d40ed0797d73eaac869d7ddbbf76ac03`. Both archives were downloaded and hashes verified before inspection. Seven-day retention is not permanent availability.

All **29 active-window capture records** identify the app. Actual snapped-line, snapped-vertex and reopened-workspace images were opened and unobstructed. The current bubble, verbose controls and overlapping synthetic geometry are unchanged technical fixtures, not accepted Northstar styling or a client layout. The runtime crash buffer was empty, which is not all-flow crash certification. Captures are not pixel-golden regression tests.

All five recorded completed-save before/after process-restart pairs matched byte-for-byte. The final seven-object format-5 draft is revision 51, 7,747 bytes, SHA-256 `478726271bea6645e36b0237df895890dd3f28992a10d4468662353eec31904d`. This verifies completed saves, not edits killed before saving finishes. Source image, locks and earlier objects are checked by the retained scenarios. No schema, project data, file identity or signing configuration changed.

The successful build still logged the known KSP/AWT background NullPointerException. Removing the unused Moshi processor and service plugins did not establish a fix. Compiler and CI-action deprecations remain; no warning-free-build claim is made. No local Android build, Ink/Cahier integration, Artemis execution, physical-device trial or release publication was performed. Only the nine Python policy tests were also run locally.

This pass makes no visual restyle, application PR merge, tablet installation, user-data clearing, signing-key replacement, private-client/reference publication, repository visibility/permission change, runtime AI feature, 3D work or framework/repository merger. Return to the whole-straight-side authoring outcome rather than expanding the reuse review indefinitely.
