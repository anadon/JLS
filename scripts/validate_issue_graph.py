#!/usr/bin/env python3
"""Validate the JLS cross-issue graph declared in machine blocks.

The templates make the ```yaml machine block the source of truth for graph
assembly (task rule 8, feature rules A-D, capstone rules E-G). This validator
checks what no per-issue linter can: DAG-ness of the combined ordering graph,
edge tier-legality, roster reciprocity, mirrors, native sub-issue agreement,
and edges pointing at closed or nonexistent issues.

Runs entirely offline from a snapshot_corpus.py dump (which includes a
closed-issue index); the only external tool is git, for G18's
evidence_commit checks against the local clone.

Usage:
  scripts/validate_issue_graph.py --snapshot DIR [--issue N] \
      [--repo-root .] [--json out.json]

Exit status: 0 no error-severity findings, 1 some, 2 environment error.

Check ids (stable): G01 parse defects; G02 nonexistent referents; G03 edge
tier-legality; G04 combined-graph cycles; G05 task blocked_by own parent;
G06 part_of_feature/roster reciprocity; G07 multiple owners; G08
serves_capstones/requires_features mirror; G09 ordering symmetry (info) and
feature<->capstone mirror obligations; G10 native sub-issue agreement; G11
edges to closed issues; G12 requires_tasks_exception without an orphan-event
REPLAN comment; G13 planned_* hygiene (filed numbers still in planned;
orphaned K/M/L/P/D scope ids); G14 mermaid vs machine block; G15
part_of_feature multiplicity; G16 self-edges/dupes/contradictions; G17
`related` hygiene; G18 evidence_commit resolution and staleness.
"""
import argparse
import json
import re
import subprocess
import sys

from issue_corpus import (COMMENT_PREFIXES, MERMAID_BLOCK, TEMPLATES,
                          finding, findings_report, load_corpus,
                          machine_block, parse_edge_field)

CHECK_VERSION = "g-v1"

# field -> (source tier, set of legal target tiers)
COMPOSITION_TARGETS = {
    "part_of_feature": ("task", {"feature"}),
    "requires_tasks": ("feature", {"task"}),
    "serves_capstones": ("feature", {"capstone"}),
    "requires_features": ("capstone", {"feature"}),
    "requires_capstones": ("capstone", {"capstone"}),
    "requires_tasks_exception": ("capstone", {"task"}),
}
# ordering targets by source tier (feature template's legality matrix; direct
# capstone-to-capstone ordering edges are forbidden — nesting goes through
# requires_capstones)
ORDERING_TARGETS = {
    "task": {"task", "feature"},
    "feature": {"task", "feature", "capstone"},
    "capstone": {"feature"},
}
PLANNED_FIELDS = {"planned_tasks", "planned_features"}
SCOPE_ID = re.compile(r"\b([KLPD]\d+|M(?!0\b|1\b|2\b|3\b|4\b)\d+)\b")
EVIDENCE_SHA = re.compile(r"^evidence_commit\s*:\s*([0-9a-f]{7,40})\b", re.M)


class Node:
    def __init__(self, iss):
        self.iss = iss
        self.tier = iss.tier
        self.machine = iss.machine
        self.fields = {}
        if self.machine and self.tier in TEMPLATES:
            for key in TEMPLATES[self.tier]["yaml_keys"]:
                if key in ("tier", "evidence_commit"):
                    continue
                self.fields[key] = parse_edge_field(self.machine, key)

    def numbers(self, key):
        f = self.fields.get(key)
        return f["numbers"] if f else []


def tier_of(n, corpus, nodes):
    if n in nodes:
        return nodes[n].tier
    if n in corpus.closed:
        for l in corpus.closed[n]["labels"]:
            if l.startswith("tier:"):
                return l.split(":", 1)[1]
    return None


