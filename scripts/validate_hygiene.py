#!/usr/bin/env python3
"""Validate JLS issue hygiene and comment-protocol conformance (H family).

Covers what the template validator (shape) and graph validator (edges) do
not: title grammar, duplicate shortlists, kind-label coherence, the
AMENDED:/REPLAN: comment protocol, DoD checkbox integrity, section
cross-references, N/A discipline, size, scratchpad leaks, citation health at
each issue's own evidence_commit, and reference resolution. Runs offline
from a snapshot_corpus.py dump; the only external tools are git (H14/H15,
against --repo-root) and gh (only in --fetch-last-edited mode).

Usage:
  scripts/validate_hygiene.py --snapshot DIR [--issue N] [--repo-root .] \
      [--json out.json] [--skip H15[,H19...]] [--last-edited PATH]
  # one-time GraphQL fetch of per-issue body lastEditedAt (writes
  # last_edited.json into the snapshot dir, then exits):
  scripts/validate_hygiene.py --snapshot DIR --fetch-last-edited

Exit status: 0 no error-severity findings, 1 some, 2 environment error.

Check ids (stable): H01 title-prefix grammar vs tier; H02 C-number id
uniqueness + referent; H03 near-duplicate title shortlist (info, feeds X01);
H04 kind-label coherence (bug needs a command+output block in §2, task
rule 3); H05 rule-3 debt register (info metric); H06 body edited after last
AMENDED:/REPLAN: comment (needs last_edited.json; corpus-level info skip
otherwise); H07 prefixed comments not mirrored on the parent; H08
unrecognized pseudo-prefixes (UPDATE:/Status: ...); H09 DoD checkbox
integrity (zero boxes = error, ticked-on-open without evidence = warn);
H10 §-reference integrity (named must resolve, bare numbers violate rule 5);
H11 N/A without a reason; H12 §7.10 LaTeX presence/balance (task tier);
H13 body size vs GitHub's 65536 limit; H14 scratchpad-path leaks (paths
committed at meta.head are exempt); H15 citation sweep via
verify_citations.py at each issue's evidence_commit; H16 vacuous evidence
(zero citations, no absence-proving search); H17 dash-joined adjacent line
permalinks; H18 issue-reference resolution (fabricated / retired-rescue
referents); H19 template-conformance delta (validate-issue-template.py
folded into the merged findings).
"""
import argparse
import contextlib
import importlib.util
import io
import json
import os
import re
import subprocess
import sys
import tempfile
import time

SCRIPT_DIR = os.path.dirname(os.path.abspath(__file__))
if SCRIPT_DIR not in sys.path:
    sys.path.insert(0, SCRIPT_DIR)

from issue_corpus import (COMMENT_PREFIXES, HEADING, TEMPLATES, find_heading,
                          finding, findings_report, load_corpus,
                          parse_edge_field, sig_tokens)

VALIDATOR = "hygiene"
CHECK_VERSION = "h-v1"

TIER_PREFIX = {"task": "TASK", "feature": "FEAT", "capstone": "CAP"}
PREFIX_TIER = {v: k for k, v in TIER_PREFIX.items()}
DOD_HEADING = {"task": "14. Completion Criteria (Definition of Done)",
               "feature": "Completion Criteria (Definition of Done)",
               "capstone": "Completion Criteria (Definition of Done)"}
# Closed-not-planned rescue vehicles (maintainer disposition pending); cites
# are legal-but-flagged, and excluded from the H18 fabrication test so the
# two H18 branches stay disjoint.
RETIRED_RESCUE = {485, 497, 498}

TITLE_PREFIX = re.compile(r"^(TASK|FEAT|CAP)-")
CNUM_ID = re.compile(r"^(TASK|FEAT|CAP)-(C(\d+)(?:-\d+)?)\b")
TITLE_ID_STRIP = re.compile(r"^\s*(?:TASK|FEAT|CAP)-[A-Za-z0-9._\-]*\s*:?\s*")
CHECKBOX = re.compile(r"^\s*[-*]\s*\[([ xX])\]\s*(.*\S)?", re.M)
# [ \t] only, never \s: fence-masking leaves long space runs, and a
# multiline ^\s* backtracks catastrophically across them.
NA_LINE = re.compile(
    r"^[ \t]*(?:[-*][ \t]+)?(?:[*_]{1,2})?[ \t]*N/?A[ \t]*(?:[*_]{1,2})?"
    r"[ \t]*[.:]?[ \t]*$", re.M | re.I)
