import copy
import importlib.util
from pathlib import Path
import unittest

spec = importlib.util.spec_from_file_location("manifest", Path(__file__).with_name("create-update-manifest.py"))
manifest = importlib.util.module_from_spec(spec)
spec.loader.exec_module(manifest)


class UpdatePublicationTest(unittest.TestCase):
    def setUp(self):
        self.release = {"draft": False, "prerelease": False, "tag_name": "v1.2.3-finflow",
            "body": "Inicio corregido.", "assets": [{"name": "FinFlow-1.2.3.apk", "size": 123,
            "browser_download_url": "https://github.com/LuisJose713/finflow_android/releases/download/v1.2.3-finflow/FinFlow-1.2.3.apk"}]}

    def test_version_is_read_from_the_actual_apk(self):
        code, name = manifest.apk_identity("package: name='com.softdevcode.finflow_android' versionCode='6' versionName='1.2.3-finflow' platformBuildVersionName='14'")
        update = manifest.make_manifest(self.release, code, name, "FinFlow-1.2.3.apk")
        self.assertEqual(update["versionCode"], 6)
        self.assertEqual(update["releaseNotes"], "Inicio corregido.")
        self.assertTrue(update["downloadUrl"].endswith(".apk"))

    def test_beta_version_can_use_base_release_tag(self):
        release = copy.deepcopy(self.release)
        release["tag_name"] = "v1.2.7"
        release["assets"][0]["name"] = "app-release.apk"
        release["assets"][0]["browser_download_url"] = "https://github.com/LuisJose713/finflow_android/releases/download/v1.2.7/app-release.apk"
        update = manifest.make_manifest(release, 10, "1.2.7-beta", "app-release.apk")
        self.assertEqual(update["versionCode"], 10)
        self.assertEqual(update["versionName"], "1.2.7-beta")
        with self.assertRaises(ValueError):
            manifest.make_manifest(release, 10, "1.2.7-rc", "app-release.apk")

    def test_other_apps_are_rejected(self):
        with self.assertRaises(ValueError):
            manifest.apk_identity("package: name='other.app' versionCode='6' versionName='1.2.3'")

    def test_unpublished_releases_and_missing_or_multiple_apks_are_rejected(self):
        for change in ({"draft": True}, {"prerelease": True}, {"assets": []},
                       {"assets": self.release["assets"] * 2}, {"tag_name": "v9.0"}):
            with self.subTest(change=change), self.assertRaises(ValueError):
                manifest.make_manifest(self.release | change, 6, "1.2.3-finflow", "FinFlow-1.2.3.apk")

    def test_external_downloads_are_rejected(self):
        release = copy.deepcopy(self.release)
        release["assets"][0]["browser_download_url"] = "https://other.example/app.apk"
        with self.assertRaises(ValueError):
            manifest.make_manifest(release, 6, "1.2.3-finflow", "FinFlow-1.2.3.apk")


if __name__ == "__main__":
    unittest.main()
