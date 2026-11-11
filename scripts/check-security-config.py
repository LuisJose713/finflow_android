"""Regression checks for Android transport, backup and repository logging policy.

This is a focused source check, not a vulnerability scanner or penetration test.
Run from any directory with: python3 scripts/check-security-config.py
"""

from pathlib import Path
import re
import xml.etree.ElementTree as ET

ROOT = Path(__file__).resolve().parent.parent
MAIN = ROOT / "app/src/main"
ANDROID = "{http://schemas.android.com/apk/res/android}"

application = ET.parse(MAIN / "AndroidManifest.xml").getroot().find("application")
assert application is not None, "Application manifest missing"
assert application.get(ANDROID + "allowBackup") == "false", "App backup must stay disabled"
assert application.get(ANDROID + "usesCleartextTraffic") == "false", "Cleartext traffic must stay disabled"

domains = {
    "root", "file", "database", "sharedpref", "external",
    "device_root", "device_file", "device_database", "device_sharedpref",
}


def check_exclusions(element):
    assert element is not None, "Backup/transfer policy missing"
    excluded = {node.get("domain") for node in element.findall("exclude") if node.get("path") == "."}
    assert domains <= excluded, f"Unprotected backup domains: {domains - excluded}"
    assert not element.findall("include"), "Backup includes contradict the disabled-backup policy"


check_exclusions(ET.parse(MAIN / "res/xml/backup_rules.xml").getroot())
extraction = ET.parse(MAIN / "res/xml/data_extraction_rules.xml").getroot()
check_exclusions(extraction.find("cloud-backup"))
check_exclusions(extraction.find("device-transfer"))

repositories = MAIN / "java/com/softdevcode/finflow_android/data/repository"
for source in repositories.glob("*.kt"):
    assert not re.search(r"\b(?:Log\.\w+|print|println)\s*\(", source.read_text()), (
        f"Repository logging can disclose financial data: {source.name}"
    )

network = (MAIN / "java/com/softdevcode/finflow_android/di/Networkmodule.kt").read_text()
assert "HttpLoggingInterceptor.Level.NONE" in network, "HTTP body/header logging must stay disabled"
print("Android security configuration checks passed")
