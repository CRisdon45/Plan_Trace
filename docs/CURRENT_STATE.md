# Current state and next-session handoff

Updated 2026-09-12 after the blue-water Northstar revision. The current
branch is **feat/expert-workspace-ui**; its verified application head is
`9b8ce97`. Draft PR #6
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

[Android 2D integrity run 34679714157](https://github.com/CRisdon45/Plan_Trace/actions/runs/34679714157)
at `9b8ce97` passed **293 tests with zero failures, errors or skips**, the
nine-test runtime-policy audit, resolved-runtime/manifest checks and debug
assembly. New regression coverage checks blue hue, strong sparse highlights,
cold-cache repeatability, unchanged stored data and concave clipping. Existing
geometry, persistence, translation and export tests remain green.

Downloaded artifact `10293507766` ZIP SHA-256:
`bfc3a8a5ac340eaae3f5a80cd8955d0e26276e60c428c6fa8ca14d4c53deb6bf`.
Focused Northstar PNG SHA-256:
`4b63e67aef28e1605a53894a28a23cf762c0b7d4402ef44c6ea72650ebaaffa6`.
Graphic comparison remains byte-identical to the prior pass at
`845e0d4882b7f6acd1c5b7dc7bbf310932edcb1a3fc45b709eeceaad6f8ad061`.
The 1000x700 organic output and small rectangular water render were opened and
visually inspected. Blue depth, connected curved caustics and short bright crests
are present. The finish is still cleaner and more uniform than the uploaded
hand-painted studies; this is progress toward the target, not owner acceptance.

[Android workspace emulator run 34679714130](https://github.com/CRisdon45/Plan_Trace/actions/runs/34679714130)
at the same revision passed all **19 scenarios**. Artifact `10293872909` was
downloaded and its ZIP SHA-256 verified as
`4f68b8acc262102988f23c30a47f18758ffd731f411d3923be823510a0fbefcd`.
Live Northstar and 1400x1000 export screenshots were opened and inspected. The
blue palette and caustics are present in both rectangular and curved pools.
The crash buffer is empty and Android reports no ANR since boot. Appearance
switching left saved project JSON byte-identical before/after at
`bff7d25cb4306b85e3312eab7adf818d509c5b82828842d5d5cc863a877590df`.
Northstar export SHA-256:
`de9dc6fdc9d7d42138de502e63d188caf254c04f920c32d6043a17fe698af7c9`.
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
Further visual work should strengthen broad watercolor variation and vary caustic
rhythm without returning to mint water or uniformly increasing line coverage.
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
