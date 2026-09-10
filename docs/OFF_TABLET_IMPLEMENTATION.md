# Off-tablet export and measurement implementation

Evidence recorded 2026-09-10. Application implementation is isolated on `feat/off-tablet-integrity`, PR #3 into `feat/2d-foundation-northstar`. Sharing this report on main/foundation does not merge the application patch or change the tablet's installed build.

## Implemented behavior

`ExportDialog.kt` now presents automatic measurement and source-underlay controls in the shared PDF/PNG section, rather than hiding measurements inside PDF-only settings. Each setting is a labeled, whole-row switch with a single change action. Existing callback/options are preserved. Saved state retains choices across format changes and recreation. Repetitive toggle markup was consolidated, format/header labels shortened, and the incorrect promise of a transparent output when hiding the underlay was removed. This is a focused export-flow repair, not acceptance of a final app-wide UI.

`VectorElement.kt` adds the last-to-first segment when measuring closed FreehandPath and PolylineElement instances with at least three stored points, matching their existing closure convention. An explicitly repeated first point contributes zero extra distance. Open and two-point draft chains do not acquire a second edge. No coordinates, serialized fields, layer membership, IDs, calibration or database migrations changed.

These are discrete integrity fixes, not implementation of connected pool/coping objects, analytic curves, area/takeoff calculations, primary radial menus or the Northstar renderer.

## Reproducible verification

The new `.github/workflows/android-2d-integrity.yml` runs:

```sh
./gradlew --no-daemon --console=plain --stacktrace :app:testDebugUnitTest :app:assembleDebug
```

GitHub hosted Ubuntu 24.04, JDK 21 and the repository's Gradle 9.4.1 / compile SDK 36.1 configuration were used. The local session could not download build dependencies, so no local Android build success is claimed. GitHub executed the real Android application build and tests, not a substituted implementation.

| Check | Application/source revision | Result |
| --- | --- | --- |
| Baseline full suite and debug assembly | `d9ef977f1b08c957240061bbb1022d690d7e8b98` | [Run 34496770033](https://github.com/CRisdon45/Plan_Trace/actions/runs/34496770033): 37 tests, 0 failures, 0 errors, 0 skipped; BUILD SUCCESSFUL. |
| Updated full suite and debug assembly | `e7e75e895ecf1c2863945088c5502ee58b04cdff` | [Run 34497580131](https://github.com/CRisdon45/Plan_Trace/actions/runs/34497580131): 49 tests, 0 failures, 0 errors, 0 skipped; BUILD SUCCESSFUL. |

The baseline's 37 tests are not a discrepancy with the historical 35-test focused run: the full command also includes ExampleUnitTest and GreetingScreenshotTest. The updated result comprises the same 37 plus the 12 new tests below. XML-derived counts and workflow job logs were inspected; no test result was inferred from source alone.

## New regression coverage

| Test class | Count | What actually runs |
| --- | --- | --- |
| ClosedPathMeasurementTest | 7 | Closed/open triangle, explicit closure, concave orthogonal loop, reversal/translation, degenerate/uncalibrated paths, repeated vertices and metric labels. Both freehand and polyline implementations are exercised. |
| ExportDialogTest | 4 | Actual Compose dialog: labeled PNG measurement switch and delivered callback, PDF/PNG state retention, saved-state restoration, and cancellation without an export callback. |
| PngMeasurementExportTest | 1 | Actual ExportManager PNG creation and native renderer: measurements off/on/off, output differences, identical repeated output, unchanged project data and decoded 640 x 480 image size. |

The PNG test uses a synthetic rectangle and reads each real file before the next export overwrites the cache filename. It is not a screenshot mockup or proof of Northstar appearance. Native Robolectric rendering and Compose interaction tests remain automated evidence, not physical-device trials.

## Build-log caveats

The successful updated run printed a KSP/AWT background-thread NullPointerException during the unit-test code-generation stage. Compilation, debug assembly and all 49 tests subsequently succeeded. The root cause and repeatability were not established in that run. Preserve it as a toolchain follow-up and inspect subsequent runs; do not describe this as a warning-free or exception-free build.

The configured missing Google-services file warning and existing Kotlin/Compose/action deprecation warnings also remain. This patch does not complete the runtime dependency audit. It does not infer that any private design data was sent to a service.

## Artifacts and safety

The updated run's report artifact ID is `10160618119`; its ZIP SHA-256 recorded by the upload action is `562ef4c25ef0aaa536d8a382aa7f7fbd133e2c2a3efa926c9586846e527bdf8c`. Reports are retained for seven days and may expire; this versioned report preserves the observed counts and run identity. No archive content was manually downloaded/re-inspected in this session.

Only test result/report paths are uploaded. No APK, client plan/photo, Northstar image, private mapping, keystore or release credentials are included. CI creates a short-lived local debug key solely to verify assembly. It cannot update an app signed with the owner's existing key; do not work around that by uninstalling or clearing data.

## Still pending

Physical S Pen/palm/barrel-button testing, tablet reachability in both orientations/handedness, the latest courtyard's composition, manual PNG/PDF visual acceptance, and recovery of real saved projects remain pending. No tablet was accessed or altered. The source fix is automated-verified, but the complete Q0 device/visual acceptance gate remains open. Continue independent Q1 work without converting that pending gate into a development-wide block.
