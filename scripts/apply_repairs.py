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
  roster-preserve  v7/v4 migration, step 1: a feature's requires_tasks is
                 UNIONED with every task still declaring the retired
                 part_of_feature naming it. Additive only — after the
                 migration the roster is the sole ownership record, so a
                 drop would be data loss. Must be applied BEFORE retire-pof.
  retire-pof     v7/v4 migration, step 2: drops the retired part_of_feature
                 key and writes a clearly-marked, non-authoritative
                 owned_by_derived back-reference in its place. Run only
                 after roster-preserve has landed.
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
import html
import json
import os
import re
import sys

from issue_corpus import TEMPLATES, find_heading, sig_tokens, load_corpus, machine_block, parse_edge_field
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


def remove_edge_key(body, key):
    """Delete `key:` and its continuation lines from the machine block.
    Returns the new body, or None if the key is not present."""
    mb = machine_block(body)
    if mb is None:
        return None
    lines = mb.split("\n")
    start = end = None
    for i, line in enumerate(lines):
        if re.match(rf"^{re.escape(key)}\s*:", line):
            start, end = i, i + 1
            inline = line.split(":", 1)[1]
            opened = "[" in inline and "]" not in inline
            for j in range(i + 1, len(lines)):
                if opened:
                    end = j + 1
                    if "]" in lines[j]:
                        break
                    continue
                if re.match(r"^\S", lines[j]):
                    break
                if re.match(r"^\s+(-|#)", lines[j]) or not lines[j].strip():
                    end = j + 1
                else:
                    break
            break
    if start is None:
        return None
    new_mb = "\n".join(lines[:start] + lines[end:])
    return body.replace(mb, new_mb, 1)


def insert_edge_key(body, after_key, newline):
    """Insert `newline` immediately after `after_key`'s last line."""
    mb = machine_block(body)
    if mb is None:
        return None
    lines = mb.split("\n")
    end = None
    for i, line in enumerate(lines):
        if re.match(rf"^{re.escape(after_key)}\s*:", line):
            end = i + 1
            inline = line.split(":", 1)[1]
            opened = "[" in inline and "]" not in inline
            for j in range(i + 1, len(lines)):
                if opened:
                    end = j + 1
                    if "]" in lines[j]:
                        break
                    continue
                if re.match(r"^\S", lines[j]):
                    break
                if re.match(r"^\s+(-|#)", lines[j]) or not lines[j].strip():
                    end = j + 1
                else:
                    break
            break
    if end is None:
        return None
    new_mb = "\n".join(lines[:end] + [newline] + lines[end:])
    return body.replace(mb, new_mb, 1)


def roster_owners(corpus):
    """task number -> sorted list of features whose requires_tasks lists it."""
    owners = {}
    for fn, fi in corpus.issues.items():
        if fi.tier != "feature":
            continue
        for t in parse_edge_field(fi.machine or "", "requires_tasks")["numbers"]:
            owners.setdefault(t, []).append(fn)
    return {t: sorted(v) for t, v in owners.items()}


def gen_roster_preserve(corpus, feature_num, ruling):
    """PRESERVE step of the v7/v4 ownership migration: union this feature's
    roster with every task still declaring the retired part_of_feature: <this
    feature>. Strictly ADDITIVE — unlike roster-regen it never drops, because
    after the migration the roster is the ONLY record and a drop is data loss."""
    feat = corpus.issues[feature_num]
    claiming = sorted(n for n, iss in corpus.issues.items()
                      if iss.tier == "task"
                      and feature_num in parse_edge_field(
                          iss.machine or "", "part_of_feature")["numbers"])
    current = parse_edge_field(feat.machine or "", "requires_tasks")["numbers"]
    adds = sorted(set(claiming) - set(current))
    if not adds:
        return None
    merged = sorted(set(current) | set(adds))
    marker = ("preserved from retired part_of_feature claims before the key "
              "was dropped (scientific-task v7 / feature v4)")
    body = replace_edge_value(feat.body, "requires_tasks", merged, marker)
    if body is None:
        return None
    comment = (
        "REPLAN: requires_tasks gains the tasks that declared the retired "
        "`part_of_feature` field naming this feature, recorded here BEFORE "
        "that key is dropped corpus-wide. Under feature v4 this roster is the "
        f"sole record of ownership, so this preserves it (ruling {ruling}).\n\n"
        + f"Added: {', '.join(f'#{n}' for n in adds)}\n"
        + "\nNothing was dropped: this repair is additive by construction. "
          "A task may now be owned by any number of features, so a roster "
          "entry is never removed merely because another feature also lists "
          "it.\n\nThe §2 roster table and mermaid graph may lag this "
          "machine-block edit; rule A repair follows in the mermaid/roster pass.")
    return body, comment, {"kind": "roster-preserve", "adds": adds}


