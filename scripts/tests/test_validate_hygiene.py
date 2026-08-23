#!/usr/bin/env python3
"""Self-test for validate_hygiene.py against a synthetic snapshot.

Run: python3 scripts/tests/test_validate_hygiene.py  (exit 0 = pass)

Builds four crafted task issues in a tempdir snapshot:
  #101 dirty  — wrong-tier title prefix (H01 error), bug label with no
                command block in §2 (H04), zero DoD checkboxes (H09),
                bare N/A (H11), math-less §7.10 (H12), dash-joined
                permalinks (H17), fabricated + retired refs (H18)
  #102 dirty  — near-duplicate title of #101 (H03), enhancement pasting a
                failure (H04 info), ticked DoD box without evidence (H09
                warn), oversized body (H13 warn)
  #103 clean  — must trigger NONE of the checks above
  #104 dirty  — missing title prefix (H01 warn)

H06's fetcher and H15's git paths are exercised only through their skip
branches (no network, no git repo required). Stdlib only, plain asserts.
"""
import json
import os
import subprocess
import sys
import tempfile

HERE = os.path.dirname(os.path.abspath(__file__))
SCRIPTS = os.path.dirname(HERE)
sys.path.insert(0, SCRIPTS)

from issue_corpus import TEMPLATES, load_corpus  # noqa: E402
import validate_hygiene as vh  # noqa: E402


def task_body(sec1="Background prose.", sec2="Steady behavior observed.",
              sec12="Related work summary.", sec710="$E = IR$",
              dod="- [ ] verify the thing works\n"):
    mb = ("```yaml\n"
          "tier: task\n"
          "evidence_commit: deadbeefdeadbeefdeadbeefdeadbeefdeadbeef\n"
          "part_of_feature: none\n"
          "blocked_by: none\n"
          "blocks: none\n"
          "related: none\n"
          "```\n")
    lines = []
    for h in TEMPLATES["task"]["headings"]:
        lines.append(f"## {h}\n")
        if h == "Status & Dependencies":
            lines.append(mb)
        elif h == "1. Background & Prior Work":
            lines.append(sec1 + "\n")
        elif h == "2. Observations":
            lines.append(sec2 + "\n")
        elif h == "12. Related Work":
            lines.append(sec12 + "\n")
        elif h == "7. Interface & Data Contract":
            lines.append("Contract overview.\n")
            for s in TEMPLATES["task"]["subheadings"]:
                content = sec710 if s == "7.10" \
                    else "N/A — not exercised by this task."
                lines.append(f"### {s} Sub\n{content}\n")
        elif h == "14. Completion Criteria (Definition of Done)":
            lines.append(dod)
        else:
            lines.append("Filler prose for this section.\n")
    return "\n".join(lines)


def mk_issue(number, title, body, kind="enhancement"):
    return {"number": number, "title": title, "body": body,
            "labels": [{"name": "tier:task"}, {"name": kind}],
            "state": "open", "updated_at": "2026-08-23T00:00:00Z"}


def build_snapshot(snap):
    body_101 = task_body(
        sec1=("Evidence spans "
              "[L10](https://github.com/x/y/blob/abcdef1/f.java#L10) - "
              "[L20](https://github.com/x/y/blob/abcdef1/f.java#L20). "
              "Related: #99999 and #485."),
        sec2="It fails sometimes when the button is clicked.",
        sec12="N/A",
        sec710="TBD, math to be written later.",
        dod="No checkboxes were written here.\n")
    body_102 = task_body(
        sec2="Observed:\n\n```\n$ mvn -q test\n[ERROR] FAILED build\n```\n",
        dod="- [x] shipped the widget\n- [ ] document it\n")
    body_102 += "\n\nPadding: " + "x" * (61000 - len(body_102))
    issues = [
        mk_issue(101, "FEAT-C999-1: duplicate widget frobnicator",
                 body_101, kind="bug"),
        mk_issue(102, "TASK-C999-1: duplicate widget frobnicator!",
                 body_102),
        mk_issue(103, "TASK-C777-9: implement adder carry chain",
                 task_body()),
        mk_issue(104, "add carry select adder", task_body()),
    ]
    with open(os.path.join(snap, "issues.jsonl"), "w") as fh:
        for iss in issues:
            fh.write(json.dumps(iss) + "\n")
    for name in ("comments.jsonl", "subissues.jsonl", "board.jsonl"):
        open(os.path.join(snap, name), "w").close()
    for name, payload in (("milestones.json", []), ("labels.json", []),
                          ("meta.json", {"fetched_at": "2026-08-23T00:00:00Z",
                                         "head": "", "repo": "example/repo",
                                         "index": {}})):
        with open(os.path.join(snap, name), "w") as fh:
            json.dump(payload, fh)


