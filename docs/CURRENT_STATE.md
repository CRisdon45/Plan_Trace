# Current state and next-session handoff

Updated 2026-09-12 after the layered-polygon Northstar water revision. The current
branch is **feat/expert-workspace-ui**; its verified application head is
`c1a06585a443d0e49389e9995ad40dbb2f908842`. Draft PR #6
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
pale mint fill. A finer connected caustic network uses a shared smooth displacement
field, varied strength, soft halos and short white crests. Geometry is normalized
to object bounds, independent of view zoom, and cached for up to 24 aspect/identity
pairs. The bounded pigment bitmap retains its separate 12 MiB cache. Both layers
survive cache eviction deterministically. Selective shoreline deposits and final
ink remain clipped/anchored to exact geometry. This follows the owner's blue-water
landscape-plan studies, not the pale freeform-pool reference. Graphic and Technical
palettes are unchanged.
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

[Android 2D integrity run 34681246579](https://github.com/CRisdon45/Plan_Trace/actions/runs/34681246579)
at `c1a0658` passed **294 tests with zero failures, errors or skips**, the
nine-test runtime-policy audit, resolved-runtime/manifest checks and debug
assembly. New regression coverage checks blue hue, strong sparse highlights,
cold-cache repeatability, unchanged stored data and concave clipping. The new
wash-only regression separates broad and fine pigment variation from caustics,
checks different object seeds and cold-cache identity, and retains diagnostic
images. Its 48-pixel block-mean brightness range is 64.16833/255, and its
six-pixel local residual is 2.8023088/255. These are fixture measurements, not
reference-match scores or tablet performance measurements. Existing
geometry, persistence, translation and export tests remain green.

Downloaded artifact `10293707944` ZIP SHA-256:
`e2c56b4d213c0211e157396d1f452ca7c8f3d48f983350fbeb7a743c3b83375d`.
Focused Northstar PNG SHA-256:
`602b6fe9558dfedf6f697bbe3f68cdf0442b77f083dfa5dacc321c22e7102975`.
1200x800 rectangular detail SHA-256:
`780f46c45ce1b54329d1e2f81838463e7ca9e4f44494f28bc417bae9cae80766`.
Graphic comparison remains byte-identical to the prior pass at
`845e0d4882b7f6acd1c5b7dc7bbf310932edcb1a3fc45b709eeceaad6f8ad061`.
The 1000x700 organic output, 1200x800 rectangular detail and wash-only diagnostic
were opened and visually inspected against the supplied blue-water studies.
Overlapping blue masses, smaller dark blooms, translucent ragged transitions
and fine pigment grain are visible under the existing connected caustics. This
is materially more painterly than `9b8ce97`, but the caustic widths/junctions are
still more uniform and geometric than the references. Do not claim full Northstar
or owner visual acceptance. The earlier polygon pass `693f009` also passed 294
tests; its emulator run was superseded/cancelled when the fine-grain pass pushed.
Local Gradle could not bootstrap (unavailable distribution/network); nine Python
policy tests and diff validation ran locally. Android build evidence is CI-only.

[Android workspace emulator run 34681246577](https://github.com/CRisdon45/Plan_Trace/actions/runs/34681246577)
at the same revision passed all **19 scenarios**. Artifact `10294565201` was
downloaded and its ZIP SHA-256 verified as
`45b0edb9d1c28603c6e7cd73d6b9d5ec3700c061c3991be30b0e21a2957b9959`.
Live Northstar and 1400x1000 export screenshots were opened and inspected. The
layered blue pigment and caustics are present in both rectangular and curved pools.
The active-window record identifies the actual development app. This remains an
overlapping synthetic interaction fixture, not a composed client plan.
The crash buffer is empty and Android reports no ANR since boot. Appearance
switching left saved project JSON byte-identical before/after at
`b647bc31080331ea17deaacf3e834b7097e560558d9658948ea21adab7291bb8`.
Northstar export SHA-256:
`43c9f6d96540859baec41e3cefc33ab8c83c68c46f51f1e12b63a128b0d99218`.
Technical and Graphic exports remain byte-identical to the previous checkpoint.

Physical S Pen, palm rejection, hardware frame time and owner visual acceptance
remain unverified. The synthetic fixtures are not client properties. No reference
images are published. PDF keeps the shared deterministic renderer; a simpler
non-artistic fallback remains acceptable if tablet export cost warrants it.

## Next outcome

Review this bounded slice into **feat/project-geometry-seam** without merging it
implicitly. First review the wash and its interaction cost on the target latest-
Android tablet; do not infer stylus or frame-time acceptance from the emulator.
Review the blue-water output against the owner's preferred landscape-plan studies.
Further visual work should refine caustic rhythm, tapered light, organic junctions
and pigment edge deposits to the full-detail reference bar. Retain the new
multiscale blue glazes and fine grain; do not return to mint water or substitute
uniformly heavier white line coverage. Measure cold texture generation and warm
redraw cost on the actual tablet before making responsiveness claims.
The next bounded geometry outcome remains rectangle/convex-polygon pool
generation using the same editable objects, target water area and explicit
outside-coping containment. Preserve direct pen placement, exact geometry,
whole-action Undo, and the now-verified Technical/Graphic/Northstar rendering
path. Do not replace this with a numeric-form milestone or restart solver/library
research.

## Boundaries still in force

No persistent per-edge tangent/radius locks, general linked solver, footprint
generator, shared curved surfaces, attached shelves/steps/spas, alternatives,
full landscape scope, portable backup or Northstar acceptance is delivered.
Physical S Pen/button/hover/palm behavior and full-layout drafting remain
pending. Private references and client geometry remain outside this public
repository. The physical tablet, installed app, signing key and user data were
not changed. No application PR was merged at this checkpoint.
