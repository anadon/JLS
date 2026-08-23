#!/usr/bin/env python3
"""Build gated repair pairs ({n}.md + {n}.comment.md) from graph findings.

The ONLY sanctioned mass-write path is: this script builds body+comment
pairs into an output dir, then gate_and_apply.py gates and posts them. This
script never touches GitHub.

Generators (deterministic, authority-directed — the templates name which
side must move):
  quote-upgrade  C04: every OK_RANGE citation (resolves at the body's own
                 evidence_commit but quotes nothing) gets the cited line's
                 text inserted right after the citation as an inline
                 backtick quote, turning it OK_QUOTED. Citations inside
                 fenced code blocks are skipped (inserting there corrupts
                 pasted evidence). Lines containing backticks are quoted by
                 their longest backtick-free normalized substring (>=8
                 chars) so the span still verifies; unquotable lines are
                 left alone and reported.
  roster-regen   G06: a feature's requires_tasks is regenerated as the exact
                 set of open tasks declaring part_of_feature: <this feature>
                 (task side is authoritative; template rule 8). The REPLAN
                 comment lists adds/drops; drops land in the comment's
                 Dropped/Retired ledger line, satisfying task rule 9.
  mirror-edge    G08 error: capstone requires_features names F, but F's
                 serves_capstones omits the capstone — append it (capstone
                 roster is the closed set; rule E).

Each generator edits ONLY the machine-block line(s) for its key, replacing
the value with a canonical inline list and a regeneration marker comment.
Prose (§2 roster tables) and mermaid blocks are NOT touched here — G14/S04
report residual disagreement and Phase-1 mermaid regeneration handles it.

Usage:
  apply_repairs.py --snapshot DIR --findings graph.json --out DIR \
      [--kinds roster-regen,mirror-edge] [--ruling RULING-ID] [--only N,...]
Then:
  gate_and_apply.py <out-parent> <repo> <commit> <out>/results.json \
      --snapshot DIR --bodies-dir <out> --comment-dir <out> [--apply]
"""
import argparse
import json
import os
import re
import sys

from issue_corpus import load_corpus, machine_block, parse_edge_field
from verify_citations import Tree, check_body, norm as vc_norm, rejoin_wrapped

EVIDENCE_SHA = re.compile(r"^evidence_commit\s*:\s*([0-9a-f]{7,40})\b", re.M)


def fenced_spans(text):
    """[(start, end)] of ``` fenced blocks — no insertions inside these."""
    spans, pos = [], 0
    while True:
        a = text.find("```", pos)
        if a < 0:
            return spans
        b = text.find("```", a + 3)
        if b < 0:
            spans.append((a, len(text)))
            return spans
        spans.append((a, b + 3))
        pos = b + 3


def quotable(line):
    """A backtick-safe, >=8-char normalized substring of the line, or None.
    Must survive verify_citations' `q in actual` check after norm()."""
    actual = vc_norm(line)
    if not actual:
        return None
    best = max(actual.split("`"), key=len).strip()
    return best if len(best) >= 8 else None


def gen_quote_upgrades(repo_root, iss):
    """(new_body, n_upgraded, n_skipped) for one issue, or None."""
    m = EVIDENCE_SHA.search(iss.machine or "")
    if not m:
        return None
    commit = m.group(1)
    tree = Tree(repo_root, commit)
    if subprocess_ok(repo_root, commit) is False:
        return None
    body = rejoin_wrapped(iss.body)  # check_body spans index the rejoined text
    results = check_body(repo_root, commit, body, tree)
    fences = fenced_spans(body)

    def in_fence(span):
        return any(a <= span[0] < b for a, b in fences)

    # one insertion point per citation-match span; collect the lines to quote
    per_span = {}
    for status, path, nums, detail, span in results:
        if status != "OK_RANGE" or in_fence(span):
            continue
        lines = tree.blob(path)
        if lines is None:
            continue
        if len(nums) == 2 and nums[1] > nums[0] + 1:
            wanted = [nums[0]]  # a range needs any line of the span quoted
        else:
            wanted = nums
        for n in wanted:
            if 1 <= n <= len(lines):
                q = quotable(lines[n - 1])
                if q:
                    per_span.setdefault(span, [])
                    if q not in per_span[span]:
                        per_span[span].append(q)

    if not per_span:
        return None
    skipped = sum(1 for s, p, nums, d, span in results
                  if s == "OK_RANGE" and span not in per_span)
    out, last = [], 0
    for span in sorted(per_span):
        out.append(body[last:span[1]])
        out.append(" (quotes: " + "; ".join(f"`{q}`"
                                            for q in per_span[span][:2]) + ")")
        last = span[1]
    out.append(body[last:])
    return "".join(out), len(per_span), skipped