NA_JUSTIFIED = re.compile(
    r"^[ \t]*(?:[-*][ \t]+)?(?:[*_]{1,2})?[ \t]*N/?A[ \t]*(?:[*_]{1,2})?"
    r"[ \t]*(?:[—–-]{1,2})[ \t]*\S", re.M | re.I)
RULE3_DEBT = re.compile(
    r"observed failure|rule\s*3\b|not yet observed|build-and-observe", re.I)
PSEUDO_PREFIX = re.compile(r"^(UPDATE|Update|Status|NOTE|Progress|PROGRESS)"
                           r"\s*[:\-]")
DASH_PERMALINK = re.compile(r"#L\d+\)\s*[-–—]\s*\[L\d+")
ISSUE_REF = re.compile(r"(?<![\w&/])#(\d{1,5})\b")
SEC_REF = re.compile(
    r"§\s*(?:(?P<num>\d+(?:\.\d+)?)(?:\s*\((?P<pname>[^)\n]{2,80})\))?"
    r"|(?P<name>[A-Z][^\n.,;:()§]{2,60}))")
# Mirrors gate_and_apply.py's scratchpad-leak gate (closed verifier blind
# spot: paths that legitimately occur committed at meta.head are exempt).
SCRATCHPAD_LEAK = re.compile(
    r"(?:^|[^\w/])((?:issues/\d+\.md)|(?:issue_\d+\.json)|(?:body_\d+\.txt)"
    r"|(?:/tmp/claude-\d+/[^\s`)\"']*))")
EVIDENCE_SHA = re.compile(r"^evidence_commit\s*:\s*[\"']?([0-9a-fA-F]{7,40})\b",
                          re.M)
MATH_BLOCK = re.compile(r"\$\$.+?\$\$", re.S)
MATH_INLINE = re.compile(r"\$(?!\$)\S(?:[^$\n]*?\S)?\$")
FENCED = re.compile(r"```([^\n`]*)\n(.*?)```", re.S)
NON_CODE_FENCES = {"yaml", "yml", "mermaid"}
CMD_OUT_TOKENS = ("exit", "Error", "Exception", "FAILED", "Traceback",
                  "error:", "BUILD FAILURE", "stderr", "AssertionError")
# H16, adapted from gate_and_apply.py's vacuous-evidence gate.
ABSENCE_SEARCH = ("git grep", "ls-tree", "ls-files", "grep -r", "find .")
ABSENCE_PROOF = ("no match", "no matches", "exit=1", "exit code 1",
                 "returns nothing", "no output", "zero hits", "absent",
                 "no results", "not found", "0 hits", "0 results",
                 "does not exist")


def mask_fences(body):
    """Blank fenced-block interiors, preserving offsets and line structure."""
    out, in_fence = [], False
    for line in body.split("\n"):
        if line.lstrip().startswith("```"):
            in_fence = not in_fence
            out.append(" " * len(line))
        elif in_fence:
            out.append(" " * len(line))
        else:
            out.append(line)
    return "\n".join(out)


def mask_inline_code(text):
    return re.sub(r"`[^`\n]*`", lambda m: " " * len(m.group(0)), text)


def _cmdish(text):
    """Does a fenced block look like a pasted command+output?"""
    if re.search(r"^\s*\$\s+\S", text, re.M):
        return True
    return any(tok in text for tok in CMD_OUT_TOKENS)


