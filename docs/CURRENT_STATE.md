# Current state and next-session handoff

Updated 2026-09-12 after the layered grass and travertine revision. The current
branch is **feat/expert-workspace-ui**; its verified application head is
`f52ea3212ddc283ef4d0be5e3d57e7ce566fc957`. Draft PR #6
targets the merged straight- and smooth-pool interaction seam in
**feat/project-geometry-seam**. Do not reset the application branch to an older
documentation checkpoint.

## Active owner correction: grass only

The owner rejected the grass at `f52ea32` as **5/10** and explicitly set a **9/10**
visual goal. The earlier checks below establish technical behavior, not accepted
art quality. Water and decking are paused. The current revision replaces the
old grass paint construction; see [the grass study](GRASS_PAINT_STUDY.md) for the
reference analysis, mathematical model and honest acceptance criteria. New Android
verification is pending at this source checkpoint. Do not describe the visual
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

Grass and travertine now have dedicated material paint through the shared live
and export renderer. `NorthstarGroundMaterials` reuses the polygon displacement
helper for overlapping glazes, selective sharp drying fronts, lifted paper,
fine granulation, clustered grass flecks/blades and mineral pores. Grass uses
deeper yellow-green/olive washes with uneven concentrations and luminous gaps.
Travertine uses warm ivory/ochre/taupe with subordinate crisp joint ink. Floating-
point accumulation prevents faint neutral layers acquiring pink/green casts;
the finished paint is converted once to ordinary ARGB. Fixed 1024-pixel-longest-
side textures and their prefiltered levels share a 24 MiB cache. Fine detail
softens beyond that texture resolution; target-tablet timing is not established.
Selective dry deposits follow the actual boundary inside its exact clip.

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

[Android 2D integrity run 34704034341](https://github.com/CRisdon45/Plan_Trace/actions/runs/34704034341)
at `f52ea32` passed **299 tests with zero failures, errors or skips**, the
nine-test runtime-policy audit, resolved-runtime/manifest checks and debug
assembly. Downloaded XML independently confirmed the test totals. The new ground
checks cover both materials after cache eviction, translation, neighboring seeds,
concave clipping, unchanged serialized elements, zero opacity, unchanged Graphic
fills, physical joint calibration equivalence, broad/fine paint variation and
neutral stone hue. Tests characterize implementation behavior, not visual acceptance.

Unit artifact `10301162860` ZIP SHA-256:
`077bb907330b2219acc1fdaeefa69c26fd8ea745c8eb8aec223d443470f864f1`.
Actual 1150x850 renderer details, composited onto paper for inspection:

- Grass PNG: `a17d9dd771f1d45e93c9ce88b584625ffdac356feaf17878a19e4a7388943763`.
- Travertine PNG: `60fb1e74389a0046e52fa124b6bbaea7e09e8005346dc141020ddc46a0456610`.

The final grass fixture has a 60-pixel block brightness range of 87.44324/255 and
a four-pixel local residual of 3.971203/255. Travertine measures 32.77011/255 and
3.101962/255 respectively. These are diagnostic measurements, not reference-match
scores or tablet performance results. The final details and supplied focused
material study were opened and inspected. The initial `a2a434f` pass passed 299
tests but exposed pink/green drift in faint stone glazes and embossed grass marks.
`9be53af` corrected compositing and deposits; its grass remained too pale.
`f52ea32` deepens selective olive concentrations while preserving light gaps.
Earlier emulator runs were superseded by the visual refinements.

Water-only detail, pigment-only and caustic-study images remain byte-identical
to `1a0836a`; Graphic organic output is also byte-identical. Its prior caustic
implementation remains described above and in the previous checkpoint. Northstar
organic output now includes the new stone treatment on its coping.

[Android workspace run 34704034310](https://github.com/CRisdon45/Plan_Trace/actions/runs/34704034310)
at the same application revision passed all **20 scenarios**, independently
counted from the retained instrumentation results. Artifact `10301547860` ZIP
SHA-256: `48bbbd292899248bde7480fe75646b7580ddbf377b599bef4fabc06f3dd9f861`.
The new scenario places two synthetic material objects in the actual workspace,
selects Northstar through the View command, captures the live app, exports the
same geometry and recreates the activity. It restores the preceding synthetic
fixture through increasing store revisions without clearing application data.
The live and reopened images and 1800x1000 export were opened and inspected;
the active-window records identify the development app. Fine material marks and
joint ink remain visible at working scale. This is a material study, not a full
landscape composition or physical-tablet performance result.

The actual two-material export SHA-256 is
`b03196946e4445dd8399fea195d15d95d8dd9cae24266247a83b85eb29ca1391`.
Appearance switching preserved saved JSON byte-for-byte at
`de083f287a3f408447bc60e16f8d96f39cc79312b2eb2c581c2945a8cbbd8e7f`.
Technical and Graphic exports remain byte-identical to `1a0836a`. The runtime
crash buffer is empty, and Android reports no ANR since boot.

Android builds ran in CI; no local Android build ran because the Gradle
distribution is unavailable here. Physical S Pen, palm rejection, hardware frame
time and owner visual acceptance remain unverified. Synthetic fixtures are not
client properties, and reference pixels are not published. PDF keeps the shared
deterministic renderer; a simpler fallback remains acceptable if tablet export
cost warrants it. No physical tablet, original app, signing key or user data was
changed. No application PR was merged.

## Next outcome

Review grass and travertine against the owner's focused layered material study,
including close detail and actual working scale. Preserve the full reference
bar: translucent washes, crisp drying fronts, bleed within paint, mineral pores,
grass detail and coherent joint hierarchy. Owner acceptance and a complete
Northstar composition remain pending. The water is explicitly paused for later
review, not reopened by material work. Contextual planting, furniture and cast
shadows should come from their own objects when that scope is taken up.

Measure cold generation and warm redraw on the target latest-Android tablet
before claiming pen responsiveness. Review the bounded application slice into
**feat/project-geometry-seam** without implicitly merging it. The queued
rectangle/convex-polygon geometry milestone remains behind the current material
work. Preserve direct pen placement, exact geometry, whole-action Undo and all
three appearances. Do not restart solver/library research or the paused 3D work.

## Boundaries still in force

No persistent per-edge tangent/radius locks, general linked solver, footprint
generator, shared curved surfaces, attached shelves/steps/spas, alternatives,
full landscape scope, portable backup or Northstar acceptance is delivered.
Physical S Pen/button/hover/palm behavior and full-layout drafting remain
pending. Private references and client geometry remain outside this public
repository. The physical tablet, installed app, signing key and user data were
not changed. No application PR was merged at this checkpoint.
