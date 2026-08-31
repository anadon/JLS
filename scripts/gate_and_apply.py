#!/usr/bin/env python3
"""Gate edited issue bodies, then apply the ones that pass.

Gates — ALL must pass, or the body is held back and NOT posted:
  1. size gate      — body <= 65536 chars, GitHub's hard limit
  2. citation gate  — every file:line resolves at the pinned commit
                      (verify_citations.py; any BAD fails the issue)
  3. vacuous-evidence gate — zero citations only if the body proves absence
  4. scratchpad-leak gate  — no pipeline cache paths in a public body
  5. template gate  — validate-issue-template.py PASSes for the tier and the
                      labels that will be applied
  6. graph gate     — (with --snapshot) the edited body, merged into the
                      snapshot graph, introduces NO NEW error-severity graph
                      findings on this issue. Pre-existing errors it does not
                      touch are someone else's fix; new ones are this edit's
                      fault.

Applying sets the body AND the tier/kind labels. With a companion comment
file (<bodies>/<n>.comment.md, or --comment-dir), the comment posts
immediately after the body edit — the templates' amendment protocol
(AMENDED:/REPLAN:) rides in that file. A failed comment post is reported as
loudly as a failed edit: a body change without its protocol comment is
itself a protocol breach.

Drift guard (with --snapshot): before every write the LIVE body is fetched
and its sha256 compared to the snapshot's; a mismatch means someone edited
the issue after the snapshot — the item is held as 'drift', never merged
blind.

Usage:
  gate_and_apply.py <scratchpad> <repoRoot> <commit> <results.json>
                    [--apply] [--snapshot DIR] [--bodies-dir DIR]
                    [--comment-dir DIR]
Without --apply it reports only (dry run). results.json entries:
  {"number": N, "tier": t, "kind_label": k, ...} — an entry may carry its own
  "evidence_commit" to override <commit> for the citation gate.
"""
import argparse
import hashlib
import json
import os
import re
import subprocess
import sys
import time

GH_LIMIT = 65536


def run(cmd, **kw):
    return subprocess.run(cmd, capture_output=True, text=True, **kw)


