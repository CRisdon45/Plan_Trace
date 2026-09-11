#!/usr/bin/env python3
"""Fail closed on missing evidence or prohibited runtime dependencies/permissions.

This is a guard for the current local-only app, not an exhaustive security audit.
It checks resolved transitive modules and generated (not source) app manifests.
System document providers and Android's share sheet run outside this app's process.
"""
from __future__ import annotations

import argparse
import json
import os
from pathlib import Path
import sys
import xml.etree.ElementTree as ET

ANDROID = "{http://schemas.android.com/apk/res/android}"
BLOCKED_GROUPS = (
    "com.google.firebase", "com.google.android.gms", "com.google.android.recaptcha",
    "com.google.ai", "com.google.genai", "com.openai", "com.anthropic",
    "dev.langchain4j", "ai.koog", "com.squareup.retrofit2", "com.squareup.moshi",
    "com.squareup.okhttp3", "io.coil-kt",
)
BLOCKED_PERMISSIONS = {
    "android.permission.INTERNET", "android.permission.ACCESS_NETWORK_STATE",
    "com.google.android.gms.permission.AD_ID",
}
BLOCKED_COMPONENTS = ("com.google.firebase.", "com.google.android.gms.", "com.google.android.recaptcha.")


def check_variant(build_dir: Path, variant: str) -> dict:
    coordinates = build_dir / "reports/runtime-policy" / f"{variant}-runtime.txt"
    if not coordinates.is_file():
        raise ValueError(f"Missing resolved {variant} runtime report")
    modules = [line.strip() for line in coordinates.read_text().splitlines() if line.strip()]
    if not modules or any(len(module.split(":")) != 3 for module in modules):
        raise ValueError(f"Empty or malformed {variant} runtime report")
    blocked = [m for m in modules if any(
        m.split(":")[0] == prefix or m.split(":")[0].startswith(prefix + ".")
        for prefix in BLOCKED_GROUPS)]
    if blocked:
        raise ValueError(f"{variant} includes prohibited runtime modules: {blocked}")
    # AGP uses one of these output roots; inspect every emitted final merged app manifest.
    manifests = sorted({p for folder in ("merged_manifest", "merged_manifests")
                        for p in (build_dir / "intermediates" / folder / variant).rglob("AndroidManifest.xml")})
    if not manifests:
        raise ValueError(f"Missing generated {variant} app manifest (not a passing check)")
    records = []
    for path in manifests:
        root = ET.parse(path).getroot()
        if root.tag != "manifest" or root.find("application") is None:
            raise ValueError(f"Not an app manifest: {path}")
        permissions = sorted({node.get(ANDROID + "name", "") for node in root
                              if node.tag.startswith("uses-permission")})
        found = BLOCKED_PERMISSIONS.intersection(permissions)
        components = sorted({node.get(ANDROID + "name", "") for node in root.iter()
                             if node.tag in ("activity", "service", "receiver", "provider", "meta-data")})
        unexpected = [name for name in components if name.startswith(BLOCKED_COMPONENTS)]
        if found or unexpected:
            raise ValueError(f"{variant} manifest contains prohibited permissions/components: {sorted(found)}, {unexpected}")
        records.append({"path": str(path.relative_to(build_dir)), "permissions": permissions,
                        "components": components})
    return {"variant": variant, "resolved_module_count": len(modules), "manifests": records}


def main() -> int:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--build-dir", type=Path, default=Path("app/build"))
    args = parser.parse_args()
    report_dir = args.build_dir / "reports/runtime-policy"
    report_dir.mkdir(parents=True, exist_ok=True)
    report = {"source": os.environ.get("GITHUB_SHA", "local"), "passed": False, "variants": []}
    try:
        for variant in ("debug", "release"):
            report["variants"].append(check_variant(args.build_dir, variant))
        report["passed"] = True
    except (ValueError, OSError, ET.ParseError) as error:
        report["error"] = str(error)
    (report_dir / "result.json").write_text(json.dumps(report, indent=2) + "\n")
    print(json.dumps(report, indent=2))
    return 0 if report["passed"] else 1


if __name__ == "__main__":
    sys.exit(main())
