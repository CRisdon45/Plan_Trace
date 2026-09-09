#!/usr/bin/env python3
"""Launch Plan Trace in an emulator, exercise Perspective, and save browser-reviewable evidence."""

from __future__ import annotations

import pathlib
import re
import subprocess
import time
import xml.etree.ElementTree as ET

OUT = pathlib.Path("smoke-artifacts")
OUT.mkdir(parents=True, exist_ok=True)
PACKAGE = "com.aistudio.plantrace.jzkrwq"
COMPONENT = f"{PACKAGE}/com.example.MainActivity"


def run(*args: str, check: bool = True, capture: bool = False) -> subprocess.CompletedProcess[str]:
    return subprocess.run(
        list(args),
        check=check,
        text=True,
        stdout=subprocess.PIPE if capture else None,
        stderr=subprocess.STDOUT if capture else None,
    )


def adb(*args: str, check: bool = True, capture: bool = False) -> subprocess.CompletedProcess[str]:
    return run("adb", *args, check=check, capture=capture)


def screenshot(name: str) -> None:
    remote = f"/sdcard/{name}.png"
    adb("shell", "screencap", "-p", remote)
    adb("pull", remote, str(OUT / f"{name}.png"))


def dump_ui(name: str) -> ET.Element:
    remote = f"/sdcard/{name}.xml"
    local = OUT / f"{name}.xml"
    adb("shell", "uiautomator", "dump", remote)
    adb("pull", remote, str(local))
    return ET.parse(local).getroot()


def node_value(node: ET.Element) -> str:
    return " | ".join(
        value for value in (
            node.attrib.get("text", ""),
            node.attrib.get("content-desc", ""),
            node.attrib.get("resource-id", ""),
        )
        if value
    )


def find_node(root: ET.Element, needle: str) -> ET.Element | None:
    needle_lower = needle.lower()
    for node in root.iter("node"):
        if needle_lower in node_value(node).lower():
            return node
    return None


def require_node(root: ET.Element, needle: str) -> ET.Element:
    node = find_node(root, needle)
    if node is None:
        visible = "\n".join(filter(None, (node_value(n) for n in root.iter("node"))))
        raise RuntimeError(f"Could not find UI node containing {needle!r}. Visible nodes:\n{visible}")
    return node


def tap_node(node: ET.Element) -> None:
    match = re.fullmatch(r"\[(\d+),(\d+)\]\[(\d+),(\d+)\]", node.attrib.get("bounds", ""))
    if not match:
        raise RuntimeError(f"Node has unusable bounds: {node.attrib}")
    left, top, right, bottom = map(int, match.groups())
    adb("shell", "input", "tap", str((left + right) // 2), str((top + bottom) // 2))


def save_text(name: str, text: str) -> None:
    (OUT / name).write_text(text, encoding="utf-8")


def main() -> None:
    adb("wait-for-device")
    adb("logcat", "-c")

    start = adb("shell", "am", "start", "-W", "-n", COMPONENT, capture=True)
    save_text("activity-start.txt", start.stdout or "")
    if "Error" in (start.stdout or ""):
        raise RuntimeError(start.stdout)

    time.sleep(6)
    screenshot("01-launch")
    launch = dump_ui("01-launch")

    require_node(launch, "S Pen Only (Palm Rejection Active)")
    perspective_button = require_node(launch, "Open Perspective preview")

    # Open the native Filament surface from the real Compose UI.
    tap_node(perspective_button)
    time.sleep(7)
    screenshot("02-perspective")
    perspective = dump_ui("02-perspective")
    require_node(perspective, "Perspective · Filament preview")
    require_node(perspective, "Drag to orbit · Pinch to zoom")

    # Exercise a real touch orbit on the SurfaceView. The exact camera result is reviewed in the PNG.
    size = adb("shell", "wm", "size", capture=True).stdout or ""
    match = re.search(r"Physical size:\s*(\d+)x(\d+)", size)
    if match:
        width, height = map(int, match.groups())
        adb(
            "shell", "input", "swipe",
            str(int(width * 0.58)), str(int(height * 0.60)),
            str(int(width * 0.72)), str(int(height * 0.50)),
            "700",
        )
        time.sleep(2)
        screenshot("03-perspective-orbit")

    close_button = require_node(perspective, "Close Perspective")
    tap_node(close_button)
    time.sleep(2)
    screenshot("04-return-to-plan")
    returned = dump_ui("04-return-to-plan")
    require_node(returned, "S Pen Only (Palm Rejection Active)")

    # Capture diagnostics after all exercised paths.
    logcat = adb("logcat", "-d", "-v", "threadtime", capture=True).stdout or ""
    save_text("logcat.txt", logcat)
    gfx = adb("shell", "dumpsys", "gfxinfo", PACKAGE, capture=True).stdout or ""
    save_text("gfxinfo.txt", gfx)

    fatal_markers = ("FATAL EXCEPTION", "Fatal signal", "ANR in " + PACKAGE)
    if any(marker in logcat for marker in fatal_markers):
        raise RuntimeError("Fatal Android runtime marker found in logcat; see smoke-artifacts/logcat.txt")

    print("Plan Trace emulator smoke test passed; evidence saved in smoke-artifacts/.")


if __name__ == "__main__":
    main()