def find_cycles(edges, nodes_set):
    """Strongly connected components of size >1 (plus self-loops), iterative
    Tarjan. Returns a list of member lists."""
    graph = {}
    for a, b in edges:
        graph.setdefault(a, []).append(b)
    index_of, low, on_stack, stack = {}, {}, set(), []
    sccs, counter = [], [0]
    for root in sorted(nodes_set):
        if root in index_of:
            continue
        work = [(root, 0)]
        while work:
            v, pi = work[-1]
            if pi == 0:
                index_of[v] = low[v] = counter[0]
                counter[0] += 1
                stack.append(v)
                on_stack.add(v)
            recurse = False
            children = graph.get(v, [])
            for i in range(pi, len(children)):
                w = children[i]
                if w not in index_of:
                    work[-1] = (v, i + 1)
                    work.append((w, 0))
                    recurse = True
                    break
                if w in on_stack:
                    low[v] = min(low[v], index_of[w])
            if recurse:
                continue
            if low[v] == index_of[v]:
                scc = []
                while True:
                    w = stack.pop()
                    on_stack.discard(w)
                    scc.append(w)
                    if w == v:
                        break
                if len(scc) > 1 or v in graph.get(v, []):
                    sccs.append(sorted(scc))
            work.pop()
            if work:
                u, _ = work[-1]
                low[u] = min(low[u], low[v])
    return sccs


