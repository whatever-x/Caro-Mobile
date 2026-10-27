#!/usr/bin/env python3
import json
import subprocess
import tempfile
from pathlib import Path

resolver = Path(__file__).with_name("resolve-scenarios.sh").resolve()
with tempfile.TemporaryDirectory() as directory:
    root = Path(directory)
    manifests = root / "scripts/screenshot"
    manifests.mkdir(parents=True)

    def write(common, android):
        (manifests / "scenarios.json").write_text(json.dumps(common))
        (manifests / "scenarios.android.json").write_text(json.dumps(android))

    def resolve(platform):
        return subprocess.run([resolver, platform], cwd=root, capture_output=True, text=True)

    write({"setting": ["shared"]}, {"setting": ["android"], "extra": ["one"]})
    assert json.loads(resolve("android").stdout) == {"setting": ["shared", "android"], "extra": ["one"]}
    assert json.loads(resolve("ios").stdout) == {"setting": ["shared"]}
    assert resolve("other").returncode != 0
    write({"setting": ["shared"]}, {})
    assert json.loads(resolve("android").stdout) == {"setting": ["shared"]}
    assert json.loads(resolve("ios").stdout) == {"setting": ["shared"]}
    (manifests / "scenarios.json").write_text('{"setting":["shared"]} {"setting":["ignored"]}')
    (manifests / "scenarios.android.json").write_text('{"setting":["android"]}')
    assert resolve("android").returncode != 0
    assert resolve("ios").returncode != 0
    write({"setting": ["shared"]}, {})
    (manifests / "scenarios.android.json").write_text('{} {}')
    assert resolve("android").returncode != 0
    assert resolve("ios").returncode != 0
    (manifests / "scenarios.android.json").write_text('')
    assert resolve("android").returncode != 0
    assert resolve("ios").returncode != 0
    write({"setting": ["shared"]}, {})
    (manifests / "scenarios.json").write_text('{"setting":["first"],"setting":["shared"]}')
    assert resolve("android").returncode != 0
    assert resolve("ios").returncode != 0
    write({"setting": ["shared"]}, {})
    (manifests / "scenarios.android.json").write_text('{"setting":["first"],"setting":["second"]}')
    assert resolve("android").returncode != 0
    assert resolve("ios").returncode != 0
    write({"setting": ["shared"]}, {"setting": ["shared"]})
    assert resolve("android").returncode != 0
    write({"setting": ["shared"]}, {"setting": ["bad_actual"]})
    assert resolve("android").returncode != 0
print("Scenario resolver checks passed")
