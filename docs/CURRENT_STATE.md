# Current state and next-session handoff

Updated 2026-09-12 after the grass-only pigment revision. The current
branch is **feat/expert-workspace-ui**; its verified application head is
`5bf421bf360c31e8c9c409f905ecdce3b8aa92f6`. Draft PR #6
targets the merged straight- and smooth-pool interaction seam in
**feat/project-geometry-seam**. Do not reset the application branch to an older
documentation checkpoint.

## Active owner correction: grass only

The owner rejected the grass at `f52ea32` as **5/10** and explicitly set a **9/10**
visual goal. The earlier checks below establish technical behavior, not accepted
art quality. Water and decking are paused. The current revision replaces the
old grass paint construction; see [the grass study](GRASS_PAINT_STUDY.md) for the
reference analysis, mathematical model and honest acceptance criteria. The new build/unit gate passed 302 tests; the real emulator gate passed all
20 scenarios. Do not describe the visual
bar as achieved merely because tests pass.

## Delivered on this branch

The real Android workspace now gives the drawing substantially more visual
priority without changing project geometry, persistence, commands, Undo/Redo or
signing. Object switching is a compact icon navigator with accessible object
names and lock state. Empty selection/status context disappears; when context
is useful it occupies a single compact bar. Smooth-mode and invalid-edit
messages are concise, and a rejected edit presents its specific reason once
while retaining its invalid accessibility category.

Routine pen segments now show only the formatted dimension beside the target.
The prior `Line` label, feedback bubble and grid narration are absent. Closing
the outline remains intentionally distinct as `Close · <dimension>`, and the
visible Close/Back/Cancel commands now share one compact strip across straight
and smooth authoring. Point count and right-angle mode remain visible, while the
permanent two-line pen instruction is gone. The underlying snap guide, exact
coordinate handling and stable radial command directions are unchanged. No
runtime dependency, schema migration, runtime AI or 3D work was added.

The authoritative design projection now has three presentation modes:
Technical, Graphic and Northstar. Object kinds deterministically select their
presentation role: pool and spa use water, paving uses paving, turf uses turf,
gravel uses gravel, walls use masonry, and coping uses paving. Site outlines
remain unfilled. Ground surfaces render beneath pools and spas, with walls
above them. Technical removes material fills, Graphic uses flat material fills,
and Northstar adds deterministic tonal structure clipped to the exact object
path. Water now uses a calm sheet-directed depth field and selective broken
edge deposits; paving and coping use a much quieter field. Northstar water also
has a bounded deterministic pigment field: paper variation, wetness, mobile pigment
and deposited pigment evolve for a fixed 32 settling steps on a grid capped at
128 cells per side. Results are seeded from stable object identity, geometry and
color, cached within 12 MiB, and clipped again by the exact vector path. Moving
an unchanged object therefore moves its existing wash instead of repainting it.
Northstar water now uses a dedicated blue/turquoise palette rather than Graphic's
pale mint fill. Caustics now use filled ribbons with continuously varying width,
tapered crests, fine edge variation and brighter confluences. Uneven site density
varies cell size, and successive invertible shears curve the shared network
without tearing its junctions. Four light passes supply soft shoulders, narrow
cores and selective crests. Visual iteration reduced excessive swirls and uniform
glow in the first ribbon pass. Geometry is normalized
to object bounds, independent of view zoom, and cached for up to 24 aspect/identity
pairs. The bounded pigment bitmap retains its separate 12 MiB cache. Both layers
survive cache eviction deterministically. Selective shoreline deposits and final
ink remain clipped/anchored to exact geometry. This follows the owner's blue-water
landscape-plan studies, not the pale freeform-pool reference. Graphic and Technical
palettes are unchanged.
Hardware canvas caustics now use explicit prefiltered image levels generated
from those same ribbons, starting at a 1024-pixel longest side and cached within
24 MiB including all levels. The nearest level at or above display resolution is
selected from the actual canvas scale and object bounds, integrating thin light
before drawing at small working sizes without enlarging an undersized level.
The raster cache includes opacity; cache eviction remains deterministic. Software PNG/PDF
output keeps the vector caustics. Raster minification and vector export may differ
slightly in antialiasing; neither representation changes the project boundary.
The water now also contains independently implemented Hobbs-inspired polygon
glazes: four blue pigments interleave across broad washes, medium blooms, small
deposits and fine marks. Recursively displaced edges retain local variation;
gaps in individual layers and correlated paper-tooth coverage supply fine grain.
A lighter blue ground and less dominant linear depth cue leave room for the
stacked pigment. `NorthstarPolygonWash.kt` caches a bounded 768-pixel-longest-side
raster within an additional 12 MiB. The existing deposition field and caustic
paths remain complementary layers, with exact clipping in the shared renderer.
No upstream source/art or new dependency was imported. See the method/reuse
record in REUSE_AND_DEPENDENCIES.md. The full detail in the owner's blue-water
studies remains the target; recommendations to omit minutiae are not a constraint.
This is an independently implemented CPU reference model, not a new dependency
or a full fluid simulation. Northstar line weight also
establishes deck below coping below water without changing Technical or Graphic
weights. Northstar is the current default for both the real workspace canvas and
PNG/PDF output. These styles are transient projection data: changing appearance
does not change saved geometry, object ordering, IDs, hit testing or revision.

