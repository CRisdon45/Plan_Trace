# Plan Trace execution plan

September 9, 2026. Authorized to proceed with implementation and verification without repeated continuation prompts. This plan preserves unfinished work across sessions; status must reflect evidence, not intention.

## Ordered goals

| Stage | Goal | Exit evidence | Initial status |
|---|---|---|---|
| 0 | Reproducible source/build baseline; preserve existing projects | Local build, commit/build identity, source/APK discrepancy recorded | In progress |
| 1 | Trustworthy 2D core | QA regressions for calibration, locks, layer/history integrity, share and world/output alignment | In progress |
| 2 | Complete document and editing workflow | Per-page state, exact shape editing, notes, overlay input, clear path completion | In progress |
| 3 | Calm tablet UI and excellent S Pen | Responsive docks, persistent settings, fit view, physical barrel/palm tests | In progress |
| 4 | Deterministic semantic Graphic renderer | Versioned roles/styles, token-driven linework/fills/water, original benchmark | Planned |
| 5 | Northstar presentation renderer | Designed plants, consistent shadows, stable wash, faithful sheet exports | Planned |
| 6 | Premium job completion | Options/versions, symbols, linked quantities, presentation/templates, portable backup | Planned |

## Active work package

Start with export alignment and scale math, truthful share delivery, full editing-state history and locked-layer mutation guards. These are concrete defects in both the QA evidence and current source. Add focused regression tests before claiming them fixed. Restore build access before broad UI or model migrations. Retain the supplied APK and existing tablet app until a replacement is built, verified and backed up; do not repeat the earlier data-clearing uninstall.

The current source is `ed8acb5` at start, and is not proven to match the tested APK. Fixes here apply to this checkout. Avoid removing known-working APK features silently; record differences and verify a source-built app separately before replacing the user's working build.

## Engineering sequence

1. Read project/build setup and reproduce the baseline build failure. Restore a reproducible launcher and only adjust dependencies with evidence.
2. Introduce shared export bounds/fit and scale-bar calculations. Draw the underlay at native world coordinates and transform it together with geometry. Verify a nonstandard image, negative/outside geometry and calibrated spans.
3. Make share launch context-correct and expose failure honestly.
4. Replace element-only undo with coherent document-edit snapshots, gate locked mutations and restore layer ownership on undo. Add gesture transaction boundaries to both relevant input paths.
5. Resolve calibration input exclusivity, toolbar interception and import scale validity; verify on a built app.
6. Continue into per-page storage, exact editing and pen interaction as separate reviewable packages.
7. Add semantic roles with migration-safe defaults before Northstar rendering. Implement each visual stage against the original benchmark in the visual spec.

## Definition of done for each package

Code compiles; relevant tests pass; runtime/visual checks are performed where needed; existing project compatibility is preserved; limitations are explicit; this log records files, evidence and remaining work. A compiler/environment blocker is not a passing test. A design document is not a shipped feature. No 3D development is included.

## Governing references

