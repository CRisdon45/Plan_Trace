"""Offline regressions for the CI guard. No app/device or network calls."""
from pathlib import Path
import tempfile
import unittest
from check_local_runtime import check_variant


class RuntimePolicyTest(unittest.TestCase):
    def setUp(self):
        self.temp = tempfile.TemporaryDirectory()
        self.addCleanup(self.temp.cleanup)
        self.build = Path(self.temp.name)
        self.report = self.build / "reports/runtime-policy/debug-runtime.txt"
        self.report.parent.mkdir(parents=True)
        self.report.write_text("androidx.core:core:1.18.0\norg.locationtech.jts:jts-core:1.20.0\n")
        self.manifest = self.build / "intermediates/merged_manifests/debug/processDebugManifest/AndroidManifest.xml"
        self.manifest.parent.mkdir(parents=True)
        self.xml = '<manifest xmlns:android="http://schemas.android.com/apk/res/android">{permissions}<application>{components}</application></manifest>'
        self.write_manifest()

    def write_manifest(self, permissions="", components=""):
        self.manifest.write_text(self.xml.format(permissions=permissions, components=components))

    def test_local_graph_and_manifest_pass(self):
        self.assertEqual(2, check_variant(self.build, "debug")["resolved_module_count"])

    def test_transitive_firebase_is_rejected(self):
        self.report.write_text(self.report.read_text() + "com.google.firebase:firebase-ai:17.0.0\n")
        with self.assertRaisesRegex(ValueError, "prohibited runtime"):
            check_variant(self.build, "debug")

    def test_network_permission_from_manifest_merge_is_rejected(self):
        self.write_manifest(permissions='<uses-permission android:name="android.permission.INTERNET"/>')
        with self.assertRaisesRegex(ValueError, "permissions/components"):
            check_variant(self.build, "debug")

    def test_sdk_specific_permission_cannot_bypass_guard(self):
        self.write_manifest(permissions='<uses-permission-sdk-23 android:name="android.permission.INTERNET"/>')
        with self.assertRaises(ValueError):
            check_variant(self.build, "debug")

    def test_initialization_provider_without_permission_is_rejected(self):
        self.write_manifest(components='<provider android:name="com.google.firebase.provider.FirebaseInitProvider"/>')
        with self.assertRaises(ValueError):
            check_variant(self.build, "debug")

    def test_missing_graph_is_not_a_pass(self):
        self.report.unlink()
        with self.assertRaisesRegex(ValueError, "Missing resolved"):
            check_variant(self.build, "debug")

    def test_empty_graph_is_not_a_pass(self):
        self.report.write_text("")
        with self.assertRaisesRegex(ValueError, "Empty"):
            check_variant(self.build, "debug")

    def test_missing_manifest_is_not_a_pass(self):
        self.manifest.unlink()
        with self.assertRaisesRegex(ValueError, "Missing generated"):
            check_variant(self.build, "debug")

    def test_source_manifest_cannot_stand_in_for_built_manifest(self):
        self.manifest.unlink()
        (self.build / "AndroidManifest.xml").write_text(self.xml.format(permissions="", components=""))
        with self.assertRaises(ValueError):
            check_variant(self.build, "debug")


if __name__ == "__main__":
    unittest.main()
