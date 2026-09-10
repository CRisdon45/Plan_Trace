# Build and verify

Use the active application branch identified in [CURRENT_STATE.md](CURRENT_STATE.md), not an assumption that the default branch contains the latest app. Documentation presence on `main` is not application parity. Check live branch/commit identity before building or installing.

## Recorded environment and commands

The earlier build report records JDK 21, Android SDK platform 36.1 and a Gradle 9.4.1 wrapper on the 2D foundation branch. Verify the actual checked-out Gradle configuration if those requirements change. Set JAVA_HOME and ANDROID_HOME to locally installed tools; do not commit local paths or credentials. Initial dependency resolution may need network access.

Run the full applicable unit-test suite and debug assembly, rather than copying an old hand-picked list that could omit new tests:

```powershell
.\gradlew.bat :app:testDebugUnitTest :app:assembleDebug
```

```sh
./gradlew :app:testDebugUnitTest :app:assembleDebug
```

These are verification instructions, not a claim they ran in the documentation session. Report environmental failures distinctly from test failures and passes. Do not invent a clean build when no build environment is available.

## Safe installation

The inspected development configuration produces `app/build/outputs/apk/debug/app-debug.apk` and uses package `com.aistudio.plantrace.jzkrwq.dev`, named Plan Trace Dev, alongside the original app. Verify the built package before installing. Preserve the original app, databases and editable work; do not uninstall, clear data or replace its release identity to simplify testing. Use disposable synthetic QA projects.

Record the actual source commit, build variant, APK checksum, commands, test reports and device/OS for a checked build. Do not label an older installed APK as the latest source. The historical original-APK/source parity issue remains distinct from new development-build verification.

## Verification dimensions

Automated checks should cover geometry, closed perimeter/area, units and registration, whole-action history, locks, valid IDs/ownership, serialization/migration and export alignment. Exercise the same command and rendering paths as the UI.

Runtime checks include creating/revising notes and shapes, calibration exclusivity, layer delete/Undo, locked mutation attempts, page/source changes, cancellation, and force-stop/reopen. Inspect source underlays as well as saved geometry. Successful retained file access is not a portable backup test.

Physical tablet checks separately establish pen/button/palm behavior, finger navigation, portrait/landscape and handedness reachability. Synthetic events are useful but cannot establish physical feel. Visual checks inspect real canvas and PNG/PDF results at working and output scale; screenshots of a mockup are not implementation evidence.

Use [BENCHMARKS.md](BENCHMARKS.md) and the Q0 resume point for task-specific acceptance. Review every capture, log and artifact before publishing from a public repository. The documentation consolidation does not add an Android workflow, run CI, or certify a new APK.
