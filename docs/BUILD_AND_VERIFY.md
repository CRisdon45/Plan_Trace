# Build and verify Plan Trace

## Local development

The repository now includes the Gradle 9.4.1 wrapper launchers and wrapper JAR. Use JDK 21 (the installed Android Studio runtime works on the development machine) and Android SDK platform 36.1. Set `JAVA_HOME` and `ANDROID_HOME` to your installed locations. Dependencies require network access on the first build.

On Windows PowerShell:

```powershell
.\gradlew.bat :app:testDebugUnitTest --tests com.example.PlanIntegrityTest --tests com.example.ExampleRobolectricTest :app:assembleDebug
```

The APK is `app/build/outputs/apk/debug/app-debug.apk`. Debug builds use `com.aistudio.plantrace.jzkrwq.dev`, display **Plan Trace Dev**, and have a `-foundation-dev` version suffix. They install beside the original application and do not share its database. Release identity remains unchanged. This is deliberate while reconciling the original APK with the available source.

The focused integrity tests cover layer/object recovery, whole-gesture history, cancellation, export bounds, physical graphic scale, native PNG alignment and share-intent permissions. The existing model tests cover geometry, measurements, straightening and serialization. Actual Android file-provider sharing is verified on the connected tablet because native Windows path separators differ from Android's paths inside the host test environment.

## Runtime regression sequence

Use disposable QA projects in Plan Trace Dev. Do not uninstall the original app or clear its storage.

1. Draw a rectangle. Duplicate and Delete through the floating controls; compare object counts. Undo deletion.
2. Drag the rectangle. One Undo must restore its entire original geometry.
3. Select Erase, start calibration across the drawing, then Cancel. Geometry must remain unchanged.
4. Create a Patio layer with multiple objects. Delete it and Undo once. Layer properties, order, active layer and all objects must return, with no orphaned references.
5. Lock Patio and attempt selection/movement. Saved object geometry must remain unchanged.
6. Import the QA 1000 × 700 image, verify Uncalibrated state, then calibrate the known reference.
7. Export PNG and PDF. The Android chooser must open; no message needs to be sent. Inspect the actual generated files for underlay/annotation alignment and graphic scale accuracy.
8. Force-stop/reopen. Compare project, geometry and calibration. Test finger pan/zoom separately from drawing.

This is a development milestone, not a premium release certification. Physical S Pen behavior, per-page markup, exact editing beyond rectangles, portrait layout and the Northstar renderer remain subsequent work.


9. Set a rectangle to a known width/height in calibrated units. Drag a corner; verify the opposite corner stays fixed and one Undo restores exact bounds.
10. Finish an open polyline; Undo it. Close a three-point path. Cancel a draft and confirm no saved object was added.
11. Revise an existing note. Confirm identity, position and styling remain unchanged; Undo restores the prior text.
12. Import an image and an extensionless-provider PDF through Files, force-stop/reopen and visually confirm the underlay survives. Geometry-only database comparison is insufficient for this check.
