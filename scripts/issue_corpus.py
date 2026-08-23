#!/usr/bin/env python3
"""Shared parsing library for JLS issue-quality tooling.

Single home for the corpus model and the parsing rules the validators share:
template specs, machine-block extraction, heading matching, tier resolution,
and the snapshot loader. validate-issue-template.py imports from here; so do
validate_issue_graph.py, validate_hygiene.py, and validate_board.py. The
heading and yaml-key logic was extracted verbatim from
validate-issue-template.py — it encodes lessons from a 679-issue audit
(2026-08-17) and seven verifier blind spots closed since; never fork it.

A snapshot directory (produced by snapshot_corpus.py) contains:
  issues.jsonl      one REST issue object per line (PRs already filtered out)
  closed.jsonl      one trimmed closed-issue record per line (number, title,
                    state_reason, labels, updated_at)
  comments.jsonl    one REST issue-comment object per line, repo-wide
  subissues.jsonl   one {"parent": N, "children": [M, ...]} per line
  board.jsonl       one {"issue": N, "status": "...", "item_id": "..."} per line
                    (issue is null for draft/PR items; extra fields allowed)
  milestones.json   REST milestone list
  labels.json       REST label list
  meta.json         {fetched_at, head, rate_limit, index: {number: {updated_at,
                    body_sha256}}}
"""
import hashlib
import json
import os
import re

# --- Template specs (source of truth: .github/ISSUE_TEMPLATE/*.md) ---------

TEMPLATES = {
    "task": {
        "version": "scientific-task v6",
        "file": "scientific_task.md",
        "label": "tier:task",
        "yaml_keys": ["tier", "evidence_commit", "part_of_feature",
                      "blocked_by", "blocks", "related"],
        "headings": [
            "Abstract", "Intended Audience & Impact", "Status & Dependencies",
            "1. Background & Prior Work", "2. Observations",
            "3. Research Question", "4. Hypothesis (falsifiable)",
            "5. Predictions", "6. Materials & Apparatus",
            "7. Interface & Data Contract", "8. Method / Experimental Design",
            "9. Data Collection & Analysis", "10. Falsification Criteria",
            "11. Threats to Validity", "12. Related Work",
            "13. Conclusion & Future Work", "Open Questions & Decisions Needed",
            "14. Completion Criteria (Definition of Done)",
        ],
        "subheadings": [f"7.{n}" for n in range(1, 13)],
    },
    "feature": {
        "version": "feature v3",
        "file": "feature.md",
        "label": "tier:feature",
        "yaml_keys": ["tier", "evidence_commit", "requires_tasks",
                      "planned_tasks", "blocked_by", "blocks",
                      "serves_capstones", "related"],
        "headings": [
            "Abstract", "Intended Audience & Impact",
            "Status & Dependency Graph",
            "1. Capability Statement & Scope Boundary",
            "2. Decomposition & Rationale",
            "3. Feature-Level Interface & Data Contract",
            "4. Global Invariants", "5. Integration Criteria & Evidence Plan",
            "6. Sequencing & Parallelism", "7. Re-planning Protocol",
            "Open Questions & Decisions Needed",
            "Completion Criteria (Definition of Done)",
        ],
        "subheadings": [],
    },
    "capstone": {
        "version": "capstone v3",
        "file": "capstone.md",
        "label": "tier:capstone",
        "yaml_keys": ["tier", "evidence_commit", "requires_features",
                      "requires_capstones", "requires_tasks_exception",
                      "planned_features", "blocked_by", "blocks", "related"],
        "headings": [
            "Abstract", "Intended Audience & Impact",
            "Status & Required Features", "1. Outcome Statement",
            "2. Required Feature Set & Sufficiency",
            "3. Cross-Feature Integration Risks",
            "4. System-Level Acceptance Criteria", "5. Re-planning Protocol",
            "Open Questions & Decisions Needed",
            "Completion Criteria (Definition of Done)",
        ],
        "subheadings": [],
    },
}

