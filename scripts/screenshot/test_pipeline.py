#!/usr/bin/env python3
"""Behavioral fixtures for the same Python entrypoints used by screenshot workflows."""
import copy
import importlib.util
import json
from pathlib import Path
import shutil
import struct
import subprocess
import sys
import tempfile
import unittest
import zlib

SCRIPT = Path(__file__).with_name("pipeline.py")
SPEC = importlib.util.spec_from_file_location("screenshot_pipeline", SCRIPT)
pipeline = importlib.util.module_from_spec(SPEC)
SPEC.loader.exec_module(pipeline)
REPO = SCRIPT.parents[2]
BASE, HEAD, MERGE, IMAGE = (c * 40 for c in "abcd")


def png(path, width, height, byte=0):
    def chunk(kind, payload):
        return struct.pack(">I", len(payload)) + kind + payload + struct.pack(">I", zlib.crc32(kind + payload) & 0xFFFFFFFF)
    data = b"\x89PNG\r\n\x1a\n"
    data += chunk(b"IHDR", struct.pack(">IIBBBBB", width, height, 8, 6, 0, 0, 0))
    data += chunk(b"IDAT", zlib.compress((b"\x00" + bytes([byte]) * width * 4) * height))
    data += chunk(b"IEND", b"")
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_bytes(data)


