#!/usr/bin/env python3
"""Validate the "JLS Roadmap" GitHub-Project board against the issue corpus.

Runs entirely offline from a snapshot_corpus.py dump; never talks to GitHub.

Status semantics are now RATIFIED in docs/board-status-ruling.md, so B03 and
B05 accuse rather than merely report, and B07 (the standing
"semantics-unwritten" marker) is retired. Milestone policy is still unwritten,
so B08 remains info-only. Ready/Blocked are DERIVED from the corpus by
scripts/derive_board_status.py; In Progress and Done are asserted by a human
or a workflow and are never derived. B10 reports where the board disagrees
with the derivation — on disagreement the board is what is wrong.

Checks:
  B01  every open issue is a board item          (error, fix: sync-roadmap-project.sh)
  B02  CLOSED-content item not in Done           (warn) + corpus-level delta breakdown
  B03  Status=Blocked <-> open blocked_by        (warn; ruling ratified)
  B04  open board item with no Status            (warn, no default column ruled)
  B05  In Progress, unassigned, no fresh STATUS: (warn; ruling ratified)
  B06  duplicate board items for one issue       (error)
  B07  RETIRED by the ruling; now reports the Status distribution only (info)
  B10  board Status disagrees with the derived value (warn)
  B08  milestone coverage/distribution           (info until milestone ruling)
  B09  label-taxonomy coverage: orphan labels, issues without area:* (info)

Corpus-level findings (B02 delta, B07, B08, B09) are reported on issue 0.

Usage:
  scripts/validate_board.py --snapshot DIR [--issue N] [--json OUT]

Exit status:
  0  no error-severity findings
  1  at least one error-severity finding
  2  usage or environment error (snapshot missing/unreadable)

Parsing rules live in scripts/issue_corpus.py, shared with the template,
graph, and hygiene validators — never fork them.
"""
import argparse
import json
import os
import sys
import time
from collections import Counter
from datetime import datetime, timedelta

from issue_corpus import (finding, findings_report, load_corpus,
                          parse_edge_field)

CHECK_VERSION = "b-v2"
STATUS_STALENESS_DAYS = 14
ORPHAN_LIST_CAP = 20


def parse_ts(s):
    """ISO-8601 with trailing Z -> aware datetime, or None."""
    if not s:
        return None
    try:
        return datetime.fromisoformat(s.replace("Z", "+00:00"))
    except ValueError:
        return None


def clip(items, cap=ORPHAN_LIST_CAP):
    items = [str(x) for x in items]
    head = ", ".join(items[:cap])
    if len(items) > cap:
        head += f" +{len(items) - cap} more"
    return head


def board_issue_items(corpus):
    """Board items that reference an issue number (drafts excluded)."""
    return [it for it in corpus.board if it.get("issue") is not None]


# --- Checks -----------------------------------------------------------------

def check_b01(corpus):
    """Every open issue must be on the board; sync-roadmap-project.sh adds it."""
    on_board = {it["issue"] for it in board_issue_items(corpus)}
    out = []
    for n in sorted(set(corpus.issues) - on_board):
        out.append(finding(
            n, "B01", "error",
            f"open issue #{n} has no item on the JLS Roadmap board "
            "(repair: scripts/sync-roadmap-project.sh)",
            objects=[n], fix_class="auto"))
    return out


def check_b02(corpus):
    """CLOSED-content items not in Done, plus the item-vs-open-issue delta."""
    out = []
    delta = Counter()
    for it in corpus.board:
        n, typ = it.get("issue"), it.get("type")
        if n is not None and it.get("state") == "CLOSED" \
                and it.get("status") != "Done":
            out.append(finding(
                n, "B02", "warn",
                f"board item for closed issue #{n} has "
                f"Status={it.get('status')!r}, expected Done",
                objects=[it.get("item_id"), it.get("status")],
                fix_class="adjudicate"))
        if n is not None and n in corpus.issues:
            continue  # backed by an open issue: not part of the delta
        if typ == "DRAFT_ISSUE" or n is None:
            delta["draft items"] += 1
        elif typ == "PULL_REQUEST":
            delta["PRs"] += 1
        elif it.get("state") == "CLOSED":
            delta["closed issues"] += 1
        else:
            delta["unknown"] += 1
    breakdown = ", ".join(
        f"{delta[k]} {k}" for k in
        ("closed issues", "PRs", "draft items", "unknown"))
    out.append(finding(
        0, "B02", "info",
        f"board carries {len(corpus.board)} items vs {len(corpus.issues)} "
        f"open issues (delta {len(corpus.board) - len(corpus.issues)}): "
        f"{breakdown}",
        objects=[len(corpus.board), len(corpus.issues), dict(delta)]))
    return out


