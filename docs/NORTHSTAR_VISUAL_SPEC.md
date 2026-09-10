# Northstar 2D visual specification

September 9, 2026. Proposed implementation contract, supplementing [Product Vision](PRODUCT_VISION.md) and [Execution Plan](EXECUTION_PLAN.md).

## Goal

Build a deterministic 2D presentation renderer that turns editable semantic plan objects into a restrained architectural illustration: legible line hierarchy, tonal materials, designed vegetation, consistent shadows and balanced sheets. The same geometry must produce a clean technical plan, a styled graphic plan and a Northstar presentation plan. Style never changes measured boundaries or saved geometry. 3D remains paused.

## Reference interpretation

The five supplied images show one coherent landscape-plan family: a full sheet and detail views. They are visual references, not five independent benchmarks. Gallery buttons, device status bars and other screenshot overlays are not part of the desired app UI or exported artwork.

- `1000008490(2).png`: overall sheet composition, white space, hierarchy, planting distribution and the circular water feature.
- `1000008531(5).png`: disciplined canopy silhouettes, negative space, turf and water detail.
- `1000008532(5).png`: paving rhythm, built edges, plant layering and shadow grounding.
- `1000008533(5).png`: light turf fields, narrow hardscape and restrained detail.
- `1000008530(4).png`: low-frequency wash variation, irregular pigment edges and retained paper highlights.

Originals remain in the supplied Downloads paths; do not bundle full screenshots as production textures or pretend the references are generated app output. The attached prose is planning input. Its earlier pause on code changes is superseded by the user's current instruction to execute. Its proposed pass order is adapted below so fills cannot obscure structural lines and shadows appear beneath their owning objects.

## Three render modes, one document

1. **Technical:** clean paper, strong geometry, controlled black/gray line weights, minimal fills, explicit dimensions.
2. **Graphic:** semantic line hierarchy, restrained flat material tones, clear water and simple designed plant symbols.
3. **Northstar:** the same plan enriched with bounded wash variation, richer plant detail, subtle texture and depth. Never a whole-image generative transformation.

Changing mode must preserve topology, scale, annotation associations, selection, history and export alignment. Generic legacy strokes keep their existing style until explicitly assigned a semantic role. Do not infer object meaning from color or a layer name.

## Starting style tokens

Line widths below are physical presentation weights in millimeters at the selected sheet scale. Screen preview uses the same sheet transform. Technical editing can show a legibility overlay without altering exported widths.

| Token | Color | Width / treatment |
|---|---|---|
| boundaryPrimary | `#343832` | 0.50 mm, restrained extent |
| boundarySecondary | `#64675E` | 0.18 mm |
| wallEdge | `#30362F` | 0.45 mm outer, 0.18 mm internal |
| poolEdge | `#294743` | 0.40 mm |
| copingEdge | `#55594D` | 0.25 mm |
| deckEdge | `#686354` | 0.25 mm |
| waterFeatureEdge | `#345953` | 0.30 mm |
| plantingBedEdge | `#656949` | 0.18 mm |
| materialJoint | `#88816C` | 0.10 mm, subdued |
| dimensionLine / labelLeader | `#545D59` | 0.13 mm |
| paper | `#FBFAF5` | nearly white; texture optional |
| waterLight / waterDeep | `#D6E9DE` / `#87B5AA` | broad, subtle variation |
| deckTravertine | `#E9E0CD` | light warm stone |
| turfPrimary | `#DDE8A6` | open light field; retained highlights |
| gravelPrimary / bedSoil | `#D6D1BF` / `#D6C2A3` | restrained granular / earth fill |
| masonryLight / masonryDark | `#DED5C5` / `#B1A58F` | surface / raised structure |
| treeCanopyLight / treeCanopyDark | `#BED0A8` / `#3D6347` | silhouette and selective interior depth |
| shrubMass / accentPlant | `#658342` / `#9CA744` | limited palette with optional small flower accents |
| shadowLight / shadowMedium / contactShadow | `#34413A` | 8% / 14% / 22% maximum starting opacity |