- [Product vision and S Pen interaction contract](PRODUCT_VISION.md)
- [Northstar visual spec, tokens, compositing and acceptance](NORTHSTAR_VISUAL_SPEC.md)
- [Tablet reports: 13 findings and premium gaps](https://github.com/CRisdon45/Plan_Trace/tree/qa/tablet-premium-audit-2026-09-09/docs/qa/2026-09-09)

## Work log

- Defined the Northstar visual contract from the five supplied images and attached prose. Adapted pass ordering for correct compositing. Recorded that semantic pool/material roles must be added rather than assumed present.
- Established a compiling source baseline, restored Gradle 9.4.1 launchers and installed an isolated Plan Trace Dev build. The historical APK/source parity question remains open.
- Implemented the first core package: shared export coordinates, scale-bar math, chooser delivery, full layer/element history, gesture transactions, calibration exclusivity, locked-mutation guards and canvas-overlay input separation. See [implementation and runtime evidence](FOUNDATION_IMPLEMENTATION.md).
- First package expanded run: **15 tests passed, zero failures** (7 integrity regressions + 8 existing model/context tests), and debug assembly succeeded. Tablet checks confirmed floating actions, whole-drag Undo, layer recovery, locked geometry protection, PNG/PDF sharing and export alignment. After the final save-serialization build was installed, a new line saved and all six QA objects survived force-stop/reopen unchanged.
- First package development APK SHA-256: `EEEA0A600CC941537EC72185AA9CF66CE095D0FFE6D45C6B6AE676A58C1B3593`. Tablet left in Select / S Pen Only mode in Plan Trace Dev. The original installed app was not replaced.

## Next executable package

Finish compatibility and correctness before styling: durable page-specific object/scale storage; exact editing beyond rectangles; layer/portrait controls; persistent input preferences. Then validate the S Pen modifier contract on hardware. Build the semantic model and original visual benchmark before enabling Graphic/Northstar styles. Use [Build and Verify](BUILD_AND_VERIFY.md) for repeatable checks.


## Second package completed

Implemented and tablet-checked rectangle corner resizing and decimal exact-size entry, existing-note revision, open/closed polyline completion and draft cancellation. All edits tested with exact Undo comparisons. Added multiline note bounds/rendering regression coverage. A visual reopen check found missing persistent source-file access; repaired document permission retention and MIME-based PDF identification, then confirmed image and PDF underlays survive force-stop/reopen. Final build: 18 passing tests, zero failures. Latest APK SHA-256: `4200195D1BF007F165EF83AF16123E84F9C0DE123C573E223EDB8D53B7A32A4B`. See the implementation report for evidence and remaining limitations.

Next priority is migration-safe page-specific document state, followed by responsive controls, persistent input preferences and the S Pen modifier contract. The broad premium roadmap remains unfinished; this milestone establishes the first reliable editing foundations.

## Third package completed: document pages

Added migration-safe source/sheet ownership of drawings, layers, active layer, scale and underlay settings; separate per-sheet session Undo/Redo; stale-background protection and off-main-thread decoding. Existing records survived the tablet database upgrade field-for-field. Two distinct PDF sheets retained their own objects, units and active layers through navigation, Undo/Redo and restart. Returning to the prior image restored its original eight objects. Current-sheet PDF output was inspected. **23 tests passed.** See [page document implementation and evidence](PAGE_DOCUMENTS_IMPLEMENTATION.md).

Next: fix clipped layer actions and narrow-screen controls, retain input preferences, then implement and physically validate the pen modifier contract. Source relinking/copy-between-sheets and multi-sheet export remain document-workflow follow-ups.

## Fourth package completed: tablet controls

Reflowed the top bar for portrait/narrow widths, repaired layer-name/action clipping with accessible controls and a layer menu, retained drawing preferences across restart, and added Fit drawing. Actual tablet checks covered lock, duplicate/Undo, a long layer name, saved Select/S Pen/colour/width/style/dimension settings, portrait layout and geometry-preserving Fit in both orientations. **23 tests passed.** See [tablet controls implementation and evidence](TABLET_CONTROLS_IMPLEMENTATION.md).

Next executable work is the S Pen modifier state machine: hold to temporarily select, restore the drawing tool after pen-up, respect locks and prevent accidental marks on button transitions. Physical device-button verification and palm behavior must be distinguished from synthetic event tests. Semantic visual rendering follows this interaction foundation.

## Fifth package completed: pen contact integrity

Implemented a latched temporary pen-button mode (Select default; optional object erase), preserved the prior tool across button release, respected calibration and locks, separated pen pointers from additional fingers, and cancelled rejected/interrupted contacts. A real synthetic rejection failure exposed loss of FLAG_CANCELED before the canvas callback; normalization at Activity dispatch fixed the repeated case. **30 tests passed.** Real-app synthetic tests confirmed whole-drag Undo, normal-tool restoration, no conversion mid-stroke, cancellation, mixed pen/finger coordinates, locked protection, object erase/Undo, calibration priority and Home interruption recovery. See [pen interaction evidence and physical-test limits](PEN_INTERACTION_IMPLEMENTATION.md).

Next: semantic material roles and a deterministic Graphic benchmark, then Northstar presentation passes. Full lasso/multi-selection, segment erasing, physical S Pen feel and advanced dock/inspector work remain tracked; do not confuse synthetic event success with hardware certification.

## Sixth package: explicit surface foundation

Added optional versioned materials to closed objects, a selection material picker, reversible original/custom styling and shared flat Graphic rendering. Tablet assignment, exact Undo and restart persistence passed; 34 automated checks pass. See [scope, evidence and remaining visual work](SURFACE_MATERIAL_IMPLEMENTATION.md). This does not complete stage 4: original benchmark, display modes and line hierarchy remain next.

## Paused checkpoint — 2026-09-10

User requested stopping implementation and uploading current work. Preserved an editable courtyard example, plain paper background, constrained selection controls and PNG measurement-option plumbing. Fresh run: 35 tests passed and debug assembly succeeded. Tablet review of this latest checkpoint and the PNG measurement control remain unfinished. See [exact resume point](CHECKPOINT_2026-09-10.md).
