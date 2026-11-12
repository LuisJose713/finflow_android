"""Describe an already built, signed APK. This script never builds an APK."""

import argparse
import hashlib
import json
from pathlib import Path
import re
import subprocess

REPOSITORY = "LuisJose713/finflow_android"
APPLICATION_ID = "com.softdevcode.finflow_android"


def apk_identity(badging):
    package = re.search(r"^package: name='([^']+)' versionCode='(\d+)' versionName='([^']+)'", badging, re.M)
    if not package or package[1] != APPLICATION_ID:
        raise ValueError("The APK must belong to FinFlow Android")
    code = int(package[2])
    name = package[3]
    if code < 1 or not name or len(name) > 80:
        raise ValueError("Invalid APK version")
    return code, name


def make_manifest(release, version_code, version_name, apk_name):
    if release.get("draft") or release.get("prerelease"):
        raise ValueError("Only stable, published releases are offered")
    tag = release["tag_name"]
    if not re.fullmatch(r"[A-Za-z0-9._-]+", tag):
        raise ValueError("Use a simple version tag, for example v1.2.3-finflow")
    if tag != f"v{version_name}":
        raise ValueError("The release tag must be v + the versionName inside the APK")
    if not re.fullmatch(r"[A-Za-z0-9._-]+\.apk", apk_name):
        raise ValueError("APK filenames must use letters, numbers, dots, hyphens or underscores")
    apks = [a for a in release.get("assets", []) if a["name"].lower().endswith(".apk")]
    if len(apks) != 1 or apks[0]["name"] != apk_name or apks[0].get("size", 0) <= 0:
        raise ValueError("Attach exactly one signed APK before publishing the release")
    url = f"https://github.com/{REPOSITORY}/releases/download/{tag}/{apk_name}"
    if apks[0]["browser_download_url"] != url:
        raise ValueError("The APK must be an asset in the FinFlow Android repository")
    return {
        "applicationId": APPLICATION_ID,
        "versionCode": version_code,
        "versionName": version_name,
        "downloadUrl": url,
        "releaseNotes": (release.get("body") or "Mejoras y correcciones de FinFlow.")[:6000],
    }


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--apk", required=True, type=Path)
    parser.add_argument("--release-json", required=True, type=Path)
    parser.add_argument("--build-tools", required=True, type=Path)
    parser.add_argument("--output", required=True, type=Path)
    args = parser.parse_args()
    subprocess.run([str(args.build_tools / "apksigner"), "verify", str(args.apk)], check=True)
    badging = subprocess.check_output([str(args.build_tools / "aapt"), "dump", "badging", str(args.apk)], text=True)
    version_code, version_name = apk_identity(badging)
    manifest = make_manifest(json.loads(args.release_json.read_text()), version_code, version_name, args.apk.name)
    with args.apk.open("rb") as binary:
        manifest["sha256"] = hashlib.file_digest(binary, "sha256").hexdigest()
    manifest["sizeBytes"] = args.apk.stat().st_size
    args.output.write_text(json.dumps(manifest, indent=2, ensure_ascii=False) + "\n", encoding="utf-8")
    print(f"Validated signed FinFlow APK: {version_name} ({version_code})")


if __name__ == "__main__":
    main()