class Ctx:
    """Per-run caches shared by the checks."""

    def __init__(self, corpus, repo_root=".", only=None, snapshot_dir=None,
                 last_edited_path=None):
        self.corpus = corpus
        self.repo = repo_root
        self.only = only
        self.snapshot_dir = snapshot_dir or corpus.dir
        self.last_edited_path = last_edited_path
        self.head = (corpus.meta or {}).get("head") or ""
        self._masked, self._masked_inline, self._sections = {}, {}, {}
        self._vc, self._vc_tried = None, False

    def issues(self):
        for n in sorted(self.corpus.issues):
            if self.only and n != self.only:
                continue
            yield n, self.corpus.issues[n]

    def all_issues(self):
        for n in sorted(self.corpus.issues):
            yield n, self.corpus.issues[n]

    def comments(self, n):
        cs = self.corpus.comments.get(n, [])
        return sorted(cs, key=lambda c: c.get("created_at") or "")

    def masked(self, n):
        if n not in self._masked:
            self._masked[n] = mask_fences(self.corpus.issues[n].body)
        return self._masked[n]

    def masked_inline(self, n):
        if n not in self._masked_inline:
            self._masked_inline[n] = mask_inline_code(self.masked(n))
        return self._masked_inline[n]

    def sections(self, n):
        """[(title, heading_start, heading_end, content_end)] from the
        fence-masked body (offsets valid in the raw body too)."""
        if n not in self._sections:
            masked = self.masked(n)
            ms = list(HEADING.finditer(masked))
            secs = []
            for i, m in enumerate(ms):
                cend = ms[i + 1].start() if i + 1 < len(ms) else len(masked)
                secs.append((m.group(1).strip(), m.start(), m.end(), cend))
            self._sections[n] = secs
        return self._sections[n]

    def section_content(self, n, canonical):
        secs = self.sections(n)
        actual, _ = find_heading([s[0] for s in secs], canonical)
        if actual is None:
            return None
        for title, _hs, he, ce in secs:
            if title == actual:
                return self.corpus.issues[n].body[he:ce]
        return None

    def section_by_num(self, n, num):
        for title, _hs, he, ce in self.sections(n):
            if re.match(rf"^{re.escape(num)}(?!\d)", title):
                return self.corpus.issues[n].body[he:ce]
        return None

    def vc(self):
        """verify_citations.py imported despite its script-style top level:
        exec with a harmless argv (no bodies) and swallow its SystemExit,
        keeping the regexes and helpers. Never fork the citation grammar."""
        if self._vc_tried:
            return self._vc
        self._vc_tried = True
        path = os.path.join(SCRIPT_DIR, "verify_citations.py")
        try:
            spec = importlib.util.spec_from_file_location("_vc_hygiene", path)
            mod = importlib.util.module_from_spec(spec)
            argv, sys.argv = sys.argv, [path, ".", "HEAD"]
            sink = io.StringIO()
            try:
                with contextlib.redirect_stdout(sink), \
                        contextlib.redirect_stderr(sink):
                    spec.loader.exec_module(mod)
            except SystemExit:
                pass
            finally:
                sys.argv = argv
            self._vc = mod if hasattr(mod, "CITE") else None
        except Exception:
            self._vc = None
        return self._vc


# --- Checks -----------------------------------------------------------------

def h01(ctx):
    out = []
    for n, iss in ctx.issues():
        tier = iss.tier
        if tier is None:
            continue                       # H19 owns undeterminable tiers
        m = TITLE_PREFIX.match(iss.title)
        if not m:
            out.append(finding(n, "H01", "warn",
                       f"title lacks the {TIER_PREFIX[tier]}- prefix for "
                       f"tier {tier}", objects=[TIER_PREFIX[tier]],
                       fix_class="auto"))
        elif PREFIX_TIER[m.group(1)] != tier:
            out.append(finding(n, "H01", "error",
                       f"title prefix {m.group(1)}- disagrees with tier "
                       f"{tier} (expected {TIER_PREFIX[tier]}-)",
                       objects=[m.group(1), tier], fix_class="adjudicate"))
    return out


def h02(ctx):
    ids = {}
    for n, iss in ctx.all_issues():
        m = CNUM_ID.match(iss.title)
        if m:
            ids.setdefault(m.group(0), []).append(n)
    out = []
    for n, iss in ctx.issues():
        m = CNUM_ID.match(iss.title)
        if not m:
            continue
        full = m.group(0)
        others = sorted(x for x in ids[full] if x != n)
        if others:
            out.append(finding(n, "H02", "error",
                       f"C-number id {full} is not unique (also on "
                       + ", ".join(f"#{x}" for x in others) + ")",
                       objects=others))
        ref = int(m.group(3))
        if ref not in ctx.corpus.issues:
            out.append(finding(n, "H02", "info",
                       f"C-number referent #{ref} is not an open issue "
                       "(referent semantics not fully pinned)",
                       objects=[ref]))
    return out


# X01-shortlist threshold. On the 2026-08-23 corpus nothing reaches 0.8;
# the closest real pair (a task and its feature sharing a title) sits at
# 0.74 — lower to ~0.7 if the dedupe pass wants a non-empty shortlist.
H03_JACCARD = 0.8


def h03(ctx):
    toks = {}
    for n, iss in ctx.all_issues():
        tt = sig_tokens(TITLE_ID_STRIP.sub("", iss.title))
        if tt:
            toks[n] = (tt, set(tt), " ".join(tt))
    nums = sorted(toks)
    out = []
    for i, a in enumerate(nums):
        _ta, sa, ja = toks[a]
        for b in nums[i + 1:]:
            _tb, sb, jb = toks[b]
            if ja == jb or len(sa & sb) / len(sa | sb) >= H03_JACCARD:
                out.append(finding(a, "H03", "info",
                           f"near-duplicate title of #{b} "
                           f"(X01 dedupe shortlist)", objects=[b]))
    return out


