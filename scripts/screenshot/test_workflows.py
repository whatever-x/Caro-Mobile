"""Run workflow Bash against controlled GitHub responses: python3 this_file.py."""

import json
import os
from pathlib import Path
import re
import subprocess
import tempfile
import textwrap
import unittest

ROOT = Path(__file__).resolve().parents[2]
PR = ROOT / ".github/workflows/screenshot-pr.yml"
GOLDEN = ROOT / ".github/workflows/screenshot-golden.yml"


def run_block(path, name):
    text = path.read_text()
    step = next(s for s in text.split("      - ") if s.startswith("name: " + name))
    return textwrap.dedent(step.split("        run: |\n", 1)[1])


class WorkflowTests(unittest.TestCase):
    def run_download(self, states, download_error=False):
        # Only GitHub/network and elapsed time are substituted. Execute the real Bash.
        with tempfile.TemporaryDirectory() as directory:
            temp = Path(directory)
            (temp / "states.json").write_text(json.dumps(states))
            (temp / "gh").write_text("#!/usr/bin/env python3\n" + textwrap.dedent('''
                import json, os, sys
                from pathlib import Path
                root = Path(os.environ["FAKE_ROOT"])
                args = sys.argv[1:]
                with (root / "calls").open("a") as f:
                    f.write(json.dumps(args) + "\\n")
                if args[0] == "api":
                    states = json.loads((root / "states.json").read_text())
                    count = int((root / "count").read_text()) if (root / "count").exists() else 0
                    (root / "count").write_text(str(count + 1))
                    state = states[min(count, len(states) - 1)]
                    if state == "api-error":
                        print("gh: HTTP 403: Resource not accessible", file=sys.stderr)
                        sys.exit(1)
                    if state is not None:
                        run = {"id": 123, "head_sha": "base-sha", "status": state[0],
                               "conclusion": state[1], "html_url": "https://example.test/run/123"}
                        if any("status=success" in arg for arg in args):
                            if state[1] == "success":
                                print("base-sha\\t123")
                        else:
                            print(json.dumps(run))
                else:
                    sys.exit(int(os.environ["DOWNLOAD_ERROR"]))
                '''))
            (temp / "gh").chmod(0o755)
            env = dict(os.environ, PATH=f"{temp}:{os.environ['PATH']}",
                       FAKE_ROOT=str(temp), DOWNLOAD_ERROR=str(int(download_error)),
                       GITHUB_REPOSITORY="whatever-x/Caro-Mobile", BASE_SHA="base-sha", PLATFORM="ios")
            script = 'sleep() { SECONDS=$((SECONDS + 600)); }\n' + run_block(PR, "Download Golden")
            result = subprocess.run(["bash", "-euo", "pipefail", "-c", script],
                                    env=env, text=True, capture_output=True, timeout=5)
            calls = [json.loads(line) for line in (temp / "calls").read_text().splitlines()]
            return result, calls

    def test_running_golden_completes_before_download(self):
        for status in ("queued", "in_progress", "waiting", "pending"):
            with self.subTest(status=status):
                result, calls = self.run_download([(status, None), ("completed", "success")])
                self.assertEqual(result.returncode, 0, result.stdout + result.stderr)
                self.assertEqual(len([c for c in calls if c[0] == "api"]), 2)
                self.assertEqual(calls[-1][0:3], ["run", "download", "123"])
                self.assertIn("screenshot-golden-ios", calls[-1])
                self.assertTrue(all("head_sha=base-sha" in c[1] for c in calls if c[0] == "api"))

    def test_terminal_failure_never_downloads(self):
        for conclusion in ("failure", "cancelled", "timed_out", "skipped"):
            with self.subTest(conclusion=conclusion):
                result, calls = self.run_download([("completed", conclusion)])
                self.assertNotEqual(result.returncode, 0)
                self.assertIn(conclusion, result.stdout + result.stderr)
                self.assertFalse(any(c[0] == "run" for c in calls))

    def test_missing_run_and_api_failure_never_download(self):
        for state in (None, "api-error"):
            result, calls = self.run_download([state])
            self.assertNotEqual(result.returncode, 0)
            self.assertFalse(any(c[0] == "run" for c in calls))

    def test_api_failure_reports_base_sha(self):
        result, calls = self.run_download(["api-error"])
        self.assertNotEqual(result.returncode, 0)
        self.assertIn("::error::Golden lookup failed for base SHA base-sha", result.stdout + result.stderr)
        self.assertIn("gh: HTTP 403: Resource not accessible", result.stderr)
        self.assertFalse(any(c[0] == "run" for c in calls))

    def test_wait_has_deadline(self):
        result, calls = self.run_download([("in_progress", None)])
        self.assertNotEqual(result.returncode, 0)
        self.assertIn("10 minutes", result.stdout + result.stderr)
        self.assertFalse(any(c[0] == "run" for c in calls))

    def test_artifact_failure_fails_workflow(self):
        result, calls = self.run_download([("completed", "success")], download_error=True)
        self.assertNotEqual(result.returncode, 0)
        self.assertIn("artifact", result.stdout + result.stderr)
        self.assertEqual(calls[-1][0], "run")

    def test_version_only_changes_reuse_dependency_hash(self):
        catalog = (ROOT / "gradle/libs.versions.toml").read_text()
        changed = re.sub(r'^version-(code|name) = ".*?"', r'version-\1 = "999"', catalog, flags=re.M)
        kotlin_changed = re.sub(r'^kotlin = ".*?"', 'kotlin = "99.0.0"', catalog, flags=re.M)
        hashes = []
        for path in (PR, GOLDEN):
            commands = [line.strip() for line in path.read_text().splitlines()
                        if line.strip().startswith("dependency_hash=")]
            self.assertTrue(commands, "The workflow must compute a version-independent dependency hash")
            command = commands[0]
            with tempfile.TemporaryDirectory() as directory:
                temp = Path(directory)
                (temp / "gradle").mkdir()
                values = []
                for contents in (catalog, changed, kotlin_changed):
                    (temp / "gradle/libs.versions.toml").write_text(contents)
                    result = subprocess.run(["bash", "-euo", "pipefail", "-c", command + '\n echo "$dependency_hash"'],
                                            cwd=temp, text=True, capture_output=True, check=True)
                    values.append(result.stdout.strip())
                self.assertEqual(values[0], values[1])
                self.assertNotEqual(values[0], values[2])
                hashes.append(values[0])
        self.assertEqual(hashes[0], hashes[1])


if __name__ == "__main__":
    unittest.main()