def gen_retire_pof(corpus, task_num, owners, ruling):
    """Drop the retired part_of_feature key and replace it with a clearly
    marked DERIVED back-reference, so a task read alone still names its owners
    without that copy ever being mistaken for the authority."""
    iss = corpus.issues[task_num]
    claimed = parse_edge_field(iss.machine or "", "part_of_feature")["numbers"]
    owns = owners.get(task_num, [])

    body = remove_edge_key(iss.body, "part_of_feature")
    resumed = body is None
    if resumed:
        # Already migrated by an earlier partial run: the retired key is gone
        # but the derived back-reference may never have been written. Converge
        # rather than leaving two shapes in the corpus.
        if "owned_by_derived:" in (iss.machine or ""):
            return None
        body = iss.body

    value = "[" + ", ".join(str(n) for n in owns) + "]" if owns else "[]"
    line = (f"owned_by_derived: {value}  # DERIVED from feature rosters, NOT "
            "authoritative;\n"
            "                        #   the owning features' requires_tasks "
            "are the only record.\n"
            "                        #   Regenerate rather than hand-edit "
            "(validator G21).")
    body = insert_edge_key(body, "evidence_commit", line)
    if body is None:
        return None

    lost = sorted(set(claimed) - set(owns))
    if resumed:
        head = ("AMENDED: adding the derived owner back-reference that a "
                "partial earlier run of this migration omitted. The retired "
                "`part_of_feature` key was already removed from this machine "
                f"block; nothing else changes (ruling {ruling}).\n\n")
        ledger = ""
    else:
        head = ("AMENDED: the retired `part_of_feature` key is removed from "
                "this machine block. Under scientific-task v7 / feature v4 a "
                "task may be owned by any number of features and declares no "
                "owner itself — each owning feature's `requires_tasks` roster "
                f"is the sole record (ruling {ruling}).\n\n")
        ledger = ("Dropped/Retired ledger: `part_of_feature: "
                  + (", ".join(f"#{n}" for n in claimed) if claimed else "none")
                  + "` — retired as a field. ")
        if owns:
            ledger += ("That ownership is now recorded on feature "
                       + ", ".join(f"#{n}" for n in owns)
                       + ", whose roster lists this task.\n")
        else:
            ledger += ("This task is in no feature roster; it is currently "
                       "unowned, which is a legal state.\n")

    warn = ""
    if lost:
        warn = (f"\n**Unresolved:** this task claimed "
                + ", ".join(f"#{n}" for n in lost)
                + " but no such roster lists it; that claim could not be "
                  "preserved and is recorded here rather than dropped "
                  "silently.\n")

    comment = (head + ledger + warn
               + "\nA non-authoritative `owned_by_derived` line records the "
                 "owners so this issue still names them when read alone. It "
                 "is derived state: validator G21 re-derives it from the "
                 "rosters and reports drift.")
    return body, comment, {"kind": "retire-pof", "was": claimed,
                           "owned_by": owns, "unpreserved": lost,
                           **({"resumed": True} if resumed else {})}

