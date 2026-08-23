#!/usr/bin/env python3
"""Self-test for scripts/validate_board.py against a synthetic snapshot.

Builds a tempdir snapshot exercising B01, B02 (both the per-item warn and the
corpus-level delta), B03 in both directions, B04, B05, and B06, and verifies
that a fully coherent issue yields no findings. Also runs the CLI end to end
for exit-code and report-schema behaviour. Stdlib only, plain asserts;
exits 0 on success.

Usage: python3 scripts/tests/test_validate_board.py
"""
import json
import os
import subprocess
import sys
import tempfile

SCRIPTS = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
sys.path.insert(0, SCRIPTS)

import validate_board  # noqa: E402
from issue_corpus import load_corpus  # noqa: E402

FETCHED = "2026-08-23T00:00:00Z"


def issue(n, body="", **extra):
    d = {"number": n, "title": f"issue {n}", "body": body, "labels": [],
         "state": "open", "updated_at": FETCHED, "assignees": []}
    d.update(extra)
    return d


def machine(**edges):
    lines = ["tier: task"] + [f"{k}: {v}" for k, v in edges.items()]
    return "```yaml\n" + "\n".join(lines) + "\n```\n"


def item(item_id, n, state="OPEN", status="Ready"):
    return {"item_id": item_id, "type": "ISSUE", "issue": n,
            "state": state, "status": status}


def write_snapshot(root, issues, board, comments=()):
    with open(os.path.join(root, "issues.jsonl"), "w") as fh:
        fh.writelines(json.dumps(i) + "\n" for i in issues)
    with open(os.path.join(root, "board.jsonl"), "w") as fh:
        fh.writelines(json.dumps(b) + "\n" for b in board)
    with open(os.path.join(root, "comments.jsonl"), "w") as fh:
        fh.writelines(json.dumps(c) + "\n" for c in comments)
    with open(os.path.join(root, "subissues.jsonl"), "w") as fh:
        pass
    with open(os.path.join(root, "milestones.json"), "w") as fh:
        json.dump([], fh)
    with open(os.path.join(root, "labels.json"), "w") as fh:
        json.dump([], fh)
    with open(os.path.join(root, "meta.json"), "w") as fh:
        json.dump({"fetched_at": FETCHED, "head": "deadbeef", "index": {}}, fh)


def of(findings, check, n=None):
    return [f for f in findings if f["check"] == check
            and (n is None or f["issue"] == n)]


