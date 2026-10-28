#!/usr/bin/env python3
"""Screenshot identity, PNG, Golden and publication contract (stdlib only)."""
import argparse
import hashlib
import json
import math
from pathlib import Path
import re
import shutil
import struct
import sys
import zlib

SCHEMA = 2
TARGETS = {"android": "androidHostTest", "ios": "iosSimulatorArm64"}
NAME = re.compile(r"[A-Za-z0-9][A-Za-z0-9._-]*\Z")
SHA = re.compile(r"[a-f0-9]{40}\Z")


def require(condition, message):
    if not condition:
        raise ValueError(message)


def read_json(path):
    require(path.is_file() and not path.is_symlink(), f"Missing or unsafe JSON: {path}")
    def unique(pairs):
        result = {}
        for key, value in pairs:
            require(key not in result, f"Duplicate JSON key: {key} in {path}")
            result[key] = value
        return result
    return json.loads(path.read_text(), object_pairs_hook=unique)


def write_json(path, value):
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_text(json.dumps(value, ensure_ascii=False, indent=2) + "\n")


def safe_name(value):
    return isinstance(value, str) and NAME.fullmatch(value) and "__" not in value and not value.endswith(("_actual", "_compare", "_before"))


def registry(value):
    require(isinstance(value, dict) and value, "Scenario registry must be a nonempty module -> IDs object")
    seen = set()
    for module, ids in value.items():
        require(safe_name(module), f"Invalid module: {module}")
        require(isinstance(ids, list) and ids, f"No scenario IDs for {module}")
        for scenario in ids:
            require(safe_name(scenario), f"Invalid scenario ID: {scenario}")
            require(scenario not in seen, f"Duplicate scenario ID: {scenario}")
            seen.add(scenario)
    return value


def configuration(value):
    require(isinstance(value, dict) and value.get("schemaVersion") == SCHEMA, "Incompatible capture schema; multilingual v2 Golden is required")
    require(value.get("languages") == ["ko", "en"], "Capture languages must be exactly ko, en")
    require(isinstance(value.get("profiles"), dict) and set(value["profiles"]) == set(TARGETS), "Both platform profiles are required")
    all_ids = set()
    for platform, profiles in value["profiles"].items():
        require(isinstance(profiles, dict) and set(profiles) == {"default", "small-large-font"}, f"Invalid profile variants: {platform}")
        for variant, profile in profiles.items():
            require(isinstance(profile, dict) and set(profile) == {"id", "widthPx", "heightPx", "density", "fontScale"}, "Malformed profile")
            require(safe_name(profile["id"]) and profile["id"] not in all_ids, "Invalid or duplicate profile ID")
            all_ids.add(profile["id"])
            for key in ("widthPx", "heightPx"):
                require(type(profile[key]) is int and 0 < profile[key] <= 8192, f"Invalid {key}")
            for key in ("density", "fontScale"):
                require(type(profile[key]) in (int, float) and math.isfinite(profile[key]) and 0 < profile[key] <= (8 if key == "density" else 4), f"Invalid {key}")
            if variant == "small-large-font":
                require(profile["widthPx"] / profile["density"] == 320 and profile["heightPx"] / profile["density"] == 480 and profile["fontScale"] == 1.5, "Small large-font contract is 320x480 logical units, fontScale 1.5")
    variants = value.get("scenarioVariants")
    require(isinstance(variants, dict) and all(safe_name(k) and v == "small-large-font" for k, v in variants.items()), "Malformed scenario variants")
    content = value.get("contentSizedScenarios")
    require(isinstance(content, list) and len(content) == len(set(content)) and all(safe_name(k) for k in content), "Malformed content-sized scenarios")
    return value


def config_hash(config):
    return hashlib.sha256(json.dumps(config, sort_keys=True, separators=(",", ":")).encode()).hexdigest()


