import json
import tempfile
import unittest
from unittest.mock import patch
from pathlib import Path

from PIL import Image

from compare import compare
from publish import load_reports, make_comment

BUILD = Path(__file__).resolve().parents[2] / "build"


def write_set(directory, scenarios, color=(255, 255, 255, 255)):
    directory.mkdir(parents=True)
    (directory / "scenarios.json").write_text(json.dumps(scenarios))
    for module, ids in scenarios.items():
        for scenario in ids:
            target = directory / module / f"{scenario}.png"
            target.parent.mkdir(parents=True, exist_ok=True)
            Image.new("RGBA", (2, 2), color).save(target)


class CompareTest(unittest.TestCase):
    def test_changed_added_and_removed_scenarios(self):
        BUILD.mkdir(exist_ok=True)
        with tempfile.TemporaryDirectory(dir=BUILD) as temporary:
            root = Path(temporary)
            golden, current, report = (root / name for name in ("golden", "current", "report"))
            write_set(golden, {"splash": ["existing", "removed"]})
            write_set(current, {"splash": ["existing", "added"]})
            Image.new("RGBA", (2, 2), (255, 0, 0, 255)).save(current / "splash/existing.png")
            compare("android", golden, current, report)
            summary = json.loads((report / "summary.json").read_text())
            self.assertEqual(summary, {
                "platform": "android", "total": 2,
                "changed": ["splash/existing"], "added": ["splash/added"], "removed": ["splash/removed"],
            })
            self.assertTrue((report / "splash/existing.diff.png").is_file())
            self.assertTrue((report / "splash/added.after.png").is_file())
            self.assertFalse((report / "splash/added.before.png").exists())
            ios = root / "ios"
            write_set(ios, {"splash": ["existing"]})
            compare("ios", ios, ios, root / "ios-report")
            publish = root / "publish"
            (publish / "android").mkdir(parents=True)
            (publish / "ios").mkdir(parents=True)
            for source, target in ((report, publish / "android"), (root / "ios-report", publish / "ios")):
                for file in source.rglob("*"):
                    if file.is_file():
                        destination = target / file.relative_to(source)
                        destination.parent.mkdir(parents=True, exist_ok=True)
                        destination.write_bytes(file.read_bytes())
            reports = load_reports(publish)
            with patch.dict("os.environ", {"GITHUB_REPOSITORY": "whatever-x/Caro-Mobile"}):
                comment = make_comment(reports, "screenshots/pr-1", "a" * 40)
            self.assertIn("splash/added", comment)
            self.assertIn("splash/removed", comment)
            self.assertIn("raw.githubusercontent.com", comment)
            (publish / "android/splash/added.after.png").unlink()
            with self.assertRaisesRegex(ValueError, "Invalid android report files"):
                load_reports(publish)

    def test_missing_golden_image_fails(self):
        BUILD.mkdir(exist_ok=True)
        with tempfile.TemporaryDirectory(dir=BUILD) as temporary:
            root = Path(temporary)
            golden, current = root / "golden", root / "current"
            write_set(golden, {"splash": ["existing"]})
            write_set(current, {"splash": ["existing"]})
            (golden / "splash/existing.png").unlink()
            with self.assertRaisesRegex(RuntimeError, "missing"):
                compare("android", golden, current, root / "report")


if __name__ == "__main__":
    unittest.main()
