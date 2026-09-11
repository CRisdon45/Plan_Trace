# Project-owned geometry: first implementation slice

Recorded 2026-09-10. This is a storage-neutral **core API and output integration**, not a newly usable pool-drawing UI or completion of Q1. Work is isolated on `feat/project-geometry-seam`, stacked above the off-tablet export fixes. No existing drawing or Room schema was migrated and no tablet was accessed.

## Implemented

`ProjectDesign` owns typed objects independently of imported PDF pages. Boundaries own finite double-precision metric coordinates, ordered vertices, stable outgoing edge IDs and exact straight or circular-arc segments. The initial arc convention is signed bulge, tan(sweep/4), bounded to a semicircle per edge; larger arcs must be explicitly split. Internal metres/y-up are a reversible technical representation, not a change to the required feet/inches workflow. The output adapter defaults to imperial labels.

Moving a vertex updates both incident edges without flattening the curve or assigning new edge identities. Arc edits preserve endpoints. Translation preserves unrelated objects. Tangency diagnostics report a changed join rather than secretly solving a constraint. Analytic perimeter and algebraic signed area do not come from rendered chords. Reversing a boundary keeps the physical edge identities and reverses signed area.

`DesignSession` applies preview/commit commands through one pure reducer. Preview, no-op and invalid operations do not mutate the document or consume Undo. One command is one Undo; Redo is invalidated only by a new real edit. Locks guard every supported mutation route. Collections are defensively owned. Revision numbers advance when undoing/redoing while the actual geometry is restored.

`DesignJsonCodec` roundtrips the new document, exact curves, identities, units, revision, locks and provenance. Unsupported versions/coordinates and corrupt objects fail the whole decode rather than silently dropping part of a yard. This is a tested serialization API, not yet integration into the application's autosave, backup or Room repository.

`DesignOutput` produces disposable, locked drawing projections for the existing `ExportManager` and native renderer. It is not another renderer and sampled points never replace the authority. Analytic perimeter labels are generated from the current object state; legacy automatic chord-length labels are disabled in the output wrappers. Sampling error and Float-conversion error share an explicit metric error budget. Excessive work, non-finite derived metrics and projections that lose too much precision are rejected rather than silently degraded. A local projection origin supports distant coordinates.

## Real evidence and scope

The fixtures are newly authored synthetic shapes: a rectangle, a two-semicircle circle, and a mixed straight/convex/concave outline with an independent patio. They are small geometry cases, **not** completed ORTHO-01/ORGANIC-01 projects, copies of client drawings or Northstar illustrations.

The existing application source paths are exercised by the PNG test after command -> JSON encoding -> decoding -> projection -> actual export. It checks repeatable PNG bytes, label on/off differences, image size and unchanged authoritative JSON. Different projection scales and sampling tolerances retain the analytic labels. Legacy TraceProject serialization is also checked without promotion or migration.

| Revision | Completed evidence |
| --- | --- |
| `3bf9200eecb5677354d420ee63d194d1cb1ac08c` | [Run 34512053545](https://github.com/CRisdon45/Plan_Trace/actions/runs/34512053545): 76 tests, zero failures/errors/skips; debug assembly succeeded. |
| `0d18c20e6dbef82948d27400dc3803fb3a53b9dd` | [Run 34512772848](https://github.com/CRisdon45/Plan_Trace/actions/runs/34512772848): 78 tests, zero failures/errors/skips; debug assembly succeeded. |

The final result is the existing 49 tests plus 29 new tests: DesignGeometryTest 13, DesignSessionTest 8, and DesignDocumentOutputTest 8. Job logs and the downloaded XML reports were both inspected. The final artifact ZIP (ID `10166611773`) verified against SHA-256 `eda9f065362d1a534296cc43c38e0e1980a549d81845b2b986fad4196ffe7b77`.

The initial run repeated the earlier KSP/AWT background NullPointerException. That exception was not present in the final run's retrieved log, but no cause-specific fix was made and the tooling follow-up remains open. Existing Google-services and deprecation warnings remain. This is not a claim of warning-free builds.

The final artifact contains two authentic 800 x 600 PNGs from the existing native renderer and the canonical JSON. Both PNGs were opened and reviewed: outlines, concave arc, labels and independent patio are visible without clipping. This is only technical evidence, not Northstar acceptance. The small caption size is not a finished presentation style. The existing auto-fit recomputes bounds when labels are omitted, so the two images have different framing despite unchanged design geometry. Fixed output-frame control remains an explicit follow-up; no same-pixel registration claim is made.

PNG SHA-256: measurements `7a8e25331b0168ed9d26f12f4172a019857a448d0d997f8ba49d037f6d4aa625`; outline `2e5ace84bbfdd318f766ef48b8e484b75857439ec54b690512bf6233ef745b7d`. These can be regenerated by the test. The artifact is retained for seven days, not indefinitely.

The local production geometry code also compiled with the available Kotlin compiler. A deterministic exploratory script exercised 1,200 positive/negative random arcs against circle-radius, reversed-interpolation, tangent and chord-error checks. That is supplementary local evidence, not an additional JUnit count or an Android/device result.

## Deliberate limits

This slice does not certify simple/non-self-intersecting topology, holes, surface Booleans or valid coping offsets. The signed-area method is explicitly algebraic, not a takeoff quantity; the output adapter consequently does not render filled surfaces or area totals. It does not maintain pool/coping/shelf attachments, solve tangency automatically, or provide spline editing. Arc/node/object/sampling/document-size budgets are explicit initial limits, not claimed field-tested capacities.

There is no new on-canvas creation/edit UI, source-page registration, integrated autosave/recovery or migration of live user data. The PDF adapter compiles, but this slice does not claim a new-path PDF visual review. The synthetic PNG output can be reviewed for outline integrity, not for Northstar acceptance or tablet usability. Primary radial interaction and 3D remain untouched; 3D is paused.

The next slice must connect this authority to a small real canvas editing and safe persistence flow instead of expanding an isolated geometry laboratory indefinitely. Keep topology/offset validation and edge-attached features tied to that workflow, with straight and concave cases in the same acceptance loop. Do not feed the disposable TraceProject projection back into the canonical document or silently promote legacy drawings.

## Publication and test environment

GitHub hosted CI runs the full application unit suite and debug assembly. It uses an ephemeral debug key and uploads only synthetic test reports/output, not APKs, private references or deployment credentials. The separate one-day source-snapshot artifact contains selected public tracked files only, for a development environment without direct GitHub network access. It includes no Git credentials, local files, SDK cache or runtime/client data.

Evidence artifacts have limited retention. A successful build does not justify replacing the ongoing tablet-test build. Preserve its signing key, installed app and data until an intentional device test. The prior runtime-service dependency cleanup remains separate work.