def expand(scenarios, config, platform):
    registry(scenarios)
    configuration(config)
    require(platform in TARGETS, "Unknown platform")
    captures = []
    for module, ids in sorted(scenarios.items()):
        for scenario in ids:
            variant = config["scenarioVariants"].get(scenario, "default")
            profile = config["profiles"][platform][variant]
            for language in config["languages"]:
                captures.append({
                    "module": module, "scenario": scenario, "language": language, "profile": profile["id"],
                    "variant": variant, "filename": f"{scenario}__{language}__{profile['id']}.png",
                    "widthPx": profile["widthPx"], "heightPx": profile["heightPx"],
                    "density": profile["density"], "fontScale": profile["fontScale"],
                    "rootBounds": "content" if scenario in config["contentSizedScenarios"] else "canvas",
                })
    require(len({key(c) for c in captures}) == len(captures), "Duplicate capture output")
    return captures


def key(capture):
    return capture["module"] + "/" + capture["filename"][:-4]


def tree_files(root):
    require(root.is_dir() and not root.is_symlink(), f"Missing or unsafe directory: {root}")
    for path in root.rglob("*"):
        require(not path.is_symlink(), f"Symlink rejected: {path}")
        require(path.is_file() or path.is_dir(), f"Unsupported file type: {path}")
    return {str(p.relative_to(root)) for p in root.rglob("*") if p.is_file()}


def png_size(path):
    require(path.is_file() and not path.is_symlink(), f"Missing or unsafe PNG: {path}")
    require(path.stat().st_size <= 64 * 1024 * 1024, f"Oversized PNG: {path}")
    data = path.read_bytes()
    require(data[:8] == b"\x89PNG\r\n\x1a\n", f"Invalid PNG signature: {path}")
    offset, size, image_format, compressed, ended = 8, None, None, [], False
    while offset < len(data):
        require(offset + 12 <= len(data), f"Truncated PNG: {path}")
        length = struct.unpack(">I", data[offset:offset + 4])[0]
        chunk = data[offset + 4:offset + 8]
        payload = data[offset + 8:offset + 8 + length]
        require(len(payload) == length and offset + 12 + length <= len(data), f"Truncated PNG chunk: {path}")
        crc = struct.unpack(">I", data[offset + 8 + length:offset + 12 + length])[0]
        require(zlib.crc32(chunk + payload) & 0xFFFFFFFF == crc, f"PNG CRC mismatch: {path}")
        if size is None:
            require(chunk == b"IHDR" and length == 13, f"Missing PNG IHDR: {path}")
            size = struct.unpack(">II", payload[:8])
            require(all(0 < n <= 8192 for n in size), f"Invalid PNG dimensions: {path}")
            depth, color, compression, filtering, interlace = payload[8:]
            require(color in (0, 2, 3, 4, 6) and depth in (1, 2, 4, 8, 16) and compression == filtering == interlace == 0, f"Unsupported PNG format: {path}")
            image_format = (depth, {0: 1, 2: 3, 3: 1, 4: 2, 6: 4}[color])
        if chunk == b"IDAT":
            compressed.append(payload)
        offset += length + 12
        if chunk == b"IEND":
            require(length == 0 and offset == len(data), f"Invalid PNG end: {path}")
            ended = True
            break
    require(ended and compressed, f"Incomplete PNG: {path}")
    row_size = (size[0] * image_format[0] * image_format[1] + 7) // 8 + 1
    expected_length = row_size * size[1]
    decoder = zlib.decompressobj()
    decoded = decoder.decompress(b"".join(compressed), expected_length + 1)
    require(decoder.eof and not decoder.unused_data and len(decoded) == expected_length, f"Invalid PNG pixels: {path}")
    require(all(decoded[offset] <= 4 for offset in range(0, len(decoded), row_size)), f"Invalid PNG filter: {path}")
    return list(size)


def capture_size(path, capture):
    size = png_size(path)
    expected = [capture["widthPx"], capture["heightPx"]]
    if capture["rootBounds"] == "canvas":
        require(size == expected, f"PNG geometry mismatch: {path}: {size} != {expected}")
    else:
        require(all(0 < actual <= limit for actual, limit in zip(size, expected)), f"Content root exceeds canvas: {path}")
    return size


