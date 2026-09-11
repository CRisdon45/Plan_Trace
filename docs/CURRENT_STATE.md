# Current state and next-session handoff

## This branch: isolated freeform Kotlin proof

Updated 2026-09-11 for **research/freeform-tangent-kotlin**, draft **PR #5 into feat/project-geometry-seam**. This is a branch-specific checkpoint, not a replacement for the concurrent application's roadmap. It branched from `80224295df4f972d0c2bd1531bcab8015e552e01`, which already contained whole-straight-side editing. Verify live refs and concurrent changes before integration. Do not reset the application branch to this research baseline.

The inherited application handoff at that base is preserved in Git history: [baseline checkpoint](https://github.com/CRisdon45/Plan_Trace/blob/80224295df4f972d0c2bd1531bcab8015e552e01/docs/CURRENT_STATE.md). That prose predates the whole-side code in the same revision; it is historical context, not a reason to undo the newer source.

## Implemented and verified here

Implementation `a852497b3a2ca7123d77b3903d90601b440e2c2c` adds original Kotlin biarc construction and two bounded tangent-preserving project commands. A local anchor edit changes four edges; a radius edit changes two. They use existing canonical boundaries, retain IDs, reject stale geometry/unsupported branches, respect existing whole-object locks and use the real coping/history/storage path. No UI control exposes these commands yet.

Final test revision **2ee37169fa74f4a8c565e7b16ca1fcbd90f0614b**, [run 34645063378](https://github.com/CRisdon45/Plan_Trace/actions/runs/34645063378), passed **265 tests in 36 suites, zero failures/errors/skips**, including 22 new test methods, plus the separate nine Python runtime-policy tests, runtime dependency/manifest checks and debug assembly. Downloaded XML and ZIP hash were verified. The 5,000 seeded biarcs are internal cases, not 5,000 additional JUnit tests.

Actual production before/after PNGs were opened. The edited/reopened exports are byte-identical, including across both full CI runs. The locally smooth but globally crossing candidate is explicitly rejected by the complete project path without consuming redo. Format 5 and existing mitred JTS coping are unchanged. A one-off source-snapshot workflow was removed after use; cleanup and later documentation do not change the tested implementation.

Read [FREEFORM_TANGENT_PROOF.md](FREEFORM_TANGENT_PROOF.md) for pinned revisions, artifacts, exact domains, derivation, reproduction and limits. The known KSP/AWT background exception appeared in the initial successful build here and remains unresolved. Existing compiler/action deprecations remain.

## Not delivered by this proof

No pen/UI exposure, physical-tablet or emulator interaction, performance/latency measurement, persistent per-edge tangent/radius locks, general linked solver or Kotlin footprint generator. No Cavalier or other new runtime dependency. No Northstar restyle, new project model, storage migration, runtime AI or 3D. No app merge, APK publication, installation, device/data clearing, signing-key change, permission change or private reference publication was performed here.

REF-FF-01 is a secondary private recreation/editing benchmark, not the required pool style or generator template. All committed fixtures are original mathematical outlines. The reference image remains outside this public repository.

## Next outcome and integration rule

Reconcile this draft with the live application branch, preserving its concurrent straight-side/UI work. Establish one complete pen-facing smooth-pool workflow through existing radial selection, meaningful handles, valid live previews with dimensions/coping, cancellation, one-operation Undo and save/reopen. Do not expose a numeric form or every technical biarc junction. Include snapping before the solve, not after it. Fixture injection alone does not prove authoring usability.

After dependable manual authoring/editing, add rectangle/convex-polygon generation using the same editable objects, target water area and explicit outside-coping containment. Use specific failures to justify additional solver/offset components rather than restarting library research. Keep 3D paused and runtime behavior deterministic/local.

Before merging, reconcile this branch-specific checkpoint with the application's then-current CURRENT_STATE rather than overwriting newer application evidence. Maintain a single authoritative project and report exactly which code, UI/device and visual checks ran.