Technical, Graphic and Northstar are now exposed through an Appearance command
appended to the existing View radial fan. Fit, Grid and Site retain their learned
directions. The compact dialog shows all three modes and the current selection;
choosing a row applies immediately without an Apply/Accept step. The current mode
is also exposed as command state for accessibility and compact layouts. It is
stored in the existing workspace-view preferences, survives activity recreation,
and is passed to both the live canvas and PNG/PDF output. It remains outside the
authoritative project JSON and does not consume Undo/Redo.

The current grass painter replaces the earlier polygon/stroke construction with
related, recursively displaced washes, pigment redistribution to each wash's
actual drying front, shared paper deposition, small reserves and ordered RGB
Kubelka-Munk glazes. `NorthstarGrassPaint.java` supplies the production pixels;
the desktop study adapter uses that same core. Seven local visual revisions
addressed angular/stamped shapes, yellow cast, flat or excessively grainy
underpainting, uniform fronts, aligned flecks and regular reserve holes. This
is an independently implemented artistic approximation informed by Hobbs and
Curtis et al., not a full fluid solver or measured spectral pigment model.

Hardware grass preparation runs on one worker, with at most three pending washes.
The initial flat underpainting remains visible until paint completion invalidates
the actual Compose canvas. Software output waits for or computes the finished
paint. The baked grass includes its underpainting, so surface opacity is applied
once. Translation and zoom retain stable pigment. The existing 1024-pixel-longest-
side cap, prefiltered levels and 24 MiB ground cache remain. Cold generation is
appreciable; many-surface cache pressure and physical tablet frame times are not
established. No responsiveness claim follows from the worker alone.

Travertine retains the previous floating-point layered stone paint and joint
renderer. Its comparison images are byte-identical to `f52ea32`; grass changes do
not imply acceptance of the paused decking. Exact silhouettes and final linework
remain in the shared renderer. No reference pixels, runtime service or dependency
were added. The current source's visual bar remains 9/10, with owner acceptance
unresolved; see [the grass study](GRASS_PAINT_STUDY.md).

Travertine's initial 12x24-inch running bond respects calibrated drawing units
and follows object movement. It is a presentation default, not a specified
product, cut layout or quantity model. Uncalibrated legacy objects use a
proportionate preview; giant extents have bounded grid density. Coping receives
stone paint without a deck grid crossing its enclosing polygon. Technical and
Graphic retain their flat fills and geometry. Reference furniture, planting and
their cast shadows are not baked into a reusable surface texture. Water remains
paused for later owner review; only polygon-helper visibility changed in its
implementation. No new dependency, reference pixels or runtime AI was added.

## Verified evidence

