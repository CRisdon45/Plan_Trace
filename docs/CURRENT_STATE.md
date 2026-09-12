# Current state and next-session handoff

Updated 2026-09-12 after the verified presentation-mode selector slice. The
current branch is **feat/expert-workspace-ui**; its verified application head is
`ecfb845`, followed only by this checkpoint documentation. Draft PR #6
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
and Northstar adds one broad deterministic tonal cue clipped to the exact object
path. Northstar is the current default for both the real workspace canvas and
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

[Android 2D integrity run 34663149927](https://github.com/CRisdon45/Plan_Trace/actions/runs/34663149927)
at `ecfb845` passed **287 tests with zero failures, errors or skips**, including
the appearance-only projection contract, deterministic clipped tonal rendering,
safe preference decoding, immediate selector behavior, current-mode semantics,
unchanged learned radial directions, the compact drawing-strip
behavior/accessibility contract, live drawing and
object navigation, the separate nine-test runtime policy audit, resolved-runtime
and manifest checks, and debug assembly.
The downloaded report archive digest is
`ed39bf88d7e5b7830698695c5ba749fe76a560895250d42fd5faf98e22d049b1`.

[Android workspace emulator run 34663149892](https://github.com/CRisdon45/Plan_Trace/actions/runs/34663149892)
passed **all 19 scenarios**, including hand-placed pool/deck outlines,
held-pointer live distances, exact snapping, rejected/cancelled side edits,
smooth-pool creation and editing, save/restart and reopening, plus Technical,
Graphic and Northstar selection through the real View command. Its artifact
digest is `c2bee666509b936a45b1aa4f071382307d66e96d0ebdc9537a4b3db6224d2dbb`.
The downloaded archive matched that digest. Visual inspection of the real
workspace, selector and three 1400x1000 vector PNGs confirms that Technical is
linework-only, Graphic is flat material color and Northstar adds the same clipped
tonal cue in canvas and export. All three use identical smooth/concave project
geometry. Project JSON before and after the selector scenario was byte-identical
with SHA-256 `610f0f6a4211b1032f3dd59d56f7547dce01eaa0b11d52f4ed940293bd536ab4`.
The site raster remains a reference layer behind projected objects.

This remains synthetic Android-emulator evidence, not physical S Pen, palm
rejection, hardware latency or owner visual acceptance. The synthetic reference
scene is intentionally not a client property. The current tonal cue is a
deliberately elementary first material layer, not a claim of private-reference
matching, full material language or Northstar renderer parity.

## Next outcome

Review this bounded slice into **feat/project-geometry-seam** without merging it
implicitly. The next bounded visual outcome is stronger water/coping/deck
hierarchy using deterministic, orientation-stable rendering primitives in the
shared canvas/export path. Use approved Northstar reference evidence when it is
available; do not substitute arbitrary texture or claim reference matching from
the synthetic scene. Preserve the now-verified Technical and Graphic baselines,
exact clipping and view-only appearance state while comparing the same
rectilinear, concave and smooth geometry.

After dependable manual authoring and the coherent interaction pass, continue
rectangle/convex-polygon pool generation using the same editable objects, target
water area and explicit outside-coping containment. Do not replace this with a
numeric-form milestone or restart solver/library research.

## Boundaries still in force

No persistent per-edge tangent/radius locks, general linked solver, footprint
generator, shared curved surfaces, attached shelves/steps/spas, alternatives,
full landscape scope, portable backup or Northstar acceptance is delivered.
Physical S Pen/button/hover/palm behavior and full-layout drafting remain
pending. Private references and client geometry remain outside this public
repository. The physical tablet, installed app, signing key and user data were
not changed. No application PR was merged at this checkpoint.
