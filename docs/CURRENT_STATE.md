# Current state and next-session handoff

Updated 2026-09-12 after the grass *wash-structure* revision. The current
branch is **feat/expert-workspace-ui**. This session rewrote the production
grass recipe in `NorthstarGrassPaint.java` and thinned silhouette accents in
`NorthstarGroundMaterials`. It did not run Android CI, merge a PR, or claim
9/10. Draft PR #6 is unchanged.

## Active owner correction: stop covering the washes

The owner set an honest **9/10** bar and rejected stamp-count finishing.
Close-ups of `f11d5fd` read as digital grass: stages 1–3 were watercolor,
then 2,800 + 12,000 marks buried the paper. The current painter uses few
related washes, reserve tongues, broken front sediment and sparse clustered
deposits. `dry()`, Kubelka-Munk tables, object-space seeds and the 1024-pixel
cap are unchanged. See [the grass study](GRASS_PAINT_STUDY.md).

Local Java compilation of the production core and adapter succeeded. Two
seeds were inspected at full frame and close crop. Stage-operation checks
pass on those pixels (lift, new deposition, olive-black tail at luminance
110). That is not owner acceptance, tablet timing, or an emulator gate.

Water and decking remain paused. Godot as a Northstar presentation plugin
is still the likely path to the remaining quality points; it is not in
this commit. Pen, project authority and Technical/Graphic stay native.

## Previous checkpoint (stamp finishing at `f11d5fd`)

Updated 2026-09-12 after the grass texture and final-detail revision. The current
branch is **feat/expert-workspace-ui**; its verified application head is
`f11d5fd271a515a5f868614176f5356175e02e9d`. Draft PR #6
targets the merged straight- and smooth-pool interaction seam in
**feat/project-geometry-seam**. Do not reset the application branch to an older
documentation checkpoint.

### Grass finishing steps 4 and 5 (historical)

The latest owner feedback accepts the improvement at `5bf421b` but identifies
it as approximately step 3 of the supplied close-up study. Current work extends
it through texture lifting and final detail. The first three paint stages are
preserved. The added passes and exact-path edge accents are described in
[the grass study](GRASS_PAINT_STUDY.md). The verified evidence below records
this follow-up separately from the original pigment revision.


The owner rejected the grass at `f52ea32` as **5/10** and explicitly set a **9/10**
visual goal. The earlier checks below establish technical behavior, not accepted
art quality. Water and decking are paused. The current revision replaces the
old grass paint construction; see [the grass study](GRASS_PAINT_STUDY.md) for the
reference analysis, mathematical model and honest acceptance criteria. The new build/unit gate passed 303 tests; the real emulator gate passed all
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

[Android 2D integrity run 34708899510](https://github.com/CRisdon45/Plan_Trace/actions/runs/34708899510)
at `f11d5fd` passed **303 tests with zero failures, errors or skips**, the nine-test
runtime-policy audit, resolved-runtime/manifest checks and debug assembly.
Downloaded XML independently confirms the totals. The new finishing test verifies
substantial lifting, new deposition and an increased dark range across cumulative
stages 3/4/5. It does not assign an artistic quality score. Existing checks cover
pigment mass, drying fronts, cold-cache repeatability, seed variation, translation,
exact concave clipping, unchanged serialization and grass opacity.

Unit artifact `10302847012` ZIP SHA-256:
`68db13812d43c55fc292c5ee2ae32bade6a5f8ff0fe69e137e1024a6f7573b49`.
Actual 1150x850 turf detail SHA-256:
`ed12accf2c7fe2760ab080c94816588f89da96b20bb9e29c1fe6fb4ed87f573e`.
Full turf and concave Android renders were opened and inspected. Android stage
3/4/5 pixels exactly match the desktop production-core study. The first three
stages remain unchanged from `5bf421b`. Three finishing iterations and two seeds
were inspected. Water detail, polygon-water wash, caustic study, Graphic organic
plan and travertine detail/concave images remain byte-identical to `5bf421b`.
The Java core and adapter also compile locally with Java 11 targets; no local
Android/Gradle build ran.

[Android workspace run 34708899494](https://github.com/CRisdon45/Plan_Trace/actions/runs/34708899494)
at the same `f11d5fd` app revision passed **all 20 scenarios**. Downloaded per-case
logs independently confirm 20 `OK (1 test)` results. The actual 1600x1000 live
workspace, 1800x1000 PNG export and reopened workspace were opened and inspected;
all show the completed texture and final detail. Active-window records identify
the intended development app. The crash buffer is empty and Android reports no
ANR since boot. These are synthetic API-35 emulator results, not tablet timing.

Emulator artifact `10302124945` ZIP SHA-256:
`8f04f8305c87b1a9836615b6340f93d70a689047792b06d4c43b3f599ddba165`.
Actual two-material export SHA-256:
`0c86e024f81dbf9bd32c5bd30b473ec6d200bbedae9aa89a8a7e78e02f49475a`.
Appearance switching preserves saved JSON byte-for-byte at
`3698c9489fb04f80ca011420954b1c2174a11dca0a08e8a5482653fb9ba6063c`.
Technical and Graphic exports remain byte-identical to `5bf421b`. The grass
fixture follows normal save/reopen/export paths and restores the preceding
synthetic project through monotonically increasing store revisions.

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