def main():
    ap = argparse.ArgumentParser()
    ap.add_argument("scratchpad")
    ap.add_argument("repo_root")
    ap.add_argument("commit")
    ap.add_argument("results")
    ap.add_argument("--apply", action="store_true")
    ap.add_argument("--snapshot", help="snapshot dir: enables the graph gate "
                    "and the live-drift guard")
    ap.add_argument("--bodies-dir", help="default <scratchpad>/migrated")
    ap.add_argument("--comment-dir", help="default = bodies dir")
    ap.add_argument("--pace", type=float, default=0.0,
                    help="seconds to sleep between issues during --apply; "
                    "GitHub's secondary content-creation limiter trips on "
                    "fast comment/edit bursts (observed ~430 in a run)")
    args = ap.parse_args()

    SP, REPO, COMMIT = args.scratchpad, args.repo_root, args.commit
    bodies_dir = args.bodies_dir or f"{SP}/migrated"
    comment_dir = args.comment_dir or bodies_dir

    results = json.load(open(args.results))
    if isinstance(results, dict):
        results = results.get("migrated", [])

    verify = f"{SP}/verify_citations.py"
    if not os.path.exists(verify):
        verify = f"{REPO}/scripts/verify_citations.py"

    corpus = baseline_graph = None
    vig = None
    if args.snapshot:
        sys.path.insert(0, f"{REPO}/scripts")
        import validate_issue_graph as vig_mod
        from issue_corpus import load_corpus
        vig = vig_mod
        corpus = load_corpus(args.snapshot)

    # Pre-edit bodies, for the citation gate's delta baseline. Without a
    # snapshot there is nothing to diff against and the gate stays absolute.
    snap_bodies = ({n: i.body for n, i in corpus.issues.items()}
                   if corpus else None)
    pre_existing = {}
    pre_existing_vacuous = []

    passed, held = [], []

    for r in results:
        n = r["number"]
        path = f"{bodies_dir}/{n}.md"
        reasons = []

        if not os.path.exists(path):
            held.append((n, ["no body written"]))
            continue

        body = open(path).read()

        # --- size gate
        if len(body) > GH_LIMIT:
            reasons.append(f"body {len(body)} > GitHub limit {GH_LIMIT}")

        # --- citation gate (DELTA, not absolute)
        # Blind spot 8: an absolute gate holds a one-line machine-block repair
        # because of a BAD citation the body already had — and a body may cite
        # a dead path *deliberately*, to record that it is unrecoverable (see
        # #370 citing docs/plan/REGISTRY.md, deleted in 742da74, precisely to
        # say so). Neither is this edit's fault. So we gate the same way the
        # graph gate does: establish the pre-edit baseline from the snapshot
        # body and hold only on citations this edit ADDS.
        commit = r.get("evidence_commit", COMMIT)
        cg = run(["python3", verify, REPO, commit, path])
        cite_line = next((l for l in cg.stdout.split("\n")
                          if "citations=" in l), "")
        if cg.returncode != 0:
            bad = [l.strip() for l in cg.stdout.split("\n")
                   if l.strip().startswith("BAD")]
            base_bad = set()
            old_body = (snap_bodies or {}).get(n)
            if old_body is not None:
                ob = f"{SP}/.baseline-{n}.md"
                with open(ob, "w") as fh:
                    fh.write(old_body)
                bg = run(["python3", verify, REPO, commit, ob])
                base_bad = {l.strip() for l in bg.stdout.split("\n")
                            if l.strip().startswith("BAD")}
                os.unlink(ob)
            introduced = [b for b in bad if b not in base_bad]
            if introduced:
                reasons.append("fabricated/unresolvable citations INTRODUCED "
                               f"by this edit: {len(introduced)}")
                reasons += [f"  {b}" for b in introduced[:5]]
            elif bad:
                pre_existing[n] = len(bad)

        # --- vacuous-evidence gate
        # A body with zero citations passes the citation gate trivially (0 BAD
        # of 0). That is legitimate ONLY for work whose subject does not exist
        # in the tree yet, and only when the body PROVES the absence with a
        # real search. Without this check a fabricating agent could clear the
        # gate by citing nothing at all.
        n_cites = 0
        if cite_line:
            try:
                n_cites = int(cite_line.split("citations=")[1].split()[0])
            except (IndexError, ValueError):
                n_cites = 0
        if n_cites == 0:
            low = body.lower()
            proves_absence = ("git grep" in low or "ls-tree" in low) and (
                "no match" in low or "exit=1" in low or "exit code 1" in low
                or "returns nothing" in low or "no output" in low
                or "zero hits" in low or "absent" in low
            )
            if not proves_absence:
                # Same blind spot the citation gate had: a body with zero
                # citations was already in that state before this edit, and a
                # machine-block-only repair cannot have caused it. Hold only
                # if THIS edit made it worse (citations existed, now none).
                had_cites = False
                ob = (snap_bodies or {}).get(n)
                if ob is not None:
                    p2 = f"{SP}/.baseline-vac-{n}.md"
                    with open(p2, "w") as fh:
                        fh.write(ob)
                    bg = run(["python3", verify, REPO, commit, p2])
                    bl = next((l for l in bg.stdout.split("\n")
                               if "citations=" in l), "")
                    try:
                        had_cites = int(bl.split("citations=")[1].split()[0]) > 0
                    except (IndexError, ValueError):
                        had_cites = False
                    os.unlink(p2)
                if ob is None or had_cites:
                    reasons.append(
                        "zero file:line citations and no absence-proving "
                        "search — rule 1 requires evidence; body carries none"
                    )
                else:
                    pre_existing_vacuous.append(n)

        # --- scratchpad-leak gate
        # The pipeline stages issue text in a private cache. A body that cites
        # "issues/584.md line 31" or "body_529.txt" leaks that cache into a
        # public issue, where it resolves to nothing. The citation gate only
        # catches these when they carry a line number, so check the filenames
        # directly.
        leaks = re.findall(
            r"(?:^|[^\w/])((?:issues/\d+\.md)|(?:issue_\d+\.json)"
            r"|(?:body_\d+\.txt)|(?:/tmp/claude-\d+/[^\s`)\"']*))",
            body,
        )
        if leaks:
            # Blind spot 7: a /tmp/claude-* path that is COMMITTED in the
            # tree (git grep finds it at the pinned commit) is the body
            # QUOTING a repo defect, not leaking its own cache. Never exempt
            # anything under the current pipeline's scratchpad.
            def committed(path):
                return path.startswith("/tmp/claude-") and run(
                    ["git", "-C", REPO, "grep", "-qF", path, commit]
                ).returncode == 0
            real = [l for l in leaks if not committed(l)]
            if real:
                uniq = sorted(set(real))[:5]
                reasons.append(
                    f"leaks pipeline scratchpad paths into a public issue "
                    f"({len(real)} refs, e.g. {', '.join(uniq)}) — cite the "
                    f"issue (#584), not the cache file"
                )

        # --- template gate
        tier = r.get("tier", "task")
        kind = r.get("kind_label", "enhancement")
        tg = run(["python3", f"{REPO}/scripts/validate-issue-template.py",
                  "--body-file", path, "--tier", tier,
                  "--label", f"tier:{tier}", "--label", kind])
        if tg.returncode != 0:
            errs = [l.strip() for l in tg.stdout.split("\n")
                    if l.strip().startswith("-")]
            reasons.append("template validator FAILED")
            reasons += [f"  {e}" for e in errs[:5]]

        if reasons:
            held.append((n, reasons))
            continue

        comment_path = f"{comment_dir}/{n}.comment.md"
        passed.append({"number": n, "path": path, "tier": tier, "kind": kind,
                       "comment": comment_path
                       if os.path.exists(comment_path) else None,
                       "chars": len(body), "cites": cite_line.strip(),
                       "observed_failure": r.get("observed_failure"),
                       "open_questions":
                       len(r.get("unverifiable_sections") or [])})

    # --- graph gate (batch-level): overlay ALL candidate bodies at once and
    # demand no NEW error-severity graph findings. Batch-level because
    # reciprocity repairs are interdependent — moving a task into its
    # declared owner's roster only stops violating the single-owner rule
    # when the OTHER feature's regenerated roster lands in the same batch.
    if corpus is not None and passed:
        baseline_graph = {f["fingerprint"]
                          for f in vig.run(corpus, REPO)
                          if f["severity"] == "error"}
        saved = {}
        for p in passed:
            n = p["number"]
            if n in corpus.issues:
                saved[n] = corpus.issues[n].body
                corpus.issues[n].body = open(p["path"]).read()
        try:
            after = [f for f in vig.run(corpus, REPO)
                     if f["severity"] == "error"]
        finally:
            for n, b in saved.items():
                corpus.issues[n].body = b
        new = [f for f in after if f["fingerprint"] not in baseline_graph]
        if new:
            batch = {p["number"] for p in passed}
            blame = {}
            for f in new:
                involved = ({f["issue"]}
                            | {o for o in f["objects"]
                               if isinstance(o, int)}) & batch
                for n in involved or batch:
                    blame.setdefault(n, []).append(
                        f"graph gate: batch overlay introduces "
                        f"{f['check']} on #{f['issue']}: "
                        f"{f['message'][:120]}")
            still = []
            for p in passed:
                if p["number"] in blame:
                    held.append((p["number"], blame[p["number"]]))
                else:
                    still.append(p)
            passed = still

    print(f"gated {len(results)}  ->  PASS {len(passed)}   HELD {len(held)}\n")
    if pre_existing_vacuous:
        print(f"  note: {len(pre_existing_vacuous)} issue(s) carry ZERO "
              "citations and no absence proof — a pre-existing rule 1 "
              "violation this edit did not cause, needing its own repair: "
              + ", ".join(f"#{n}" for n in sorted(pre_existing_vacuous)))
        print()
    if pre_existing:
        tot = sum(pre_existing.values())
        print(f"  note: {len(pre_existing)} issue(s) carry {tot} PRE-EXISTING "
              "bad citation(s)")
        print("  not introduced by this edit — passed the delta gate, still "
              "worth a separate repair:")
        for k in sorted(pre_existing)[:10]:
            print(f"    #{k}: {pre_existing[k]}")
        print()
    for n, why in held:
        print(f"  HELD #{n}")
        for w in why:
            print(f"        {w}")

    if not args.apply:
        print("\n(dry run — pass --apply to post)")

    applied, failed, drifted = [], [], []
    if args.apply:
        print()
        snap_index = corpus.meta.get("index", {}) if corpus else {}
        for p in passed:
            n = p["number"]

            # --- drift guard
            if snap_index:
                live = run(["gh", "api", f"repos/anadon/JLS/issues/{n}",
                            "--jq", ".body"], cwd=REPO)
                if live.returncode != 0:
                    failed.append((n, "drift check fetch failed: "
                                   + live.stderr.strip()[:150]))
                    continue
                # jq appends exactly ONE newline to the raw body; the body's
                # own trailing newlines are real content — strip only jq's.
                live_body = (live.stdout[:-1]
                             if live.stdout.endswith("\n") else live.stdout)
                live_sha = hashlib.sha256(live_body.encode()).hexdigest()
                snap_sha = snap_index.get(str(n), {}).get("body_sha256")
                if snap_sha and live_sha != snap_sha:
                    drifted.append(n)
                    print(f"  DRIFT   #{n}: live body != snapshot — held, "
                          "re-audit against a fresh snapshot")
                    continue

            def retried(cmd):
                """Run cmd; on failure back off through the secondary
                content-creation limiter (60/180/600s) before giving up."""
                for delay in (0, 60, 180, 600):
                    if delay:
                        time.sleep(delay)
                    res = run(cmd, cwd=REPO)
                    if res.returncode == 0:
                        return res
                return res

            res = retried(["gh", "issue", "edit", str(n),
                           "--body-file", p["path"],
                           "--add-label", f"tier:{p['tier']}",
                           "--add-label", p["kind"]])
            if res.returncode != 0:
                failed.append((n, res.stderr.strip()[:200]))
                print(f"  FAILED  #{n}: {res.stderr.strip()[:200]}")
                continue
            applied.append(n)
            print(f"  applied #{n}  ({p['chars']} chars, {p['cites']})")

            if p["comment"]:
                cres = retried(["gh", "issue", "comment", str(n),
                                "--body-file", p["comment"]])
                if cres.returncode != 0:
                    failed.append((n, "PROTOCOL COMMENT FAILED after body "
                                   "edit: " + cres.stderr.strip()[:150]))
                    print(f"  COMMENT-FAILED #{n} — body edited but its "
                          "AMENDED/REPLAN comment did not post; fix by hand")
                else:
                    print(f"  commented #{n}")
            if args.pace:
                time.sleep(args.pace)

    report = {"passed": [p["number"] for p in passed],
              "held": {str(n): w for n, w in held},
              "applied": applied,
              "drifted": drifted,
              "apply_failed": {str(n): e for n, e in failed},
              "detail": passed}
    out = f"{SP}/gate-report-{os.path.basename(args.results)}"
    json.dump(report, open(out, "w"), indent=1)
    print(f"\nwrote {out}")
    if args.apply:
        print(f"applied {len(applied)}   apply-failed {len(failed)}   "
              f"held {len(held)}   drifted {len(drifted)}")


if __name__ == "__main__":
    main()
