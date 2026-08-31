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
tier-legality; G04 combined-graph cycles; G05 RETIRED (task->feature ordering
is now illegal outright — G03 owns it); G06 roster entries resolve to open
tasks; G07 RETIRED (a task may be owned by many features); G08
serves_capstones/requires_features mirror; G09 ordering symmetry (info) and
feature<->capstone mirror obligations; G10 native sub-issue agreement; G11
edges to closed issues; G12 surviving retired capstone->task exception
REPLAN comment; G13 planned_* hygiene (filed numbers still in planned;
orphaned K/M/L/P/D scope ids); G14 mermaid vs machine block; G15
G15 RETIRED (multiple owners are legal); G16 self-edges/dupes/
contradictions; G17
`related` hygiene; G18 evidence_commit resolution and staleness.
"""
import argparse
import json
import os
import re
import subprocess
import sys

from issue_corpus import (COMMENT_PREFIXES, MERMAID_BLOCK, TEMPLATES,
                          finding, findings_report, load_corpus,
                          machine_block, parse_edge_field,
                          RETIRED_YAML_KEYS, yaml_key_state)

CHECK_VERSION = "g-v1"

# field -> (source tier, set of legal target tiers)
# The model is strictly layered (feature v4 is canonical): each tier composes
# the tier below and orders against its own tier. NO EDGE POINTS UPWARD.
# A task declares no composition edge at all — ownership lives solely in each
# owning feature's requires_tasks roster, and a task may be owned by many.
COMPOSITION_TARGETS = {
    "requires_tasks": ("feature", {"task"}),
    "serves_capstones": ("feature", {"capstone"}),
    "requires_features": ("capstone", {"feature"}),
    "requires_capstones": ("capstone", {"capstone"}),
}
# ordering targets by source tier. Capstone-to-capstone ordering is LEGAL as
# of capstone v4 (v3 routed it through an upward feature->capstone edge, which
# the corrected model forbids).
ORDERING_TARGETS = {
    "task": {"task"},
    "feature": {"feature", "task"},
    "capstone": {"capstone", "feature"},
}
PLANNED_FIELDS = {"planned_tasks", "planned_features"}
SCOPE_ID = re.compile(r"\b([KLPD]\d+|M(?!0\b|1\b|2\b|3\b|4\b)\d+)\b")
EVIDENCE_SHA = re.compile(r"^evidence_commit\s*:\s*([0-9a-f]{7,40})\b", re.M)


def planned_entries(mb, key):
    """List entries under a planned_* key.

    These hold PROSE, not issue numbers, so parse_edge_field's `numbers` is
    always empty for them — counting that would report zero unfiled work for
    a feature carrying nine unfiled scopes. Count list entries directly.
    """
    if not mb:
        return []
    m = re.search(r"^" + re.escape(key) + r":[ \t]*(.*)$", mb, re.M)
    if not m:
        return []
    inline = m.group(1).split("#")[0].strip()
    if inline.startswith("["):
        inner = inline[1:inline.rfind("]")] if "]" in inline else inline[1:]
        return [x.strip() for x in inner.split(",") if x.strip()]
    if inline in ("~", "null", "none", "[]"):
        return []
    out = []
    for line in mb[m.end():].split("\n"):
        if re.match(r"^\s+-\s+", line):
            out.append(line.strip()[1:].strip())
        elif re.match(r"^\S", line):
            break
    return out


# A child task that cites its OWN PARENT feature's integration criterion by
# number is the highest-precision signal of a feature rule B violation that
# exists in this corpus. Rule B requires at least one §5 criterion that no
# single child covers alone; when the child's own body says "this is #F's
# Integration Criterion 5", that criterion is covered alone by construction.
# Two independent audits converged on this as "the most reliable tell".
# Backticks/brackets around the reference are common, so they are tolerated.
PARENT_CRIT = re.compile(
    r"[`\[]?#(\d{2,4})[`\]]?(?:'s)?\s+(?:own\s+)?(?:§\s*5\s+)?"
    r"(?:Integration\s+)?[Cc]riteri(?:on|a)\s*"
    r"(\d+(?:\s*(?:[-\u2013,]|and)\s*\d+)*)"
    r"|[`\[]?#(\d{2,4})[`\]]?(?:'s)?\s+(I|IC|AC)-?(\d+)\b")


def cited_parent_criteria(body, parent):
    """Criterion labels this body attributes to `parent`, by number."""
    out = set()
    for m in PARENT_CRIT.finditer(body or ""):
        ref = m.group(1) or m.group(3)
        if not ref or int(ref) != parent:
            continue
        out.add((m.group(2) or "").strip() or
                f"{m.group(4)}-{m.group(5)}")
    return sorted(x for x in out if x)


class Node:
    def __init__(self, iss):
        self.iss = iss
        self.tier = iss.tier
        self.machine = iss.machine
        self.fields = {}
        # Retired keys live in their own map, NOT in self.fields: G01 treats an
        # absent entry in self.fields as a defect, and a retired key is absent
        # from every healthy issue. G12/G20 read self.retired instead.
        self.retired = {}
        if self.machine and self.tier in TEMPLATES:
            for key in TEMPLATES[self.tier]["yaml_keys"]:
                if key in ("tier", "evidence_commit"):
                    continue
                self.fields[key] = parse_edge_field(self.machine, key)
            for key in RETIRED_YAML_KEYS:
                self.retired[key] = parse_edge_field(self.machine, key)

    def retired_numbers(self, key):
        f = self.retired.get(key)
        return f["numbers"] if f else []

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
                    # Direction matters. `A blocked_by B` means A depends on
                    # B, so judge against A's row. `A blocks B` means B
                    # depends on A, so judge against B's row — using A's row
                    # for both silently legalises every upward edge written
                    # from the parent's side (e.g. `feature blocks task`,
                    # which IS `task blocked_by feature`).
                    if key == "blocked_by":
                        dependent, depended = node.tier, ttier
                    else:
                        dependent, depended = ttier, node.tier
                    legal = ORDERING_TARGETS.get(dependent, set())
                    if ttier and depended not in legal:
                        rel = (f"this {node.tier} depends on #{t} "
                               f"(tier:{ttier})" if key == "blocked_by"
                               else f"#{t} (tier:{ttier}) depends on this "
                                    f"{node.tier}")
                        F.append(finding(n, "G03", "error",
                                         f"{key}: {rel}, but a {dependent} "
                                         f"may depend on {sorted(legal)} "
                                         "only — no upward edges",
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

        # G20: machine-block keys retired by the v7/v4 tier-model correction.
        # G05 (task blocked_by its own parent) and G15 (multiple parents) are
        # both retired with the field they policed: task->feature ordering is
        # now illegal outright (G03 owns it) and multiple owners are legal.
        for rk, why in RETIRED_YAML_KEYS.items():
            if yaml_key_state(node.machine, rk) != "absent":
                F.append(finding(n, "G20", "warn",
                                 f"machine block still carries retired key "
                                 f"`{rk}` — {why}",
                                 objects=[rk], fix_class="body"))

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
            # G12: capstone rule G's orphaned-scope exception is RETIRED
            # (capstone v4) — shared task ownership removes the condition it
            # existed for. Any surviving entry is scope that must be re-homed
            # into a feature roster, which is a stronger finding than the old
            # "missing REPLAN comment" warning.
            exc = node.retired_numbers("requires_tasks_exception")
            for t in exc:
                F.append(finding(n, "G12", "error",
                                 f"requires_tasks_exception #{t} — retired "
                                 "capstone->task edge; re-home the task into "
                                 "a feature's requires_tasks (a task may be "
                                 "in any number of rosters) and drop the key",
                                 objects=[t], fix_class="adjudicate"))

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
    # G07 (single-owner) is RETIRED: a task may appear in any number of
    # feature rosters — a shared task is simply shared (feature v4).
    owners = {}
    for n, node in sorted(nodes.items()):
        if node.tier == "feature":
            for t in node.numbers("requires_tasks"):
                owners.setdefault(t, []).append(n)

    # G06: the roster is now the SOLE authority for ownership, so there is no
    # second side to reciprocate against. What remains checkable is that every
    # roster entry is really an open task of this repo.
    # G19: planned_tasks / planned_features hold REAL scope that is not an
    # issue, not on the board, and not executable by any workflow. The
    # templates stage work there and require each entry to "resolve to a
    # number via REPLAN when it is filed", but attach no gate forcing that
    # resolution — so the scope can sit unfiled indefinitely while the parent
    # looks complete. It is not: a feature carrying planned_tasks cannot
    # satisfy rule B, and a capstone carrying planned_features cannot satisfy
    # rule E, because part of the roster does not exist yet.
    for n, node in sorted(nodes.items()):
        if not want(n):
            continue
        key = ("planned_tasks" if node.tier == "feature"
               else "planned_features" if node.tier == "capstone" else None)
        if key is None:
            continue
        entries = planned_entries(node.machine, key)
        if entries:
            F.append(finding(n, "G19", "warn",
                             f"{key} holds {len(entries)} unfiled scope "
                             f"item(s) — this work is not an issue, not on "
                             f"the board and not executable; the "
                             f"{'rule B' if node.tier == 'feature' else 'rule E'}"
                             " sufficiency argument is provisional until each "
                             "is filed and resolved by REPLAN",
                             objects=[key, len(entries)],
                             fix_class="adjudicate"))

    # G22: rule B risk — a child that claims its parent's own criterion.
    for n, node in sorted(nodes.items()):
        if node.tier != "feature" or not want(n):
            continue
        for t in node.numbers("requires_tasks"):
            child = nodes.get(t)
            if child is None:
                continue
            crits = cited_parent_criteria(child.iss.body, n)
            if crits:
                F.append(finding(
                    n, "G22", "warn",
                    f"child #{t} names this feature's own integration "
                    f"criteri{'on' if len(crits) == 1 else 'a'} "
                    f"{', '.join(crits)} as its own deliverable — if no other "
                    "§5 criterion is jointly owned, rule B is not met and "
                    "this is a folder, not a feature",
                    objects=[t] + crits, fix_class="adjudicate"))

    # G21: owned_by_derived is an OPTIONAL, explicitly non-authoritative
    # convenience copy of "which features list this task", so a task read
    # alone still names its owners (scientific-task v7 removed the
    # authoritative field). Being derived, it can rot — so it is checked
    # against the rosters, which are the truth, and never the other way round.
    for n, node in sorted(nodes.items()):
        if node.tier != "task" or not want(n):
            continue
        state = yaml_key_state(node.machine, "owned_by_derived")
        if state == "absent":
            continue
        got = sorted(parse_edge_field(node.machine,
                                      "owned_by_derived")["numbers"])
        real = sorted(owners.get(n, []))
        if got != real:
            F.append(finding(n, "G21", "warn",
                             f"owned_by_derived {got or '[]'} disagrees with "
                             f"the feature rosters that actually list this "
                             f"task {real or '[]'} — it is derived state; "
                             "regenerate it, do not hand-edit",
                             objects=sorted(set(got) ^ set(real)),
                             fix_class="auto"))

    for n, node in sorted(nodes.items()):
        if node.tier == "feature":
            declared_children = set(node.numbers("requires_tasks"))
            for t in sorted(declared_children):
                if not want(n):
                    continue
                child = nodes.get(t)
                if child is None:
                    # A roster legitimately RETAINS a completed child: that is
                    # how a reader sees the feature's work is done. Only an
                    # entry that resolves to nothing, or to abandoned scope
                    # still being counted as delivered, is a defect.
                    rec = corpus.closed.get(t)
                    if rec is None:
                        F.append(finding(
                            n, "G06", "error",
                            f"requires_tasks lists #{t}, which is not an open "
                            "issue and not a known closed one — a roster is "
                            "the sole record of ownership, so a dangling "
                            "entry silently drops that scope",
                            objects=[t], fix_class="body"))
                    elif (rec.get("state_reason") or "") == "not_planned":
                        F.append(finding(
                            n, "G06", "warn",
                            f"requires_tasks lists #{t}, closed as NOT "
                            "PLANNED — the roster still counts abandoned "
                            "scope as delivered; drop it via REPLAN or "
                            "re-home the scope",
                            objects=[t], fix_class="adjudicate"))
                elif child.tier != "task":
                    F.append(finding(
                        n, "G06", "error",
                        f"requires_tasks lists #{t}, which is tier "
                        f"{child.tier}, not task",
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

        # G10 native sub-issues vs machine block.
        # A task may be owned by many features, and GitHub's native sub-issue
        # link is single-parent, so it can only ever mirror ONE owner. It is
        # therefore checked for CONSISTENCY (the native parent must be one of
        # the real owners), never for completeness.
        native_parent = corpus.parent.get(n)
        if node.tier == "task":
            declared_owners = owners.get(n, [])
        elif node.tier == "feature":
            declared_owners = node.numbers("serves_capstones")
        else:
            declared_owners = []
        if want(n) and declared_owners:
            if native_parent is None:
                F.append(finding(n, "G10", "warn",
                                 f"owned by {declared_owners} but no native "
                                 "sub-issue link exists",
                                 objects=list(declared_owners),
                                 fix_class="auto"))
            elif native_parent not in declared_owners:
                F.append(finding(n, "G10", "error",
                                 f"native sub-issue parent #{native_parent} "
                                 f"is not among this issue's owners "
                                 f"{declared_owners} (the machine block is "
                                 "the source of truth)",
                                 objects=[native_parent] + list(declared_owners),
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
                    if key == "serves_capstones":
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


# ---------------------------------------------------------------------------
# Coverage report: is the corpus a complete PLAN, not just a set of legal edges
# ---------------------------------------------------------------------------
# Every G-check above asks whether a declared edge is well-formed and legal.
# None of them ask whether the capstone -> feature -> task tree actually covers
# its own stated scope. That is a different question and it has a different
# answer shape: almost everything here is a judgment call for the maintainer,
# so this is a REPORT, never a pass/fail gate.

ROADMAP_DOC = re.compile(
    r"docs/(capability-roadmap/[\w.\-]+\.md|grand-architecture\.md"
    r"|standards-adoption/[\w.\-]+\.md)")
C_FAMILY = re.compile(r"^(?:TASK|FEAT|CAP)-(C\d+)")


def cited_paths(body):
    """Source paths a body cites, by either spelling. No git, no resolution —
    this is a co-citation signal, not a verification."""
    out = set()
    for m in re.finditer(r"([\w./\-]+\.(?:java|py|md|xml|yml|yaml|sh|v|sv|"
                         r"json|txt|cfg|toml|properties)):\d+", body or ""):
        out.add(m.group(1))
    for m in re.finditer(r"github\.com/[\w.\-]+/[\w.\-]+/blob/[0-9a-f]{7,40}/"
                         r"([^\s)`\"'#]+)#L\d+", body or ""):
        out.add(m.group(1))
    return out


def coverage(corpus, repo_root):
    nodes = {n: Node(i) for n, i in corpus.issues.items()}
    tier = lambda t: [n for n, nd in nodes.items() if nd.tier == t]
    caps, feats, tasks = tier("capstone"), tier("feature"), tier("task")

    cap_feat = {n: nodes[n].numbers("requires_features") for n in caps}
    feat_task = {n: nodes[n].numbers("requires_tasks") for n in feats}
    owned_feats = {x for v in cap_feat.values() for x in v}
    owned_tasks = {x for v in feat_task.values() for x in v}

    roster_orphan_tasks = sorted(n for n in tasks if n not in owned_tasks)
    # Under feature v4 a roster is the ONLY record of ownership, so "in no
    # roster" is the whole of orphanhood; there is no second, weaker tier.
    full_orphan_tasks = roster_orphan_tasks

    rootedness = {
        "features_serving_no_capstone":
            sorted(n for n in feats if n not in owned_feats),
        "tasks_in_no_feature_roster": roster_orphan_tasks,
        "tasks_in_no_roster_at_all": full_orphan_tasks,
        "capstones_requiring_nothing":
            sorted(n for n in caps if not cap_feat[n]),
    }
    decomposition = {
        "features_with_empty_roster":
            sorted(n for n in feats if not feat_task[n]),
        "features_with_single_task":
            sorted(n for n in feats if len(feat_task[n]) == 1),
        "capstones_with_single_feature":
            sorted(n for n in caps if len(cap_feat[n]) == 1),
    }

    # --- roadmap traceability, both directions
    docs = {}
    cap_docs = {}
    for n, nd in nodes.items():
        hits = set(ROADMAP_DOC.findall(nd.iss.body or ""))
        if nd.tier == "capstone":
            cap_docs[n] = hits
        for d in hits:
            e = docs.setdefault(d, {"capstones": [], "features": 0,
                                    "tasks": 0})
            if nd.tier == "capstone":
                e["capstones"].append(n)
            elif nd.tier == "feature":
                e["features"] += 1
            else:
                e["tasks"] += 1
    on_disk = []
    for sub in ("capability-roadmap", "standards-adoption"):
        d = os.path.join(repo_root, "docs", sub)
        if os.path.isdir(d):
            on_disk += [f"{sub}/{f}" for f in sorted(os.listdir(d))
                        if f.endswith(".md")]
    on_disk.append("grand-architecture.md")
    roadmap = {
        "docs": {d: {"capstones": sorted(v["capstones"]),
                     "features": v["features"], "tasks": v["tasks"]}
                 for d, v in sorted(docs.items())},
        "capstones_citing_no_roadmap_doc":
            sorted(n for n in caps if not cap_docs.get(n)),
        "docs_cited_by_no_capstone":
            sorted(d for d in on_disk
                   if not docs.get(d, {}).get("capstones")),
        "docs_cited_by_nothing":
            sorted(d for d in on_disk if d not in docs),
    }

    # --- `related` edge characterization
    paths = {n: cited_paths(nd.iss.body) for n, nd in nodes.items()}
    fam = {}
    for n, nd in nodes.items():
        m = C_FAMILY.match(nd.iss.title or "")
        fam[n] = m.group(1) if m else None
    rel = {n: set(nodes[n].numbers("related")) for n in nodes}
    pairs, recip, cross_tier, cross_fam, no_shared = set(), 0, 0, 0, 0
    for a, ts in rel.items():
        for b in ts:
            if b not in nodes:
                continue
            pairs.add((min(a, b), max(a, b)))
    for a, b in pairs:
        if a in rel.get(b, ()) and b in rel.get(a, ()):
            recip += 1
        if nodes[a].tier != nodes[b].tier:
            cross_tier += 1
        if fam[a] != fam[b]:
            cross_fam += 1
        if not (paths[a] & paths[b]):
            no_shared += 1
    related = {"distinct_pairs": len(pairs),
               "reciprocated": recip,
               "one_way": len(pairs) - recip,
               "cross_tier": cross_tier,
               "cross_c_family": cross_fam,
               "no_shared_cited_path": no_shared}

    # --- co-citation shortlist: pairs sharing a cited path, for X04
    by_path = {}
    for n, ps in paths.items():
        for p_ in ps:
            by_path.setdefault(p_, []).append(n)
    hot = {p_: sorted(ns) for p_, ns in by_path.items()
           if 2 <= len(ns) <= 12}

    return {"schema": 1, "report": "coverage", "version": "cov-v1",
            "snapshot": corpus.meta,
            "totals": {"capstone": len(caps), "feature": len(feats),
                       "task": len(tasks)},
            "rootedness": rootedness, "decomposition": decomposition,
            "roadmap": roadmap, "related_edges": related,
            "shared_path_shortlist": hot}


def print_coverage(c):
    t = c["totals"]
    print(f"corpus: {t['capstone']} capstones, {t['feature']} features, "
          f"{t['task']} tasks\n")
    r = c["rootedness"]
    print("ROOTEDNESS — is every issue attached to the tree?")
    print(f"  features serving no capstone            {len(r['features_serving_no_capstone']):4}")
    print(f"  tasks in no feature roster              {len(r['tasks_in_no_feature_roster']):4}")
    print(f"    (a roster is the only ownership record) {len(r['tasks_in_no_roster_at_all']):4}")
    print(f"  capstones requiring nothing             {len(r['capstones_requiring_nothing']):4}")
    d = c["decomposition"]
    print("\nDECOMPOSITION — is each parent actually decomposed?")
    print(f"  features with an empty requires_tasks   {len(d['features_with_empty_roster']):4}  (fails feature rule B)")
    print(f"  features with exactly one task          {len(d['features_with_single_task']):4}")
    print(f"  capstones with exactly one feature      {len(d['capstones_with_single_feature']):4}")
    m = c["roadmap"]
    print("\nROADMAP TRACEABILITY — both directions")
    print(f"  capstones citing no roadmap/arch doc    {len(m['capstones_citing_no_roadmap_doc']):4}")
    print(f"  docs no capstone claims                 {len(m['docs_cited_by_no_capstone']):4}")
    print(f"  docs nothing at all cites               {len(m['docs_cited_by_nothing']):4}")
    e = c["related_edges"]
    print("\n`related` WEB — structure or decoration?")
    print(f"  distinct pairs                          {e['distinct_pairs']:4}")
    print(f"  reciprocated / one-way                  {e['reciprocated']:4} / {e['one_way']}")
    print(f"  cross-tier                              {e['cross_tier']:4}")
    print(f"  cross C-family                          {e['cross_c_family']:4}")
    print(f"  with no shared cited path               {e['no_shared_cited_path']:4}")
    print(f"\nX04 shortlist: {len(c['shared_path_shortlist'])} paths cited by 2-12 issues")


def main():
    ap = argparse.ArgumentParser(description=__doc__,
                                 formatter_class=argparse.RawDescriptionHelpFormatter)
    ap.add_argument("--snapshot", required=True)
    ap.add_argument("--issue", type=int, help="limit findings to one issue's "
                    "ego network")
    ap.add_argument("--repo-root", default=".")
    ap.add_argument("--json", help="write findings report here")
    ap.add_argument("--coverage", metavar="OUT.json",
                    help="instead of the findings pass, emit the corpus "
                    "coverage report: rootedness, decomposition adequacy, "
                    "roadmap traceability and `related`-web structure. "
                    "A report, never a gate — most of it is a maintainer call.")
    args = ap.parse_args()

    try:
        corpus = load_corpus(args.snapshot)
    except OSError as e:
        print(f"cannot load snapshot: {e}", file=sys.stderr)
        sys.exit(2)

    if args.coverage:
        c = coverage(corpus, args.repo_root)
        print_coverage(c)
        with open(args.coverage, "w") as fh:
            json.dump(c, fh, indent=1)
        sys.exit(0)

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