[Android 2D integrity run 34706762595](https://github.com/CRisdon45/Plan_Trace/actions/runs/34706762595)
at `5bf421b` passed **302 tests with zero failures, errors or skips**, the nine-test
runtime-policy audit, resolved-runtime/manifest checks and debug assembly. The
downloaded XML independently confirms these totals. Three new pigment tests
check conserved deposited mass, concentration at external/internal wet fronts,
unchanged dry paper, no invented image-crop rim, and thin fractional masks. The
shared-renderer checks also cover cold-cache repeatability, seed variation,
translation, exact concave clipping, unchanged serialization and correct grass
opacity. The local production core and PNG adapter compile with Java 11 targets;
no local Android/Gradle build ran.

Unit artifact `10302150595` ZIP SHA-256:
`d71713c087e849424b685c130b75368a56891f25b676aa7874ab73669274636b`.
The actual 1150x850 turf detail SHA-256 is
`34595dbd221ab3ccde94b2799f31489007e3d784cef4df2f8a2ed06dc6c7fa23`.
Its 60px block range is 63.82 and 4px residual is 11.81; these are implementation
characterization, not quality scores. Actual turf detail and concave images were
opened and inspected. The water detail, polygon-water wash, caustic study,
Graphic organic plan, and travertine detail/concave images are byte-identical to
`f52ea32`. Seven local paint iterations and three inspected seeds informed the
visual changes; no reference pixels or flattering crops serve as test fixtures.

The earlier `00712dd` CI runs were superseded by the final grain adjustment and
cancelled. They are not reported as passing checks. [Android workspace run 34706762612](https://github.com/CRisdon45/Plan_Trace/actions/runs/34706762612)
at the same final app revision passed **all 20 scenarios**. Downloaded per-case
instrumentation logs independently confirm 20 `OK (1 test)` results. The real
1600x1000 live grass view, 1800x1000 export and reopened view were opened and
inspected: all contain the completed new paint, not the temporary flat fill.
Active-window records identify the intended development app. The crash buffer
is empty, and Android reports no ANR since boot. These are synthetic API-35
emulator results, not physical-tablet performance or visual acceptance.

Emulator artifact `10302136925` ZIP SHA-256:
`7c37ca09508bac95ff78b48782e1478c04eb7bd355a4d121b7e718fe98902222`.
Actual two-material export SHA-256:
`79e45b41c0110e40b622871543f4c3232bbe673d27dded35409bd8668a206ca7`.
Appearance switching preserves saved JSON byte-for-byte at
`69ec97d1ed89eb324513238c895a52e997a6d5601f6a59a9a12b5c9622520d19`.
Technical and Graphic exports remain byte-identical to `f52ea32`. The grass
fixture uses the existing normal save/reopen/export paths and restores the
preceding synthetic project through monotonically increasing store revisions.

Physical S Pen, palm rejection, hardware frame time and owner visual acceptance
remain unverified. No original app/data, tablet installation, signing change or
application PR merge was performed.

## Next outcome

Stay on grass until the owner accepts the focused reference match. Judge the
full surface and its cumulative layer sheet together with real workspace and
export captures. The target remains 9/10; do not self-certify it from tests,
color statistics, increased detail, or the existence of a watercolor algorithm.
Keep scrutinizing connected wash structure, selectively sharp fronts with
interior pigment buildup, paper reserves, slightly bled segments and varied
minute deposits. Do not revert to the rejected `f52ea32` grass or treat its
technical checks as visual approval. Water and decking remain paused.

Measure cold preparation, cache pressure and warm redraw on the latest-Android
target tablet before claiming pen responsiveness. Preserve exact geometry,
whole-action Undo, persistence and all three appearances. The queued geometry,
planting and 3D work is not reopened by this material study. Draft PR #6 remains
unmerged, targeting **feat/project-geometry-seam**.

## Boundaries still in force

No persistent per-edge tangent/radius locks, general linked solver, footprint
generator, shared curved surfaces, attached shelves/steps/spas, alternatives,
full landscape scope, portable backup or Northstar acceptance is delivered.
Physical S Pen/button/hover/palm behavior and full-layout drafting remain
pending. Private references and client geometry remain outside this public
repository. The physical tablet, installed app, signing key and user data were
not changed. No application PR was merged at this checkpoint.
