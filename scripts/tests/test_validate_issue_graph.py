#!/usr/bin/env python3
"""Self-test for validate_issue_graph.py against a synthetic snapshot.

Run: python3 scripts/tests/test_validate_issue_graph.py  (exit 0 = pass)
"""
import json
import os
import subprocess
import sys
import tempfile

HERE = os.path.dirname(os.path.abspath(__file__))
SCRIPTS = os.path.dirname(HERE)
sys.path.insert(0, SCRIPTS)

from issue_corpus import load_corpus, parse_edge_field  # noqa: E402
import validate_issue_graph as vig  # noqa: E402


def mk_issue(number, tier, title, machine_lines, labels=None, body_extra=""):
    body = (f"# x\n```yaml\ntier: {tier}\n"
            + "\n".join(machine_lines) + "\n```\n" + body_extra)
    if tier in ("feature", "capstone"):
        body += "\n```mermaid\nflowchart TD\n  A --> B\n```\n"
    return {"number": number, "title": title, "body": body,
            "labels": [{"name": f"tier:{tier}"}, {"name": "enhancement"}],
            "state": "open", "updated_at": "2026-08-23T00:00:00Z"}


def build_snapshot(tmp):
    ec = subprocess.run(["git", "-C", SCRIPTS, "rev-parse", "HEAD"],
                        capture_output=True, text=True).stdout.strip()
    issues = [
        # 10: healthy task, owned by feature 20. Declares no owner itself —
        #     under feature v4 the roster is the sole ownership record.
        mk_issue(10, "task", "TASK-A", [
            f"evidence_commit: {ec}",
            # stale derived back-reference: really owned by 20, claims 21
            "owned_by_derived: [21]",
            "blocked_by: none", "blocks: none", "related: none"],
            # G22: a child claiming its own parent's criterion by number is
            # the highest-precision rule-B tell. Backtick-wrapped on purpose:
            # that spelling is common in the corpus and must still match.
            body_extra="P8 verifies `#20`'s own Integration Criterion 3.\n"),
        # 11: illegal UPWARD ordering edge to feature 20 (G03), self-edge
        #     (G16), edge to nonexistent (G02), retired key still present (G20)
        mk_issue(11, "task", "TASK-B", [
            f"evidence_commit: {ec}", "part_of_feature: 20",
            "blocked_by: [20, 11, 99999]", "blocks: none", "related: none"]),
        # 12: prose in an edge field (G01), unresolvable commit (G18)
        mk_issue(12, "task", "TASK-C", [
            "evidence_commit: deadbeefdeadbeefdeadbeefdeadbeefdeadbeef",
            "blocked_by: [ask-the-maintainer]", "blocks: none",
            "related: none"]),
        # 13/14: task<->task ordering cycle (G04)
        mk_issue(13, "task", "TASK-D", [
            f"evidence_commit: {ec}",
            "blocked_by: [14]", "blocks: none", "related: none"]),
        mk_issue(14, "task", "TASK-E", [
            f"evidence_commit: {ec}",
            "blocked_by: [13]", "blocks: none", "related: none"]),
        # 20: roster lists 10 and 13, plus 99998 which is not an open issue
        #     (G06). serves capstone 30, not mirrored there (G08).
        mk_issue(20, "feature", "FEAT-A", [
            f"evidence_commit: {ec}", "requires_tasks: [10, 13, 99998]",
            "planned_tasks: none", "blocked_by: none", "blocks: none",
            "serves_capstones: [30]", "related: none"]),
        # 21: ALSO owns task 13 — sharing is legal now, must NOT raise G07
        mk_issue(21, "feature", "FEAT-B", [
            f"evidence_commit: {ec}", "requires_tasks:", '  - "#13"  # note',
            "planned_tasks: none", "blocked_by: none", "blocks: none",
            "serves_capstones: none", "related: none"]),
        # 30: capstone. requires_features omits 20 though 20 serves it (G08
        #     on the 20 side). Retains a retired requires_tasks_exception
        #     (G12 error + G20 warn). blocked_by capstone 31 is now LEGAL.
        mk_issue(30, "capstone", "CAP-A", [
            f"evidence_commit: {ec}", "requires_features: [21]",
            "requires_capstones: none", "requires_tasks_exception: [10]",
            "planned_features: none", "blocked_by: [31]", "blocks: none",
            "related: none"]),
        # 31: capstone with an illegal downward ordering edge to a task (G03)
        mk_issue(31, "capstone", "CAP-B", [
            f"evidence_commit: {ec}", "requires_features: none",
            "requires_capstones: none",
            "planned_features: none", "blocked_by: [10]", "blocks: [30]",
            "related: none"]),
    ]
    with open(os.path.join(tmp, "issues.jsonl"), "w") as fh:
        for d in issues:
            fh.write(json.dumps(d) + "\n")
    for name, content in (("meta.json", {"fetched_at": "x", "head": ec,
                                         "index": {}}),):
        with open(os.path.join(tmp, name), "w") as fh:
            json.dump(content, fh)
    open(os.path.join(tmp, "subissues.jsonl"), "w").write(
        json.dumps({"parent": 20, "children": [10],
                    "children_state": {"10": "open"}}) + "\n")
    return tmp