def subprocess_ok(repo_root, commit):
    import subprocess
    return subprocess.run(["git", "-C", repo_root, "cat-file", "-e",
                           commit + "^{commit}"],
                          capture_output=True).returncode == 0


def replace_edge_value(body, key, numbers, marker):
    """Replace `key:`'s value (inline, wrapped-inline, or block list) inside
    the machine block with a canonical inline list. Returns new body or None
    if the key line cannot be located."""
    mb = machine_block(body)
    if mb is None:
        return None
    lines = mb.split("\n")
    start = end = None
    for i, line in enumerate(lines):
        if re.match(rf"^{re.escape(key)}\s*:", line):
            start = i
            end = i + 1
            inline = line.split(":", 1)[1]
            opened = "[" in inline and "]" not in inline
            for j in range(i + 1, len(lines)):
                if opened:
                    end = j + 1
                    if "]" in lines[j]:
                        break
                    continue
                # block-list / hanging-comment continuation: indented lines
                # belong to this key only until the next top-level key
                if re.match(r"^\S", lines[j]):
                    break
                if re.match(r"^\s+(-|#)", lines[j]) or not lines[j].strip():
                    end = j + 1
                else:
                    break
            break
    if start is None:
        return None
    value = "[" + ", ".join(str(n) for n in numbers) + "]" if numbers else "[]"
    newline = f"{key}: {value}  # {marker}"
    new_mb = "\n".join(lines[:start] + [newline] + lines[end:])
    return body.replace(mb, new_mb, 1)


def gen_roster_regen(corpus, feature_num, ruling):
    feat = corpus.issues[feature_num]
    declared = sorted(n for n, iss in corpus.issues.items()
                      if iss.tier == "task"
                      and parse_edge_field(iss.machine or "",
                                           "part_of_feature")["numbers"]
                      == [feature_num])
    current = parse_edge_field(feat.machine or "", "requires_tasks")["numbers"]
    adds = sorted(set(declared) - set(current))
    drops = sorted(set(current) - set(declared))
    if not adds and not drops:
        return None
    marker = "regenerated from part_of_feature declarations (task side authoritative)"
    body = replace_edge_value(feat.body, "requires_tasks", declared, marker)
    if body is None:
        return None
    comment = (
        "REPLAN: requires_tasks regenerated from the corpus's "
        "part_of_feature declarations — the task-side field is "
        "authoritative (task template rule 8; ruling "
        f"{ruling}).\n\n"
        + (f"Added: {', '.join(f'#{n}' for n in adds)}\n" if adds else "")
        + (f"Dropped/Retired ledger: {', '.join(f'#{n}' for n in drops)} — "
           "removed because no open task declares part_of_feature: "
           f"{feature_num}; each remains linked from its own machine block "
           "if it still relates.\n" if drops else "")
        + "\nThe §2 roster table and mermaid graph may lag this machine-block "
          "regeneration; rule A repair follows in the mermaid/roster pass.")
    return body, comment, {"adds": adds, "drops": drops}


def gen_mirror_edge(corpus, feature_num, capstone_num, ruling):
    feat = corpus.issues[feature_num]
    cur = parse_edge_field(feat.machine or "", "serves_capstones")["numbers"]
    if capstone_num in cur:
        return None
    new = sorted(set(cur) | {capstone_num})
    marker = f"mirror of capstone #{capstone_num} requires_features (rule E closed set)"
    body = replace_edge_value(feat.body, "serves_capstones", new, marker)
    if body is None:
        return None
    comment = (
        f"AMENDED: serves_capstones now mirrors capstone #{capstone_num}, "
        "whose requires_features names this feature — the capstone roster "
        "is the closed required set (capstone rule E; ruling "
        f"{ruling}). Machine-block-only edit; no scope change.")
    return body, comment, {"added": capstone_num}