def identity(config, platform, source_sha):
    require(isinstance(source_sha, str) and SHA.fullmatch(source_sha), "Invalid source SHA")
    return {"schema_version": SCHEMA, "configuration_hash": config_hash(config), "platform": platform,
            "source_sha": source_sha, "languages": config["languages"],
            "profiles": config["profiles"][platform]}


def load_inputs(root):
    return registry(read_json(root / "scripts/screenshot/scenarios.json")), configuration(read_json(root / "scripts/screenshot/capture-config.json"))


def record_golden(root, platform, source_sha, destination):
    scenarios, config = load_inputs(root)
    captures = expand(scenarios, config, platform)
    require(not destination.exists(), f"Golden destination already exists: {destination}")
    for module in scenarios:
        source = root / f"feature/{module}/build/outputs/roborazzi/{TARGETS[platform]}"
        expected = {c["filename"] for c in captures if c["module"] == module}
        require(tree_files(source) == expected, f"Registry/output mismatch in {module}/{platform}; missing or unregistered captures")
    for capture in captures:
        source = root / f"feature/{capture['module']}/build/outputs/roborazzi/{TARGETS[platform]}/{capture['filename']}"
        capture["pngSize"] = capture_size(source, capture)
        target = destination / capture["module"] / capture["filename"]
        target.parent.mkdir(parents=True, exist_ok=True)
        shutil.copyfile(source, target)
    write_json(destination / "scenarios.json", scenarios)
    write_json(destination / "capture-config.json", config)
    write_json(destination / "metadata.json", {**identity(config, platform, source_sha), "captures": captures})


def validate_golden(baseline, current_config, platform, base_sha):
    require((baseline / "metadata.json").is_file(), "Legacy/missing Golden metadata: multilingual v2 migration requires a new Golden at the exact base SHA; another SHA or PR output cannot substitute")
    config = configuration(read_json(baseline / "capture-config.json"))
    require(config_hash(config) == config_hash(current_config), "Incompatible Golden configuration; regenerate only at the exact merged base SHA")
    scenarios = registry(read_json(baseline / "scenarios.json"))
    captures = expand(scenarios, config, platform)
    metadata = read_json(baseline / "metadata.json")
    for name, expected in identity(config, platform, base_sha).items():
        require(metadata.get(name) == expected, f"Golden identity mismatch: {name}")
    require(tree_files(baseline) == {"metadata.json", "scenarios.json", "capture-config.json"} | {c["module"] + "/" + c["filename"] for c in captures}, "Golden files do not match registry")
    measured = []
    for capture in captures:
        measured.append({**capture, "pngSize": capture_size(baseline / capture["module"] / capture["filename"], capture)})
    require(metadata.get("captures") == measured, "Golden capture metadata/PNG mismatch")
    return measured


def validate_golden_run(run, artifacts, base_sha, platform):
    require(run.get("head_sha") == base_sha and run.get("head_branch") == "develop" and run.get("event") == "push", "Wrong-SHA/branch/event Golden run")
    require(run.get("status") == "completed" and run.get("conclusion") == "success", "Golden run did not succeed")
    matches = [a for a in artifacts.get("artifacts", []) if a.get("name") == f"screenshot-golden-{platform}"]
    require(len(matches) == 1 and matches[0].get("expired") is False, "Golden artifact is missing, expired, or ambiguous")
    return matches[0]


def restore_golden(root, platform, base_sha, baseline):
    _, config = load_inputs(root)
    captures = validate_golden(baseline, config, platform, base_sha)
    for capture in captures:
        # Removed modules need no build directory. Their captures remain in baseline for reporting.
        if not (root / f"feature/{capture['module']}/build.gradle.kts").is_file():
            continue
        target = root / f"feature/{capture['module']}/build/outputs/roborazzi/{TARGETS[platform]}/{capture['filename']}"
        target.parent.mkdir(parents=True, exist_ok=True)
        shutil.copyfile(baseline / capture["module"] / capture["filename"], target)


