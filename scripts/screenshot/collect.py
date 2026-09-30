#!/usr/bin/env python3
"""등록된 스크린샷을 검증하고 CI 아티팩트로 수집한다."""

import argparse
import json
import shutil
from pathlib import Path

ROOT = Path(__file__).resolve().parents[2]
SCENARIOS_PATH = Path(__file__).with_name("scenarios.json")
SCENARIOS = json.loads(SCENARIOS_PATH.read_text())


def collect(platform: str, destination: Path) -> None:
    if platform not in {"androidHostTest", "iosSimulatorArm64"}:
        raise ValueError(f"지원하지 않는 테스트 대상: {platform}")
    expected = sum(map(len, SCENARIOS.values()))
    if expected == 0:
        raise ValueError("등록된 스크린샷 시나리오가 없습니다")
    destination = destination.resolve()
    build_root = (ROOT / "build").resolve()
    if destination == build_root or not destination.is_relative_to(build_root):
        raise ValueError(f"출력 경로는 {build_root} 하위여야 합니다: {destination}")
    if destination.exists():
        shutil.rmtree(destination)
    for module, ids in SCENARIOS.items():
        source_dir = ROOT / "feature" / module / "build/outputs/roborazzi" / platform
        actual = {path.name for path in source_dir.glob("*.png")}
        wanted = {f"{scenario}.png" for scenario in ids}
        if actual != wanted:
            raise RuntimeError(
                f"{platform}/{module}: 누락={sorted(wanted - actual)}, 등록되지 않은 이미지={sorted(actual - wanted)}"
            )
        target_dir = destination / module
        target_dir.mkdir(parents=True, exist_ok=True)
        for name in sorted(wanted):
            shutil.copyfile(source_dir / name, target_dir / name)
    shutil.copyfile(SCENARIOS_PATH, destination / "scenarios.json")
    print(f"{platform} 스크린샷 {expected}개 수집 완료: {destination}")


if __name__ == "__main__":
    parser = argparse.ArgumentParser()
    parser.add_argument("platform", choices=["androidHostTest", "iosSimulatorArm64"])
    parser.add_argument("destination", type=Path)
    args = parser.parse_args()
    collect(args.platform, args.destination)