def h04(ctx):
    out = []
    for n, iss in ctx.issues():
        if iss.tier != "task":
            continue        # rule 3 (§2 Observations) is a task-template rule
        labels = set(iss.labels)
        content = ctx.section_content(n, "2. Observations")
        has_block = False
        for m in FENCED.finditer(content or ""):
            if m.group(1).strip().lower() in NON_CODE_FENCES:
                continue
            if _cmdish(m.group(2)):
                has_block = True
                break
        if "bug" in labels and not has_block:
            out.append(finding(n, "H04", "error",
                       "label `bug` but §2 Observations carries no fenced "
                       "command+output block (template rule 3)",
                       fix_class="body"))
        elif "enhancement" in labels and has_block:
            out.append(finding(n, "H04", "info",
                       "possible bug mislabel: `enhancement` but §2 pastes "
                       "an observed failure"))
    return out


def h05(ctx):
    out = []
    for n, iss in ctx.issues():
        hits = sorted({m.group(0).lower()
                       for m in RULE3_DEBT.finditer(iss.body)})
        if hits:
            out.append(finding(n, "H05", "info",
                       "rule-3 debt marker(s) on record: " + ", ".join(hits),
                       objects=hits))
    return out


def h06(ctx):
    path = ctx.last_edited_path or os.path.join(ctx.snapshot_dir,
                                                "last_edited.json")
    if not os.path.exists(path):
        return [finding(0, "H06", "info",
                        "H06 skipped: no last_edited.json (run "
                        "--fetch-last-edited to produce it)")]
    with open(path) as fh:
        last_edited = json.load(fh)
    out = []
    for n, iss in ctx.issues():
        edited = last_edited.get(str(n))
        if not edited:
            continue
        stamps = [c.get("created_at") for c in ctx.comments(n)
                  if (c.get("body") or "").lstrip().startswith(
                      ("AMENDED:", "REPLAN:"))]
        stamps = [s for s in stamps if s]
        if not stamps:
            out.append(finding(n, "H06", "warn",
                       f"body edited ({edited}) but no AMENDED:/REPLAN: "
                       "comment exists (AMENDED-backfill candidate)",
                       objects=[edited], fix_class="comment"))
        elif edited > max(stamps):
            out.append(finding(n, "H06", "warn",
                       f"body edited ({edited}) after the last "
                       f"AMENDED:/REPLAN: comment ({max(stamps)})",
                       objects=[edited, max(stamps)], fix_class="comment"))
    return out


def h07(ctx):
    out = []
    for n, iss in ctx.issues():
        mb = iss.machine
        if not mb:
            continue
        if iss.tier == "task":
            parents = parse_edge_field(mb, "part_of_feature")["numbers"]
        elif iss.tier == "feature":
            parents = parse_edge_field(mb, "serves_capstones")["numbers"]
        else:
            continue
        parents = [p for p in parents if p in ctx.corpus.issues]
        if not parents:
            continue
        prefixes = set()
        for c in ctx.comments(n):
            b = (c.get("body") or "").lstrip()
            for p in COMMENT_PREFIXES:
                if b.startswith(p):
                    prefixes.add(p)
                    break
        for pref in sorted(prefixes):
            for par in parents:
                mirrored = any(
                    (pc.get("body") or "").lstrip().startswith(pref)
                    and re.search(rf"#{n}\b", pc.get("body") or "")
                    for pc in ctx.comments(par))
                if not mirrored:
                    out.append(finding(n, "H07", "warn",
                               f"{pref} comment not mirrored on parent "
                               f"#{par} (no {pref} comment there mentioning "
                               f"#{n})", objects=[par, pref],
                               fix_class="comment"))
    return out


def h08(ctx):
    out = []
    for n, iss in ctx.issues():
        for c in ctx.comments(n):
            b = (c.get("body") or "").lstrip()
            if any(b.startswith(p) for p in COMMENT_PREFIXES):
                continue
            m = PSEUDO_PREFIX.match(b)
            if m:
                out.append(finding(n, "H08", "info",
                           f"unrecognized pseudo-prefix {m.group(1)!r} on "
                           f"comment {c.get('id')} (not a protocol prefix)",
                           objects=[c.get("id")], fix_class="comment"))
    return out


