# Current state and next-session handoff

Updated 2026-09-12 after the verified bounded-watercolor slice. The current
branch is **feat/expert-workspace-ui**; its verified application head is
`65425ec`, followed by evidence-retention commit `ea1cf66` and this checkpoint
documentation. Draft PR #6
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
path. Water now uses a calm sheet-directed depth field and a restrained inner
edge cue; paving and coping use a much quieter field. Northstar water also has
a bounded deterministic pigment field: paper variation, wetness, mobile pigment
and deposited pigment evolve for a fixed 32 settling steps on a grid capped at
128 cells per side. Results are seeded from stable object identity, geometry and
color, cached within 12 MiB, and clipped again by the exact vector path. Moving
an unchanged object therefore moves its existing wash instead of repainting it.
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

[Android 2D integrity run 34671107391](https://github.com/CRisdon45/Plan_Trace/actions/runs/34671107391)
at `ea1cf66` verified application revision `65425ec` and passed **290 tests with
zero failures, errors or skips**. The two added regressions exercise seeded
repeatability, per-object variation, settling/deposition, approximate pigment
mass conservation, varied alpha and strict mask containment. Existing geometry,
appearance, hierarchy, persistence and export tests remain green. The separate
nine-test runtime policy audit, resolved-runtime and manifest checks, and debug
assembly also passed. Artifact `10291145153` was downloaded and its ZIP SHA-256
is `67a5ef145f32586884893d960434d47c611f84b89cae71ffea78c39bdcc6068f`.
Its retained 1000x700 Graphic and Northstar comparison PNGs have SHA-256 values
`845e0d4882b7f6acd1c5b7dc7bbf310932edcb1a3fc45b709eeceaad6f8ad061`
and `5f05ff7fd6e4db9134e84c4f38614bbbb8ea8c3bb9f9de702d497693dba8c0b9`.

[Android workspace emulator run 34670500283](https://github.com/CRisdon45/Plan_Trace/actions/runs/34670500283)
at `65425ec` passed **all 19 scenarios**, including the full existing authoring,
editing, snapping, rejection, Undo/Redo, save/restart and export suite plus
Technical, Graphic and Northstar selection through the real View command. The
downloaded artifact ZIP SHA-256 is
`cd87af9750e3541dbdd39072ea85435b09af10cae64fd72a515c2ca2820f8b4e`.
Project JSON before and after appearance switching was byte-identical at
`e37ff3f81267e3e576baac95e73ee81c65fe3088e18423ada14df7dcc267d27c`.
Technical, Graphic and Northstar export SHA-256 values are respectively
`415a57b76cc2eb87a70a49f05141654e227cb7309e35346fe13121bc7da63f9c`,
`2ecd6727d388c5115d0f5ace9ad0c183655809249f5cf268895fa2599cab097d`
and `d15f923fc6d35e8c25fcba5ec1cba803d7a9ba6e165fdbdb09bb12c7cfdb41f4`.
Visual inspection of the real workspace, full-layout exports and focused render
pair confirms a restrained irregular water wash, exact containment, stable
coping geometry and subordinate paving. The runtime crash buffer was empty and
Android reported no ANR since boot.

This remains synthetic Android-emulator evidence, not physical S Pen, palm
rejection, hardware latency or owner visual acceptance. The synthetic reference
scene is intentionally not a client property. These broad deterministic cues
and the bounded CPU pigment model are still an early material layer, not a claim
of private-reference matching, physical watercolor, complete paving joints,
full material language or Northstar renderer parity. PDF continues through the
same deterministic renderer; a simpler non-artistic PDF fallback remains an
acceptable future option if target-device export cost proves excessive.

## Next outcome

Review this bounded slice into **feat/project-geometry-seam** without merging it
implicitly. First review the wash and its interaction cost on the target latest-
Android tablet; do not infer stylus or frame-time acceptance from the emulator.
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
