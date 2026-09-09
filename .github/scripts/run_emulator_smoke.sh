#!/bin/sh
set -u

mkdir -p smoke-artifacts
adb logcat -c

# Run instrumentation without destroying the emulator on failure. We always capture the actual
# display plus renderer diagnostics first, then propagate the original test status to Actions.
set +e
gradle :app:connectedDebugAndroidTest --stacktrace --console=plain > smoke-artifacts/instrumentation.log 2>&1
TEST_EXIT=$?
set -e

cat smoke-artifacts/instrumentation.log
adb exec-out screencap -p > smoke-artifacts/after-instrumentation.png || true
adb logcat -d -v threadtime > smoke-artifacts/logcat-after-instrumentation.txt || true
adb logcat -d -v threadtime -s PlanTraceFilament:I '*:S' > smoke-artifacts/filament-after-instrumentation.txt || true
adb shell dumpsys gfxinfo com.example > smoke-artifacts/gfxinfo-after-instrumentation.txt 2>&1 || true
adb shell dumpsys SurfaceFlinger > smoke-artifacts/surfaceflinger-after-instrumentation.txt 2>&1 || true

[ "$TEST_EXIT" -eq 0 ] || exit "$TEST_EXIT"

# Reinstall the clean app after instrumentation so the Python smoke pass exercises a normal launch
# rather than whatever activity/database state the tests happened to leave behind.
adb install -r app/build/outputs/apk/debug/app-debug.apk
python3 .github/scripts/emulator_smoke.py