def collect(root, platform, base_sha, head_sha, source_sha, number, baseline, report, originals):
    scenarios, config = load_inputs(root)
    before = {key(c): c for c in validate_golden(baseline, config, platform, base_sha)}
    captures = expand(scenarios, config, platform)
    require(not report.exists() and not originals.exists(), "Report destination already exists")
    groups = {"changed": [], "added": [], "removed": sorted(set(before) - {key(c) for c in captures})}
    for module in scenarios:
        source = root / f"feature/{module}/build/outputs/roborazzi/{TARGETS[platform]}"
        tree_files(source)
        results = read_json(root / f"feature/{module}/build/test-results/roborazzi/{TARGETS[platform]}/results-summary.json")
        results = results.get("results")
        require(isinstance(results, list), f"Malformed Roborazzi results: {module}")
        by_name = {}
        for result in results:
            require(isinstance(result, dict) and isinstance(result.get("golden_file_path"), str), "Malformed result path")
            name = result["golden_file_path"].replace("\\", "/").split("/")[-1]
            require(name not in by_name, f"Duplicate result: {name}")
            require(result.get("type") in ("changed", "added", "unchanged"), "Unexpected Roborazzi result type")
            by_name[name] = result["type"]
        expected = [c for c in captures if c["module"] == module]
        require(set(by_name) == {c["filename"] for c in expected}, f"Registry/result mismatch: {module}/{platform}")
        for capture in expected:
            capture_key, stem = key(capture), capture["filename"][:-4]
            kind = by_name[capture["filename"]]
            require((kind == "added") == (capture_key not in before), f"Existing Golden missing or new scenario not added: {capture_key}")
            if kind == "unchanged":
                capture["pngSize"] = capture_size(source / capture["filename"], capture)
                continue
            groups[kind].append(capture_key)
            actual = source / (stem + "_actual.png")
            capture["pngSize"] = capture_size(actual, capture)
            target = originals / module
            target.mkdir(parents=True, exist_ok=True)
            shutil.copyfile(actual, target / actual.name)
            if kind == "changed":
                shutil.copyfile(baseline / module / capture["filename"], target / (stem + "_before.png"))
            diagnostic = source / (stem + ("_compare.png" if kind == "changed" else "_actual.png"))
            png_size(diagnostic)
            (report / module).mkdir(parents=True, exist_ok=True)
            shutil.copyfile(diagnostic, report / module / diagnostic.name)
    require(type(number) is int and number > 0 and SHA.fullmatch(head_sha), "Invalid PR identity")
    summary = {**identity(config, platform, source_sha), "pr_number": number, "base_sha": base_sha, "head_sha": head_sha,
               "total": len(captures), "captures": captures, **groups}
    write_json(report / "summary.json", summary)
    write_json(originals / "summary.json", summary)
    return summary


def parse_capture_key(value, config, platform):
    require(isinstance(value, str) and value.count("/") == 1, "Unsafe capture path")
    module, stem = value.split("/")
    parts = stem.split("__")
    require(safe_name(module) and len(parts) == 3 and safe_name(parts[0]), "Malformed capture identity")
    scenario, language, profile = parts
    variant = config["scenarioVariants"].get(scenario, "default")
    require(language in config["languages"] and profile == config["profiles"][platform][variant]["id"], "Wrong language/profile in capture identity")
    return module, scenario, language, profile