def remove_list_entries(body, key, targets):
    """Remove specific entries from a BLOCK-style machine-block list, line by
    line, leaving every surviving entry's own YAML annotation byte-identical.

    replace_edge_value() rewrites the whole field as a canonical inline list,
    which destroys the per-entry comments that carry the ordering RATIONALE —
    unacceptable when the corpus rule is that an issue must stay complete
    enough to act on without outside context. Returns (new_body, removed_text)
    where removed_text maps target -> the exact lines removed, so the comment
    ledger can quote what it took away. Falls back to (None, {}) when the
    field is not block-style; the caller then uses replace_edge_value."""
    mb = machine_block(body)
    if mb is None:
        return None, {}
    lines = mb.split("\n")
    start = None
    for i, line in enumerate(lines):
        if re.match(rf"^{re.escape(key)}\s*:", line):
            start = i
            break
    if start is None:
        return None, {}
    # block style only: the key line must carry no inline value
    if lines[start].split(":", 1)[1].split("#")[0].strip():
        return None, {}
    end = start + 1
    while end < len(lines) and not re.match(r"^\S", lines[end]):
        end += 1
    body_lines = lines[start + 1:end]

    # group each "- N" entry with its trailing continuation/comment lines
    groups, cur, cur_num = [], [], None
    for ln in body_lines:
        m = re.match(r"^\s+-\s*\"?#?(\d+)\"?", ln)
        if m:
            if cur:
                groups.append((cur_num, cur))
            cur, cur_num = [ln], int(m.group(1))
        else:
            cur.append(ln)
    if cur:
        groups.append((cur_num, cur))

    tset, removed, kept = set(targets), {}, []
    for num, glines in groups:
        if num in tset:
            removed[num] = "\n".join(glines)
        else:
            kept.extend(glines)
    if not removed:
        return None, {}
    if not [g for g in groups if g[0] is not None and g[0] not in tset]:
        kept = ["  []"]
    new_mb = "\n".join(lines[:start + 1] + kept + lines[end:])
    return body.replace(mb, new_mb, 1), removed


def gen_heading_normalize(corpus, num, ruling):
    """Rename drifted section headings back to their canonical template text.

    Touches ONLY heading lines. Section prose is never read, moved or
    rewritten: the corpus rule is that an issue must not be condensed or
    abridged, so a cosmetic rename has to be provably cosmetic.

    find_heading(headings, canonical) -> (actual, drifted). It takes the LIST
    of heading strings, not the body, and returns a 2-tuple that is always
    truthy — so its result must be unpacked, never tested for truth.
    """
    iss = corpus.issues[num]
    tier = iss.tier
    if tier not in TEMPLATES:
        return None
    body = iss.body
    lines = [(m.group(1), m.group(2))
             for m in re.finditer(r"^(#{2,4})[ \t]*(.+?)[ \t]*$", body, re.M)]
    names = [t for _, t in lines]
    counts = {}
    for t in names:
        counts[t] = counts.get(t, 0) + 1

    renames, annotated = [], []
    for canon in TEMPLATES[tier]["headings"]:
        actual, drifted = find_heading(names, canon)
        if actual is None or not drifted or actual == canon:
            continue
        if counts.get(actual, 0) != 1:
            continue                      # ambiguous: leave for a human
        # NEVER let a rename delete text. Some headings carry an author
        # annotation the canonical form has no room for -- e.g.
        # "1. Background & Prior Work (current state, landed at 2eb3e0c)".
        # Renaming those to canonical would silently drop the annotation,
        # which is exactly the abridgement this corpus forbids. find_heading
        # already resolves them, so citations work today; leave them to a
        # human who can decide where the extra text belongs.
        # Compare on entity-unescaped, stem-matched tokens. Raw token-set
        # difference is too crude: it treats `&amp;` (entity damage) and
        # "Hypotheses"/"Hypothesis" (a plural) as author annotation and
        # skips repairs that are plainly safe.
        canon_stems = {t[:6] for t in sig_tokens(canon)}
        extra = [t for t in sig_tokens(html.unescape(actual))
                 if not any(t[:6] == c or c.startswith(t[:6])
                            or t.startswith(c) for c in canon_stems)]
        if extra:
            annotated.append((actual, canon, sorted(extra)))
            continue
        hashes = next(h for h, t in lines if t == actual)
        renames.append((hashes, actual, canon))
    if not renames:
        return None

    for hashes, actual, canon in renames:
        old_line = f"{hashes} {actual}"
        new_line = f"{hashes} {canon}"
        if body.count("\n" + old_line + "\n") != 1:
            return None                   # not uniquely locatable: skip issue
        body = body.replace("\n" + old_line + "\n", "\n" + new_line + "\n", 1)

    rows = "\n".join(f"- `{a}` -> `{c}`" for _, a, c in renames)
    comment = (
        "AMENDED: section heading(s) renamed to the canonical template text. "
        "Cross-references cite section NAMES (task rule 5), so a heading that "
        "drifts from the template silently breaks every citation resolving "
        f"against it (ruling {ruling}).\n\n"
        f"{rows}\n\n"
        "Only the heading lines changed. No section content was read, moved, "
        "reordered, summarised or removed — this edit is cosmetic by "
        "construction. Each rename was applied only where the canonical "
        "heading was absent, the drifted heading resolved to exactly one "
        "line, and that line was uniquely locatable in the body.\n\n"
        "Where the drift is an HTML entity (`&amp;` for `&`), the heading was "
        "damaged by a lossy tracker read path — see #489, which documents "
        "that defect. This edit repairs the heading only; entity damage "
        "elsewhere in the body is reported, not auto-repaired, because "
        "`&lt;`/`&gt;` are frequently intentional there.")
    return body, comment, {"kind": "heading-normalize",
                           "renames": [[a, c] for _, a, c in renames],
                           **({"skipped_annotated": annotated}
                              if annotated else {})}


