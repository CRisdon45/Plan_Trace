#!/usr/bin/env bash
set -euo pipefail
mkdir -p emulator-evidence
export PATH="$ANDROID_HOME/platform-tools:$ANDROID_HOME/emulator:$ANDROID_HOME/cmdline-tools/latest/bin:$PATH"
# Pin both SDK-tool and emulator lookup directories; their defaults can differ.
export ANDROID_USER_HOME="${RUNNER_TEMP:-/tmp}/plantrace-android/.android"
export ANDROID_EMULATOR_HOME="$ANDROID_USER_HOME"
export ANDROID_AVD_HOME="$ANDROID_USER_HOME/avd"
mkdir -p "$ANDROID_AVD_HOME"
package=com.aistudio.plantrace.jzkrwq.dev
serial=emulator-5554
function device() { timeout 30 adb -s "$serial" "$@"; }
function finish() {
  timeout 15 adb -s "$serial" exec-out screencap -p > emulator-evidence/final-screen.png 2>/dev/null || true
  timeout 15 adb -s "$serial" shell uiautomator dump /sdcard/final-ui.xml >/dev/null 2>&1 || true
  device pull /sdcard/final-ui.xml emulator-evidence/final-ui.xml >/dev/null 2>&1 || true
  device logcat -b crash -d > emulator-evidence/crash-buffer.txt 2>&1 || true
  device logcat -d -s AndroidRuntime TestRunner ActivityTaskManager > emulator-evidence/test-runtime-log.txt 2>&1 || true
  device pull "/sdcard/Android/data/$package/files/workspace-evidence" emulator-evidence/ >/dev/null 2>&1 || true
  device emu kill >/dev/null 2>&1 || true
}
trap finish EXIT
# AVD is created in this disposable runner only. Never target a USB device.
printf 'no\n' | timeout 90 avdmanager create avd --force --name plantrace_workspace_ci \
  --path "$ANDROID_AVD_HOME/plantrace_workspace_ci.avd" --package 'system-images;android-35;google_apis;x86_64'
test -s "$ANDROID_AVD_HOME/plantrace_workspace_ci.avd/config.ini"
printf 'avd.ini.encoding=UTF-8\npath=%s\ntarget=android-35\n' "$ANDROID_AVD_HOME/plantrace_workspace_ci.avd" > "$ANDROID_AVD_HOME/plantrace_workspace_ci.ini"
timeout 30 emulator -list-avds | tee emulator-evidence/available-avds.txt
grep -Fxq plantrace_workspace_ci emulator-evidence/available-avds.txt
emulator -avd plantrace_workspace_ci -port 5554 -no-window -no-audio -no-boot-anim -no-snapshot \
  -gpu swiftshader_indirect -memory 2048 -cores 2 -accel on > emulator-evidence/emulator.log 2>&1 &
emulator_pid=$!
sleep 2
kill -0 "$emulator_pid" || { cat emulator-evidence/emulator.log; exit 1; }
timeout 180 adb -s "$serial" wait-for-device
booted=false
for n in $(seq 1 120); do
  if [[ "$(device shell getprop sys.boot_completed | tr -d '\r')" == "1" ]]; then booted=true; break; fi
  sleep 2