def check_b03(corpus):
    """Blocked <-> open blocked_by coherence. Info-only until Status ruling."""
    out = []
    for it in board_issue_items(corpus):
        iss = corpus.issues.get(it["issue"])
        if iss is None:
            continue  # closed content: B02's business
        status = it.get("status")
        if status not in ("Blocked", "Ready", "In Progress"):
            continue
        block = iss.machine
        numbers = parse_edge_field(block, "blocked_by")["numbers"] if block else []
        open_blockers = sorted(b for b in set(numbers) if b in corpus.issues)
        if status == "Blocked" and not open_blockers:
            out.append(finding(
                iss.number, "B03", "warn",
                f"Status=Blocked but machine-block blocked_by "
                f"({numbers or 'none'}) lists no OPEN issue",
                objects=["blocked-without-open-blocker", numbers]))
        elif status in ("Ready", "In Progress") and open_blockers:
            out.append(finding(
                iss.number, "B03", "warn",
                f"Status={status} while blocked_by lists open issue(s) "
                f"{open_blockers}",
                objects=["open-blocker-not-blocked", status, open_blockers]))
    return out


def check_b04(corpus):
    """Open issue on the board with no Status. No default column is ruled."""
    out = []
    for it in board_issue_items(corpus):
        if it["issue"] in corpus.issues and not it.get("status"):
            out.append(finding(
                it["issue"], "B04", "warn",
                f"open issue #{it['issue']} is on the board with no Status "
                "(no default column ruled yet)",
                objects=[it.get("item_id")], fix_class="adjudicate"))
    return out


def check_b05(corpus):
    """In Progress with nobody assigned and no fresh STATUS: comment."""
    fetched = parse_ts(corpus.meta.get("fetched_at"))
    if fetched is None:
        return []  # cannot judge staleness without the snapshot timestamp
    cutoff = fetched - timedelta(days=STATUS_STALENESS_DAYS)
    out = []
    for it in board_issue_items(corpus):
        iss = corpus.issues.get(it["issue"])
        if iss is None or it.get("status") != "In Progress":
            continue
        assignees = iss.raw.get("assignees") or []
        if not assignees and iss.raw.get("assignee"):
            assignees = [iss.raw["assignee"]]
        if assignees:
            continue
        fresh = False
        for c in corpus.comments.get(iss.number, []):
            if not (c.get("body") or "").startswith("STATUS:"):
                continue
            ts = parse_ts(c.get("created_at"))
            if ts and ts >= cutoff:
                fresh = True
                break
        if not fresh:
            out.append(finding(
                iss.number, "B05", "warn",
                f"Status=In Progress but no assignee and no STATUS: comment "
                f"in the last {STATUS_STALENESS_DAYS} days "
                f"(as of {corpus.meta.get('fetched_at')})",
                objects=["in-progress-stale"]))
    return out


def check_b06(corpus):
    """One board item per issue number."""
    counts = Counter(it["issue"] for it in board_issue_items(corpus))
    out = []
    for n, cnt in sorted(counts.items()):
        if cnt < 2:
            continue
        item_ids = sorted(it.get("item_id") or "?"
                          for it in board_issue_items(corpus)
                          if it["issue"] == n)
        out.append(finding(
            n, "B06", "error",
            f"issue #{n} has {cnt} board items ({clip(item_ids)})",
            objects=item_ids, fix_class="auto"))
    return out


def check_b07(corpus):
    """Standing marker: Status semantics unwritten; ruling pending."""
    dist = Counter(it.get("status") or "(none)" for it in corpus.board)
    shown = ", ".join(f"{k}={v}" for k, v in
                      sorted(dist.items(), key=lambda kv: (-kv[1], kv[0])))
    return [finding(
        0, "B07", "info",
        "Status distribution over "
        f"{len(corpus.board)} items: {shown}. (The former "
        "'semantics unwritten' marker is retired: the ruling is ratified in "
        "docs/board-status-ruling.md and B03/B05 now accuse.)",
        objects=[dict(dist)])]


def check_b10(corpus, findings_paths):
    """Board Status vs the value derived from the corpus.

    The ruling (docs/board-status-ruling.md) makes Ready/Blocked a FUNCTION of
    the corpus, so a board that disagrees is the thing that is wrong. The
    derivation lives in derive_board_status.py and is imported rather than
    reimplemented — two copies of a rule this load-bearing would drift.

    In Progress and Done are asserted, never derived, so they are skipped.
    Without --findings only the corpus-derivable clauses apply (open
    blocked_by, non-empty planned_*), which under-reports rather than
    over-reports: it can miss a Blocked, never invent one.
    """
    try:
        from derive_board_status import derive
    except Exception:
        return []
    error_issues, cycle_issues = {}, set()
    for path in findings_paths or []:
        try:
            data = json.load(open(path))
        except Exception:
            continue
        for f in data.get("findings", []):
            if f.get("check") == "G04":
                cycle_issues.add(f.get("issue"))
                cycle_issues.update(x for x in f.get("objects", [])
                                    if isinstance(x, int))
            if f.get("severity") == "error" and f.get("issue"):
                error_issues.setdefault(f["issue"], set()).add(f["check"])

    want = derive(corpus, error_issues, cycle_issues)
    out = []
    for it in corpus.board:
        n = it.get("issue")
        if not n or n not in corpus.issues:
            continue
        cur = it.get("status")
        if cur in ("In Progress", "Done"):
            continue
        exp, why = want.get(n, (None, ""))
        if exp and cur != exp:
            out.append(finding(
                n, "B10", "warn",
                f"board Status={cur or '<none>'} but the corpus derives "
                f"{exp}" + (f" ({why[:150]})" if why else "")
                + " — Status is derived; run derive_board_status.py --apply",
                objects=["status-drift", cur, exp], fix_class="auto"))
    return out