def gen_drop_illegal(corpus, num, drops, ruling):
    """Remove ordering entries the corrected tier model forbids (G03).

    `drops` is {field: [target, ...]}. The edges are REMOVED from the machine
    block and enumerated in the comment's Dropped/Retired ledger — task rule 9
    requires that any edit which narrows a claim make the omission auditable
    from the comment alone. An upward ordering edge carried real intent ("wait
    for that feature"), and the faithful re-expression is an edge to the
    specific sibling TASKS involved; that is a per-edge judgment, so the
    comment asks for it explicitly rather than this generator guessing a
    roster-wide fan-out that could deadlock the task against its own owner."""
    iss = corpus.issues[num]
    body = iss.body
    ledger = []
    for field, targets in sorted(drops.items()):
        cur = parse_edge_field(iss.machine or "", field)["numbers"]
        keep = [x for x in cur if x not in set(targets)]
        if keep == cur:
            continue
        # Prefer the line-wise removal: it leaves surviving entries' YAML
        # annotations untouched. Only fall back to the canonical inline
        # rewrite when the field is not block-style (nothing to preserve).
        nb, removed = remove_list_entries(body, field, targets)
        if nb is None:
            marker = ("upward ordering edge(s) removed — corrected tier "
                      "model (feature v4); see this issue's AMENDED comment")
            nb = replace_edge_value(body, field, keep, marker)
            removed = {}
            if nb is None:
                return None
        body = nb
        for t in sorted(targets):
            tt = corpus.issues[t].tier if t in corpus.issues else "unknown"
            ledger.append(f"- `{field}: #{t}` (tier:{tt}) — removed")
            if removed.get(t):
                quoted = "\n".join("  > " + ln.strip()
                                   for ln in removed[t].split("\n")
                                   if ln.strip())
                if quoted:
                    ledger.append("  Its recorded rationale, preserved "
                                  "verbatim:\n" + quoted)
    if not ledger:
        return None
    comment = (
        "AMENDED: ordering edge(s) removed from the machine block. The "
        "corrected tier model is strictly layered — a task depends on tasks, "
        "a feature on features and tasks, a capstone on capstones and "
        "features — and **no ordering edge may point upward** "
        f"(feature v4; ruling {ruling}).\n\n"
        "**Dropped/Retired ledger** (task rule 9):\n"
        + "\n".join(ledger)
        + "\n\nWhy these were illegal: an ordering edge aimed at a composite "
          "gates that composite's integration/close-out, never its children's "
          "start. A task that appeared to \"wait on a feature\" therefore had "
          "no enforceable meaning at the task tier.\n\n"
          "**Action required if the dependency was real:** re-express it as an "
          "edge to the specific sibling TASK(S) actually waited on. This edit "
          "deliberately does not fan the edge out across the whole roster: "
          "that would be a guess, and where the task is itself in that "
          "roster it would deadlock the task against its own owner.\n\n"
          "These edges were also the sole cause of the corpus's reported "
          "dependency cycles; removing them restores the DAG.")
    return body, comment, {"kind": "drop-illegal",
                           "drops": {k: sorted(v) for k, v in drops.items()}}


