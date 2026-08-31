#!/usr/bin/env python3
"""Derive each open issue's board Status from the corpus and (optionally) set it.

WHY THIS EXISTS. The end state for this repo is that a workflow can pick up
issues whose board Status is "Ready" and execute them unattended. Before this
script there was no such contract: Status had no written meaning (see
validate_board.py's B07 marker), nothing derived it, and NOTHING IN THE REPO
COULD SET IT -- `gh project item-edit` appears nowhere. Status was whatever a
human last clicked. This script makes Status a function of the corpus instead
of an opinion, so "Ready" means the same thing every time it is read.

THE RULE (docs/board-status-ruling.md is the normative statement):

  Blocked  if any of:
             - a `blocked_by` entry is still open
             - the issue sits in an ordering/composition cycle (graph G04)
             - it carries an error-severity finding from the graph, hygiene or
               template validators, or the `needs-template-fix` label
             - feature/capstone only: `planned_tasks` / `planned_features` is
               non-empty, so its own sufficiency argument is provisional and
               its children are not all filed yet
  Ready    otherwise.

The last two clauses are the ones `blocked_by` alone cannot express, and they
are the reason readiness has to be derived rather than hand-set.

NOT DERIVED. "In Progress" and "Done" are human/workflow signals about work
that is under way or finished -- this script never assigns them and never
overwrites them. It only ever moves an issue between Ready and Blocked.

Usage:
  scripts/derive_board_status.py --snapshot DIR [--findings F.json ...]
      [--project-title "JLS Roadmap"] [--owner anadon] [--apply] [--pace S]

Exit status: 0 board already agrees (or --apply succeeded), 1 drift found in
dry-run, 2 environment/precondition failure.
"""
import argparse
import json
import os
import subprocess
import sys
import time

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
from issue_corpus import load_corpus, parse_edge_field  # noqa: E402

DERIVED = ("Ready", "Blocked")      # the only two states this script owns
UNTOUCHED = ("In Progress", "Done")  # human/workflow signals, never derived


def gh(args, check=True):
    r = subprocess.run(["gh"] + args, capture_output=True, text=True)
    if check and r.returncode != 0:
        sys.exit(f"gh {' '.join(args[:3])} failed: {r.stderr.strip()[:400]}")
    return r.stdout


def resolve_project(owner, title):
    """Resolve the project by TITLE, fatally on ambiguity.

    sync-roadmap-project.sh takes the same stance and states the reason:
    guessing here writes to the wrong board.
    """
    out = gh(["project", "list", "--owner", owner, "--format", "json",
              "--limit", "100"])
    hits = [p for p in json.loads(out).get("projects", [])
            if p.get("title") == title]
    if len(hits) != 1:
        sys.exit(f"project title {title!r} matched {len(hits)} projects under "
                 f"{owner}; refusing to guess which board to write")
    return hits[0]["number"], hits[0]["id"]


def status_field(project_id):
    q = """
    query($id:ID!){ node(id:$id){ ... on ProjectV2 { fields(first:50){ nodes{
      ... on ProjectV2SingleSelectField { id name options { id name } } } } } } }
    """
    out = gh(["api", "graphql", "-f", f"query={q}", "-f", f"id={project_id}"])
    for f in json.loads(out)["data"]["node"]["fields"]["nodes"]:
        if f and f.get("name") == "Status":
            return f["id"], {o["name"]: o["id"] for o in f["options"]}
    sys.exit("no single-select field named 'Status' on this project")


def derive(corpus, error_issues, cycle_issues):
    """issue number -> (derived status, reason)."""
    out = {}
    for n, iss in corpus.issues.items():
        mb = iss.machine or ""
        why = []
        blockers = [b for b in parse_edge_field(mb, "blocked_by")["numbers"]
                    if b in corpus.issues]
        if blockers:
            why.append("blocked_by still open: "
                       + ", ".join(f"#{b}" for b in sorted(blockers)))
        if n in cycle_issues:
            why.append("sits in a dependency cycle (G04)")
        if n in error_issues:
            why.append("carries error-severity validator finding(s): "
                       + ", ".join(sorted(error_issues[n])))
        if "needs-template-fix" in iss.labels:
            why.append("labelled needs-template-fix")
        if iss.tier in ("feature", "capstone"):
            key = ("planned_tasks" if iss.tier == "feature"
                   else "planned_features")
            if parse_edge_field(mb, key)["state"] == "populated" and \
                    _has_entries(mb, key):
                why.append(f"{key} is non-empty — children not all filed, so "
                           "its sufficiency argument is provisional")
        out[n] = ("Blocked", "; ".join(why)) if why else ("Ready", "")
    return out