class ScreenshotPipelineTest(unittest.TestCase):
    def setUp(self):
        self.temp = tempfile.TemporaryDirectory()
        self.addCleanup(self.temp.cleanup)
        self.root = Path(self.temp.name)
        self.scenarios = {"home": ["home.home.with_deck"], "setting": ["setting.setting.account"]}
        self.config = pipeline.read_json(REPO / "scripts/screenshot/capture-config.json")
        # Small complete PNG fixtures exercise the production contract without rendering UI.
        for profiles in self.config["profiles"].values():
            profiles["default"]["widthPx"] = 12
            profiles["default"]["heightPx"] = 18
        self.inputs()
        self.baseline = self.root / "baseline"
        self.context = {"pr_number": 195, "base_sha": BASE, "head_sha": HEAD, "merge_sha": MERGE, "run_id": 123}

    def inputs(self):
        pipeline.write_json(self.root / "scripts/screenshot/scenarios.json", self.scenarios)
        pipeline.write_json(self.root / "scripts/screenshot/capture-config.json", self.config)
        for module in self.scenarios:
            (self.root / f"feature/{module}").mkdir(parents=True, exist_ok=True)
            (self.root / f"feature/{module}/build.gradle.kts").write_text("// fixture\n")

    def output(self, capture):
        return self.root / f"feature/{capture['module']}/build/outputs/roborazzi/{pipeline.TARGETS['android']}"

    def golden(self):
        for capture in pipeline.expand(self.scenarios, self.config, "android"):
            png(self.output(capture) / capture["filename"], capture["widthPx"], capture["heightPx"])
        pipeline.record_golden(self.root, "android", BASE, self.baseline)
        return pipeline.read_json(self.baseline / "metadata.json")

    def collect_fixture(self, changes=None):
        changes = changes or {}
        captures = pipeline.expand(self.scenarios, self.config, "android")
        for module in self.scenarios:
            results = []
            for capture in (c for c in captures if c["module"] == module):
                kind = changes.get(pipeline.key(capture), "unchanged")
                results.append({"golden_file_path": "/absolute/output/" + capture["filename"], "type": kind})
                if kind != "unchanged":
                    for suffix in ("_actual.png", "_compare.png") if kind == "changed" else ("_actual.png",):
                        png(self.output(capture) / (capture["filename"][:-4] + suffix), capture["widthPx"], capture["heightPx"], 1)
            pipeline.write_json(self.root / f"feature/{module}/build/test-results/roborazzi/androidHostTest/results-summary.json", {"results": results})
        return pipeline.collect(self.root, "android", BASE, HEAD, MERGE, 195, self.baseline, self.root / "report", self.root / "originals")

    def test_full_registry_cross_product_unique_names_and_platform_symmetry(self):
        scenarios, config = pipeline.load_inputs(REPO)
        expected = sum(map(len, scenarios.values())) * len(config["languages"])
        android = pipeline.expand(scenarios, config, "android")
        ios = pipeline.expand(scenarios, config, "ios")
        self.assertEqual(expected, len(android))
        self.assertEqual(expected, len(ios))
        self.assertEqual(len(android + ios), len({c["filename"] for c in android + ios}))
        self.assertEqual({(c["module"], c["scenario"], c["language"]) for c in android}, {(c["module"], c["scenario"], c["language"]) for c in ios})
        for captures in (android, ios):
            for capture in captures:
                self.assertEqual(capture["variant"] == "small-large-font", capture["scenario"] == "setting.notification.small.large-font")
        self.assertEqual("home.home.with_deck__ko__pixel10-pro.png", next(c["filename"] for c in android if c["scenario"] == "home.home.with_deck" and c["language"] == "ko"))
        self.assertEqual((1280, 2856, 3.0), tuple(config["profiles"]["android"]["default"][k] for k in ("widthPx", "heightPx", "density")))
        self.assertEqual((1206, 2622, 3.0), tuple(config["profiles"]["ios"]["default"][k] for k in ("widthPx", "heightPx", "density")))

    def test_malformed_duplicate_and_unsafe_registrations(self):
        for value in ({}, {"../escape": ["ok"]}, {"home": []}, {"home": ["id", "id"]}, {"a": ["id"], "b": ["id"]}, {"a": ["id__ko"]}, {"a": ["id_actual"]}, {"a": [7]}):
            with self.subTest(value=value), self.assertRaises(ValueError):
                pipeline.registry(value)
        path = self.root / "duplicate.json"
        path.write_text('{"home": ["one"], "home": ["two"]}')
        with self.assertRaisesRegex(ValueError, "Duplicate JSON key"):
            pipeline.read_json(path)

    def test_configuration_rejects_wrong_language_missing_platform_and_bad_geometry(self):
        for change in (lambda c: c.update(languages=["ko"]), lambda c: c["profiles"].pop("ios"), lambda c: c["profiles"]["android"]["default"].update(density=495), lambda c: c["profiles"]["ios"]["small-large-font"].update(fontScale=1.0)):
            value = copy.deepcopy(self.config)
            change(value)
            with self.assertRaises(ValueError):
                pipeline.configuration(value)

    def test_golden_collection_and_restore_preserve_identity_and_png(self):
        metadata = self.golden()
        self.assertEqual(BASE, metadata["source_sha"])
        self.assertEqual([12, 18], metadata["captures"][0]["pngSize"])
        shutil.rmtree(self.root / "feature/home/build")
        pipeline.restore_golden(self.root, "android", BASE, self.baseline)
        capture = metadata["captures"][0]
        self.assertTrue((self.output(capture) / capture["filename"]).is_file())

    def test_golden_registration_missing_and_extra_outputs_fail(self):
        captures = pipeline.expand(self.scenarios, self.config, "android")
        for capture in captures:
            png(self.output(capture) / capture["filename"], 12, 18)
        missing = self.output(captures[0]) / captures[0]["filename"]
        missing.unlink()
        with self.assertRaisesRegex(ValueError, "Registry/output mismatch"):
            pipeline.record_golden(self.root, "android", BASE, self.baseline)
        png(missing, 12, 18)
        png(self.output(captures[0]) / "not-registered.png", 12, 18)
        with self.assertRaisesRegex(ValueError, "Registry/output mismatch"):
            pipeline.record_golden(self.root, "android", BASE, self.baseline)

    def test_legacy_wrong_sha_platform_schema_config_locale_profile_and_missing_image_fail(self):
        self.golden()
        metadata_path = self.baseline / "metadata.json"
        original = metadata_path.read_text()
        for name, value in (("source_sha", HEAD), ("platform", "ios"), ("schema_version", 1), ("configuration_hash", "0" * 64), ("languages", ["ko"]), ("profiles", self.config["profiles"]["ios"])):
            metadata = json.loads(original)
            metadata[name] = value
            pipeline.write_json(metadata_path, metadata)
            with self.subTest(name=name), self.assertRaises(ValueError):
                pipeline.validate_golden(self.baseline, self.config, "android", BASE)
        metadata_path.write_text(original)
        incompatible = copy.deepcopy(self.config)
        incompatible["profiles"]["android"]["default"]["widthPx"] += 1
        with self.assertRaisesRegex(ValueError, "Incompatible Golden configuration"):
            pipeline.validate_golden(self.baseline, incompatible, "android", BASE)
        metadata_path.unlink()
        with self.assertRaisesRegex(ValueError, "Legacy/missing Golden"):
            pipeline.validate_golden(self.baseline, self.config, "android", BASE)
        metadata_path.write_text(original)
        next(self.baseline.glob("home/*.png")).unlink()
        with self.assertRaisesRegex(ValueError, "Golden files do not match"):
            pipeline.validate_golden(self.baseline, self.config, "android", BASE)

    def test_missing_failed_expired_and_wrong_sha_golden_runs_fail(self):
        run = {"head_sha": BASE, "head_branch": "develop", "event": "push", "status": "completed", "conclusion": "success"}
        artifacts = {"artifacts": [{"name": "screenshot-golden-android", "expired": False}]}
        pipeline.validate_golden_run(run, artifacts, BASE, "android")
        cases = [(dict(run, head_sha=HEAD), artifacts), (dict(run, conclusion="failure"), artifacts), (dict(run, status="in_progress"), artifacts), (run, {"artifacts": []}), (run, {"artifacts": [{"name": "screenshot-golden-android", "expired": True}]}), (run, {"artifacts": [{"name": "screenshot-golden-ios", "expired": False}]})]
        for current, available in cases:
            with self.subTest(run=current, artifacts=available), self.assertRaises(ValueError):
                pipeline.validate_golden_run(current, available, BASE, "android")

    def test_png_geometry_corruption_and_symlink_fail(self):
        capture = pipeline.expand(self.scenarios, self.config, "android")[0]
        path = self.root / "image.png"
        png(path, 13, 18)
        with self.assertRaisesRegex(ValueError, "geometry mismatch"):
            pipeline.capture_size(path, capture)
        data = bytearray(path.read_bytes())
        data[-1] ^= 1
        path.write_bytes(data)
        with self.assertRaisesRegex(ValueError, "CRC mismatch"):
            pipeline.png_size(path)
        link = self.root / "link.png"
        link.symlink_to(path)
        with self.assertRaises(ValueError):
            pipeline.png_size(link)

    def test_collect_changed_unchanged_added_removed_and_publication_allowlist(self):
        self.golden()
        self.scenarios = {"home": ["home.home.with_deck", "home.new.scenario"]}
        self.inputs()
        current = pipeline.expand(self.scenarios, self.config, "android")
        changes = {pipeline.key(c): "added" if c["scenario"] == "home.new.scenario" else "changed" if c["language"] == "ko" else "unchanged" for c in current}
        summary = self.collect_fixture(changes)
        self.assertEqual(1, len(summary["changed"]))
        self.assertEqual(2, len(summary["added"]))
        self.assertEqual(2, len(summary["removed"]))
        pipeline.validate_report(self.root / "originals", self.context, "android", self.config)
        self.assertEqual(4, len(list((self.root / "originals").rglob("*.png"))))
        png(self.root / "originals/unexpected.png", 12, 18)
        with self.assertRaisesRegex(ValueError, "allowlist mismatch"):
            pipeline.validate_report(self.root / "originals", self.context, "android", self.config)

    def test_existing_capture_cannot_be_reclassified_as_added(self):
        self.golden()
        capture = pipeline.expand(self.scenarios, self.config, "android")[0]
        with self.assertRaisesRegex(ValueError, "Existing Golden missing"):
            self.collect_fixture({pipeline.key(capture): "added"})

    def test_missing_duplicate_and_unregistered_platform_results_fail(self):
        for mutation in (lambda results: results.pop(), lambda results: results.append(results[0].copy()), lambda results: results[0].update(golden_file_path="unregistered.png")):
            with self.subTest(mutation=mutation):
                shutil.rmtree(self.root / "feature", ignore_errors=True)
                shutil.rmtree(self.baseline, ignore_errors=True)
                shutil.rmtree(self.root / "report", ignore_errors=True)
                shutil.rmtree(self.root / "originals", ignore_errors=True)
                self.inputs()
                self.golden()
                self.collect_fixture()
                path = self.root / "feature/home/build/test-results/roborazzi/androidHostTest/results-summary.json"
                result = pipeline.read_json(path)
                mutation(result["results"])
                pipeline.write_json(path, result)
                shutil.rmtree(self.root / "report")
                shutil.rmtree(self.root / "originals")
                with self.assertRaises(ValueError):
                    pipeline.collect(self.root, "android", BASE, HEAD, MERGE, 195, self.baseline, self.root / "report", self.root / "originals")

    def test_report_identity_profile_language_and_path_tampering_fail(self):
        self.golden()
        summary = self.collect_fixture()
        path = self.root / "originals/summary.json"
        for field, value in (("head_sha", BASE), ("base_sha", HEAD), ("source_sha", HEAD), ("platform", "ios"), ("schema_version", 1), ("configuration_hash", "bad")):
            pipeline.write_json(path, {**summary, field: value})
            with self.subTest(field=field), self.assertRaises(ValueError):
                pipeline.validate_report(path.parent, self.context, "android", self.config)
        for name in ("home/../escape", "home/home.home.with_deck__fr__pixel10-pro", "home/home.home.with_deck__ko__iphone18-pro"):
            with self.subTest(name=name), self.assertRaises(ValueError):
                pipeline.parse_capture_key(name, self.config, "android")
        pipeline.write_json(path, summary)
        summary["captures"].pop()
        summary["total"] -= 1
        pipeline.write_json(path, summary)
        with self.assertRaisesRegex(ValueError, "Missing/duplicate language"):
            pipeline.validate_report(path.parent, self.context, "android", self.config)

    def test_platform_comments_before_after_added_removed_unchanged_and_legacy_preservation(self):
        self.golden()
        summary = self.collect_fixture()
        self.assertEqual("skip", pipeline.comment_plan(summary, [])["action"])
        legacy = "<!-- caro-screenshot-report -->\nlegacy report"
        self.assertEqual("skip", pipeline.comment_plan(summary, [{"id": 11, "body": legacy, "user": {"login": "github-actions[bot]"}}])["action"])
        self.assertEqual("update", pipeline.comment_plan(summary, [{"id": 12, "body": "<!-- caro-screenshot-report:android -->", "user": {"login": "github-actions[bot]"}}])["action"])
        self.assertEqual("skip", pipeline.comment_plan(summary, [{"id": 13, "body": "<!-- caro-screenshot-report:ios -->", "user": {"login": "github-actions[bot]"}}])["action"])
        self.assertIn("시각적 변경 없음", pipeline.comment_markdown(summary, "whatever-x/Caro-Mobile", ""))
        values = [pipeline.key(c) for c in summary["captures"]]
        summary.update(changed=[values[0]], added=[values[1]], removed=["home/home.removed__ko__pixel10-pro"])
        body = pipeline.comment_markdown(summary, "whatever-x/Caro-Mobile", IMAGE)
        self.assertIn("### Android 스크린샷", body)
        self.assertNotIn("iOS", body)
        self.assertNotIn("iphone18-pro", body)
        self.assertEqual(1, body.count("![before]"))
        self.assertEqual(2, body.count("![after]"))
        self.assertIn(f"/{IMAGE}/android/", body)
        self.assertIn("시나리오 제거 | — | —", body)
        self.assertEqual("create", pipeline.comment_plan(summary, [{"id": 11, "body": legacy, "user": {"login": "github-actions[bot]"}}])["action"])
        ios = copy.deepcopy(summary)
        ios["platform"] = "ios"
        for kind in ("changed", "added", "removed"):
            ios[kind] = [x.replace("pixel10-pro", "iphone18-pro") for x in ios[kind]]
        ios_body = pipeline.comment_markdown(ios, "whatever-x/Caro-Mobile", IMAGE)
        self.assertIn("### iOS 스크린샷", ios_body)
        self.assertNotIn("Android", ios_body)
        self.assertNotIn("pixel10-pro", ios_body)

    def test_stale_closed_wrong_base_and_fork_publication_guards(self):
        pr = {"number": 195, "state": "open", "base": {"ref": "develop", "sha": BASE}, "head": {"sha": HEAD, "repo": {"full_name": "whatever-x/Caro-Mobile"}}}
        self.assertTrue(pipeline.current_pr(self.context, pr, "whatever-x/Caro-Mobile"))
        for change in (lambda p: p.update(state="closed"), lambda p: p["head"].update(sha=MERGE), lambda p: p["base"].update(sha=HEAD), lambda p: p["base"].update(ref="main"), lambda p: p["head"]["repo"].update(full_name="fork/Caro-Mobile")):
            current = copy.deepcopy(pr)
            change(current)
            self.assertFalse(pipeline.current_pr(self.context, current, "whatever-x/Caro-Mobile"))

    def test_cli_publisher_validates_both_platforms_and_rejects_asymmetric_coverage(self):
        self.golden()
        android = self.collect_fixture()
        ios = copy.deepcopy(android)
        ios.update(platform="ios", profiles=self.config["profiles"]["ios"])
        for capture in ios["captures"]:
            capture["profile"] = capture["profile"].replace("pixel10-pro", "iphone18-pro")
            capture["filename"] = capture["filename"].replace("pixel10-pro", "iphone18-pro")
        pipeline.write_json(self.root / "build/screenshot-context/context.json", self.context)
        for platform, summary in (("android", android), ("ios", ios)):
            pipeline.write_json(self.root / f"build/screenshot-publish/{platform}/summary.json", summary)
        output = self.root / "outputs"
        command = [sys.executable, str(SCRIPT), "validate-publish", "--root", str(self.root), "--output", str(output)]
        result = subprocess.run(command, cwd=self.root, text=True, capture_output=True)
        self.assertEqual(0, result.returncode, result.stderr)
        self.assertIn("has-images=false", output.read_text())
        self.assertIn("android-has-changes=false", output.read_text())
        ios["captures"] = [c for c in ios["captures"] if c["module"] != "setting"]
        ios["total"] = len(ios["captures"])
        pipeline.write_json(self.root / "build/screenshot-publish/ios/summary.json", ios)
        result = subprocess.run(command, cwd=self.root, text=True, capture_output=True)
        self.assertNotEqual(0, result.returncode)
        self.assertIn("Platform coverage mismatch", result.stderr)

    def test_cli_production_entrypoint_creates_and_rejects_exact_identity(self):
        self.golden()
        result = subprocess.run([sys.executable, str(SCRIPT), "restore", "--root", str(self.root), "--baseline", str(self.baseline), "--platform", "android", "--base-sha", BASE], text=True, capture_output=True)
        self.assertEqual(0, result.returncode, result.stderr)
        result = subprocess.run([sys.executable, str(SCRIPT), "restore", "--root", str(self.root), "--baseline", str(self.baseline), "--platform", "android", "--base-sha", HEAD], text=True, capture_output=True)
        self.assertNotEqual(0, result.returncode)
        self.assertIn("Golden identity mismatch: source_sha", result.stderr)


if __name__ == "__main__":
    unittest.main()