def h09(ctx):
    out = []
    for n, iss in ctx.issues():
        tier = iss.tier
        if tier is None:
            continue
        content = ctx.section_content(n, DOD_HEADING[tier])
        if content is None:
            out.append(finding(n, "H09", "error",
                       "Completion Criteria (Definition of Done) section "
                       "missing", fix_class="body"))
            continue
        boxes = CHECKBOX.findall(content)
        if not boxes:
            out.append(finding(n, "H09", "error",
                       "Completion Criteria carries no markdown checkboxes",
                       fix_class="body"))
            continue
        ticked = [txt or "" for mark, txt in boxes if mark in "xX"]
        if not ticked:
            continue
        cbodies = [re.sub(r"\s+", " ", c.get("body") or "")
                   for c in ctx.comments(n)]
        dod_cited = any("DoD" in cb for cb in cbodies)
        unevidenced = []
        for txt in ticked:
            nt = re.sub(r"\s+", " ", txt).strip()
            if dod_cited or (nt and any(nt in cb for cb in cbodies)):
                continue
            unevidenced.append(txt)
        if unevidenced:
            out.append(finding(n, "H09", "warn",
                       f"{len(unevidenced)} DoD box(es) ticked on an open "
                       "issue with no DoD-evidence comment",
                       objects=[t[:80] for t in unevidenced]))
    return out


CRITERION_ID = re.compile(r"^[A-Z]{1,3}[-–]?\d+\b")
# Canonical heading names across ALL tiers: a task citing its parent
# feature's "§ Global Invariants" is a cross-issue reference the same-body
# check must not flag.
ALL_CANONICAL_HEADINGS = [h for spec in TEMPLATES.values()
                          for h in spec["headings"]]


def h10(ctx):
    """§-reference integrity, calibrated on the real corpus (2026-08-23):
    `§N (<gloss>)` is the sanctioned idiom and the gloss is usually a
    content note, not a section name — so a number-anchored reference
    passes if the numbered heading exists, and only falls back to
    gloss-vs-heading matching when it does not. References preceded by a
    `#N` on the same line cite ANOTHER issue's section and cannot be
    checked against this body. Criterion ids (C1, P1-P5, AC-2 ...) after
    `§` are criterion references, not section names."""
    out = []
    for n, iss in ctx.issues():
        body = ctx.masked(n)          # fences masked; keep inline code, the
        titles = [s[0] for s in ctx.sections(n)]  # names often carry `code`
        body_low = body.lower()

        def cross_issue(pos):
            pre = body[max(0, pos - 48):pos]
            return re.search(r"#\d+[^\n§]{0,40}$", pre) is not None

        def numbered(num):
            return ctx.section_by_num(n, num) is not None

        def resolved(name):
            """A named reference resolves against a same-body heading, a
            canonical heading of any tier (cross-issue citation), or a
            recurring in-body label (bold paragraph labels like
            'Boundary:' or 'Recorded decisions' are anchors too)."""
            if CRITERION_ID.match(name):
                return True             # criterion id, not a section name
            words = [w for w in re.split(r"\s+", name.strip()) if w][:8]
            for k in range(len(words), 0, -1):
                cand = " ".join(words[:k])
                if find_heading(titles, cand)[0] is not None:
                    return True
                if find_heading(ALL_CANONICAL_HEADINGS, cand)[0] is not None:
                    return True
                if body_low.count(cand.lower()) >= 2:
                    return True         # the reference plus its label
            return False

        unresolved, bare = set(), set()
        for m in SEC_REF.finditer(body):
            if m.group("num") and m.group("pname"):
                name = m.group("pname").strip()
                # a path or :line gloss cites a section of a repo document
                # (docs/*.md §2.1 ...), not a section of this body
                doc_ref = re.search(r"[\w./-]+\.\w{1,5}\b|:\d+", name)
                if cross_issue(m.start()) or numbered(m.group("num")) \
                        or doc_ref or resolved(name):
                    continue
                unresolved.add(f"§{m.group('num')} ({name})")
            elif m.group("num"):
                bare.add("§" + m.group("num"))
            else:
                name = m.group("name").strip()
                if cross_issue(m.start()) or resolved(name):
                    continue
                unresolved.add("§ " + name)
        for ref in sorted(unresolved):
            out.append(finding(n, "H10", "error",
                       f"section reference {ref!r} matches no heading in "
                       "this body", objects=[ref], fix_class="body"))
        if bare:
            out.append(finding(n, "H10", "warn",
                       "bare-number section reference(s) violate rule 5: "
                       + ", ".join(sorted(bare)), objects=sorted(bare),
                       fix_class="body"))
    return out


def h11(ctx):
    out = []
    for n, iss in ctx.issues():
        lines = [i for i, line in enumerate(ctx.masked(n).split("\n"), 1)
                 if NA_LINE.match(line)]
        if lines:
            out.append(finding(n, "H11", "warn",
                       f"{len(lines)} 'N/A' without a reason "
                       f"(line(s) {lines[:10]}; write 'N/A — <reason>')",
                       objects=lines, fix_class="body"))
    return out