BANNED_YAML_KEYS = ("task_id", "band_mw", "ordering_after")

# Machine-block edge fields, by the direction template rules assign them.
EDGE_FIELDS = {
    "task": {"composition": ["part_of_feature"],
             "ordering": ["blocked_by", "blocks"],
             "soft": ["related"]},
    "feature": {"composition": ["requires_tasks", "serves_capstones"],
                "planned": ["planned_tasks"],
                "ordering": ["blocked_by", "blocks"],
                "soft": ["related"]},
    "capstone": {"composition": ["requires_features", "requires_capstones",
                                 "requires_tasks_exception"],
                 "planned": ["planned_features"],
                 "ordering": ["blocked_by", "blocks"],
                 "soft": ["related"]},
}

# Comment prefixes the templates recognize (task rule 9 / feature rule C).
COMMENT_PREFIXES = ("STATUS:", "REFUTED:", "HANDOFF:", "SUPERSEDED:",
                    "AMENDED:", "REPLAN:", "WAIVED:")

YAML_BLOCK = re.compile(r"```ya?ml\s*\n(.*?)```", re.S)
HEADING = re.compile(r"^#{1,4}\s*(.+?)\s*$", re.M)
MERMAID_BLOCK = re.compile(r"```mermaid\s*\n(.*?)```", re.S)
STOP = {"and", "the", "of", "a", "to", "for", "in", "on", "its", "definition"}


def norm(s):
    s = s.lower().replace("&", "and")
    s = re.sub(r"[^a-z0-9. ]+", " ", s)
    return re.sub(r"\s+", " ", s).strip()


def sig_tokens(title):
    """Significant words, in order, with the leading section number stripped."""
    t = re.sub(r"^\s*\d+(\.\d+)*\.?\s*", "", norm(title))
    return [w for w in t.split() if w and w not in STOP]


def sig(title):
    return set(sig_tokens(title))


def yaml_key_state(block, key):
    """'absent' | 'empty' | 'populated', honouring block-style lists."""
    lines = block.split("\n")
    for i, line in enumerate(lines):
        m = re.match(rf"^{re.escape(key)}\s*:(.*)$", line)
        if not m:
            continue
        if re.sub(r"#.*$", "", m.group(1)).strip():
            return "populated"
        for nxt in lines[i + 1:]:
            if not nxt.strip():
                continue
            if not re.match(r"^\s+", nxt):
                break
            if re.sub(r"#.*$", "", nxt).strip():
                return "populated"
        return "empty"
    return "absent"


def find_heading(headings, canonical):
    """(actual, drifted) if the section is present, else (None, False).

    A drift match needs BOTH half the canonical's significant words AND the
    canonical's LEADING significant word (compared on a 6-char stem, so
    "Hypothesis"/"Hypotheses" match). Overlap alone is too loose: it lets
    "Acceptance criteria" stand in for "10. Falsification Criteria" on the
    shared word "criteria", which would suppress a real missing-section error.
    """
    canon_n = norm(canonical)
    for raw in headings:
        if norm(raw) == canon_n:
            return raw, False

    want_tokens = sig_tokens(canonical)
    if not want_tokens:
        return None, False
    want, lead = set(want_tokens), want_tokens[0][:6]

    best, best_score = None, 0.0
    for raw in headings:
        have = sig_tokens(raw)
        if not any(w.startswith(lead) or lead.startswith(w[:6]) for w in have):
            continue
        score = len(want & set(have)) / len(want)
        if score > best_score:
            best, best_score = raw, score
    return (best, True) if best_score >= 0.5 else (None, False)


# --- Machine block ----------------------------------------------------------

def machine_block(body):
    """The first ```yaml block carrying a `tier:` key, or None."""
    for b in YAML_BLOCK.findall(body or ""):
        if re.search(r"^tier\s*:", b, re.M):
            return b
    return None


