# Current state and next-session handoff

Updated 2026-09-12 after the verified first object-driven Northstar rendering
slice. The current branch is **feat/expert-workspace-ui**; its verified code
head is `b1cc519`, followed only by this checkpoint documentation. Draft PR #6
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

## Verified evidence

[Android 2D integrity run 34660738450](https://github.com/CRisdon45/Plan_Trace/actions/runs/34660738450)
at `b1cc519` passed **283 tests with zero failures, errors or skips**, including
the appearance-only projection contract, deterministic clipped tonal rendering,
the compact drawing-strip behavior/accessibility contract, live drawing and
object navigation, the separate nine-test runtime policy audit, resolved-runtime
and manifest checks, and debug assembly.
The downloaded report archive digest is
`b7a2602ceaf132d5cfbafbdce7404565532ad2382396f57a3fdbfa585b662ccd`.

[Android workspace emulator run 34660738449](https://github.com/CRisdon45/Plan_Trace/actions/runs/34660738449)
passed **all 18 scenarios**, including hand-placed pool/deck outlines,
held-pointer live distances, exact snapping, rejected/cancelled side edits,
smooth-pool creation and editing, save/restart and reopening. Its artifact
digest is
`1e28b4d7a66e1e62da6f1398204ce9009d1f7c660f48c6f2ad5be2e9c6aef029`.
The downloaded archive matched that digest. Visual inspection of the real
workspace and vector PNG evidence confirms that paving/ground is behind water
and coping, the restrained cue remains inside exact rectilinear, concave and
smooth boundaries, overlapping objects remain legible, and reopening/exporting
does not change the appearance or saved design. The site raster remains a
reference layer behind the projected objects.

This remains synthetic Android-emulator evidence, not physical S Pen, palm
rejection, hardware latency or owner visual acceptance. The synthetic reference
scene is intentionally not a client property. The current tonal cue is a
deliberately elementary first material layer, not a claim of private-reference
matching, full material language or Northstar renderer parity. Technical and
Graphic are tested projection modes but do not yet have an in-workspace
selector.

## Next outcome

Review this bounded slice into **feat/project-geometry-seam** without merging it
implicitly. The next outcome is a compact, pen-accessible presentation-mode
selector that exposes Technical, Graphic and Northstar without rearranging the
learned radial directions. Store it as workspace view preference rather than
project data, carry the selected appearance through the live canvas and export
path, and capture all three outputs from the same authoritative geometry. Then
advance the restrained water/coping/deck material language against approved
visual references rather than adding arbitrary texture.

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
