#!/usr/bin/env bash
# GPU Northstar grass bake. Needs a display (Xvfb is fine). --headless is dummy
# GL and will write empty images.
set -euo pipefail
ROOT="$(cd "$(dirname "$0")" && pwd)"
OUT="${1:-$ROOT/build/grass-study}"
SEED="${2:-ground-study}"
GODOT_VERSION="4.7.2-stable"
if [[ -z "${GODOT:-}" ]]; then
  CACHE="${GODOT_CACHE:-$ROOT/.godot-engine}"
  BIN="$CACHE/Godot_v${GODOT_VERSION}_linux.x86_64"
  if [[ ! -x "$BIN" ]]; then
    mkdir -p "$CACHE"
    zip="$CACHE/godot.zip"
    curl -L --fail -o "$zip" \
      "https://github.com/godotengine/godot/releases/download/${GODOT_VERSION}/Godot_v${GODOT_VERSION}_linux.x86_64.zip"
    unzip -o "$zip" -d "$CACHE"
    chmod +x "$BIN"
  fi
  GODOT="$BIN"
fi
mkdir -p "$OUT"
ARGS=( --audio-driver Dummy --rendering-method gl_compatibility --path "$ROOT" --script res://baker.gd -- "$OUT" "$SEED" )
if command -v xvfb-run >/dev/null && [[ -z "${DISPLAY:-}" || "${FORCE_XVFB:-}" == "1" ]]; then
  xvfb-run -a -s "-screen 0 2048x1434x24" "$GODOT" "${ARGS[@]}"
else
  "$GODOT" "${ARGS[@]}"
fi