DECOMP_HEAD = re.compile(r"^\|\s*Task\s*\|.*\|\s*$", re.M | re.I)


def gen_roster_table_sync(corpus, feature_num, adds, ruling):
    """Append rows to §2's decomposition table for tasks added to the roster.

    Machine-block roster edits deliberately did not touch §2's prose table,
    because rewriting prose is not a mechanical edit. That left rule A drift:
    the block lists a child the table does not. Appending a row is the one
    repair that is provably additive — no existing cell is read, moved or
    rewritten, so nothing can be abridged."""
    iss = corpus.issues[feature_num]
    body = iss.body
    m = DECOMP_HEAD.search(body)
    if not m:
        return None
    # find the end of the table: the header, its separator, then data rows
    lines = body.split("\n")
    hdr = body[:m.start()].count("\n")
    i = hdr + 1
    if i < len(lines) and set(lines[i].strip()) <= set("|-: "):
        i += 1
    else:
        return None
    while i < len(lines) and lines[i].lstrip().startswith("|"):
        i += 1
    present = "\n".join(lines[hdr:i])
    new_rows = []
    for t, why in adds:
        if re.search(rf"\|\s*#{t}\b", present):
            continue
        new_rows.append(f"| #{t} | {why} | Filed — open; re-homed into this "
                        "roster by the v7/v4 ownership migration |")
    if not new_rows:
        return None
    body = "\n".join(lines[:i] + new_rows + lines[i:])
    listed = ", ".join(f"#{t}" for t, _ in adds
                       if not re.search(rf"\|\s*#{t}\b", present))
    comment = (
        f"REPLAN: §2's decomposition table gains a row for {listed}, which the "
        "machine block already lists in `requires_tasks`. The earlier roster "
        "edit changed the machine block only and said so at the time; this "
        "closes that rule A gap (prose must agree with the block, and the "
        f"block is the source of truth). Ruling {ruling}.\n\n"
        "**Purely additive:** rows were appended. No existing row, cell or "
        "sentence was read, reordered, reworded or removed.\n\n"
        "The ```mermaid graph in Status & Dependency Graph may still lag — "
        "regenerating it is a judgement about layout, not a mechanical edit, "
        "and is left for the next touch of this issue.")
    return body, comment, {"kind": "roster-table-sync",
                           "rows_added": [t for t, _ in adds]}


