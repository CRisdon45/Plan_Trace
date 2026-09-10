# Editable workspace and virtual Android verification

Checkpoint: 2026-09-10. The project-owned geometry now has a reachable editing screen and a saved local draft inside the real application. This is an opt-in outline workspace, not a finished client-design release. Work remains on `feat/project-geometry-seam`, PR #4 stacked above PR #3. The foundation/tablet application and legacy project database are unchanged by this branch until deliberately integrated.

## What is reachable

Open **Projects -> Design workspace · preview**. Add a straight pool outline, a concave/convex pool outline, or a paving outline. Select an object through its chip or boundary. Drag a round vertex handle, an amber edge-midpoint curve handle, or the boundary to move the object. Preview is transient; the completed gesture is one command and one Undo. Undo, Redo, Delete/Undo, Fit, perimeter feedback in feet, and explicit Touch edit are available.

The screen uses the existing WatercolorRenderer through a disposable projection of ProjectDesign. It does not maintain a second editable set of legacy polylines or use a demonstration-only renderer. The stored authority retains exact lines/arcs and stable IDs. PNG/PDF actions call the existing export dialog, DesignOutput and share helper, although this checkpoint's emulator scenarios do not exercise that new screen's export/share action.

Default input intends pen editing and finger navigation. Touch edit explicitly enables touch authoring for testing and appropriate use. Gesture handling consumes the final up position, cancels previews on interruption, avoids committing a two-pointer interruption, and rejects stale-revision commands. Physical stylus, hover, pressure, barrel-button and palm behavior remain unverified. The primary radial interaction is still a requirement, not something this toolbar implements.

## Persistence and its boundaries

The workspace owns one opt-in draft in app-private `project-design/workspace.json`, separate from the legacy Room database. No existing plan is promoted, migrated or overwritten. Writes are serialized off the UI thread, use AndroidX AtomicFile replacement and are read back for equality. The Saved indicator refers to the current verified disk revision, not merely a queued write.

The store rejects stale revisions, conflicting content at the same revision, and replacement by another document identity. Corrupt files do not become a silently reset empty design. Interrupted replacement can recover the committed base; an incomplete first save is reported instead of being mistaken for no draft. Errors remain visible with retry/unsaved-exit handling.

This is not portable backup, project-version history, a multi-document workspace, multi-process synchronization or a guarantee for a process killed before saving finishes. Undo history is session-local and is not persisted across process death. The restart evidence below deliberately waits for Saved before stopping the app.

## Completed evidence

