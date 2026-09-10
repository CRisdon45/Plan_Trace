#!/usr/bin/env bash
set -euo pipefail
mkdir -p emulator-evidence
export PATH="$ANDROID_HOME/platform-tools:$ANDROID_HOME/emulator:$ANDROID_HOME/cmdline-tools/latest/bin:$PATH"
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
printf 'no\n' | avdmanager create avd --force --name plantrace_workspace_ci --package 'system-images;android-35;google_apis;x86_64'
emulator -avd plantrace_workspace_ci -port 5554 -no-window -no-audio -no-boot-anim -no-snapshot \
  -gpu swiftshader_indirect -memory 2048 -cores 2 -accel on > emulator-evidence/emulator.log 2>&1 &
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
timeout 120 adb -s "$serial" install -r app/build/outputs/apk/debug/app-debug.apk
timeout 120 adb -s "$serial" install -r app/build/outputs/apk/androidTest/debug/app-debug-androidTest.apk
device logcat -c
function run_case() {
  local name="$1"
  timeout 180 adb -s "$serial" shell am instrument -w -r -e class "com.example.DesignWorkspaceDeviceTest#$name" \
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
{
  echo "Source: ${GITHUB_SHA:-local}"
  echo 'Completed: creation, vertex drag, curve drag, finger navigation, cancellation, Delete/Undo/Redo, disk save, process restart, activity recreation.'
  echo "API: $(device shell getprop ro.build.version.sdk | tr -d '\r')"
  echo "ABI: $(device shell getprop ro.product.cpu.abi | tr -d '\r')"
  device shell wm size
  device shell wm density
  echo 'Physical S Pen/palm and Northstar acceptance: NOT RUN.'
} | tee emulator-evidence/RESULT.txt
if [[ -n "${GITHUB_STEP_SUMMARY:-}" ]]; then cat emulator-evidence/RESULT.txt >> "$GITHUB_STEP_SUMMARY"; fi