def validate_report(directory, context, platform, config):
    summary = read_json(directory / "summary.json")
    for field in ("pr_number", "base_sha", "head_sha"):
        require(summary.get(field) == context.get(field), f"Report/context mismatch: {field}")
    require(summary.get("source_sha") == context.get("merge_sha"), "Report did not capture the pinned merge SHA")
    for name, expected in identity(config, platform, context["merge_sha"]).items():
        require(summary.get(name) == expected, f"Report identity mismatch: {name}")
    require(type(summary.get("total")) is int and summary["total"] > 0, "Invalid total")
    captures = summary.get("captures")
    require(isinstance(captures, list) and len(captures) == summary["total"], "Missing capture metadata")
    scenarios = {}
    for capture in captures:
        require(isinstance(capture, dict), "Malformed capture metadata")
        value = key(capture)
        module, scenario, _, _ = parse_capture_key(value, config, platform)
        scenarios.setdefault(module, set()).add(scenario)
    expected = expand({m: sorted(ids) for m, ids in scenarios.items()}, config, platform)
    expected_map = {key(c): c for c in expected}
    require(len(expected_map) == len(captures), "Missing/duplicate language capture")
    capture_map = {}
    for capture in captures:
        value = key(capture)
        require(value not in capture_map and value in expected_map, "Duplicate/unexpected capture")
        require({k: v for k, v in capture.items() if k != "pngSize"} == expected_map[value], "Capture metadata mismatch")
        size = capture.get("pngSize")
        require(isinstance(size, list) and len(size) == 2 and all(type(n) is int and n > 0 for n in size), "Missing measured PNG geometry")
        if capture["rootBounds"] == "canvas":
            require(size == [capture["widthPx"], capture["heightPx"]], "Reported PNG geometry mismatch")
        else:
            require(size[0] <= capture["widthPx"] and size[1] <= capture["heightPx"], "Reported content bounds exceed canvas")
        capture_map[value] = capture
    seen, files = set(), {"summary.json"}
    for kind in ("changed", "added", "removed"):
        values = summary.get(kind)
        require(isinstance(values, list), f"Invalid {kind} list")
        for value in values:
            parse_capture_key(value, config, platform)
            require(value not in seen, "Duplicate/overlapping change categories")
            seen.add(value)
            if kind == "removed":
                require(value not in capture_map, "Removed capture still exists")
                continue
            require(value in capture_map, "Change missing from current captures")
            suffixes = ["_actual.png", "_before.png"] if kind == "changed" else ["_actual.png"]
            for suffix in suffixes:
                name = value + suffix
                files.add(name)
                size = capture_size(directory / name, capture_map[value])
                if suffix == "_actual.png":
                    require(size == capture_map[value]["pngSize"], "Published PNG does not match measured geometry")
    require(tree_files(directory) == files, "Publication file allowlist mismatch")
    return summary


def current_pr(context, pr, repository):
    return (pr.get("number") == context.get("pr_number") and pr.get("state") == "open"
            and pr.get("base", {}).get("ref") == "develop"
            and pr.get("base", {}).get("sha") == context.get("base_sha")
            and pr.get("head", {}).get("sha") == context.get("head_sha")
            and (pr.get("head", {}).get("repo") or {}).get("full_name") == repository)


def marker(platform):
    require(platform in TARGETS, "Unknown comment platform")
    return f"<!-- caro-screenshot-report:{platform} -->"


def comment_plan(summary, comments):
    platform_marker = marker(summary["platform"])
    matches = [c for c in comments if c.get("user", {}).get("login") == "github-actions[bot]"
               and isinstance(c.get("body"), str) and platform_marker in c["body"]]
    require(len(matches) <= 1, "Multiple platform screenshot comments; resolve duplicates before publishing")
    existing = matches[0].get("id") if matches else None
    require(existing is None or type(existing) is int and existing > 0, "Invalid existing comment ID")
    action = "update" if existing else "create" if any(summary[k] for k in ("changed", "added", "removed")) else "skip"
    return {"comment-id": existing or "", "action": action}