def run(corpus, repo_root, only_issue=None):
    F = []
    nodes = {n: Node(iss) for n, iss in corpus.issues.items()}
    all_known = set(corpus.issues) | set(corpus.closed)
    max_number = max(all_known) if all_known else 0

    def want(n):
        return only_issue is None or n == only_issue

    # --- per-issue field scans ---------------------------------------------
    for n, node in sorted(nodes.items()):
        if not want(n):
            continue
        if node.machine is None or node.tier not in TEMPLATES:
            F.append(finding(n, "G01", "error",
                             "no parseable machine block / tier — graph "
                             "checks skipped for this issue",
                             fix_class="body"))
            continue

        for key, f in node.fields.items():
            if key in PLANNED_FIELDS:
                continue
            if f["bad"]:
                F.append(finding(n, "G01", "error",
                                 f"{key}: non-numeric or malformed entries "
                                 f"{f['bad']} (edge fields hold issue "
                                 "numbers or the literal `none`)",
                                 objects=[key] + f["bad"], fix_class="body"))
            if f["hash_form"]:
                F.append(finding(n, "G01", "info",
                                 f"{key}: entries use quoted '#N' style "
                                 f"{f['hash_form']} — numbers parse fine; "
                                 "bare integers are the template form",
                                 objects=[key] + f["hash_form"],
                                 fix_class="body"))
            if f["state"] != "populated":
                F.append(finding(n, "G01", "error",
                                 f"{key}: machine-block key {f['state']}",
                                 objects=[key], fix_class="body"))

            for t in f["numbers"]:
                if t == n:
                    F.append(finding(n, "G16", "error",
                                     f"{key}: self-edge", objects=[key, t],
                                     fix_class="body"))
                    continue
                if t not in all_known:
                    sev = "error" if t > max_number + 30 else "warn"
                    F.append(finding(n, "G02", sev,
                                     f"{key}: #{t} is not a known issue"
                                     + ("" if sev == "error" else
                                        " (may be a PR — PRs share the "
                                        "number space)"),
                                     objects=[key, t], fix_class="adjudicate"))
                    continue
                ttier = tier_of(t, corpus, nodes)
                if key in COMPOSITION_TARGETS:
                    src, legal = COMPOSITION_TARGETS[key]
                    if node.tier == src and ttier and ttier not in legal:
                        F.append(finding(n, "G03", "error",
                                         f"{key}: #{t} is tier:{ttier}, "
                                         f"legal targets: {sorted(legal)}",
                                         objects=[key, t],
                                         fix_class="adjudicate"))
                elif key in ("blocked_by", "blocks"):
                    legal = ORDERING_TARGETS.get(node.tier, set())
                    if ttier and ttier not in legal:
                        F.append(finding(n, "G03", "error",
                                         f"{key}: #{t} is tier:{ttier}; "
                                         f"{node.tier} ordering edges may "
                                         f"target {sorted(legal)} only",
                                         objects=[key, t],
                                         fix_class="adjudicate"))
                if t in corpus.closed and key != "related":
                    reason = corpus.closed[t].get("state_reason") or "closed"
                    sev = "warn" if reason == "not_planned" else "info"
                    F.append(finding(n, "G11", sev,
                                     f"{key}: #{t} is closed "
                                     f"({reason}): "
                                     f"\"{corpus.closed[t]['title'][:60]}\"",
                                     objects=[key, t],
                                     fix_class="adjudicate"))

            dupes = {t for t in f["numbers"] if f["numbers"].count(t) > 1}
            if dupes:
                F.append(finding(n, "G16", "warn",
                                 f"{key}: duplicate entries {sorted(dupes)}",
                                 objects=[key] + sorted(dupes),
                                 fix_class="body"))

        both = set(node.numbers("blocked_by")) & set(node.numbers("blocks"))
        if both:
            F.append(finding(n, "G16", "error",
                             f"issues in both blocked_by and blocks: "
                             f"{sorted(both)}", objects=sorted(both),
                             fix_class="adjudicate"))

        rel = node.fields.get("related")
        if rel and (n in rel["numbers"]):
            F.append(finding(n, "G17", "warn", "related: self-reference",
                             objects=[n], fix_class="body"))

        # G15 + G05
        pof = node.numbers("part_of_feature")
        if node.tier == "task":
            if len(pof) > 1:
                F.append(finding(n, "G15", "error",
                                 f"part_of_feature names {len(pof)} features "
                                 f"{pof}; a task is part_of at most ONE",
                                 objects=pof, fix_class="adjudicate"))
            if pof and pof[0] in node.numbers("blocked_by"):
                F.append(finding(n, "G05", "error",
                                 f"task is blocked_by its own parent feature "
                                 f"#{pof[0]} — a parent gates close-out, not "
                                 "child start; this deadlocks",
                                 objects=[pof[0]], fix_class="body"))

        # G13: filed numbers still sitting in planned_*
        for key in PLANNED_FIELDS & set(node.fields):
            f = node.fields[key]
            filed = [t for t in f["numbers"] if t in all_known]
            if filed:
                F.append(finding(n, "G13", "warn",
                                 f"{key} lists filed issue(s) {filed} — "
                                 "filed children belong in the requires_* "
                                 "roster", objects=[key] + filed,
                                 fix_class="body"))
        # Scope-id scan only where the ids can actually originate: bodies
        # citing the retired rescue vehicles #485/#497/#498 (34 known
        # citers). Corpus-wide the pattern drowns in line-refs (L100),
        # cache levels, and ordinary "P2" prose — 25k false positives.
        if re.search(r"#(485|497|498)\b", node.iss.body):
            ids = sorted({m.group(0)
                          for m in SCOPE_ID.finditer(node.iss.body)
                          if not (m.group(0).startswith("L")
                                  and len(m.group(0)) > 3)})
            if ids:
                F.append(finding(n, "G13", "info",
                                 f"acceptance/kill-criteria scope ids {ids} "
                                 "have no open source issue (numeric source "
                                 "#498 closed not-planned)",
                                 objects=ids, fix_class="adjudicate"))

        # G12
        if node.tier == "capstone":
            exc = node.numbers("requires_tasks_exception")
            if exc:
                comments = corpus.comments.get(n, [])
                for t in exc:
                    ok = any(c.get("body", "").startswith("REPLAN:")
                             and f"#{t}" in c.get("body", "")
                             and "orphan" in c.get("body", "").lower()
                             for c in comments)
                    if not ok:
                        F.append(finding(n, "G12", "warn",
                                         f"requires_tasks_exception #{t} has "
                                         "no REPLAN comment recording the "
                                         "orphan event (capstone rule G)",
                                         objects=[t], fix_class="comment"))

        # G14 mermaid
        if node.tier in ("feature", "capstone"):
            blocks = MERMAID_BLOCK.findall(node.iss.body)
            if not blocks:
                F.append(finding(n, "G14", "error",
                                 "no ```mermaid block (template requires a "
                                 "dependency flowchart)", fix_class="body"))
            else:
                mm_nums = set()
                for b in blocks:
                    mm_nums |= {int(x) for x in
                                re.findall(r"#?(\d{2,4})\b", b)}
                machine_targets = {t for key in node.fields
                                   for t in node.numbers(key)
                                   if key != "related"}
                if machine_targets:
                    overlap = len(mm_nums & machine_targets)
                    if overlap * 2 < len(machine_targets):
                        F.append(finding(
                            n, "G14", "info",
                            "mermaid not machine-comparable (mentions "
                            f"{overlap}/{len(machine_targets)} machine-block "
                            "edges by number)", fix_class="body"))
                    else:
                        missing = sorted(machine_targets - mm_nums)
                        if missing:
                            F.append(finding(
                                n, "G14", "warn",
                                f"mermaid omits machine-block edge(s) to "
                                f"{missing} (rule A: regenerate from the "
                                "machine block)", objects=missing,
                                fix_class="body"))

        # G18 evidence commit
        m = EVIDENCE_SHA.search(node.machine)
        if not m:
            F.append(finding(n, "G18", "error",
                             "evidence_commit missing or not a sha",
                             fix_class="body"))
        else:
            sha = m.group(1)
            r = subprocess.run(["git", "-C", repo_root, "cat-file", "-e",
                                sha + "^{commit}"], capture_output=True)
            if r.returncode != 0:
                F.append(finding(n, "G18", "error",
                                 f"evidence_commit {sha} does not resolve in "
                                 "the local clone", objects=[sha],
                                 fix_class="adjudicate"))
            else:
                r = subprocess.run(["git", "-C", repo_root, "merge-base",
                                    "--is-ancestor", sha, "HEAD"],
                                   capture_output=True)
                if r.returncode != 0:
                    F.append(finding(n, "G18", "warn",
                                     f"evidence_commit {sha} is not an "
                                     "ancestor of HEAD", objects=[sha],
                                     fix_class="adjudicate"))
                else:
                    cnt = subprocess.run(
                        ["git", "-C", repo_root, "rev-list", "--count",
                         f"{sha}..HEAD"], capture_output=True, text=True)
                    behind = int(cnt.stdout.strip() or 0)
                    if behind > 0:
                        F.append(finding(n, "G18", "info",
                                         f"evidence_commit {sha} is {behind} "
                                         "commits behind HEAD",
                                         objects=[sha, behind],
                                         fix_class="body"))

    # --- cross-issue: reciprocity, mirrors, ownership -----------------------
    owners = {}
    for n, node in sorted(nodes.items()):
        if node.tier == "feature":
            for t in node.numbers("requires_tasks"):
                owners.setdefault(t, []).append(n)
    for t, fs in sorted(owners.items()):
        if len(fs) > 1 and (want(t) or any(want(f) for f in fs)):
            F.append(finding(min(fs), "G07", "error",
                             f"task #{t} appears in requires_tasks of "
                             f"{len(fs)} features: {fs} (single-owner rule)",
                             objects=[t] + fs, fix_class="adjudicate"))

    for n, node in sorted(nodes.items()):
        if node.tier == "task":
            for f_num in node.numbers("part_of_feature"):
                feat = nodes.get(f_num)
                if feat and feat.tier == "feature" and want(f_num):
                    roster = set(feat.numbers("requires_tasks"))
                    if n not in roster:
                        F.append(finding(
                            f_num, "G06", "error",
                            f"task #{n} declares part_of_feature: {f_num} "
                            "but is absent from requires_tasks — the task's "
                            "field is authoritative; the roster must REPLAN",
                            objects=[n], fix_class="body"))
        if node.tier == "feature":
            declared_children = set(node.numbers("requires_tasks"))
            for t in sorted(declared_children):
                child = nodes.get(t)
                if child and child.tier == "task" and want(n):
                    cpof = child.numbers("part_of_feature")
                    if not cpof or cpof[0] != n:
                        F.append(finding(
                            n, "G06", "error",
                            f"requires_tasks lists #{t} but that task's "
                            f"part_of_feature is "
                            f"{cpof[0] if cpof else 'none'} — task field is "
                            "authoritative; REPLAN this roster",
                            objects=[t], fix_class="body"))
            for c_num in node.numbers("serves_capstones"):
                cap = nodes.get(c_num)
                if cap and cap.tier == "capstone" and want(n):
                    if n not in set(cap.numbers("requires_features")):
                        F.append(finding(
                            n, "G08", "warn",
                            f"serves_capstones names #{c_num} but that "
                            "capstone's requires_features omits this feature "
                            "(closed-set rule E — either the capstone must "
                            "list it or this claim is stale)",
                            objects=[c_num], fix_class="adjudicate"))
        if node.tier == "capstone":
            for f_num in node.numbers("requires_features"):
                feat = nodes.get(f_num)
                if feat and feat.tier == "feature" and want(f_num):
                    if n not in set(feat.numbers("serves_capstones")):
                        F.append(finding(
                            f_num, "G08", "error",
                            f"capstone #{n} requires this feature but "
                            "serves_capstones omits it — mirror the edge "
                            "(capstone roster is the closed set)",
                            objects=[n], fix_class="body"))

        # G09 ordering symmetry
        for t in node.numbers("blocked_by"):
            other = nodes.get(t)
            if other and n not in set(other.numbers("blocks")) and want(n):
                tiers = {node.tier, other.tier}
                sev = ("warn" if tiers == {"feature", "capstone"} else "info")
                F.append(finding(n, "G09", sev,
                                 f"blocked_by #{t} not mirrored by blocks "
                                 "on the other side"
                                 + (" (feature<->capstone mirror obligation)"
                                    if sev == "warn" else ""),
                                 objects=[t], fix_class="body"))

        # G10 native sub-issues vs machine block
        native_parent = corpus.parent.get(n)
        declared_parent = None
        if node.tier == "task":
            pof = node.numbers("part_of_feature")
            declared_parent = pof[0] if pof else None
        elif node.tier == "feature":
            sc = node.numbers("serves_capstones")
            declared_parent = sc[0] if len(sc) == 1 else None
        if want(n) and declared_parent:
            if native_parent is None:
                F.append(finding(n, "G10", "warn",
                                 f"machine block names parent "
                                 f"#{declared_parent} but no native "
                                 "sub-issue link exists",
                                 objects=[declared_parent],
                                 fix_class="auto"))
            elif native_parent != declared_parent and node.tier == "task":
                F.append(finding(n, "G10", "error",
                                 f"native sub-issue parent #{native_parent} "
                                 f"disagrees with part_of_feature "
                                 f"#{declared_parent} (machine block is "
                                 "source of truth)",
                                 objects=[native_parent, declared_parent],
                                 fix_class="auto"))

    # --- G04: combined ordering graph is a DAG ------------------------------
    edges = set()
    for n, node in nodes.items():
        for t in node.numbers("blocked_by"):
            if t in nodes:
                edges.add((t, n))
        for t in node.numbers("blocks"):
            if t in nodes:
                edges.add((n, t))
        for key, (src, _) in COMPOSITION_TARGETS.items():
            if node.tier != src:
                continue
            for t in node.numbers(key):
                if t in nodes:
                    if key in ("part_of_feature", "serves_capstones"):
                        edges.add((n, t))       # child before parent
                    else:                        # requires_*: children first
                        edges.add((t, n))
    for scc in find_cycles(edges, set(nodes)):
        if any(want(m) for m in scc):
            F.append(finding(min(scc), "G04", "error",
                             f"ordering+composition cycle among {scc} — the "
                             "combined graph must stay a DAG "
                             "(which edge is wrong is a maintainer call)",
                             objects=scc, fix_class="adjudicate"))
    return F


def main():
    ap = argparse.ArgumentParser(description=__doc__,
                                 formatter_class=argparse.RawDescriptionHelpFormatter)
    ap.add_argument("--snapshot", required=True)
    ap.add_argument("--issue", type=int, help="limit findings to one issue's "
                    "ego network")
    ap.add_argument("--repo-root", default=".")
    ap.add_argument("--json", help="write findings report here")
    args = ap.parse_args()

    try:
        corpus = load_corpus(args.snapshot)
    except OSError as e:
        print(f"cannot load snapshot: {e}", file=sys.stderr)
        sys.exit(2)

    F = run(corpus, args.repo_root, args.issue)
    for f in F:
        print(f"{f['severity'].upper():5}  #{f['issue']:<5} {f['check']}  "
              f"{f['message']}")
    errs = [f for f in F if f["severity"] == "error"]
    print(f"\n{len(F)} findings ({len(errs)} errors) across "
          f"{len({f['issue'] for f in F})} issues")
    if args.json:
        with open(args.json, "w") as fh:
            json.dump(findings_report("graph", CHECK_VERSION, args.snapshot,
                                      corpus.meta, F), fh, indent=1)
    sys.exit(1 if errs else 0)


if __name__ == "__main__":
    main()