def main():
    tmp = tempfile.mkdtemp(prefix="hygtest-")
    snap = os.path.join(tmp, "snap")
    os.makedirs(snap)
    build_snapshot(snap)

    # --- library path: run() directly ---------------------------------------
    corpus = load_corpus(snap)
    F = vh.run(corpus, repo_root=tmp, skip={"H15"}, snapshot_dir=snap)
    got = {(f["issue"], f["check"], f["severity"]) for f in F}

    def has(issue, check, sev):
        assert (issue, check, sev) in got, (
            f"expected {check}/{sev} on #{issue}; got:\n"
            + "\n".join(sorted(f"  #{a} {b} {c}" for a, b, c in got)))

    has(101, "H01", "error")     # FEAT- prefix on a task
    has(104, "H01", "warn")      # no prefix at all
    has(101, "H04", "error")     # bug without command+output block in §2
    has(102, "H04", "info")      # enhancement pasting an observed failure
    has(101, "H09", "error")     # zero DoD checkboxes
    has(102, "H09", "warn")      # ticked box, open issue, no evidence
    has(101, "H11", "warn")      # bare N/A in §12
    has(101, "H12", "error")     # math-less non-N/A §7.10
    has(102, "H13", "warn")      # body > 60000 chars
    has(101, "H17", "error")     # dash-joined adjacent line permalinks
    has(101, "H18", "error")     # #99999 fabricated
    has(101, "H18", "warn")      # #485 retired rescue vehicle
    has(0, "H06", "info")        # skip branch: no last_edited.json

    # H03: pair emitted once, owned by the lower number
    pairs = [f for f in F if f["check"] == "H03"]
    assert any(f["issue"] == 101 and 102 in f["objects"] for f in pairs), pairs
    assert not any(f["issue"] == 102 for f in pairs), pairs
    assert not any(f["issue"] == 103 or 103 in f["objects"]
                   for f in pairs), pairs

    # the clean issue triggers none of the tested checks
    tested = {"H01", "H03", "H04", "H09", "H11", "H12", "H13", "H17", "H18"}
    dirty_on_clean = [f for f in F
                      if f["issue"] == 103 and f["check"] in tested]
    assert not dirty_on_clean, dirty_on_clean

    # every finding is schema-complete
    for f in F:
        assert f["severity"] in ("error", "warn", "info"), f
        assert f["fix_class"] in ("auto", "comment", "body", "adjudicate"), f
        assert len(f["fingerprint"]) == 64, f

    # --- CLI path: exit code, report envelope, --skip, --issue --------------
    out = os.path.join(tmp, "findings.json")
    r = subprocess.run(
        [sys.executable, os.path.join(SCRIPTS, "validate_hygiene.py"),
         "--snapshot", snap, "--repo-root", tmp, "--json", out,
         "--skip", "H15"],
        capture_output=True, text=True)
    assert r.returncode == 1, (r.returncode, r.stdout[-2000:], r.stderr)
    rep = json.load(open(out))
    assert rep["validator"] == "hygiene", rep["validator"]
    assert rep["check_version"] == "h-v1", rep["check_version"]
    assert rep["snapshot"]["path"] == snap
    cli_got = {(f["issue"], f["check"], f["severity"])
               for f in rep["findings"]}
    assert cli_got == got, (cli_got ^ got)

    r2 = subprocess.run(
        [sys.executable, os.path.join(SCRIPTS, "validate_hygiene.py"),
         "--snapshot", snap, "--repo-root", tmp, "--issue", "103",
         "--json", os.path.join(tmp, "one.json"), "--skip", "H15"],
        capture_output=True, text=True)
    one = json.load(open(os.path.join(tmp, "one.json")))
    assert all(f["issue"] == 103 for f in one["findings"]), one["findings"]
    assert not [f for f in one["findings"] if f["check"] in tested]
    assert r2.returncode == 0, (r2.returncode, r2.stdout[-2000:])

    print(f"hygiene self-test: OK ({len(F)} findings on synthetic corpus)")


if __name__ == "__main__":
    main()
