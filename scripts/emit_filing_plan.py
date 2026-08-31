#!/usr/bin/env python3
"""Emit the filing plan for scope that exists only as prose in planned_*.

WHY. A feature's `planned_tasks` and a capstone's `planned_features` hold real,
carefully-written scope that is NOT an issue: it is not on the board, not
countable as work, and no workflow can execute it. The templates require each
entry to "resolve to a number via REPLAN when it is filed" but attach no gate
forcing that, so it accumulates. Until it is filed, the parent cannot honestly
claim its children carry it out (feature rule B / capstone rule E), which is
exactly the completeness property the corpus is supposed to guarantee.

This script does NOT author issue bodies. It emits the work-list: one row per
unfiled scope, with the verbatim text, the owner, the tier to file at, the
edges the new issue should carry, and a filing ORDER derived from the
dependency graph so that scopes whose owner is already unblocked come first.

Usage:
  scripts/emit_filing_plan.py --snapshot DIR [--out-md F.md] [--out-json F.json]
"""
import argparse
import json
import os
import re
import sys

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
from issue_corpus import load_corpus, parse_edge_field  # noqa: E402
from validate_issue_graph import planned_entries  # noqa: E402


FILED_RE = re.compile(
    r"\b(?:filed as|now filed|is filed|already filed|-- *filed)\b"
    r"[^.;]{0,40}#(\d{2,4})", re.I)


def classify(text, open_nums):
    """Bucket a planned_* entry by how strong the evidence is that it is
    ALREADY filed. Counting every entry as unfiled overstates the gap: some
    say so in their own text and merely were not cleared out afterwards
    (graph check G13), and those need a REPLAN to remove, not a new issue."""
    mentions = sorted(set(int(m) for m in re.findall(r"#(\d{2,4})\b", text)))
    m = FILED_RE.search(text)
    if m and int(m.group(1)) in open_nums:
        return "already-filed", mentions, int(m.group(1))
    if mentions:
        return "mentions-issue", mentions, None
    return "unfiled", mentions, None


