#!/usr/bin/env python3
"""Re-point GitHub's NATIVE sub-issue links to match the corrected tier model.

GitHub's sub-issue graph is separate from the machine-block graph and was built
under the retired model, where a capstone could adopt a task directly
(`requires_tasks_exception`). Under capstone v4 there is no capstone->task edge
at all: a task's parent is a FEATURE. Graph check G10 reports the disagreement,
but it cannot fix it — the links live in GitHub, not in the body.

The native link is single-parent while ownership is now many-to-many, so this
only ever asserts ONE owner: the link is a convenience pointer, and each owning
feature's requires_tasks roster remains the authority.

Usage:
  scripts/repoint_subissues.py --snapshot DIR --map task=feature[,task=feature...]
      [--apply]

Exit: 0 ok / 1 nothing to do / 2 environment error.
"""
import argparse
import json
import os
import subprocess
import sys

REPO = "anadon/JLS"


def gh_json(args):
    r = subprocess.run(["gh"] + args, capture_output=True, text=True)
    if r.returncode:
        return None, r.stderr.strip()[:300]
    return (json.loads(r.stdout) if r.stdout.strip() else {}), None


def main():
    ap = argparse.ArgumentParser()
    ap.add_argument("--snapshot", required=True)
    ap.add_argument("--map", required=True,
                    help="comma-separated task=feature pairs")
    ap.add_argument("--repo", default=REPO)
    ap.add_argument("--apply", action="store_true")
    args = ap.parse_args()

    ids, parent = {}, {}
    with open(os.path.join(args.snapshot, "issues.jsonl")) as fh:
        for line in fh:
            d = json.loads(line)
            ids[d["number"]] = d.get("id")
    sp = os.path.join(args.snapshot, "subissues.jsonl")
    if os.path.exists(sp):
        with open(sp) as fh:
            for line in fh:
                d = json.loads(line)
                for c in d["children"]:
                    parent[c] = d["parent"]

    pairs = []
    for tok in args.map.split(","):
        t, f = tok.split("=")
        pairs.append((int(t), int(f)))

    todo = []
    for task, feat in pairs:
        cur = parent.get(task)
        if cur == feat:
            print(f"  #{task}: already parented to #{feat}")
            continue
        if ids.get(task) is None:
            print(f"  #{task}: no database id in snapshot — skipped")
            continue
        todo.append((task, feat, cur))

    if not todo:
        print("nothing to re-point")
        return 1
    print(f"re-point {len(todo)} native sub-issue link(s):")
    for task, feat, cur in todo:
        print(f"  #{task}: parent #{cur} (capstone) -> #{feat} (feature)")
    if not args.apply:
        print("\n(dry run — pass --apply)")
        return 0

    ok = 0
    for task, feat, cur in todo:
        sid = ids[task]
        if cur is not None:
            _, err = gh_json(["api", "-X", "DELETE",
                              f"repos/{args.repo}/issues/{cur}/sub_issue",
                              "-F", f"sub_issue_id={sid}"])
            if err:
                print(f"  #{task}: detach from #{cur} FAILED: {err}")
                continue
        _, err = gh_json(["api", "-X", "POST",
                          f"repos/{args.repo}/issues/{feat}/sub_issues",
                          "-F", f"sub_issue_id={sid}"])
        if err:
            print(f"  #{task}: attach to #{feat} FAILED: {err}")
            continue
        ok += 1
        print(f"  #{task}: now a sub-issue of #{feat}")
    print(f"\nre-pointed {ok}/{len(todo)}")
    return 0


if __name__ == "__main__":
    sys.exit(main())
