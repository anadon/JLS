#!/usr/bin/env python3
"""Gate migrated issue bodies, then apply the ones that pass.

Two gates, both must pass, or the body is held back and NOT posted:
  1. citation gate  — every file:line resolves at the pinned commit
                      (verify_citations.py; any BAD fails the issue)
  2. template gate  — validate-issue-template.py PASSes for the tier and the
                      labels that will be applied
  3. size gate      — body <= 65536 chars, GitHub's hard limit

Applying sets the body AND the tier/kind labels, since "properly constructed"
includes the labels. Original bodies are preserved in GitHub's edit history and
in all-open-full.json.

Usage:
  gate_and_apply.py <scratchpad> <repoRoot> <commit> <results.json> [--apply]
Without --apply it reports only (dry run).
"""
import json, os, re, subprocess, sys

SP, REPO, COMMIT, RESULTS = sys.argv[1:5]
APPLY = "--apply" in sys.argv[5:]

GH_LIMIT = 65536

results = json.load(open(RESULTS))
if isinstance(results, dict):
    results = results.get("migrated", [])


def run(cmd, **kw):
    return subprocess.run(cmd, capture_output=True, text=True, **kw)


passed, held = [], []

for r in results:
    n = r["number"]
    path = f"{SP}/migrated/{n}.md"
    reasons = []

    if not os.path.exists(path):
        held.append((n, ["no body written"]))
        continue

    body = open(path).read()

    # --- size gate
    if len(body) > GH_LIMIT:
        reasons.append(f"body {len(body)} > GitHub limit {GH_LIMIT}")

    # --- citation gate
    cg = run(["python3", f"{SP}/verify_citations.py", REPO, COMMIT, path])
    cite_line = next((l for l in cg.stdout.split("\n") if "citations=" in l), "")
    if cg.returncode != 0:
        bad = [l.strip() for l in cg.stdout.split("\n") if l.strip().startswith("BAD")]
        reasons.append(f"fabricated/unresolvable citations: {len(bad)}")
        reasons += [f"  {b}" for b in bad[:5]]

    # --- vacuous-evidence gate
    # A body with zero citations passes the citation gate trivially (0 BAD of
    # 0). That is legitimate ONLY for work whose subject does not exist in the
    # tree yet, and only when the body PROVES the absence with a real search.
    # Without this check a fabricating agent could clear the gate by citing
    # nothing at all.
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
            reasons.append(
                "zero file:line citations and no absence-proving search — "
                "rule 1 requires evidence; body carries none"
            )

    # --- scratchpad-leak gate
    # The pipeline stages issue text in a private cache. A body that cites
    # "issues/584.md line 31" or "body_529.txt" leaks that cache into a public
    # issue, where it resolves to nothing. The citation gate only catches these
    # when they carry a line number, so check the filenames directly.
    leaks = re.findall(
        r"(?:^|[^\w/])((?:issues/\d+\.md)|(?:issue_\d+\.json)|(?:body_\d+\.txt)"
        r"|(?:/tmp/claude-\d+/[^\s`)\"']*))",
        body,
    )
    if leaks:
        uniq = sorted(set(leaks))[:5]
        reasons.append(
            f"leaks pipeline scratchpad paths into a public issue "
            f"({len(leaks)} refs, e.g. {', '.join(uniq)}) — cite the issue "
            f"(#584), not the cache file"
        )

    # --- template gate
    tier = r.get("tier", "task")
    kind = r.get("kind_label", "enhancement")
    tg = run(["python3", f"{REPO}/scripts/validate-issue-template.py",
              "--body-file", path, "--tier", tier,
              "--label", f"tier:{tier}", "--label", kind])
    if tg.returncode != 0:
        errs = [l.strip() for l in tg.stdout.split("\n") if l.strip().startswith("-")]
        reasons.append("template validator FAILED")
        reasons += [f"  {e}" for e in errs[:5]]

    if reasons:
        held.append((n, reasons))
        continue

    passed.append({"number": n, "path": path, "tier": tier, "kind": kind,
                   "chars": len(body), "cites": cite_line.strip(),
                   "observed_failure": r.get("observed_failure"),
                   "open_questions": len(r.get("unverifiable_sections") or [])})

print(f"gated {len(results)}  ->  PASS {len(passed)}   HELD {len(held)}\n")
for n, why in held:
    print(f"  HELD #{n}")
    for w in why:
        print(f"        {w}")

if not APPLY:
    print("\n(dry run — pass --apply to post)")

applied, failed = [], []
if APPLY:
    print()
    for p in passed:
        n = p["number"]
        cmd = ["gh", "issue", "edit", str(n),
               "--body-file", p["path"],
               "--add-label", f"tier:{p['tier']}",
               "--add-label", p["kind"]]
        res = run(cmd, cwd=REPO)
        if res.returncode == 0:
            applied.append(n)
            print(f"  applied #{n}  ({p['chars']} chars, {p['cites']})")
        else:
            failed.append((n, res.stderr.strip()[:200]))
            print(f"  FAILED  #{n}: {res.stderr.strip()[:200]}")

report = {"passed": [p["number"] for p in passed],
          "held": {str(n): w for n, w in held},
          "applied": applied,
          "apply_failed": {str(n): e for n, e in failed},
          "detail": passed}
out = f"{SP}/gate-report-{os.path.basename(RESULTS)}"
json.dump(report, open(out, "w"), indent=1)
print(f"\nwrote {out}")
if APPLY:
    print(f"applied {len(applied)}   apply-failed {len(failed)}   held {len(held)}")