def h12(ctx):
    out = []
    for n, iss in ctx.issues():
        if iss.tier != "task":
            continue
        content = ctx.section_by_num(n, "7.10")
        if content is None:
            continue                    # missing subsection is H19's finding
        if content.count("$$") % 2:
            out.append(finding(n, "H12", "error",
                       "§7.10 has unbalanced $$ delimiters",
                       fix_class="body"))
            continue
        has_math = bool(MATH_BLOCK.search(content)
                        or MATH_INLINE.search(MATH_BLOCK.sub("", content)))
        if has_math or NA_JUSTIFIED.search(content):
            continue
        out.append(finding(n, "H12", "error",
                   "§7.10 carries no $...$/$$...$$ math and is not a "
                   "justified N/A", fix_class="body"))
    return out


def h13(ctx):
    out = []
    for n, iss in ctx.issues():
        size = len(iss.body)
        if size > 65536:
            out.append(finding(n, "H13", "error",
                       f"body {size} chars exceeds GitHub's 65536 limit",
                       objects=[size], fix_class="body"))
        elif size > 60000:
            out.append(finding(n, "H13", "warn",
                       f"body {size} chars is nearing GitHub's 65536 limit",
                       objects=[size], fix_class="body"))
    return out


def h14(ctx):
    cache = {}

    def committed(path):
        """Exempt path strings that occur committed at meta.head (closed
        verifier blind spot: some leak-shaped strings are legitimate)."""
        if path not in cache:
            if not ctx.head:
                cache[path] = False
            else:
                r = subprocess.run(
                    ["git", "-C", ctx.repo, "grep", "-q", "-F", "-e", path,
                     ctx.head], capture_output=True)
                cache[path] = r.returncode == 0
        return cache[path]

    out = []
    for n, iss in ctx.issues():
        leaks = sorted({m for m in SCRATCHPAD_LEAK.findall(iss.body)})
        leaks = [l for l in leaks if not committed(l)]
        if leaks:
            out.append(finding(n, "H14", "error",
                       "scratchpad path leak(s) in a live body: "
                       + ", ".join(leaks[:5]), objects=leaks,
                       fix_class="body"))
    return out


def h15(ctx):
    vc_script = os.path.join(SCRIPT_DIR, "verify_citations.py")
    resolved = {}

    def commit_ok(sha):
        if sha not in resolved:
            r = subprocess.run(
                ["git", "-C", ctx.repo, "cat-file", "-e", sha + "^{commit}"],
                capture_output=True)
            resolved[sha] = r.returncode == 0
        return resolved[sha]

    out, groups = [], {}
    for n, iss in ctx.issues():
        m = EVIDENCE_SHA.search(iss.machine or "")
        sha = m.group(1) if m else None
        if not sha or not commit_ok(sha):
            out.append(finding(n, "H15", "info",
                       "H15 skipped (G18 covers unresolvable commit)"))
            continue
        groups.setdefault(sha, []).append(n)

    # Batch per evidence_commit so verify_citations.py's blob cache is shared
    # across every body pinned to the same commit.
    with tempfile.TemporaryDirectory(prefix="hygiene-h15-") as td:
        for sha, ns in sorted(groups.items()):
            for i in range(0, len(ns), 100):
                batch = ns[i:i + 100]
                paths = []
                for n in batch:
                    p = os.path.join(td, f"{n}.md")
                    with open(p, "w") as fh:
                        fh.write(ctx.corpus.issues[n].body)
                    paths.append(p)
                r = subprocess.run(
                    [sys.executable, vc_script, ctx.repo, sha] + paths,
                    capture_output=True, text=True)
                try:
                    rep = json.loads(r.stderr.strip().splitlines()[-1])
                except (ValueError, IndexError):
                    out.append(finding(0, "H15", "info",
                               f"H15 batch at {sha[:12]} produced no "
                               f"parsable report ({len(batch)} issues "
                               "unswept)"))
                    continue
                for entry in rep:
                    n = int(os.path.basename(entry["body"]).split(".")[0])
                    c = entry["counts"]
                    out.append(finding(n, "H15", "info",
                               f"citations at {sha[:12]}: "
                               f"quoted={c['OK_QUOTED']} "
                               f"range_only={c['OK_RANGE']} bad={c['BAD']}",
                               objects=[c["OK_QUOTED"], c["OK_RANGE"],
                                        c["BAD"]]))
                    if c["BAD"]:
                        bads = [f"{b['path']}:{b['lines']} — {b['why']}"
                                for b in entry["bad"]]
                        out.append(finding(n, "H15", "error",
                                   f"{c['BAD']} BAD citation(s) at "
                                   f"evidence_commit {sha[:12]}: "
                                   + "; ".join(bads[:5]),
                                   objects=bads[:10], fix_class="body"))
    return out


