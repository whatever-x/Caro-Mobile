#!/usr/bin/env python3
"""Copy the complete, explicitly named screenshot set into a CI artifact."""

import argparse
import json
import shutil
from pathlib import Path

ROOT = Path(__file__).resolve().parents[2]
SCENARIOS = json.loads((Path(__file__).with_name("scenarios.json")).read_text())


def collect(platform: str, destination: Path) -> None:
    if platform not in {"androidHostTest", "iosSimulatorArm64"}:
        raise ValueError(f"Unknown platform: {platform}")
    expected = sum(map(len, SCENARIOS.values()))
    if expected != 14:
        raise ValueError(f"Expected 14 scenarios, found {expected}")
    destination = destination.resolve()
    build_root = (ROOT / "build").resolve()
    if destination == build_root or not destination.is_relative_to(build_root):
        raise ValueError(f"Output must be a directory below {build_root}")
    if destination.exists():
        shutil.rmtree(destination)
    for module, ids in SCENARIOS.items():
        source_dir = ROOT / "feature" / module / "build/outputs/roborazzi" / platform
        actual = {path.name for path in source_dir.glob("*.png")}
        wanted = {f"{scenario}.png" for scenario in ids}
        if actual != wanted:
            raise RuntimeError(
                f"{platform}/{module}: missing={sorted(wanted - actual)}, unexpected={sorted(actual - wanted)}"
            )
        target_dir = destination / module
        target_dir.mkdir(parents=True, exist_ok=True)
        for name in sorted(wanted):
            shutil.copyfile(source_dir / name, target_dir / name)
    print(f"Collected {expected} {platform} screenshots in {destination}")


if __name__ == "__main__":
    parser = argparse.ArgumentParser()
    parser.add_argument("platform", choices=["androidHostTest", "iosSimulatorArm64"])
    parser.add_argument("destination", type=Path)
    args = parser.parse_args()
    collect(args.platform, args.destination)