def main():
    ap = argparse.ArgumentParser(description=__doc__,
                                 formatter_class=argparse.RawDescriptionHelpFormatter)
    ap.add_argument("--snapshot", required=True)
    ap.add_argument("--findings", help="graph findings json (required for "
                    "roster-regen / mirror-edge kinds)")
    ap.add_argument("--out", required=True)
    ap.add_argument("--kinds", default="roster-regen,mirror-edge")
    ap.add_argument("--repo-root", default=".")
    ap.add_argument("--ruling", default="UNRATIFIED",
                    help="ruling id cited in every protocol comment")
    ap.add_argument("--only", help="comma-separated issue allowlist")
    args = ap.parse_args()

    corpus = load_corpus(args.snapshot)
    findings = (json.load(open(args.findings))["findings"]
                if args.findings else [])
    kinds = set(args.kinds.split(","))
    only = ({int(x) for x in args.only.split(",")} if args.only else None)
    os.makedirs(args.out, exist_ok=True)

    results, built = [], {}

    def emit(num, body, comment, tier, note):
        with open(f"{args.out}/{num}.md", "w") as fh:
            fh.write(body)
        with open(f"{args.out}/{num}.comment.md", "w") as fh:
            fh.write(comment)
        ec = EVIDENCE_SHA.search(machine_block(body) or "")
        results.append({"number": num, "tier": tier,
                        "kind_label": "bug" if "bug" in
                        corpus.issues[num].labels else "enhancement",
                        **({"evidence_commit": ec.group(1)} if ec else {}),
                        "repair": note})
        built[num] = note

    if "roster-regen" in kinds:
        feats = sorted({f["issue"] for f in findings
                        if f["check"] == "G06"
                        and corpus.issues.get(f["issue"])
                        and corpus.issues[f["issue"]].tier == "feature"})
        for num in feats:
            if only and num not in only or num in built:
                continue
            r = gen_roster_regen(corpus, num, args.ruling)
            if r:
                emit(num, r[0], r[1], "feature", {"kind": "roster-regen",
                                                  **r[2]})

    if "quote-upgrade" in kinds:
        targets = (sorted(only) if only
                   else sorted(corpus.issues))
        for num in targets:
            if num in built or num not in corpus.issues:
                continue
            iss = corpus.issues[num]
            r = gen_quote_upgrades(args.repo_root, iss)
            if not r:
                continue
            new_body, n_up, n_skip = r
            sha = EVIDENCE_SHA.search(iss.machine or "").group(1)
            comment = (
                "AMENDED: citation quotes added mechanically from "
                f"evidence_commit {sha} (rule 1's quote obligation; ruling "
                "0004). No claim text changed.")
            emit(num, new_body, comment, iss.tier or "task",
                 {"kind": "quote-upgrade", "upgraded": n_up,
                  "unquotable_skipped": n_skip})

    if "mirror-edge" in kinds:
        for f in findings:
            if f["check"] != "G08" or f["severity"] != "error":
                continue
            feat_num, cap_num = f["issue"], f["objects"][0]
            if only and feat_num not in only or feat_num in built:
                continue
            r = gen_mirror_edge(corpus, feat_num, cap_num, args.ruling)
            if r:
                emit(feat_num, r[0], r[1], "feature",
                     {"kind": "mirror-edge", **r[2]})

    with open(f"{args.out}/results.json", "w") as fh:
        json.dump(results, fh, indent=1)
    print(f"built {len(results)} repair pair(s) in {args.out}")
    for num, note in sorted(built.items()):
        print(f"  #{num}: {note}")
    if args.ruling == "UNRATIFIED":
        print("\nWARNING: comments cite ruling UNRATIFIED — file the ruling "
              "before --apply.")


if __name__ == "__main__":
    main()
