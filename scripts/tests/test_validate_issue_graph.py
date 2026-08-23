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
        # 10: healthy task under feature 20
        mk_issue(10, "task", "TASK-A", [
            f"evidence_commit: {ec}", "part_of_feature: 20",
            "blocked_by: none", "blocks: none", "related: none"]),
        # 11: task blocked_by its own parent (G05), self-edge (G16),
        #     edge to nonexistent (G02 error), part_of two features (G15)
        mk_issue(11, "task", "TASK-B", [
            f"evidence_commit: {ec}", "part_of_feature: [20, 21]",
            "blocked_by: [20, 11, 99999]", "blocks: none", "related: none"]),
        # 12: task with prose in edge field (G01), unresolvable commit (G18)
        mk_issue(12, "task", "TASK-C", [
            "evidence_commit: deadbeefdeadbeefdeadbeefdeadbeefdeadbeef",
            "part_of_feature: none",
            "blocked_by: [ask-the-maintainer]", "blocks: none",
            "related: none"]),
        # 13/14: ordering cycle (G04)
        mk_issue(13, "task", "TASK-D", [
            f"evidence_commit: {ec}", "part_of_feature: none",
            "blocked_by: [14]", "blocks: none", "related: none"]),
        mk_issue(14, "task", "TASK-E", [
            f"evidence_commit: {ec}", "part_of_feature: none",
            "blocked_by: [13]", "blocks: none", "related: none"]),
        # 20: feature whose roster omits 10 and 11 (G06 x2), claims 13 which
        #     points elsewhere (G06), serves capstone 30 not mirrored (G08)
        mk_issue(20, "feature", "FEAT-A", [
            f"evidence_commit: {ec}", "requires_tasks: [13]",
            "planned_tasks: none", "blocked_by: none", "blocks: none",
            "serves_capstones: [30]", "related: none"]),
        # 21: feature also claiming 13 (G07 with 20); quoted hash form entry
        mk_issue(21, "feature", "FEAT-B", [
            f"evidence_commit: {ec}", "requires_tasks:", '  - "#13"  # note',
            "planned_tasks: none", "blocked_by: none", "blocks: none",
            "serves_capstones: none", "related: none"]),
        # 30: capstone requiring feature 22 (nonexistent -> G02 warn range),
        #     requires_features omits 20 though 20 serves it (G08 on 20 side
        #     already); ordering edge to capstone forbidden -> use feature
        mk_issue(30, "capstone", "CAP-A", [
            f"evidence_commit: {ec}", "requires_features: [21]",
            "requires_capstones: none", "requires_tasks_exception: [10]",
            "planned_features: none", "blocked_by: none", "blocks: none",
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

    has(11, "G05")                 # blocked_by own parent
    has(11, "G16")                 # self-edge
    has(11, "G15")                 # two part_of_feature values
    has(11, "G02")                 # 99999 nonexistent
    has(12, "G01")                 # prose in blocked_by
    has(12, "G18")                 # unresolvable evidence_commit
    has(13, "G04")                 # 13<->14 cycle (min node owns it)
    has(20, "G06")                 # roster omits declaring child
    has(20, "G08", "warn")         # serves_capstones not mirrored
    has(20, "G07")                 # task 13 claimed by 20 and 21
    has(21, "G01", "info")         # quoted '#13' style
    has(30, "G12", "warn")         # exception without REPLAN comment
    # healthy task 10 must carry no error-severity finding
    assert not any(i == 10 and s == "error" for i, c, s in got), got
    print("graph self-test: OK", f"({len(F)} findings on synthetic corpus)")


if __name__ == "__main__":
    main()