def h16(ctx):
    vc = ctx.vc()
    if vc is None:
        return [finding(0, "H16", "info",
                        "H16 skipped: verify_citations.py not importable")]
    out = []
    for n, iss in ctx.issues():
        body = iss.body
        cites = sum(1 for m in vc.CITE.finditer(body)
                    if re.findall(r"\d+", m.group("lines")
                                  or m.group("plines") or ""))
        cites += sum(1 for _ in vc.PERMALINK.finditer(body))
        if cites:
            continue
        low = body.lower()
        if any(s in low for s in ABSENCE_SEARCH) \
                and any(p in low for p in ABSENCE_PROOF):
            continue
        out.append(finding(n, "H16", "warn",
                   "zero file:line citations and no absence-proving search "
                   "text (rule 1 requires evidence)", fix_class="body"))
    return out


def h17(ctx):
    out = []
    for n, iss in ctx.issues():
        hits = DASH_PERMALINK.findall(iss.body)
        if hits:
            out.append(finding(n, "H17", "error",
                       f"{len(hits)} dash-joined adjacent line permalink(s) "
                       "— corrupted range link(s); rejoin as a single "
                       "#Lx-Ly permalink", objects=[len(hits)],
                       fix_class="body"))
    return out


def h18(ctx):
    ceiling = max(ctx.corpus.issues) + 30
    out = []
    for n, iss in ctx.issues():
        nums = sorted({int(m.group(1))
                       for m in ISSUE_REF.finditer(ctx.masked_inline(n))})
        fabricated = [x for x in nums
                      if x > ceiling and x not in RETIRED_RESCUE]
        rescue = [x for x in nums if x in RETIRED_RESCUE]
        if fabricated:
            out.append(finding(n, "H18", "error",
                       "likely fabricated issue reference(s): "
                       + ", ".join(f"#{x}" for x in fabricated),
                       objects=fabricated))
        if rescue:
            out.append(finding(n, "H18", "warn",
                       "cites retired tier:meta rescue vehicle (closed "
                       "not-planned): "
                       + ", ".join(f"#{x}" for x in rescue),
                       objects=rescue))
    return out


def h19(ctx):
    tpl = os.path.join(SCRIPT_DIR, "validate-issue-template.py")
    fd, tmp = tempfile.mkstemp(suffix=".json", prefix="hygiene-h19-")
    os.close(fd)
    try:
        r = subprocess.run(
            [sys.executable, tpl, "--all-open", "--snapshot",
             ctx.snapshot_dir, "--json", tmp],
            capture_output=True, text=True)
        if r.returncode not in (0, 1):
            return [finding(0, "H19", "info",
                            f"H19 skipped: template validator exited "
                            f"{r.returncode}")]
        with open(tmp) as fh:
            results = json.load(fh)
    finally:
        os.unlink(tmp)
    out = []
    for res in results:
        if res.get("conforms"):
            continue
        n = res.get("number") or 0
        if ctx.only and n != ctx.only:
            continue
        errs = res.get("errors") or ["nonconforming"]
        out.append(finding(n, "H19", "error", errs[0], objects=errs,
                           fix_class="body"))
    return out


CHECKS = [("H01", h01), ("H02", h02), ("H03", h03), ("H04", h04),
          ("H05", h05), ("H06", h06), ("H07", h07), ("H08", h08),
          ("H09", h09), ("H10", h10), ("H11", h11), ("H12", h12),
          ("H13", h13), ("H14", h14), ("H15", h15), ("H16", h16),
          ("H17", h17), ("H18", h18), ("H19", h19)]


def run(corpus, repo_root=".", only=None, skip=(), snapshot_dir=None,
        last_edited_path=None, timings=None):
    ctx = Ctx(corpus, repo_root, only, snapshot_dir, last_edited_path)
    skip = {s.upper() for s in skip}
    findings = []
    for cid, fn in CHECKS:
        if cid in skip:
            continue
        t0 = time.time()
        findings.extend(fn(ctx))
        if timings is not None:
            timings[cid] = time.time() - t0
    if only:
        findings = [f for f in findings if f["issue"] == only]
    return findings


# --- lastEditedAt fetcher (H06's one GraphQL dependency) --------------------