def check_coverage(tmp, corpus):
    """--coverage is a REPORT, not a gate: it must always exit 0 and must
    describe the same synthetic tree the findings pass sees."""
    out = os.path.join(tmp, "cov.json")
    r = subprocess.run(
        [sys.executable, os.path.join(SCRIPTS, "validate_issue_graph.py"),
         "--snapshot", tmp, "--repo-root", SCRIPTS, "--coverage", out],
        capture_output=True, text=True)
    assert r.returncode == 0, (
        "--coverage must exit 0 even on a corpus full of findings; "
        f"got {r.returncode}\n{r.stderr}")
    c = json.load(open(out))
    assert c["report"] == "coverage" and c["version"] == "cov-v1", c
    assert c["totals"] == {"capstone": 2, "feature": 2, "task": 5}, c["totals"]

    root = c["rootedness"]
    # 21 is required by capstone 30; 20 is required by nobody
    assert root["features_serving_no_capstone"] == [20], root
    # tasks 10 and 13 are in rosters; 11, 12, 14 are in none
    assert root["tasks_in_no_feature_roster"] == [11, 12, 14], root
    # a roster is the only ownership record, so that IS the orphan set
    assert root["tasks_in_no_roster_at_all"] == [11, 12, 14], root
    assert root["capstones_requiring_nothing"] == [31], root

    dec = c["decomposition"]
    assert dec["features_with_empty_roster"] == [], dec
    assert dec["features_with_single_task"] == [21], dec
    assert dec["capstones_with_single_feature"] == [30], dec

    # the report carries the structural keys downstream tooling reads
    for k in ("roadmap", "related_edges", "shared_path_shortlist"):
        assert k in c, f"coverage report missing {k}"
    assert c["related_edges"]["distinct_pairs"] == 0, c["related_edges"]


def main():
    # parser unit checks
    blk = 'a: [1, 2,\n   3]  # wrapped\nb:\n  - "#5"  # q\nc: none\n'
    f = parse_edge_field(blk, "a")
    assert f["numbers"] == [1, 2, 3], f
    f = parse_edge_field(blk, "b")
    assert f["numbers"] == [5] and f["hash_form"] == ["#5"], f
    f = parse_edge_field(blk, "c")
    assert f["none"] and not f["numbers"], f

    tmp = build_snapshot(tempfile.mkdtemp(prefix="graphtest-"))
    corpus = load_corpus(tmp)
    F = vig.run(corpus, SCRIPTS)
    got = {(f["issue"], f["check"], f["severity"]) for f in F}

    def has(issue, check, sev="error"):
        assert (issue, check, sev) in got, (
            f"expected {check}/{sev} on #{issue}; got "
            + "\n".join(sorted(f"{a} {b} {c}" for a, b, c in got)))

    has(11, "G03")                 # task blocked_by a FEATURE — upward, illegal
    has(11, "G16")                 # self-edge
    has(11, "G02")                 # 99999 nonexistent
    has(11, "G20", "warn")         # retired part_of_feature key still present
    has(12, "G01")                 # prose in blocked_by
    has(12, "G18")                 # unresolvable evidence_commit
    has(13, "G04")                 # 13<->14 cycle (min node owns it)
    has(20, "G06")                 # roster entry 99998 is not an open task
    has(20, "G08", "warn")         # serves_capstones not mirrored
    has(21, "G01", "info")         # quoted '#13' style
    has(30, "G12")                 # retired capstone->task exception survives
    has(30, "G20", "warn")         # ...and the retired key itself
    has(31, "G03")                 # capstone blocked_by a TASK — illegal

    # --- the corrected model must NOT fire the retired checks
    retired = {c for _, c, _ in got} & {"G05", "G07", "G15"}
    assert not retired, f"retired checks still firing: {retired}"
    # task 13 is owned by BOTH 20 and 21 — sharing is legal, no finding at all
    assert not any(c == "G07" for _, c, _ in got), got
    # capstone 30 blocked_by capstone 31 is legal ordering -> no G03 on 30
    assert (30, "G03", "error") not in got, "capstone->capstone ordering is legal"
    has(10, "G21", "warn")         # derived back-reference drifted from roster
    has(20, "G22", "warn")         # child #10 claims feature #20's criterion 3
    # G22 must not fire where no child claims a parent criterion
    assert not any(i == 21 and c == "G22" for i, c, s in got), got
    # ...and G21 is a WARN, never an error: task 10 stays error-clean
    assert not any(i == 10 and s == "error" for i, c, s in got), got

    check_coverage(tmp, corpus)
    print("graph self-test: OK", f"({len(F)} findings on synthetic corpus)")


if __name__ == "__main__":
    main()
