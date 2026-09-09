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

# Keep the visual proof tied to the exact semantic mesh we intend to exercise. A rectangle prism
# is one authoritative vector element and five generated faces (top + four sides). This also guards
# against the earlier race where the raw stylus injection hit the previous Pen tool and produced an
# eight-face freehand ribbon, and against duplicate AndroidView project submissions rebuilding the
# GPU mesh twice during initial composition.
python3 - <<'PY'
from pathlib import Path
log_path = Path('smoke-artifacts/filament-after-instrumentation.txt')
text = log_path.read_text(encoding='utf-8', errors='replace')
project_lines = [line for line in text.splitlines() if 'setProject title=' in line]
mesh_lines = [line for line in text.splitlines() if 'Meshes rebuilt ' in line]

if len(project_lines) != 1:
    raise SystemExit(f'Expected exactly one Filament project submission, found {len(project_lines)}')
if 'elements=1 faces=5' not in project_lines[0]:
    raise SystemExit(f'Perspective proof did not render the expected rectangle prism: {project_lines[0]}')
if len(mesh_lines) != 1:
    raise SystemExit(f'Expected exactly one Filament mesh rebuild, found {len(mesh_lines)}')
if 'frontFaceInverted=false' not in text:
    raise SystemExit('Filament Perspective is not using the normal front-face convention')
if 'Rendered frame=1' not in text:
    raise SystemExit('Filament never presented its first frame during instrumentation')

print('Validated Filament mesh contract: 1 rectangle element -> 5 faces -> 1 GPU rebuild')
PY

[ "$TEST_EXIT" -eq 0 ] || exit "$TEST_EXIT"

# Reinstall the clean app so the second smoke pass exercises a normal launch independent of the
# instrumentation database/activity state.
adb install -r app/build/outputs/apk/debug/app-debug.apk
python3 .github/scripts/emulator_smoke.py