These are initial art-direction values, to be tuned against an original benchmark scene at sheet size and detail zoom. They are not measured color samples from the images.

## Rendering and compositing contract

Use document coordinates for geometry, background registration, material boundaries and texture anchoring. A shared world-to-sheet transform drives PNG, PDF and preview. No independent stretching of the background. Bounds include visible artwork and annotation extents, with deliberate margins.

Order: paper → registered underlay → ground material fills → ground material details/water → structural outlines → raised-object shadow and object groups, back to front → vegetation shadow and canopy groups → labels/dimensions → small intentional accent details. Foreground groups may occlude background groups by explicit depth; transparent fills must not obscure final boundary ink. Selection guides and editing handles belong to an interaction overlay and never export.

Each render item carries a stable ID, geometry, semantic role, material/style reference and ordering information. Existing source primitives are a starting point, not proof that this semantic model already exists. Add optional versioned semantic metadata with backward-compatible decoding and user-visible assignment before relying on it.

All variation derives from stable object ID + style version + explicit seed. Never seed from frame time, zoom, render order or export resolution. Patterns are anchored in document coordinates. Reopening, panning and exporting cannot rearrange leaves or wash patches. Cache by geometry/style revision; invalidate explicitly.

## Material and symbol rules

- **Water:** clipped to a valid pool region, pale blue-green with a small number of broad depth bands. Steps and benches remain readable. No moving caustics or photoreal noise in the initial renderer.
- **Turf:** light tonal field, 2–4 broad variation regions and ample paper highlights. No random high-frequency speckles or obvious repeated tile borders.
- **Hardscape:** accurate boundaries and scaled joint spacing. Pattern density reduces at sheet zoom without changing construction geometry. Keep joints subordinate to outer edges.
- **Vegetation:** six initial categories: canopy tree, palm, shrub mass, accent succulent, flowering accent, groundcover. Define distinct silhouettes with bounded lobes/fronds and intentional negative space. Variation is repeatable and modest; no scribbled branch spaghetti or translucent noise blobs.
- **Shadows:** one sheet light direction; initial offset toward southeast, based on explicit height or symbol category. Clip to appropriate surfaces where defined. Contact shadows are small; overlapping shadows have a capped effective density. Flat grass does not cast a tree-like shadow.
- **Line warmth:** optional subtle style-only edge variation must never make measured boundaries ambiguous. Technical mode remains crisp. Use stable caps/joins and avoid repeated overshoot decorations everywhere.

## Sheet discipline

Preserve calm margins, a bounded title area, clear project/revision identity and a correct graphic scale. Labels use a clean sans-serif, typically 2.5–3.5 mm printed height; dimensions have their own subdued hierarchy and background clearance. Leaders avoid object interiors where possible. Collision warnings and manual placement precede automatic layout promises.

The primary design feature receives visual emphasis; for a pool project this is usually the pool. The supplied garden reference uses a smaller circular water feature, so do not force every project into a giant-pool composition. Planting frames the design and cannot bury the geometry or labels.

## Acceptance

An original editable benchmark scene must contain a pool with steps/coping, adjoining patio, turf, wall, gravel, planting and labeled measurements. Review Technical, Graphic and Northstar at full sheet and 200% detail, in PNG and PDF.

- Same object boundaries, scale and relative alignment in every output mode.
- Stable output across identical renders, after reopen and across zoom changes (allow platform anti-aliasing tolerance, no layout drift).
- At thumbnail size, primary circulation, water and built edges are distinguishable.
- Fills remain light enough for linework; plant silhouettes remain readable individually and in groups.
- Shadows agree in direction; detail density never becomes a noisy texture blanket.
- Dimensions and labels remain legible at actual print size and do not collide with the title block.
- Reviewer compares authentic renderer output to the reference qualities; no reference image is substituted as a completed render.

## Order

Correct geometry/export → semantic model and line hierarchy → flat regions → water → plant symbols → depth/shadows → nuanced wash and sheet polish. The first milestone is trustworthy technical output. Richer visual modes are gated on that foundation, not used to hide its defects.
