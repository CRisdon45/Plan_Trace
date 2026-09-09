#!/bin/sh
set -u

mkdir -p smoke-artifacts
adb logcat -c

# Run instrumentation without destroying the emulator on failure. Always preserve the rendered
# display plus renderer diagnostics before propagating the original test status.
set +e
gradle :app:connectedDebugAndroidTest --stacktrace --console=plain > smoke-artifacts/instrumentation.log 2>&1
TEST_EXIT=$?
set -e

cat smoke-artifacts/instrumentation.log

# The renderer test captures this shared-storage PNG while its Perspective window is still open.
# Treat missing/corrupt visual evidence as a CI failure, not as an optional artifact.
adb pull /sdcard/filament-generated-geometry.png smoke-artifacts/filament-generated-geometry.png
python3 - <<'PY'
from pathlib import Path
path = Path('smoke-artifacts/filament-generated-geometry.png')
data = path.read_bytes()
if len(data) < 1024 or not data.startswith(b'\x89PNG\r\n\x1a\n'):
    raise SystemExit(f'Invalid Filament evidence PNG: {len(data)} bytes')
print(f'Validated Filament evidence PNG: {len(data)} bytes')
PY

adb exec-out screencap -p > smoke-artifacts/after-instrumentation.png || true
adb logcat -d -v threadtime > smoke-artifacts/logcat-after-instrumentation.txt || true
adb logcat -d -v threadtime -s PlanTraceFilament:I '*:S' > smoke-artifacts/filament-after-instrumentation.txt || true
adb shell dumpsys gfxinfo com.example > smoke-artifacts/gfxinfo-after-instrumentation.txt 2>&1 || true
adb shell dumpsys SurfaceFlinger > smoke-artifacts/surfaceflinger-after-instrumentation.txt 2>&1 || true

[ "$TEST_EXIT" -eq 0 ] || exit "$TEST_EXIT"

# Reinstall the clean app so the second smoke pass exercises a normal launch independent of the
# instrumentation database/activity state.
adb install -r app/build/outputs/apk/debug/app-debug.apk
python3 .github/scripts/emulator_smoke.py
