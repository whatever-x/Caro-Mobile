#!/usr/bin/env python3
"""Publish a same-repository PR's changed images and update one sticky comment."""

import json
import os
import re
import shutil
import subprocess
import sys
import tempfile
import urllib.parse
import urllib.request
from pathlib import Path

MARKER = "<!-- caro-screenshot-report -->"


def github(method: str, route: str, data=None):
    request = urllib.request.Request(
        f"https://api.github.com/repos/{os.environ['GITHUB_REPOSITORY']}/{route}",
        data=None if data is None else json.dumps(data).encode(),
        headers={
            "Accept": "application/vnd.github+json",
            "Authorization": f"Bearer {os.environ['GH_TOKEN']}",
            "X-GitHub-Api-Version": "2022-11-28",
            "Content-Type": "application/json",
        },
        method=method,
    )
    with urllib.request.urlopen(request) as response:
        return json.load(response)


def run(*args, cwd=None):
    subprocess.run(args, cwd=cwd, check=True)


def load_reports(root: Path):
    reports = {}
    for platform in ("android", "ios"):
        directory = root / platform
        summary = json.loads((directory / "summary.json").read_text())
        changed = summary.get("changed")
        added = summary.get("added")
        removed = summary.get("removed")
        if summary.get("platform") != platform or not isinstance(summary.get("total"), int) or summary["total"] <= 0 or any(not isinstance(items, list) for items in (changed, added, removed)):
            raise ValueError(f"Invalid {platform} summary")
        reported = changed + added + removed
        if len(reported) != len(set(reported)) or any(
            not isinstance(name, str) or re.fullmatch(r"[A-Za-z0-9][A-Za-z0-9._-]*/[A-Za-z0-9][A-Za-z0-9._-]*", name) is None
            for name in reported
        ) or len(changed) + len(added) > summary["total"]:
            raise ValueError(f"Invalid {platform} scenario IDs")
        wanted = {"summary.json"}
        for name in changed:
            for side in ("before", "after", "diff"):
                wanted.add(f"{name}.{side}.png")
        for name in added:
            wanted.add(f"{name}.after.png")
        actual = {str(path.relative_to(directory)) for path in directory.rglob("*") if path.is_file()}
        if actual != wanted:
            raise ValueError(f"Invalid {platform} report files: missing={wanted - actual}, extra={actual - wanted}")
        reports[platform] = {"total": summary["total"], "changed": sorted(changed), "added": sorted(added), "removed": sorted(removed)}
    return reports


def publish_branch(root: Path, reports, branch: str, base_sha: str, head_sha: str):
    repo = os.environ["GITHUB_REPOSITORY"]
    with tempfile.TemporaryDirectory(prefix="caro-screenshot-") as temporary:
        checkout = Path(temporary)
        run("git", "init", "--initial-branch", "screenshot-report", str(checkout))
        run("git", "-C", str(checkout), "config", "user.name", "github-actions[bot]")
        run("git", "-C", str(checkout), "config", "user.email", "41898282+github-actions[bot]@users.noreply.github.com")
        run("git", "-C", str(checkout), "remote", "add", "origin", f"https://github.com/{repo}.git")
        (checkout / "README.md").write_text(f"Screenshot report for PR #{os.environ['PR_NUMBER']}\n\nBase: {base_sha}\nHead: {head_sha}\n")
        for platform, report in reports.items():
            for name in report["changed"] + report["added"]:
                for side in (("before", "after", "diff") if name in report["changed"] else ("after",)):
                    relative = Path(platform) / f"{name}.{side}.png"
                    target = checkout / relative
                    target.parent.mkdir(parents=True, exist_ok=True)
                    shutil.copyfile(root / relative, target)
        run("git", "-C", str(checkout), "add", ".")
        run("git", "-C", str(checkout), "commit", "-m", f"chore: PR #{os.environ['PR_NUMBER']} screenshot report")
        run("gh", "auth", "setup-git")
        run("git", "-C", str(checkout), "push", "--force", "origin", f"HEAD:refs/heads/{branch}")


def make_comment(reports, branch: str, base_sha: str):
    repo = os.environ["GITHUB_REPOSITORY"]
    counts = " / ".join(f"{platform} {report['total']}" for platform, report in reports.items())
    lines = [MARKER, "### Screenshot Test", f"Golden: `develop` `{base_sha}` · {counts} scenarios"]
    if not any(report["changed"] or report["added"] or report["removed"] for report in reports.values()):
        lines.append("시각적 변경 없음")
    else:
        lines.append("이미지 차이는 report-only입니다. 변경된 화면만 표시합니다.")
        lines.append("| Platform / scenario | Before | After | Diff |")
        lines.append("| --- | --- | --- | --- |")
        for platform, report in reports.items():
            for name in report["changed"] + report["added"]:
                cells = [f"`{platform}/{name}`"]
                for side in ("before", "after", "diff"):
                    if name in report["added"] and side != "after":
                        cells.append("—")
                    else:
                        path = urllib.parse.quote(f"{platform}/{name}.{side}.png")
                        url = f"https://raw.githubusercontent.com/{repo}/{branch}/{path}"
                        cells.append(f"![{side}]({url})")
                lines.append("| " + " | ".join(cells) + " |")
            if report["removed"]:
                lines.append(f"{platform}에서 제거된 시나리오: " + ", ".join(f"`{name}`" for name in report["removed"]))
    return "\n\n".join(lines[:3]) + "\n\n" + "\n".join(lines[3:])


def update_comment(number: int, body: str):
    route = f"issues/{number}/comments"
    existing = None
    page = 1
    while True:
        comments = github("GET", f"{route}?per_page=100&page={page}")
        for comment in comments:
            if MARKER in comment.get("body", "") and comment.get("user", {}).get("type") == "Bot":
                existing = comment
                break
        if existing or len(comments) < 100:
            break
        page += 1
    if existing:
        github("PATCH", f"issues/comments/{existing['id']}", {"body": body})
    else:
        github("POST", route, {"body": body})


def main(root: Path):
    number = int(os.environ["PR_NUMBER"])
    base_sha = os.environ["BASE_SHA"]
    head_sha = os.environ["HEAD_SHA"]
    if number <= 0 or len(base_sha) != 40 or len(head_sha) != 40:
        raise ValueError("Invalid PR identity")
    pr = github("GET", f"pulls/{number}")
    if pr["head"]["sha"] != head_sha or pr["base"]["sha"] != base_sha:
        raise RuntimeError("PR changed while screenshots were running; rerun on the latest head/base")
    reports = load_reports(root)
    branch = f"screenshots/pr-{number}"
    publish_branch(root, reports, branch, base_sha, head_sha)
    update_comment(number, make_comment(reports, branch, base_sha))
    print(f"Updated PR #{number} screenshot report")


if __name__ == "__main__":
    main(Path(sys.argv[1]))
