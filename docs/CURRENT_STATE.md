# Current state and next-session handoff

Updated 2026-09-12 after the flowing-caustic Northstar water revision. The current
branch is **feat/expert-workspace-ui**; its verified application head is
`1a0836a1e94c656bde62347117ac0520165a78ae`. Draft PR #6
targets the merged straight- and smooth-pool interaction seam in
**feat/project-geometry-seam**. Do not reset the application branch to an older
documentation checkpoint.

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

## Verified evidence

[Android 2D integrity run 34701250784](https://github.com/CRisdon45/Plan_Trace/actions/runs/34701250784)
at `1a0836a` passed **296 tests with zero failures, errors or skips**, the
nine-test runtime-policy audit, resolved-runtime/manifest checks and debug
assembly. Regression coverage checks blue hue, strong sparse highlights,
cold-cache repeatability, unchanged stored data and concave clipping. The
wash-only regression separates broad and fine pigment variation from caustics,
checks different object seeds and cold-cache identity, and retains diagnostic
images. Its 48-pixel block-mean brightness range is 64.16833/255, and its
six-pixel local residual is 2.8023088/255. These are fixture measurements, not
reference-match scores or tablet performance measurements. Existing
geometry, persistence, translation and export tests remain green. New isolated
caustic evidence checks cache repeatability, different object seeds, fine threads,
broader folds and preserved open water. In its 1000x600 fixture, 25,081 pixels have
alpha above 140 (4.18%), and 2,682 have alpha above 220 (0.45%). Scan intersections
include both 1–2-pixel threads and 4–9-pixel folds/confluences. These characterize
this fixture and do not establish physical optics or owner acceptance.
The live-raster branch also has cold-cache, opacity, destination-containment and
equivalent-screen-size coverage. Actual hardware-canvas screenshot review confirms
continuous fine light in the rectangular pool, small spa and curved pool.

Downloaded artifact `10299819332` ZIP SHA-256:
`2fe1bd3d0cf17712c4d92971db32ddd86475a7c0829a1cfd64d59d23b03520c7`.
Focused Northstar PNG SHA-256:
`7b21aac4261a1a1fd470fb807601b0f5320497de9c45f97c23fb21bc98b1b743`.
1200x800 rectangular detail SHA-256:
`ed10e188282b1a9357b383972faaee9c7092c2d0df42d63da7368abddc6e280b`.
The focused vector outputs and caustic study remain byte-identical to `1808ec5`
after the hardware minification fix.
Graphic comparison remains byte-identical to the prior pass at
`845e0d4882b7f6acd1c5b7dc7bbf310932edcb1a3fc45b709eeceaad6f8ad061`.
The 1000x700 organic output, 1200x800 rectangular detail and isolated caustic
study were opened and visually inspected against the supplied blue-water studies.
The final light has gentler flowing contours, tapered brightness and fine/broad
variation while leaving the layered pigment visible. The implementation review
considers this caustic pass ready for the next material study. Owner visual
acceptance and the full Northstar composition remain separate pending outcomes.
The pigment-only image remains byte-identical to `c1a0658`, SHA-256
`f83b846f697832aa5954d0bbf6fd7bf1150a88205473ea2b244444675ac9778e`.
The first ribbon pass `2ca2741` passed 295 tests but was visually revised to reduce
repeated swirls and excessive uniform light; its emulator run was superseded.
The refined-vector pass `1808ec5` passed 295 tests and 19 emulator scenarios, but
its actual hardware screenshot exposed dotted/fragmented small caustics despite
correct exports. That visual failure prompted the filtered live rendering fix.
The first filtered pass `9af3ee6` passed 296 tests but its hardware mipmap hint
was insufficient; the live screenshot still broke up. The explicit prefiltered
levels replace that hint. The equivalent-screen-size regression exercises the
canvas transform used to select the level.
The first explicit-level pass `9f90d3a` resolved the visible breakup but selected
a level below screen resolution, softening the small pools too much. The final
selection uses the nearest prefiltered level at or above display resolution,
limiting the remaining reduction to less than 2:1 while preserving more detail.
Android builds and policy checks for this pass ran in CI. No local Android
build ran; the Gradle distribution was unavailable in this workspace.

[Android workspace emulator run 34701250774](https://github.com/CRisdon45/Plan_Trace/actions/runs/34701250774)
at the same revision passed all **19 scenarios**. Artifact `10300501791` was
downloaded and its ZIP SHA-256 verified as
`2a4f819609af2f9e54e8f947a113d2e75da4b73cf2631eca6f7d4c8aefcbe17b`.
Live Northstar and 1400x1000 export screenshots were opened and inspected. The
layered blue pigment and caustics are present in both rectangular and curved pools.
The final live view preserves fine continuous contours without the fragmentation
of the vector-only hardware pass or the blur of the undersized image level.
The active-window record identifies the actual development app. This remains an
overlapping synthetic interaction fixture, not a composed client plan.
The crash buffer is empty and Android reports no ANR since boot. Appearance
switching left saved project JSON byte-identical before/after at
`d856cbaf925e0ce42a6d6cfb14423805d1f236bd8c0f8facd18f98dfd850b8e7`.
Northstar export SHA-256:
`2ded46c30c9e98b2ba2b0fb9da5d38aa3ed806645720fe6f06fe16cfd98a93a4`.
Technical and Graphic exports remain byte-identical to the `c1a0658` checkpoint.

Physical S Pen, palm rejection, hardware frame time and owner visual acceptance
remain unverified. The synthetic fixtures are not client properties. No reference
images are published. PDF keeps the shared deterministic renderer; a simpler
non-artistic fallback remains acceptable if tablet export cost warrants it.

## Next outcome

The owner's requested sequence is **caustics, then grass and decking**, using the
staged watercolor layering in the uploaded material study. Preserve this water
pass while developing layered green washes/texture and warm paving washes,
material variation and subordinate joint ink through the actual renderer. Keep
the full reference detail as the bar and inspect both working-view and export
images. Do not substitute a separate polished demonstration for app output.

Measure cold texture generation and warm redraw on the target latest-Android
tablet before claiming pen responsiveness. Review the bounded application slice
into **feat/project-geometry-seam** without implicitly merging it. The queued
rectangle/convex-polygon geometry milestone remains behind this material work;
preserve direct pen placement, exact geometry, whole-action Undo and all three
appearances. Do not restart solver/library research or the paused 3D work.

## Boundaries still in force

No persistent per-edge tangent/radius locks, general linked solver, footprint
generator, shared curved surfaces, attached shelves/steps/spas, alternatives,
full landscape scope, portable backup or Northstar acceptance is delivered.
Physical S Pen/button/hover/palm behavior and full-layout drafting remain
pending. Private references and client geometry remain outside this public
repository. The physical tablet, installed app, signing key and user data were
not changed. No application PR was merged at this checkpoint.
