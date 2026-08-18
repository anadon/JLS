#!/usr/bin/env python3
"""Validate JLS issues against their tier issue template.

The three templates in .github/ISSUE_TEMPLATE/ are the only sanctioned issue
shapes. The compact `task_id` / `band_mw` / `ordering_after` form is NOT a
shortcut and never was; it is reported as a hard failure.

Tier identity comes from the ```yaml machine block's `tier:` key — the templates
call that block the source of truth for graph assembly — cross-checked against
the `tier:*` label.

Usage:
  # one issue, fetched via gh (what CI runs)
  scripts/validate-issue-template.py --issue 960
  # a body already on disk
  scripts/validate-issue-template.py --body-file /tmp/body.md --tier task
  # sweep every open issue
  scripts/validate-issue-template.py --all-open [--json report.json]

Exit status:
  0  every validated issue conforms
  1  at least one issue does not conform
  2  usage or environment error

Design notes, both learned from a 679-issue audit on 2026-08-17:
  - A YAML key populated by an indented block-style list on FOLLOWING lines is
    populated. Reading only the same line reports false emptiness.
  - A heading may carry a suffix or a plural ("4. Hypotheses (falsifiable)",
    "1. Background & Prior Work (current state, landed at 2eb3e0c)") and is
    still that section. Matching is by title similarity, never by section
    number alone -- "2. Engine Decision" is NOT "2. Decomposition & Rationale".
"""
import argparse, json, re, subprocess, sys

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

YAML_BLOCK = re.compile(r"```ya?ml\s*\n(.*?)```", re.S)
HEADING = re.compile(r"^#{1,4}\s*(.+?)\s*$", re.M)
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


def validate(number, body, labels, forced_tier=None):
    body = body or ""
    labels = set(labels or [])
    tier_labels = {l for l in labels if l.startswith("tier:")}

    blocks = YAML_BLOCK.findall(body)
    machine = next((b for b in blocks if re.search(r"^tier\s*:", b, re.M)), None)
    declared = None
    if machine:
        m = re.search(r"^tier\s*:(.*)$", machine, re.M)
        declared = re.sub(r"#.*$", "", m.group(1)).strip() if m else None

    tier = forced_tier or (declared if declared in TEMPLATES else None)
    if tier is None and len(tier_labels) == 1:
        cand = next(iter(tier_labels)).split(":", 1)[1]
        tier = cand if cand in TEMPLATES else None

    errors, warnings = [], []

    banned = [k for k in BANNED_YAML_KEYS
              if re.search(rf"^{k}\s*:", body, re.M)]
    if banned:
        errors.append(
            f"compact issue form detected (yaml keys: {', '.join(banned)}). "
            "The compact form is not a sanctioned shape -- migrate this issue to "
            "the full tier template."
        )

    if tier is None:
        errors.append(
            "tier undeterminable: no `tier:` key in a ```yaml machine block and "
            "no single `tier:*` label. Cannot pick a template to validate against."
        )
        return {"number": number, "tier": None, "template": None,
                "conforms": False, "errors": errors, "warnings": warnings}

    spec = TEMPLATES[tier]
    headings = HEADING.findall(body)

    missing = []
    for canonical in spec["headings"]:
        found, drifted = find_heading(headings, canonical)
        if found is None:
            missing.append(canonical)
        elif drifted:
            warnings.append(f"heading title drift: '{canonical}' -> '{found}'")
    if missing:
        errors.append(f"missing required section(s): {'; '.join(missing)}")

    missing_sub = [s for s in spec["subheadings"]
                   if not re.search(rf"^#{{1,4}}\s*{re.escape(s)}(?!\d)", body, re.M)]
    if missing_sub:
        errors.append(
            f"missing §7 subsection(s): {', '.join(missing_sub)} "
            "(each is required; use 'N/A — <reason>' if genuinely inapplicable)"
        )

    if machine is None:
        errors.append("no ```yaml machine block carrying `tier:`")
    else:
        absent, empty = [], []
        for key in spec["yaml_keys"]:
            st = yaml_key_state(machine, key)
            if st == "absent":
                absent.append(key)
            elif st == "empty":
                empty.append(key)
        if absent:
            errors.append(f"machine block missing key(s): {', '.join(absent)}")
        if empty:
            errors.append(f"machine block key(s) unpopulated: {', '.join(empty)}")

    if spec["label"] not in labels:
        errors.append(f"missing tier label `{spec['label']}`")
    if declared and declared != tier:
        errors.append(f"`tier: {declared}` in yaml disagrees with label `{tier}`")
    if len(tier_labels) > 1:
        errors.append(f"multiple tier labels: {', '.join(sorted(tier_labels))}")
    if not (labels & {"bug", "enhancement"}):
        errors.append("missing kind label: set `bug` or `enhancement`")

    return {"number": number, "tier": tier, "template": spec["version"],
            "conforms": not errors, "errors": errors, "warnings": warnings}


def gh_json(args):
    r = subprocess.run(["gh"] + args, capture_output=True, text=True)
    if r.returncode != 0:
        print(f"gh failed: {r.stderr.strip()}", file=sys.stderr)
        sys.exit(2)
    return json.loads(r.stdout)


def render(res):
    head = f"issue #{res['number']}" if res["number"] else "body"
    if res["conforms"]:
        out = [f"PASS  {head}  ({res['template']})"]
    else:
        out = [f"FAIL  {head}  ({res['template'] or 'no template'})"]
        out += [f"        - {e}" for e in res["errors"]]
    out += [f"        ~ {w}" for w in res["warnings"]]
    return "\n".join(out)


def main():
    ap = argparse.ArgumentParser(description=__doc__,
                                 formatter_class=argparse.RawDescriptionHelpFormatter)
    g = ap.add_mutually_exclusive_group(required=True)
    g.add_argument("--issue", type=int, help="issue number to fetch and validate")
    g.add_argument("--body-file", help="validate a body already on disk")
    g.add_argument("--all-open", action="store_true", help="sweep every open issue")
    ap.add_argument("--tier", choices=sorted(TEMPLATES), help="force the tier")
    ap.add_argument("--label", action="append", default=[],
                    help="label to assume with --body-file (repeatable)")
    ap.add_argument("--json", help="write the full report to this path")
    ap.add_argument("--limit", type=int, default=2000)
    args = ap.parse_args()

    results = []
    if args.issue:
        d = gh_json(["issue", "view", str(args.issue), "--json",
                     "number,body,labels"])
        results.append(validate(d["number"], d["body"],
                                [l["name"] for l in d["labels"]], args.tier))
    elif args.body_file:
        results.append(validate(None, open(args.body_file).read(),
                                args.label, args.tier))
    else:
        for d in gh_json(["issue", "list", "--state", "open", "--limit",
                          str(args.limit), "--json", "number,body,labels"]):
            results.append(validate(d["number"], d["body"],
                                    [l["name"] for l in d["labels"]], None))

    for res in results:
        print(render(res))

    bad = [r for r in results if not r["conforms"]]
    if len(results) > 1:
        print(f"\n{len(results) - len(bad)}/{len(results)} conforming; "
              f"{len(bad)} need migration")
    if args.json:
        with open(args.json, "w") as fh:
            json.dump(results, fh, indent=1)

    sys.exit(1 if bad else 0)


if __name__ == "__main__":
    main()
