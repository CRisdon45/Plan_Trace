# Freeform tangent editing: Kotlin integration proof

Checkpoint: 2026-09-11. **The bounded geometry proof now has a verified pen-facing Android workflow; it is not a pool generator or physical-tablet acceptance.**

## Decision and isolation

Continue with Plan Trace's existing canonical `DesignBoundary`, not a replacement geometry model. The biarc research is now implemented in Kotlin and exercised through the actual project command, derived coping, history, serialization, atomic file store and production export renderer.

Work is isolated on `research/freeform-tangent-kotlin`, draft [PR #5](https://github.com/CRisdon45/Plan_Trace/pull/5), branched from `80224295df4f972d0c2bd1531bcab8015e552e01` on `feat/project-geometry-seam`. That source already contained whole-straight-side editing, although its then-current handoff still described the earlier checkpoint. Source and tests, not stale prose, informed this base selection. This work does not overwrite later application/UI work. No application PR was merged.

The command-only implementation is `a852497b3a2ca7123d77b3903d90601b440e2c2c`. The two additional adversarial/quantity tests are at `2ee37169fa74f4a8c565e7b16ca1fcbd90f0614b`. Pen-facing authoring/editing is `9eb25e57590758c9394ca6d4a8f49c20c297239b`; the tested correction is `2f28ac9d2d6ff873b79450fac95dbf5d950fb7d5`. Later documentation-only commits do not imply another application validation result.

## Pen-facing integration

`SmoothPoolDraft` turns three to thirty-two deliberate guide points into the existing canonical line/arc boundary without storing guide-only state. The real canvas and radial commands now support creating and closing that pool, following coping, selecting one meaningful smooth anchor or radius scope, previewing a valid tangent-preserving edit, cancellation, one-operation commit, Undo/Redo and save/reopen. Shape anchors can use the existing exact object snap before the solve. Radius and shape picking use the existing exact finite-arc projection.

The interaction keeps the existing project command, renderer and format-5 store as authority. It does not expose every internal biarc junction, add a numeric form, persist a second constraint graph or infer hidden points. System-injected stylus tests cover the Android routing path; physical S Pen feel remains pending.

## What changed

`TangentSpanEditing.kt` contains a small independently written Kotlin construction helper using existing exact circular arcs, straight segments and bulges. Endpoint/tangent biarc mathematics was adapted from [Ryan Juckett's Biarc Interpolation](https://www.ryanjuckett.com/biarc-interpolation/). No upstream implementation source was copied. No new numerical, native or runtime dependency was introduced.

`MoveTangentAnchor` keeps the two outer connection positions and tangent directions fixed and preserves the tangent at the selected anchor. It changes at most four neighboring edges without changing edge/vertex identities or edge count. The starting biarc balance is inferred from the existing arcs, avoiding a jump to equal-balance geometry at the beginning of a small edit.

`SetTangentRadius` fixes the outer positions and tangents of two adjacent arcs while solving one requested radius and the neighboring radius/junction. It preserves directed curvature branches and explicitly rejects singular, reversed, collapsed or unsupported results. It is not a general simultaneous multi-radius solver.

Both commands use the real `DesignCommands` and `DesignSession` path. They reject stale expected boundaries, respect existing whole-object locks and require a pool/spa with following coping. Complete next-state validation happens before history changes. No-op edits preserve redo. The helper itself is deliberately not a whole-boundary topology validator.

The existing version-5 serializer and atomic workspace store remain unchanged. Pairing and the biarc balance are transient construction choices, not a second saved pool model. The current JTS-based, mitred version-1 coping generator is unchanged.

## Mathematical implementation notes

A unit-chord, translated coordinate frame is used to reduce numerical scale sensitivity. Biarc balance is restricted to `[1e-4, 1e4]`; the selected endpoint chord is restricted to `[1e-5, 1000]` metres. These are implementation support limits, not recommended pool dimensions or building-code rules.

The helper returns two minor arcs (or supported straight segments). It does not silently insert nodes to split major arcs. Near-semicircular balance recovery, backward branches, unsupported shallow bulges and degenerate inputs fail explicitly. The canonical model's existing bulge and edge-size restrictions still apply.

For the radius operation, let `t0,t1` be directed unit endpoint tangents, `n0,n1` their left normals, `s0` the requested signed first radius, and `C0 = P + s0*n0`. With `v = Q - C0`, the second signed radius follows from directed circle tangency:

```
s1 = (s0*s0 - dot(v,v)) / (2*(dot(v,n1) + s0))
C1 = Q + s1*n1
J  = (s0*C1 - s1*C0) / (s0-s1)
```

The implementation applies these equations in the normalized frame, guards singular denominators, reconstructs the actual arcs, and independently checks directed tangents and the requested radius. This is an implementation of ordinary circle geometry, not a claim of a new mathematical discovery. Numeric residuals are not survey/construction accuracy.

## Executed evidence

### Local actual Kotlin geometry

The actual repository `DesignGeometry.kt`, the new helper and shared proof cases were compiled with Kotlin/JVM 1.9.0 and executed on OpenJDK 21.0.11. All eleven pure proof groups passed. This did not compile or execute Android UI, JTS, persistence or the full application locally.

The 5,000 seeded biarcs use seed `20260911`, chord lengths 0.1–20 metres, endpoints within the declared generated region, endpoint tangent deviations within +/-1 radian of the chord and balances 0.5–2. No failed cases were discarded. Maximum observed directed tangent defect was `1.5853984791647235e-13` radians. Circle samples were checked against the actual canonical circle calculation.

Other pure cases include three explicit accepted degeneracies and invalid-input rejections, sixteen closed families with 4/6/8/10 guide points, a wraparound edit, non-unit initial balance, no-op/tiny-move behavior, reversed winding, a one-million-metre translation check and an independent sampled area comparison. The randomized scenarios are internal cases of one test, not 5,000 additional JUnit tests.

### Full application CI and reports

Initial run [34644488351](https://github.com/CRisdon45/Plan_Trace/actions/runs/34644488351) at `a852497` passed 263 tests from 35 XML suites, zero failures/errors/skips, the separate nine runtime-policy Python tests, runtime dependency/manifest checks and debug assembly. Its downloaded report ZIP was verified against SHA-256 `f23597f841c51be762b37e62d75422504e096e597fdb4f2c396e08e5acc04519` (artifact `10281222119`).

Final run [34645063378](https://github.com/CRisdon45/Plan_Trace/actions/runs/34645063378) at `2ee37169fa74f4a8c565e7b16ca1fcbd90f0614b` passed **265 tests from 36 XML suites, zero failures/errors/skips**, plus the separate nine runtime-policy tests and debug assembly. This includes 22 new test methods: eleven geometry, nine document/Robolectric and two global-safety/quantity tests. The actual downloaded XML was counted; seeded internal cases were not added to the JUnit total. Resolved runtime checks passed with 104 debug and 97 release modules and no network permission in the checked merged manifests.

Final report artifact `10282127123` was downloaded and its ZIP SHA-256 verified as `45149a0658a3a948a85e7607d3b190b902c1d7975666dad14309f7d96d8d95c9`. The report files include `reports/design-geometry/freeform/{before,after,reopened}.png` and the synthetic saved documents. All three corresponding PNGs are also byte-identical across the two full CI runs. GitHub's seven-day artifact retention is not permanent availability. A conversation evidence bundle retains the reviewed report files and source.

The one-off source snapshot workflow was removed afterward in `2f480a0cf7befbf87f09ce96840b29ce6e2f869b`. It is not part of the final application diff. This removal and later documentation do not change the tested source, tests or integrity workflow.

The added document tests exercise previews without committing or saving, a single Undo for the accepted change, redo, copied and reopened version-5 JSON, the actual atomic file store, stale-boundary rejection, whole-object locks, non-pool rejection, unsupported radius requests, and production PNG export. These are unit/Robolectric tests, not a physical S Pen or Android emulator session.

The final adversarial case deliberately permits the **local** biarc solve, checks that all directed joins remain smooth, independently confirms the sampled whole polygon is invalid using JTS, and requires the real project command to reject it without losing the accepted state or redo. This tests why tangency alone is insufficient. Sampled topology validation is not an exact-predicate geometric proof.

### Original synthetic pool results

| State | Analytic signed area (m²) | Analytic perimeter (m) |
| --- | ---: | ---: |
| Before | 31.890534577236146 | 22.03656667394719 |
| After moving the anchor | 33.20697425406142 | 22.327396008914054 |
| After the subsequent radius edit | 33.14311301012192 | 22.301856871278513 |

The anchor displacement is `(0.35, 0.40)` metres. Four edges change; the remaining **12 of 16** retain their exact endpoints/bulges/IDs. The radius edit changes two edges; the other **14 of 16** remain unchanged. It requests an 8% radius reduction to `1.1668057045476037` m and obtains `1.1668057045476043` m. The synthetic pool's maximum observed join defect is below `2e-15` radians. These values describe the arithmetic fixture, not a proposed client design or construction tolerance.

The before, after and reopened PNGs were produced by the existing `DesignOutput.png` renderer. The before/after images were opened, and reopened images were checked byte-for-byte. The first two differ, and the edited/reopened PNG bytes match exactly. Views automatically frame their content, so the before/after images alone are not evidence of fixed screen positions for distant vertices. The geometry comparisons establish unchanged remote geometry. This is neither a new Northstar style nor visual acceptance of the current rendering.

## Final integrated evidence, limits and retained issues

At tested source `2f28ac9`, [integrity run 34650212981](https://github.com/CRisdon45/Plan_Trace/actions/runs/34650212981) passed 275 tests in 36 suites with zero failures/errors/skips, the separate nine runtime-policy tests, runtime graph/manifest checks and debug assembly. Its downloaded artifact SHA-256 is `93e4f7ac18d0248f17ec9a1dcb4aac81f7b36586cdca039ef4d5a8906669f478`.

[Workspace emulator run 34650212974](https://github.com/CRisdon45/Plan_Trace/actions/runs/34650212974) passed all 18 scenarios, including smooth authoring, local anchor/radius edits, invalid radius rejection, cancellation/flagged pen-up/second-contact safety, Undo/Redo and save/reopen. All 41 active-window records identify the app, the new frames were opened and unobstructed, all seven saved restart pairs match, and no ANR or crash was recorded. The artifact SHA-256 is `c8286d8274bccb017ccd6f0cffeba9cf4d47360f647a0477d8343eadbd05d4f4`. The final format-5 document is revision 63, eight objects and 10,825 bytes with SHA-256 `6311f9c7efb7205dda95a0c92282aee19155c5ae602fd96be2d1791fca1f7a7f`.

This is disposable API-35 emulator evidence using system-injected stylus contacts, not physical-tablet interaction, stylus timing, palm rejection, button/hover feel, hand occlusion, hardware performance or owner visual acceptance. No APK was published or installed on the physical tablet; no original app, signing key or device data was changed. Release dependencies and manifests were resolved/processed, but a release build was not assembled.

The existing successful-build KSP/AWT background `NullPointerException` appeared again in the current full run. It was not fixed or established harmless. Existing compiler/action deprecations remain. Passing tests are not a claim of warning-free builds or a complete security audit.

There are no persistent per-edge radius/tangent locks or general linked/whole-shape solve. Unrelated geometry is held by this selected edit scope, not by a newly implemented complete constraint system. The smoothness helper is bounded; it does not handle all line/arc transitions, arbitrary semicircles, major arcs or general infeasible constraint resolution. Coping/validity retain their previously declared sampled tolerances and limits.

Cavalier Contours was not integrated or executed. Do not install multiple candidate solvers or change the existing coping semantics on the basis of this proof. Footprint generation is not implemented in Kotlin; the earlier Python results remain separate research evidence.

## Next outcome

Reconcile and integrate this verified draft with `feat/project-geometry-seam`. Do not reset that branch or discard its straight-side history. The next separate slice is the coherent expert workspace UI/visual hierarchy pass across straight and smooth authoring/editing. The current screenshots prove interaction, not an accepted shell or Northstar appearance. Keep physical-tablet testing explicit.

Once manual authoring/editing is dependable, proceed to rectangle/convex-polygon generation with target water area and outside-coping containment. Use these same editable objects. Broader linked locks, concave envelopes and specialist arc operations each need their own concrete acceptance cases; avoid another open-ended library survey.

REF-FF-01 remains one secondary recreation/editing benchmark, not an imposed pool style, arc count, generator template or design target. Its image and identifying derivatives are not included in this public branch. All current fixtures are original mathematical geometry.