| Scope | Source / run | Observed result |
| --- | --- | --- |
| Initial workspace | PR merge checkout `769d75f55981746fa0b5dbe4184aa35c23efd579`, application change `b21ad155f774e510a6ae28f611e4b4440852041b`; [run 34517110243](https://github.com/CRisdon45/Plan_Trace/actions/runs/34517110243) | 89 unit tests, zero failures/errors/skips; debug assembly succeeded. |
| Final application refinement | Push checkout `b4fb724cba3f48d18c394df0e54fbec92a6e8a66`; [run 34518645007](https://github.com/CRisdon45/Plan_Trace/actions/runs/34518645007) | 90 unit tests, zero failures/errors/skips; debug assembly succeeded. Downloaded XML totals were independently inspected. |
| Actual Android emulator | `b01cec4698262086a07fecc11b2696dc4cdfed20`; [run 34518920691](https://github.com/CRisdon45/Plan_Trace/actions/runs/34518920691) | App and instrumentation builds succeeded. Two separately invoked Android scenario tests passed, followed by a byte-for-byte saved-document comparison. |

The final app source/tests are the same at b4fb724 and b01cec4; the latter changes only AVD setup in the emulator script. The unit suite is the previous 78 plus six editing/picking/viewport tests and six storage regressions. The two emulator scenarios are separate from the 90-unit-test count.

The virtual device ran API 35, x86_64, with a 1600 x 1000 display override and density 240 on a disposable GitHub Ubuntu runner with KVM. It is not a Samsung hardware or S Pen simulation. No local Android-build success is claimed.

### Scenario 1: create, edit, cancel and save

The real app opens through Projects. On a fresh disposable emulator the test creates a rectangular outline, verifies finger navigation leaves canonical geometry unchanged, enables Touch edit, and drags a vertex using its screen hit target. It verifies one revision change and retained edge IDs, then exact geometry restoration by Undo/Redo. It creates the curved outline and changes a concave arc without moving its endpoints or the other object. A canceled vertex gesture leaves the saved document unchanged. Delete/Undo restores the object. Leaving and reopening the screen retains the same draft. Result: **OK (1 test)**.

### Scenario 2: real process restart and activity recreation

The script force-stops the application after the verified save and reads its canonical file. A separate instrumentation invocation starts the app, reopens the workspace and compares its two objects and document state with the expected saved authority. Activity recreation retains the same geometry. Another force-stop and file extraction produces a byte-identical document. Result: **OK (1 test)**; final file comparison succeeded. Both files are 1,239 bytes, revision 8, with SHA-256 `7a6277b0295d981c51d92e23c8ef79aae0b9de2f851945eeed4cf1457e19053b`.

## Authentic captures and artifacts

Downloaded unit-report artifact `10168893133` verified against ZIP SHA-256 `82378d2da75cd74508acc9f5e73beaa956391811d21283d41437b43d14d203f9`. Downloaded emulator artifact `10169059105` verified against ZIP SHA-256 `fdc760cd98fd1eaee37c37e168d606b202a01122cae276d4ef90ea92913e1eb4`. Retention is seven days, not permanent storage.

The emulator artifact contains scenario results, canonical before/after files, app runtime diagnostics, UI semantics, and actual screenshots under `workspace-evidence/`. The edited and recreated screenshots were opened. The recreated screen shows two preserved outlines, selected curve/vertex handles, feet-based perimeter and Saved on device. It has SHA-256 `69f91a6329460f91da1d491aac6bd98e77d56a7ed7ccb8f4fc5a9328a470bd66`. The captured runtime crash buffer was empty; this does not imply every possible app flow is crash-free.

These are authentic outline-workspace captures, not Northstar approval or final UI designs. Capture is not explicitly synchronized to a settled frame: one image was taken immediately after selection and precedes the selected-state semantic dump. Do not treat these as pixel-golden tests. The recreated screen also exposes low-contrast system-bar icons; system-bar/settled-frame polish remains a follow-up. Portrait, handedness, pinch synthesis, screen export/share, physical stylus and performance were not part of these scenarios.

## Failure found and corrected

The first virtual-device attempt built the application but did not launch its AVD: the emulator could not find the named configuration. It was superseded/canceled and is not passed app evidence. Explicit AVD creation and lookup paths, startup presence checks, bounded adb/instrumentation commands and failure captures corrected the harness; the final run completed normally. An intermediate attempt was likewise superseded, not accepted.

The previously observed KSP/AWT background NullPointerException still appeared during the successful final emulator build. Its cause is not resolved. Existing Google-services and deprecation warnings remain. Successful tasks and Android scenarios are not a warning-free-build claim or completion of the inherited runtime-service dependency audit.

## Next bounded outcome

Use this actual editing/save flow to establish a first valid pool-boundary and following-coping operation, with straight and concave cases, explicit invalid-result handling, one Undo, stable identities and persisted/reopened intent. Keep exact numeric editing and primary radial interaction close to that workflow rather than expanding a generic toolbar. Before presenting filled surfaces or takeoff area, validate topology and offsets. Current signed area remains algebraic only.

Missing features include arbitrary outline creation, source-image/site registration, multi-project storage, portable recovery, attached steps/shelves, shared surfaces, associative dimensions, radial menus, and live Northstar rendering. Q1 remains partial. Original client/Northstar images remain private. No tablet connection, user-app replacement, data clearing, schema migration, deployment, APK publication, permission change or 3D work occurred.
