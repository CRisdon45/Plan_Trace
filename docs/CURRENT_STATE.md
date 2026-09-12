# Current state and next-session handoff

Updated 2026-09-12 after the verified water/coping/deck hierarchy slice. The
current branch is **feat/expert-workspace-ui**; its verified application head is
`59ff0e0`, followed only by this checkpoint documentation. Draft PR #6
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
edge cue; paving and coping use a much quieter field. Northstar line weight also
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

[Android 2D integrity run 34664674103](https://github.com/CRisdon45/Plan_Trace/actions/runs/34664674103)
at `59ff0e0` passed **288 tests with zero failures, errors or skips**, including
the appearance-only projection contract, deterministic clipped sheet-directed
water depth, quieter paving, preserved Technical/Graphic weights and the
Northstar deck/coping/water edge hierarchy. Selector semantics, learned radial
directions, compact workspace behavior, geometry, persistence and export tests
also remain green. The separate nine-test runtime policy audit, resolved-runtime
and manifest checks, and debug assembly passed.
The downloaded report archive digest is
`1105556cf5227f1abdcd9b58cc75bf9eebba6fd1934a8f85bdb51cc6d0241a64`.
The generated connected-coping PNG digest is
`59fdba3fc43a7182d23306800e7542af284b8cc90051d04cde1b37ed2366e0a9`.

[Android workspace emulator run 34664674104](https://github.com/CRisdon45/Plan_Trace/actions/runs/34664674104)
passed **all 19 scenarios**, including hand-placed pool/deck outlines,
held-pointer live distances, exact snapping, rejected/cancelled side edits,
smooth-pool creation and editing, save/restart and reopening, plus Technical,
Graphic and Northstar selection through the real View command. Its artifact
digest is `e802200815a717bd32d43260284f2e73ca60a67ea8f315edc1aba798d21eb626`.
The downloaded archive matched that digest. Visual inspection of the real
workspace, selector and three 1400x1000 vector PNGs confirms that Technical is
linework-only and Graphic remains the prior flat material baseline. Northstar
now gives the water stronger directional depth and inner edge definition while
the exact coping band and paving stay subordinate in both canvas and export.
All three use identical smooth/concave project geometry. Project JSON before and
after the selector scenario was byte-identical with SHA-256
`9c864d95df3e9a224974793c72ef8f35d6fa684f6d9e6241d7b56486b3bebc25`.
Technical and Graphic export digests are unchanged from the prior verified slice;
the Northstar export digest is
`65c7f509db30cedd9b21ab96270dbd72cbde0bfa41409a8b3eeb27d00422c1a4`.
The site raster remains a reference layer behind projected objects.

This remains synthetic Android-emulator evidence, not physical S Pen, palm
rejection, hardware latency or owner visual acceptance. The synthetic reference
scene is intentionally not a client property. These broad deterministic cues
are still an early material layer, not a claim of private-reference matching,
complete paving joints, full material language or Northstar renderer parity.

## Next outcome

Review this bounded slice into **feat/project-geometry-seam** without merging it
implicitly. The next bounded product outcome is rectangle/convex-polygon pool
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