def main():
    tmp = tempfile.TemporaryDirectory(prefix="board-selftest-")
    root = tmp.name

    issues = [
        issue(1, machine()),                          # coherent: on board, Ready
        issue(2, machine()),                          # NOT on board -> B01
        issue(3, machine(blocked_by="[99]")),         # Blocked, blocker closed -> B03
        issue(4, machine(blocked_by="[1]")),          # Ready with open blocker -> B03
        issue(5, machine()),                          # on board twice -> B06
        issue(6, machine()),                          # null Status -> B04
        issue(8, machine()),                          # In Progress, unassigned -> B05
    ]
    board = [
        item("I1", 1),
        item("I3", 3, status="Blocked"),
        item("I4", 4, status="Ready"),
        item("I5a", 5), item("I5b", 5),
        item("I6", 6, status=None),
        item("I7", 7, state="CLOSED", status="Ready"),   # closed, not Done -> B02
        item("I8", 8, status="In Progress"),
    ]
    write_snapshot(root, issues, board)

    findings = validate_board.run_checks(load_corpus(root))

    # B01: only issue 2 is missing from the board.
    b01 = of(findings, "B01")
    assert [f["issue"] for f in b01] == [2], b01
    assert b01[0]["severity"] == "error" and b01[0]["fix_class"] == "auto", b01

    # B02 per-item warn on the closed-not-Done item.
    b02w = [f for f in of(findings, "B02", 7) if f["severity"] == "warn"]
    assert len(b02w) == 1, of(findings, "B02")
    # B02 corpus-level delta: 8 items vs 7 open issues -> 1 closed issue.
    b02d = [f for f in of(findings, "B02", 0) if f["severity"] == "info"]
    assert len(b02d) == 1 and "1 closed issues" in b02d[0]["message"], b02d
    assert "0 PRs" in b02d[0]["message"], b02d

    # B03 both directions, info-only.
    b03_blocked = of(findings, "B03", 3)
    assert len(b03_blocked) == 1 and b03_blocked[0]["severity"] == "info", \
        b03_blocked
    assert "no OPEN issue" in b03_blocked[0]["message"], b03_blocked
    b03_ready = of(findings, "B03", 4)
    assert len(b03_ready) == 1 and "[1]" in b03_ready[0]["message"], b03_ready
    assert b03_ready[0]["severity"] == "info", b03_ready

    # B04: null Status on an open issue.
    b04 = of(findings, "B04")
    assert [f["issue"] for f in b04] == [6], b04
    assert b04[0]["severity"] == "warn" and b04[0]["fix_class"] == "adjudicate"

    # B05: In Progress, unassigned, no STATUS: comment.
    b05 = of(findings, "B05")
    assert [f["issue"] for f in b05] == [8] and b05[0]["severity"] == "info", b05

    # B06: duplicate items for issue 5.
    b06 = of(findings, "B06")
    assert [f["issue"] for f in b06] == [5], b06
    assert b06[0]["severity"] == "error" and b06[0]["fix_class"] == "auto", b06

    # Coherent issue 1 yields nothing.
    assert not [f for f in findings if f["issue"] == 1], \
        [f for f in findings if f["issue"] == 1]

    # Standing corpus-level reports exist exactly once (B07/B08) / twice (B09).
    assert len(of(findings, "B07", 0)) == 1
    assert len(of(findings, "B08", 0)) == 1
    assert len(of(findings, "B09", 0)) == 2

    # Every finding carries a fingerprint; fingerprints are unique.
    fps = [f["fingerprint"] for f in findings]
    assert all(fps) and len(set(fps)) == len(fps)

    # CLI end to end: errors present -> exit 1; report schema is sane.
    out_json = os.path.join(root, "out.json")
    proc = subprocess.run(
        [sys.executable, os.path.join(SCRIPTS, "validate_board.py"),
         "--snapshot", root, "--json", out_json],
        capture_output=True, text=True)
    assert proc.returncode == 1, (proc.returncode, proc.stdout, proc.stderr)
    with open(out_json) as fh:
        report = json.load(fh)
    assert report["validator"] == "board" and report["check_version"] == "b-v1"
    assert report["snapshot"]["fetched_at"] == FETCHED
    assert len(report["findings"]) == len(findings)

    # --issue filter narrows to one issue's findings.
    proc = subprocess.run(
        [sys.executable, os.path.join(SCRIPTS, "validate_board.py"),
         "--snapshot", root, "--issue", "3", "--json", out_json],
        capture_output=True, text=True)
    assert proc.returncode == 0, (proc.returncode, proc.stdout, proc.stderr)
    with open(out_json) as fh:
        narrowed = json.load(fh)["findings"]
    assert {f["issue"] for f in narrowed} == {3} and \
        {f["check"] for f in narrowed} == {"B03"}, narrowed

    # A fully coherent snapshot exits 0.
    clean = tempfile.TemporaryDirectory(prefix="board-selftest-clean-")
    write_snapshot(clean.name, [issue(1, machine())], [item("I1", 1)])
    proc = subprocess.run(
        [sys.executable, os.path.join(SCRIPTS, "validate_board.py"),
         "--snapshot", clean.name],
        capture_output=True, text=True)
    assert proc.returncode == 0, (proc.returncode, proc.stdout, proc.stderr)

    # Missing snapshot dir -> environment error 2.
    proc = subprocess.run(
        [sys.executable, os.path.join(SCRIPTS, "validate_board.py"),
         "--snapshot", os.path.join(root, "nope")],
        capture_output=True, text=True)
    assert proc.returncode == 2, (proc.returncode, proc.stdout, proc.stderr)

    print("test_validate_board: OK "
          f"({len(findings)} findings on the synthetic snapshot)")


if __name__ == "__main__":
    main()