def declared_tier(body):
    """The `tier:` value from the machine block, or None."""
    block = machine_block(body)
    if not block:
        return None
    m = re.search(r"^tier\s*:(.*)$", block, re.M)
    return re.sub(r"#.*$", "", m.group(1)).strip() if m else None


def resolve_tier(body, labels, forced=None):
    """Tier per template rule 8: machine block first, single tier:* label
    as fallback. Returns a TEMPLATES key or None."""
    if forced:
        return forced
    declared = declared_tier(body)
    if declared in TEMPLATES:
        return declared
    tier_labels = {l for l in (labels or []) if l.startswith("tier:")}
    if len(tier_labels) == 1:
        cand = next(iter(tier_labels)).split(":", 1)[1]
        if cand in TEMPLATES:
            return cand
    return None


_ISSUE_REF = re.compile(r"^#?(\d+)$")


def strip_yaml_comment(s):
    """Drop a trailing `# comment`, but never a `#` inside a quoted span —
    `- "#558"  # note` must yield `- "#558"`, not `- "`."""
    out, in_q = [], None
    for ch in s:
        if in_q:
            out.append(ch)
            if ch == in_q:
                in_q = None
            continue
        if ch in ("'", '"'):
            in_q = ch
        elif ch == "#":
            break
        out.append(ch)
    return "".join(out)


def parse_edge_field(block, key):
    """Parse one machine-block edge field into issue numbers.

    Returns {"state": absent|empty|populated, "numbers": [int, ...],
             "none": bool, "bad": [raw, ...], "hash_form": [raw, ...]}.
    Accepts inline lists (`[1, 2]`), inline lists WRAPPED across following
    lines (seen in live capstone blocks), block lists (`- 1` / `- "#558"`),
    bare scalars, and the literal `none`. Numeric-with-`#` entries parse
    into `numbers` and are additionally reported in `hash_form` (a style
    deviation); non-numeric entries land in `bad` (a real defect).
    """
    out = {"state": yaml_key_state(block, key), "numbers": [], "none": False,
           "bad": [], "hash_form": []}
    if out["state"] != "populated":
        return out

    lines = block.split("\n")
    raw_items = []
    for i, line in enumerate(lines):
        m = re.match(rf"^{re.escape(key)}\s*:(.*)$", line)
        if not m:
            continue
        inline = strip_yaml_comment(m.group(1)).strip()
        if inline.startswith("[") and not inline.endswith("]"):
            # inline list wrapped across continuation lines
            for nxt in lines[i + 1:]:
                if not re.match(r"^\s+", nxt):
                    break
                inline += " " + strip_yaml_comment(nxt).strip()
                if inline.rstrip().endswith("]"):
                    break
            inline = inline.strip()
        if inline:
            if inline.startswith("[") and inline.endswith("]"):
                raw_items += [x.strip() for x in inline[1:-1].split(",")
                              if x.strip()]
            else:
                raw_items.append(inline)
        else:
            for nxt in lines[i + 1:]:
                if not nxt.strip():
                    continue
                if not re.match(r"^\s+", nxt):
                    break
                item = strip_yaml_comment(nxt).strip()
                if item.startswith("-"):
                    item = item[1:].strip()
                if item:
                    raw_items.append(item)
        break

    for raw in raw_items:
        raw = raw.strip().strip('"').strip("'")
        if not raw:
            continue
        if raw.lower() in ("none", "null", "[]", "~"):
            out["none"] = True
            continue
        m = _ISSUE_REF.match(raw)
        if m:
            out["numbers"].append(int(m.group(1)))
            if raw.startswith("#"):
                out["hash_form"].append(raw)
        else:
            out["bad"].append(raw)
    return out


def edge_fields(tier):
    """All machine-block fields of `tier` that name other issues."""
    groups = EDGE_FIELDS.get(tier, {})
    return [k for ks in groups.values() for k in ks]


# --- Snapshot model ---------------------------------------------------------