def comment_markdown(summary, repository, image_sha):
    platform = summary["platform"]
    require(re.fullmatch(r"[A-Za-z0-9_.-]+/[A-Za-z0-9_.-]+", repository), "Invalid repository")
    require(not (summary["changed"] or summary["added"]) or SHA.fullmatch(image_sha or ""), "Immutable image commit SHA is required")
    title = "Android 스크린샷" if platform == "android" else "iOS 스크린샷"
    lines = [marker(platform), f"### {title}", "", f"Golden: `develop` `{summary['base_sha']}` · 이미지 차이는 report-only입니다.", "", "| Scenario / language / profile | Result | Before | After |", "| --- | --- | --- | --- |"]
    for kind, label in (("changed", "변경"), ("added", "신규 · After"), ("removed", "시나리오 제거")):
        for value in summary[kind]:
            _, stem = value.split("/")
            scenario, language, profile = stem.split("__")
            url = f"https://raw.githubusercontent.com/{repository}/{image_sha}/{platform}/{value}"
            before = f"![before]({url}_before.png)" if kind == "changed" else "—"
            after = f"![after]({url}_actual.png)" if kind != "removed" else "—"
            lines.append(f"| `{scenario}` / `{language}` / `{profile}` | {label} | {before} | {after} |")
    if not any(summary[k] for k in ("changed", "added", "removed")):
        lines.extend(["", "시각적 변경 없음"])
    return "\n".join(lines) + "\n"


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("command", choices=("golden", "restore", "collect", "validate-run", "validate-publish", "comment", "comment-plan", "current", "expand"))
    parser.add_argument("--root", type=Path, default=Path("."))
    parser.add_argument("--platform", choices=tuple(TARGETS))
    parser.add_argument("--source-sha")
    parser.add_argument("--base-sha")
    parser.add_argument("--head-sha")
    parser.add_argument("--pr-number", type=int)
    parser.add_argument("--baseline", type=Path, default=Path("build/screenshot-baseline"))
    parser.add_argument("--destination", type=Path, default=Path("build/screenshot-golden"))
    parser.add_argument("--context", type=Path, default=Path("build/screenshot-context/context.json"))
    parser.add_argument("--run", type=Path)
    parser.add_argument("--artifacts", type=Path)
    parser.add_argument("--summary", type=Path)
    parser.add_argument("--repository")
    parser.add_argument("--comments", type=Path)
    parser.add_argument("--image-sha", default="")
    parser.add_argument("--output", type=Path)
    args = parser.parse_args()
    if args.command == "golden":
        record_golden(args.root, args.platform, args.source_sha, args.destination)
    elif args.command == "restore":
        restore_golden(args.root, args.platform, args.base_sha, args.baseline)
    elif args.command == "collect":
        collect(args.root, args.platform, args.base_sha, args.head_sha, args.source_sha, args.pr_number, args.baseline,
                Path("build/screenshot-report"), Path("build/screenshot-before-after"))
    elif args.command == "validate-run":
        validate_golden_run(read_json(args.run), read_json(args.artifacts), args.base_sha, args.platform)
    elif args.command == "validate-publish":
        _, config = load_inputs(args.root)
        context = read_json(args.context)
        summaries = [validate_report(Path(f"build/screenshot-publish/{p}"), context, p, config) for p in TARGETS]
        # Common UI must cover the same logical scenario/language pairs on both platforms.
        require({(c["module"], c["scenario"], c["language"]) for c in summaries[0]["captures"]} ==
                {(c["module"], c["scenario"], c["language"]) for c in summaries[1]["captures"]}, "Platform coverage mismatch")
        values = {"number": context["pr_number"], "base": context["base_sha"],
                  "has-images": str(any(s["changed"] or s["added"] for s in summaries)).lower()}
        for summary in summaries:
            values[f"{summary['platform']}-has-changes"] = str(any(summary[k] for k in ("changed", "added", "removed"))).lower()
        args.output.write_text("".join(f"{k}={v}\n" for k, v in values.items()))
    elif args.command == "comment":
        args.output.write_text(comment_markdown(read_json(args.summary), args.repository, args.image_sha))
    elif args.command == "comment-plan":
        pages = read_json(args.comments)
        require(isinstance(pages, list) and all(isinstance(page, list) for page in pages), "Invalid comments response")
        values = comment_plan(read_json(args.summary), [comment for page in pages for comment in page])
        args.output.write_text("".join(f"{k}={v}\n" for k, v in values.items()))
    elif args.command == "current":
        print(str(current_pr(read_json(args.context), read_json(args.run), args.repository)).lower())
    else:
        scenarios, config = load_inputs(args.root)
        print(json.dumps(expand(scenarios, config, args.platform), indent=2))


if __name__ == "__main__":
    try:
        main()
    except (ValueError, KeyError, TypeError, OSError, struct.error, zlib.error) as error:
        print(f"::error::{error}", file=sys.stderr)
        sys.exit(1)