def fetch_last_edited(snapshot_dir):
    corpus = load_corpus(snapshot_dir)
    repo = (corpus.meta or {}).get("repo") or ""
    if "/" not in repo:
        print("meta.json carries no owner/name `repo` field", file=sys.stderr)
        return 2
    owner, name = repo.split("/", 1)
    numbers = sorted(corpus.issues)
    out = {}
    for i in range(0, len(numbers), 100):
        chunk = numbers[i:i + 100]
        fields = " ".join(f"i{n}: issue(number: {n}) {{ lastEditedAt }}"
                          for n in chunk)
        query = (f'query {{ repository(owner: "{owner}", name: "{name}") '
                 f'{{ {fields} }} }}')
        r = subprocess.run(["gh", "api", "graphql", "-f", f"query={query}"],
                           capture_output=True, text=True)
        if r.returncode != 0:
            print(f"gh api graphql failed: {r.stderr.strip()[:300]}",
                  file=sys.stderr)
            return 2
        data = json.loads(r.stdout).get("data", {}).get("repository") or {}
        for n in chunk:
            out[str(n)] = (data.get(f"i{n}") or {}).get("lastEditedAt")
        print(f"  fetched {min(i + 100, len(numbers))}/{len(numbers)}")
    dest = os.path.join(snapshot_dir, "last_edited.json")
    with open(dest, "w") as fh:
        json.dump(out, fh, indent=0, sort_keys=True)
    print(f"wrote {dest} ({len(out)} issues, "
          f"{sum(1 for v in out.values() if v)} with edits)")
    return 0


def main():
    ap = argparse.ArgumentParser(
        description=__doc__,
        formatter_class=argparse.RawDescriptionHelpFormatter)
    ap.add_argument("--snapshot", required=True,
                    help="snapshot_corpus.py dump directory")
    ap.add_argument("--issue", type=int, help="limit findings to one issue")
    ap.add_argument("--repo-root", default=".",
                    help="local JLS clone (git for H14/H15)")
    ap.add_argument("--json", help="write the findings report here")
    ap.add_argument("--skip", action="append", default=[], metavar="CHECK",
                    help="check id(s) to skip, repeatable or comma-joined "
                         "(e.g. --skip H15)")
    ap.add_argument("--last-edited",
                    help="JSON {number: iso8601-or-null} for H06 (default: "
                         "<snapshot>/last_edited.json)")
    ap.add_argument("--fetch-last-edited", action="store_true",
                    help="fetch body lastEditedAt via batched GraphQL, write "
                         "last_edited.json into the snapshot dir, and exit")
    args = ap.parse_args()

    if not os.path.isdir(args.snapshot):
        print(f"not a snapshot directory: {args.snapshot}", file=sys.stderr)
        sys.exit(2)

    if args.fetch_last_edited:
        sys.exit(fetch_last_edited(args.snapshot))

    try:
        corpus = load_corpus(args.snapshot)
    except (OSError, ValueError) as e:
        print(f"cannot load snapshot: {e}", file=sys.stderr)
        sys.exit(2)

    skip = set()
    for s in args.skip:
        skip.update(x.strip().upper() for x in s.split(",") if x.strip())

    timings = {}
    t0 = time.time()
    findings = run(corpus, args.repo_root, args.issue, skip,
                   args.snapshot, args.last_edited, timings)
    total = time.time() - t0

    for f in findings:
        print(f"{f['severity'].upper():5}  #{f['issue']:<5} {f['check']}  "
              f"{f['message']}")

    print(f"\n{'check':6} {'error':>6} {'warn':>6} {'info':>6} {'secs':>8}")
    for cid, _fn in CHECKS:
        if cid in skip:
            print(f"{cid:6} {'-':>6} {'-':>6} {'-':>6}  skipped")
            continue
        by = {"error": 0, "warn": 0, "info": 0}
        for f in findings:
            if f["check"] == cid:
                by[f["severity"]] += 1
        print(f"{cid:6} {by['error']:6} {by['warn']:6} {by['info']:6} "
              f"{timings.get(cid, 0.0):8.2f}")
    errs = [f for f in findings if f["severity"] == "error"]
    print(f"\n{len(findings)} findings ({len(errs)} errors) across "
          f"{len({f['issue'] for f in findings})} issues in {total:.1f}s")

    if args.json:
        with open(args.json, "w") as fh:
            json.dump(findings_report(VALIDATOR, CHECK_VERSION, args.snapshot,
                                      corpus.meta, findings), fh, indent=1)
        print(f"wrote {args.json}")

    sys.exit(1 if errs else 0)


if __name__ == "__main__":
    main()
