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
  # sweep offline from a snapshot_corpus.py dump (no gh calls)
  scripts/validate-issue-template.py --all-open --snapshot DIR

Exit status:
  0  every validated issue conforms
  1  at least one issue does not conform
  2  usage or environment error

Parsing rules live in scripts/issue_corpus.py, shared with the graph, hygiene,
and board validators. Design notes, both learned from a 679-issue audit on
2026-08-17:
  - A YAML key populated by an indented block-style list on FOLLOWING lines is
    populated. Reading only the same line reports false emptiness.
  - A heading may carry a suffix or a plural ("4. Hypotheses (falsifiable)",
    "1. Background & Prior Work (current state, landed at 2eb3e0c)") and is
    still that section. Matching is by title similarity, never by section
    number alone -- "2. Engine Decision" is NOT "2. Decomposition & Rationale".
"""
import argparse, json, re, subprocess, sys

from issue_corpus import (TEMPLATES, BANNED_YAML_KEYS, YAML_BLOCK, HEADING,
                          find_heading, load_corpus, yaml_key_state)


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
    ap.add_argument("--snapshot",
                    help="with --all-open: read issues from this snapshot "
                         "directory instead of calling gh")
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
    elif args.snapshot:
        corpus = load_corpus(args.snapshot)
        for iss in sorted(corpus.issues.values(), key=lambda i: i.number):
            results.append(validate(iss.number, iss.body, iss.labels, None))
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