class Issue:
    __slots__ = ("number", "title", "body", "labels", "milestone", "state",
                 "state_reason", "updated_at", "raw")

    def __init__(self, d):
        self.number = d["number"]
        self.title = d.get("title") or ""
        self.body = d.get("body") or ""
        self.labels = [l["name"] if isinstance(l, dict) else l
                       for l in d.get("labels") or []]
        ms = d.get("milestone")
        self.milestone = ms.get("title") if isinstance(ms, dict) else ms
        self.state = d.get("state", "open")
        self.state_reason = d.get("state_reason")
        self.updated_at = d.get("updated_at")
        self.raw = d

    @property
    def tier(self):
        return resolve_tier(self.body, self.labels)

    @property
    def machine(self):
        return machine_block(self.body)

    @property
    def body_sha256(self):
        return hashlib.sha256(self.body.encode()).hexdigest()


class Corpus:
    def __init__(self, snapshot_dir):
        self.dir = snapshot_dir
        self.issues = {}          # number -> Issue (open issues only)
        self.closed = {}          # number -> {title, state_reason, labels, ...}
        self.comments = {}        # issue number -> [comment dict, ...]
        self.children = {}        # parent number -> [child numbers]
        self.parent = {}          # child number -> parent number
        self.board = []           # [{"issue": N|None, "status": ...}, ...]
        self.milestones = []
        self.labels = []
        self.meta = {}

        with open(os.path.join(snapshot_dir, "issues.jsonl")) as fh:
            for line in fh:
                iss = Issue(json.loads(line))
                self.issues[iss.number] = iss

        p = os.path.join(snapshot_dir, "closed.jsonl")
        if os.path.exists(p):
            with open(p) as fh:
                for line in fh:
                    d = json.loads(line)
                    self.closed[d["number"]] = d

        p = os.path.join(snapshot_dir, "comments.jsonl")
        if os.path.exists(p):
            with open(p) as fh:
                for line in fh:
                    c = json.loads(line)
                    m = re.search(r"/issues/(\d+)$", c.get("issue_url") or "")
                    if m:
                        self.comments.setdefault(int(m.group(1)), []).append(c)

        p = os.path.join(snapshot_dir, "subissues.jsonl")
        if os.path.exists(p):
            with open(p) as fh:
                for line in fh:
                    d = json.loads(line)
                    self.children[d["parent"]] = d["children"]
                    for ch in d["children"]:
                        self.parent[ch] = d["parent"]

        p = os.path.join(snapshot_dir, "board.jsonl")
        if os.path.exists(p):
            with open(p) as fh:
                self.board = [json.loads(line) for line in fh]

        for name in ("milestones", "labels", "meta"):
            p = os.path.join(snapshot_dir, f"{name}.json")
            if os.path.exists(p):
                with open(p) as fh:
                    setattr(self, name, json.load(fh))


def load_corpus(snapshot_dir):
    return Corpus(snapshot_dir)


# --- Findings ---------------------------------------------------------------

def fingerprint(check, issue, objects):
    """Stable dedupe key: check id + issue + normalized finding objects."""
    norm_obj = json.dumps(sorted(objects, key=str), separators=(",", ":"))
    return hashlib.sha256(f"{check}|{issue}|{norm_obj}".encode()).hexdigest()


def finding(issue, check, severity, message, objects=None, fix_class="adjudicate"):
    objects = objects if objects is not None else []
    return {"issue": issue, "check": check, "severity": severity,
            "message": message, "objects": objects,
            "fingerprint": fingerprint(check, issue, objects),
            "fix_class": fix_class}


def findings_report(validator, check_version, snapshot_dir, meta, findings_list):
    return {"schema": 1, "validator": validator, "check_version": check_version,
            "snapshot": {"path": snapshot_dir,
                         "fetched_at": meta.get("fetched_at"),
                         "head": meta.get("head")},
            "findings": findings_list}