def _has_entries(mb, key):
    """planned_* hold PROSE, not numbers, so parse_edge_field's `numbers` is
    always empty for them — count list entries directly instead."""
    import re
    m = re.search(r"^" + key + r":[ \t]*(.*)$", mb, re.M)
    if not m:
        return False
    inline = m.group(1).split("#")[0].strip()
    if inline in ("[]", "~", "null", "none", ""):
        if inline:
            return False
    elif inline.startswith("["):
        return bool(inline.strip("[]").strip())
    for line in mb[m.end():].split("\n"):
        if re.match(r"^\s+-\s+", line):
            return True
        if re.match(r"^\S", line):
            break
    return False


def main():
    ap = argparse.ArgumentParser(description=__doc__,
                                 formatter_class=argparse.RawDescriptionHelpFormatter)
    ap.add_argument("--snapshot", required=True)
    ap.add_argument("--findings", action="append", default=[],
                    help="validator findings JSON; repeatable")
    ap.add_argument("--owner", default="anadon")
    ap.add_argument("--project-title", default="JLS Roadmap")
    ap.add_argument("--apply", action="store_true",
                    help="write Status. Default is a dry run.")
    ap.add_argument("--pace", type=float, default=0.3)
    args = ap.parse_args()

    if not os.path.isdir(args.snapshot):
        sys.exit(2)
    corpus = load_corpus(args.snapshot)

    error_issues, cycle_issues = {}, set()
    for path in args.findings:
        for f in json.load(open(path))["findings"]:
            if f["check"] == "G04":
                cycle_issues.update(x for x in f.get("objects", [])
                                    if isinstance(x, int))
                cycle_issues.add(f["issue"])
            if f["severity"] == "error" and f["issue"]:
                error_issues.setdefault(f["issue"], set()).add(f["check"])

    want = derive(corpus, error_issues, cycle_issues)

    board = {}
    bp = os.path.join(args.snapshot, "board.jsonl")
    if os.path.exists(bp):
        for line in open(bp):
            d = json.loads(line)
            if d.get("issue"):
                board[d["issue"]] = (d.get("item_id"), d.get("status"))

    drift, skipped = [], []
    for n, (status, why) in sorted(want.items()):
        item = board.get(n)
        if not item:
            continue
        item_id, cur = item
        if cur in UNTOUCHED:
            skipped.append((n, cur))
            continue
        if cur != status:
            drift.append((n, cur, status, why, item_id))

    print(f"derived over {len(want)} open issues: "
          f"{sum(1 for s, _ in want.values() if s == 'Ready')} Ready, "
          f"{sum(1 for s, _ in want.values() if s == 'Blocked')} Blocked")
    print(f"board disagrees on {len(drift)}; "
          f"{len(skipped)} left alone (In Progress/Done are not derived)")
    for n, cur, new, why, _ in drift[:40]:
        print(f"  #{n}: {cur or '<none>'} -> {new}"
              + (f"   ({why[:110]})" if why else ""))
    if len(drift) > 40:
        print(f"  ... and {len(drift) - 40} more")

    if not args.apply:
        print("\n(dry run — pass --apply to write Status)")
        return 1 if drift else 0

    num, pid = resolve_project(args.owner, args.project_title)
    fid, opts = status_field(pid)
    for s in DERIVED:
        if s not in opts:
            sys.exit(f"Status has no option named {s!r}; ruling and board "
                     "vocabulary disagree — fix the board, not this script")
    ok = 0
    for n, cur, new, why, item_id in drift:
        r = subprocess.run(
            ["gh", "project", "item-edit", "--id", item_id,
             "--project-id", pid, "--field-id", fid,
             "--single-select-option-id", opts[new]],
            capture_output=True, text=True)
        if r.returncode:
            print(f"  FAILED #{n}: {r.stderr.strip()[:160]}")
        else:
            ok += 1
            print(f"  set #{n} -> {new}")
        time.sleep(args.pace)
    print(f"\nupdated {ok}/{len(drift)}")
    return 0


if __name__ == "__main__":
    sys.exit(main())