done
[[ "$booted" == true ]] || { echo 'Emulator did not finish booting'; exit 1; }
[[ "$(device shell getprop ro.kernel.qemu | tr -d '\r')" == "1" ]] || { echo 'Refusing a non-emulator target'; exit 1; }
device shell wm size 1600x1000
device shell wm density 240
device shell settings put global window_animation_scale 0
device shell settings put global transition_animation_scale 0
device shell settings put global animator_duration_scale 0
device shell input keyevent 82
sleep 3 # Allow initial display reconfiguration to settle before launching tests.
timeout 120 adb -s "$serial" install -r app/build/outputs/apk/debug/app-debug.apk
timeout 120 adb -s "$serial" install -r app/build/outputs/apk/androidTest/debug/app-debug-androidTest.apk
device logcat -c
function run_case() {
  local name="$1"
  local test_class="${2:-com.example.DesignWorkspaceDeviceTest}"
  timeout 180 adb -s "$serial" shell am instrument -w -r -e class "$test_class#$name" \
    "$package.test/androidx.test.runner.AndroidJUnitRunner" | tee "emulator-evidence/$name.txt"
  grep -qE 'OK \([1-9][0-9]* tests?\)' "emulator-evidence/$name.txt"
  ! grep -qE 'FAILURES!!!|INSTRUMENTATION_FAILED|Process crashed' "emulator-evidence/$name.txt"
}
run_case createEditUndoPersist
device shell am force-stop "$package"
device exec-out run-as "$package" cat files/project-design/workspace.json > emulator-evidence/saved-before-restart.json
run_case reopenAfterProcessDeath
device shell am force-stop "$package"
device exec-out run-as "$package" cat files/project-design/workspace.json > emulator-evidence/saved-after-restart.json
cmp emulator-evidence/saved-before-restart.json emulator-evidence/saved-after-restart.json
# Independent follow-on scenarios keep the existing synthetic draft; no pm clear or reinstall.
run_case radialEditingSafety com.example.RadialWorkflowDeviceTest
# Source-image cases reuse the existing synthetic design without clearing user-style state.
run_case siteImportCalibrateAndMove com.example.SiteWorkspaceDeviceTest
device shell am force-stop "$package"
device exec-out run-as "$package" cat files/project-design/workspace.json > emulator-evidence/site-before-restart.json
run_case siteReopenAndExport com.example.SiteWorkspaceDeviceTest
device shell am force-stop "$package"
device exec-out run-as "$package" cat files/project-design/workspace.json > emulator-evidence/site-after-restart.json
cmp emulator-evidence/site-before-restart.json emulator-evidence/site-after-restart.json

# Layout changes happen after the source/restart scenarios. Keep the test app
# foreground while Android applies display changes, rather than reconfiguring the launcher.
function layout_display() {
  local width="$1" height="$2" density="$3"
  local activity
  activity="$(device shell cmd package resolve-activity --brief "$package" | tr -d '\r' | tail -n 1)"
  [[ "$activity" == "$package/"* ]] || { echo "Application activity could not be resolved"; exit 1; }
  device shell am start -W -n "$activity" > "emulator-evidence/display-${width}x${height}-launch.txt"
  device shell wm size "${width}x${height}"
  device shell wm density "$density"
  sleep 3
}
layout_display 1000 1600 240
run_case portraitCommands com.example.RadialWorkflowDeviceTest
device shell am force-stop "$package"
layout_display 800 1400 400
run_case compactCommands com.example.RadialWorkflowDeviceTest
device shell am force-stop "$package"
device exec-out run-as "$package" cat files/project-design/workspace.json > emulator-evidence/saved-after-radial-review.json

{
  echo "Source: ${GITHUB_SHA:-local}"
  echo 'Completed: existing edit/save/restart scenarios, real grid-snapped drag, canceled size entry, freeform uniform sizing, four-corner wheel access, disabled actions, portrait and 320dp compact fallback.'
  echo 'Additional: owned raster intake, calibrated source, second-distance match/disagreement without rescaling, source move/cancel/Undo, restart and actual PNG/PDF output. External picker UI NOT automated.'
  echo 'Every accepted in-app screenshot checks the Android active-window package; obstructed frames fail and are retained, never dismissed.'
  echo 'Display cases: landscape 1600x1000@240, portrait 1000x1600@240, compact 800x1400@400.'
  echo "API: $(device shell getprop ro.build.version.sdk | tr -d '\r')"
  echo "ABI: $(device shell getprop ro.product.cpu.abi | tr -d '\r')"
  device shell wm size
  device shell wm density
  echo 'Physical S Pen/palm and Northstar acceptance: NOT RUN.'
} | tee emulator-evidence/RESULT.txt
if [[ -n "${GITHUB_STEP_SUMMARY:-}" ]]; then cat emulator-evidence/RESULT.txt >> "$GITHUB_STEP_SUMMARY"; fi
