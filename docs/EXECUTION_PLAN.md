# Plan Trace execution plan

September 9, 2026. Authorized to proceed with implementation and verification without repeated continuation prompts. This plan preserves unfinished work across sessions; status must reflect evidence, not intention.

## Ordered goals

| Stage | Goal | Exit evidence | Initial status |
|---|---|---|---|
| 0 | Reproducible source/build baseline; preserve existing projects | Local build, commit/build identity, source/APK discrepancy recorded | In progress |
| 1 | Trustworthy 2D core | QA regressions for calibration, locks, layer/history integrity, share and world/output alignment | In progress |
| 2 | Complete document and editing workflow | Per-page state, exact shape editing, notes, overlay input, clear path completion | Planned |
| 3 | Calm tablet UI and excellent S Pen | Responsive docks, persistent settings, fit view, physical barrel/palm tests | Planned |
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
- Final expanded run: **15 tests passed, zero failures** (7 integrity regressions + 8 existing model/context tests), and debug assembly succeeded. Tablet checks confirmed floating actions, whole-drag Undo, layer recovery, locked geometry protection, PNG/PDF sharing and export alignment. After the final save-serialization build was installed, a new line saved and all six QA objects survived force-stop/reopen unchanged.
- Final development APK SHA-256: `EEEA0A600CC941537EC72185AA9CF66CE095D0FFE6D45C6B6AE676A58C1B3593`. Tablet left in Select / S Pen Only mode in Plan Trace Dev. The original installed app was not replaced.

## Next executable package

Finish compatibility and correctness before styling: source polyline completion; durable page-specific object/scale storage; exact resize and note editing; layer/portrait controls; persistent input preferences. Then validate the S Pen modifier contract on hardware. Build the semantic model and original visual benchmark before enabling Graphic/Northstar styles. Use [Build and Verify](BUILD_AND_VERIFY.md) for repeatable checks.