def main():
    ap = argparse.ArgumentParser()
    ap.add_argument("--snapshot", required=True)
    ap.add_argument("--out-md", default="docs/filing-plan.md")
    ap.add_argument("--out-json", default="docs/filing-plan.json")
    args = ap.parse_args()

    corpus = load_corpus(args.snapshot)
    rows = []
    for n, iss in sorted(corpus.issues.items()):
        key = ("planned_tasks" if iss.tier == "feature"
               else "planned_features" if iss.tier == "capstone" else None)
        if not key:
            continue
        entries = planned_entries(iss.machine or "", key)
        if not entries:
            continue
        mb = iss.machine or ""
        blockers = [b for b in parse_edge_field(mb, "blocked_by")["numbers"]
                    if b in corpus.issues]
        serves = parse_edge_field(mb, "serves_capstones")["numbers"] \
            if iss.tier == "feature" else []
        for i, text in enumerate(entries, 1):
            bucket, mentions, filed_as = classify(text, set(corpus.issues))
            rows.append({
                "bucket": bucket,
                "already_filed_as": filed_as,
                "owner": n,
                "owner_tier": iss.tier,
                "owner_title": iss.title,
                "owner_blocked_by_open": blockers,
                "owner_serves_capstones": serves,
                "file_as_tier": "task" if iss.tier == "feature" else "feature",
                "index": i,
                "scope_verbatim": text.strip().strip('"'),
                "mentions_existing_issues": mentions,
            })

    # Filing order: owners that are not themselves waiting on open work first;
    # then by owner number so the sequence is stable across runs.
    rows.sort(key=lambda r: (len(r["owner_blocked_by_open"]), r["owner"],
                             r["index"]))
    for i, r in enumerate(rows, 1):
        r["order"] = i

    os.makedirs(os.path.dirname(args.out_json) or ".", exist_ok=True)
    with open(args.out_json, "w") as fh:
        json.dump({"schema": 1, "report": "filing-plan",
                   "snapshot": corpus.meta, "count": len(rows),
                   "rows": rows}, fh, indent=1)

    owners = sorted({r["owner"] for r in rows})
    unblocked = [r for r in rows if not r["owner_blocked_by_open"]]
    stale = [r for r in rows if r["bucket"] == "already-filed"]
    ambig = [r for r in rows if r["bucket"] == "mentions-issue"]
    certain = [r for r in rows if r["bucket"] == "unfiled"]
    with open(args.out_md, "w") as fh:
        w = fh.write
        w("# Filing plan — scope that is planned but not filed\n\n")
        w(f"**{len(rows)} planned scope items across {len(owners)} owning "
          f"issues.** Generated from the corpus snapshot; regenerate rather "
          "than hand-editing.\n\n")
        w("Not all of these need a new issue. Bucketed by how strong the "
          "evidence is that the work is already filed:\n\n")
        w(f"| bucket | rows | what to do |\n|---|---|---|\n")
        w(f"| **certainly unfiled** | {len(certain)} | file a new issue |\n")
        w(f"| mentions an issue number | {len(ambig)} | check each — the "
          "reference may be context, or may be the filed child |\n")
        w(f"| says \"filed as #N\" and #N is open | {len(stale)} | do NOT "
          "file; clear the entry with a REPLAN (graph check G13) |\n\n")
        w("Each row is work the plan already describes in prose but that has "
          "no issue, no board item, and no way to be executed. Until a row is "
          "filed, its owner's sufficiency argument (feature rule B / capstone "
          "rule E) is provisional — graph check **G19** reports this "
          "standing.\n\n")
        w("**This plan deliberately does not author bodies.** The scope text "
          "below is quoted verbatim so that whoever files it starts from what "
          "was actually planned, not from a paraphrase.\n\n")
        w("## Order\n\n")
        w(f"- **{len(unblocked)}** rows belong to owners with no open "
          "`blocked_by` — these can be filed immediately and are listed "
          "first.\n")
        w(f"- **{len(rows) - len(unblocked)}** belong to owners still waiting "
          "on open work; filing them early is legal but the child cannot "
          "start.\n")
        if stale:
            w(f"- **{len(stale)}** rows state in their own text that they are "
              "already filed; those are bookkeeping, not work.\n")
        w("\n---\n\n")
        cur = None
        for r in rows:
            if r["owner"] != cur:
                cur = r["owner"]
                w(f"\n## #{r['owner']} — {r['owner_title']}\n\n")
                w(f"- tier: `{r['owner_tier']}`; file its children as "
                  f"`{r['file_as_tier']}`\n")
                if r["owner_blocked_by_open"]:
                    w("- owner is BLOCKED by open: "
                      + ", ".join(f"#{b}" for b in r["owner_blocked_by_open"])
                      + "\n")
                if r["owner_serves_capstones"]:
                    w("- serves capstone(s): "
                      + ", ".join(f"#{c}" for c in r["owner_serves_capstones"])
                      + "\n")
                w("\n")
            w(f"**{r['order']}.** ({r['file_as_tier']}) ")
            if r["bucket"] == "already-filed":
                w(f"✅ **ALREADY FILED as #{r['already_filed_as']}** — do not "
                  "re-file; clear this entry with a REPLAN")
            elif r["mentions_existing_issues"]:
                w("⚠ mentions "
                  + ", ".join(f"#{x}" for x in r["mentions_existing_issues"])
                  + " — check whether that IS this child")
            w(f"\n\n> {r['scope_verbatim']}\n\n")
            w(f"On filing: add to `#{r['owner']}`'s "
              + ("`requires_tasks`" if r["file_as_tier"] == "task"
                 else "`requires_features`")
              + " and remove this entry from `"
              + ("planned_tasks" if r["file_as_tier"] == "task"
                 else "planned_features")
              + "` in the same REPLAN.\n\n")
    print(f"{len(rows)} planned rows across {len(owners)} owners")
    print(f"  certainly unfiled : {len(certain)}")
    print(f"  mentions an issue : {len(ambig)}  (needs a per-row check)")
    print(f"  already filed     : {len(stale)}  (clear via REPLAN, do not file)")
    print(f"  rows whose owner has no open blocker: {len(unblocked)}")
    print(f"wrote {args.out_md} and {args.out_json}")


if __name__ == "__main__":
    main()