def gen_roster_add(corpus, feature_num, add_tasks, ruling, basis):
    """Additive roster edit from an explicit, maintainer-confirmed mapping.
    Used to re-home tasks that sit in no roster. Never drops."""
    feat = corpus.issues[feature_num]
    current = parse_edge_field(feat.machine or "", "requires_tasks")["numbers"]
    adds = sorted(set(add_tasks) - set(current))
    if not adds:
        return None
    merged = sorted(set(current) | set(adds))
    marker = "re-homed unowned task(s) per the confirmed orphan disposition"
    body = replace_edge_value(feat.body, "requires_tasks", merged, marker)
    if body is None:
        return None
    lines = "\n".join(f"- #{n} — {basis.get(n, 'maintainer-confirmed disposition')}"
                       for n in adds)
    comment = (
        "REPLAN: requires_tasks gains task(s) that sat in NO feature roster. "
        "Under feature v4 a roster is the sole record of ownership, so an "
        "unrostered task is unowned — these were re-homed here by confirmed "
        f"disposition (ruling {ruling}; see docs/orphan-task-dispositions.md)."
        f"\n\nAdded:\n{lines}\n"
        "\nNothing was dropped; this repair is additive. A task may be owned "
        "by any number of features, so listing it here does not remove it "
        "from any other roster.\n\nThe §2 roster table and mermaid graph may "
        "lag this machine-block edit; rule A repair follows in the "
        "mermaid/roster pass.")
    return body, comment, {"kind": "roster-add", "adds": adds}


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
    ap.add_argument("--map", help="roster-add: JSON {feature: {tasks:[..], "
                    "basis:{task: why}}} of maintainer-confirmed re-homings")
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

    if "heading-normalize" in kinds:
        for num in sorted(corpus.issues):
            if (only and num not in only) or num in built:
                continue
            r = gen_heading_normalize(corpus, num, args.ruling)
            if r:
                emit(num, r[0], r[1], corpus.issues[num].tier or "task", r[2])

    if "drop-illegal" in kinds:
        per = {}
        for f in findings:
            if f["check"] != "G03" or f["severity"] != "error":
                continue
            objs = f.get("objects") or []
            if len(objs) != 2 or objs[0] not in ("blocked_by", "blocks"):
                continue
            per.setdefault(f["issue"], {}).setdefault(objs[0], []).append(objs[1])
        for num in sorted(per):
            if (only and num not in only) or num in built:
                continue
            if num not in corpus.issues:
                continue
            r = gen_drop_illegal(corpus, num, per[num], args.ruling)
            if r:
                emit(num, r[0], r[1], corpus.issues[num].tier or "task", r[2])

    if "roster-table-sync" in kinds:
        mapping = json.load(open(args.map))
        for fnum, spec in sorted(mapping.items(), key=lambda kv: int(kv[0])):
            num = int(fnum)
            if (only and num not in only) or num in built:
                continue
            if num not in corpus.issues:
                continue
            basis = {int(k): v for k, v in (spec.get("basis") or {}).items()}
            adds = [(int(t), basis.get(int(t), "re-homed by the v7/v4 "
                                       "ownership migration"))
                    for t in spec["tasks"]]
            r = gen_roster_table_sync(corpus, num, adds, args.ruling)
            if r:
                emit(num, r[0], r[1], "feature", r[2])

    if "roster-add" in kinds:
        mapping = json.load(open(args.map))
        for fnum, spec in sorted(mapping.items(), key=lambda kv: int(kv[0])):
            num = int(fnum)
            if (only and num not in only) or num in built:
                continue
            if num not in corpus.issues:
                print(f"  SKIP #{num}: not an open issue")
                continue
            basis = {int(k): v for k, v in (spec.get("basis") or {}).items()}
            r = gen_roster_add(corpus, num, [int(t) for t in spec["tasks"]],
                               args.ruling, basis)
            if r:
                emit(num, r[0], r[1], "feature", r[2])

    # --- v7/v4 ownership migration. ORDER IS LOAD-BEARING: roster-preserve
    # must run (and be applied) before retire-pof, or the only record of a
    # task's ownership is deleted before it has been written down anywhere.
    if "roster-preserve" in kinds:
        feats = sorted(n for n, i in corpus.issues.items()
                       if i.tier == "feature")
        for num in feats:
            if (only and num not in only) or num in built:
                continue
            r = gen_roster_preserve(corpus, num, args.ruling)
            if r:
                emit(num, r[0], r[1], "feature", r[2])

    if "retire-pof" in kinds:
        owners = roster_owners(corpus)
        # Select both shapes: issues that still carry the retired key, AND
        # tasks a previous partial run already stripped but left without the
        # derived back-reference. Keying only on the retired key's presence
        # makes a half-finished migration unresumable.
        targets = sorted(
            n for n, i in corpus.issues.items()
            if parse_edge_field(i.machine or "",
                                "part_of_feature")["state"] != "absent"
            or (i.tier == "task"
                and "owned_by_derived:" not in (i.machine or "")))
        for num in targets:
            if (only and num not in only) or num in built:
                continue
            r = gen_retire_pof(corpus, num, owners, args.ruling)
            if r:
                emit(num, r[0], r[1], corpus.issues[num].tier or "task", r[2])

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
