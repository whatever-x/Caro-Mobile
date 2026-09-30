#!/usr/bin/env python3
"""Compare complete Golden/current sets; image differences never fail the process."""

import argparse
import json
import re
import shutil
from pathlib import Path

from PIL import Image, ImageChops

BUILD_ROOT = (Path(__file__).resolve().parents[2] / "build").resolve()


def image_files(directory: Path) -> set[str]:
    return {str(path.relative_to(directory)) for path in directory.rglob("*.png")}


def scenario_files(directory: Path) -> set[str]:
    scenarios = json.loads((directory / "scenarios.json").read_text())
    if not isinstance(scenarios, dict) or not scenarios:
        raise ValueError(f"Invalid scenario list: {directory}")
    if any(
        not isinstance(module, str) or re.fullmatch(r"[A-Za-z0-9][A-Za-z0-9._-]*", module) is None
        or not isinstance(ids, list) or not ids
        or any(not isinstance(scenario, str) or re.fullmatch(r"[A-Za-z0-9][A-Za-z0-9._-]*", scenario) is None for scenario in ids)
        for module, ids in scenarios.items()
    ):
        raise ValueError(f"Invalid scenario list: {directory}")
    names = [f"{module}/{scenario}.png" for module, ids in scenarios.items() for scenario in ids]
    if not names or len(names) != len(set(names)):
        raise ValueError(f"Invalid scenario list: {directory}")
    return set(names)


def compare(platform: str, golden: Path, current: Path, report: Path) -> None:
    report = report.resolve()
    if report == BUILD_ROOT or not report.is_relative_to(BUILD_ROOT):
        raise ValueError(f"Report must be a directory below {BUILD_ROOT}")
    baseline = scenario_files(golden)
    expected = scenario_files(current)
    for name, directory, wanted in (("Golden", golden, baseline), ("current", current, expected)):
        actual = image_files(directory)
        if actual != wanted:
            raise RuntimeError(f"{name} {platform}: missing={sorted(wanted - actual)}, unexpected={sorted(actual - wanted)}")
    if report.exists():
        shutil.rmtree(report)
    report.mkdir(parents=True)
    changed = []
    added = []
    for relative in sorted(expected - baseline):
        stem = Path(relative).with_suffix("")
        target = report / stem.parent
        target.mkdir(parents=True, exist_ok=True)
        shutil.copyfile(current / relative, target / f"{stem.name}.after.png")
        added.append(str(stem))
    for relative in sorted(expected & baseline):
        with Image.open(golden / relative) as before_file, Image.open(current / relative) as after_file:
            before = before_file.convert("RGBA")
            after = after_file.convert("RGBA")
        if before.size == after.size and ImageChops.difference(before, after).getbbox(alpha_only=False) is None:
            continue
        stem = Path(relative).with_suffix("")
        target = report / stem.parent
        target.mkdir(parents=True, exist_ok=True)
        before.save(target / f"{stem.name}.before.png")
        after.save(target / f"{stem.name}.after.png")
        size = (max(before.width, after.width), max(before.height, after.height))
        before_canvas = Image.new("RGBA", size)
        after_canvas = Image.new("RGBA", size)
        before_canvas.paste(before, (0, 0))
        after_canvas.paste(after, (0, 0))
        ImageChops.difference(before_canvas, after_canvas).save(target / f"{stem.name}.diff.png")
        changed.append(str(stem))
    removed = [str(Path(name).with_suffix("")) for name in sorted(baseline - expected)]
    (report / "summary.json").write_text(json.dumps({"platform": platform, "total": len(expected), "changed": changed, "added": added, "removed": removed}, indent=2))
    print(f"{platform}: {len(changed)} changed, {len(added)} added, {len(removed)} removed / {len(expected)} current")


if __name__ == "__main__":
    parser = argparse.ArgumentParser()
    parser.add_argument("platform", choices=["android", "ios"])
    parser.add_argument("golden", type=Path)
    parser.add_argument("current", type=Path)
    parser.add_argument("report", type=Path)
    args = parser.parse_args()
    compare(args.platform, args.golden, args.current, args.report)
