# Explicit surface materials: first Graphic foundation

Added six optional surface assignments: Water, Paving, Turf, Gravel, Planting bed and Masonry. Select a rectangle, ellipse or closed path, then choose Material from its floating controls. Original style removes the assignment and restores the stored colors, fill and stroke style. Manual Recolor or Watercolor Wash clears the assignment before applying the requested custom styling.

Assignments are optional, versioned JSON fields on each object. Existing drawings retain their original appearance. Assignment does not alter IDs, vertices, dimensions, pressure, original colors or stroke widths. Copy operations retain the assignment; existing object history and sheet persistence include it. Unknown material names/versions fall back to original styling; preservation of unknown future metadata is not implemented.

Canvas, PNG and PDF use the same renderer. Assigned closed regions receive flat restrained fills and material outline colors. Layer/object opacity still applies. This is the first Graphic foundation, not the completed Northstar renderer: physical line-weight hierarchy, mode switching, paving joints, washes, planting assets, shadows and the original full-plan benchmark remain pending. Current object stroke widths are retained, so excessively heavy source strokes remain heavy.

## Verification

- 34 automated checks pass, including four new material checks covering round-trip preservation, legacy/future-field tolerance, repeatable native rendering with contained fill, and eligible surface geometry.
- Installed the development build on Samsung SM-X520; original app remains separate.
- In QA_Pen_Contract, selected the existing rectangle and chose Water from the actual menu. Visually checked flat water fill, clean outline and accessible controls.
- Compared saved objects before/after: only material and materialVersion changed. One Undo restored every object exactly; Redo and force-stop/reopen retained the assignment exactly.
- Local tablet evidence: `../Plan_Trace-Foundation-QA/material-menu.xml`, `material-water.png`, and snapshots `material-before`, `material-water`, `material-undo`, `material-reopen` (outside the repository).
- Shared-renderer code covers exports; a new material-specific tablet PDF/PNG inspection is still pending. Do not interpret the existing export tests as a full visual export certification for this package.

## Next

Create an original editable pool/patio/planting benchmark and compare its canvas, PNG and PDF outputs. Add explicit Technical/Graphic display modes and scale-aware line hierarchy before Northstar textures. Keep 3D paused.