def check_b08(corpus):
    """Milestone coverage report. Info-only until the milestone ruling."""
    per_ms = Counter(i.milestone for i in corpus.issues.values() if i.milestone)
    carried = sum(per_ms.values())
    parts = []
    known = set()
    for ms in corpus.milestones:
        title = ms.get("title")
        known.add(title)
        parts.append(f"{title!r} ({ms.get('state')}): {per_ms.get(title, 0)}")
    for title in sorted(set(per_ms) - known):
        parts.append(f"{title!r} (not in milestones.json): {per_ms[title]}")
    return [finding(
        0, "B08", "info",
        f"milestone coverage: {carried} of {len(corpus.issues)} open issues "
        f"carry a milestone; per-milestone open counts: "
        f"{'; '.join(parts) or 'no milestones defined'}",
        objects=[carried, len(corpus.issues), dict(per_ms)])]


def check_b09(corpus):
    """Label-taxonomy coverage: orphan labels and missing area:* labels."""
    attach = Counter()
    for iss in corpus.issues.values():
        attach.update(set(iss.labels))
    defined = sorted(l["name"] if isinstance(l, dict) else l
                     for l in corpus.labels)
    orphans = [name for name in defined if attach.get(name, 0) < 2]
    out = [finding(
        0, "B09", "info",
        f"{len(orphans)} of {len(defined)} defined labels are attached to "
        f"fewer than 2 open issues: {clip(orphans) or '(none)'}",
        objects=["orphan-labels", orphans])]
    no_area = sorted(n for n, iss in corpus.issues.items()
                     if not any(l.startswith("area:") for l in iss.labels))
    out.append(finding(
        0, "B09", "info",
        f"{len(no_area)} of {len(corpus.issues)} open issues carry no "
        f"area:* label" + (f": {clip(no_area)}" if no_area else ""),
        objects=["no-area-label", len(no_area)]))
    return out


CHECKS = (check_b01, check_b02, check_b03, check_b04, check_b05,
          check_b06, check_b07, check_b08, check_b09)


def run_checks(corpus, findings_paths=None):
    out = []
    for check in CHECKS:
        out.extend(check(corpus))
    out.extend(check_b10(corpus, findings_paths))
    return out


# --- CLI ----------------------------------------------------------------------

def main(argv=None):
    ap = argparse.ArgumentParser(
        description="Validate the JLS Roadmap board against a corpus snapshot.")
    ap.add_argument("--snapshot", required=True,
                    help="snapshot directory from snapshot_corpus.py")
    ap.add_argument("--issue", type=int, default=None,
                    help="restrict findings to one issue number "
                         "(0 = corpus-level findings)")
    ap.add_argument("--json", dest="json_out", default=None,
                    help="write the findings report to this path")
    ap.add_argument("--findings", action="append", default=[],
                    help="graph/hygiene findings JSON, for B10's full "
                         "derivation; repeatable. Without it B10 applies only "
                         "the corpus-derivable clauses and under-reports.")
    args = ap.parse_args(argv)

    started = time.monotonic()
    if not os.path.isdir(args.snapshot):
        print(f"error: snapshot directory not found: {args.snapshot}",
              file=sys.stderr)
        return 2
    try:
        corpus = load_corpus(args.snapshot)
    except (OSError, json.JSONDecodeError, KeyError) as e:
        print(f"error: cannot load snapshot {args.snapshot}: {e}",
              file=sys.stderr)
        return 2

    findings = run_checks(corpus, args.findings)
    if args.issue is not None:
        findings = [f for f in findings if f["issue"] == args.issue]

    if args.json_out:
        report = findings_report("board", CHECK_VERSION, args.snapshot,
                                 corpus.meta, findings)
        with open(args.json_out, "w") as fh:
            json.dump(report, fh, indent=2)
            fh.write("\n")

    by_check = {}
    for f in findings:
        by_check.setdefault(f["check"], Counter())[f["severity"]] += 1
    print(f"board validator {CHECK_VERSION}: {len(corpus.board)} items, "
          f"{len(corpus.issues)} open issues, {len(findings)} findings "
          f"({time.monotonic() - started:.2f}s)")
    for check in sorted(by_check):
        sev = by_check[check]
        print(f"  {check}: " + ", ".join(
            f"{sev[s]} {s}" for s in ("error", "warn", "info") if sev[s]))
    for f in findings:
        if f["severity"] != "info":
            print(f"  [{f['severity']}] {f['check']} #{f['issue']}: "
                  f"{f['message']}")

    return 1 if any(f["severity"] == "error" for f in findings) else 0


if __name__ == "__main__":
    sys.exit(main())
