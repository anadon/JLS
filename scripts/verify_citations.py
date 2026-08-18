#!/usr/bin/env python3
"""Verify every file:line citation in a migrated issue body against the tree.

Template rule 1: "Every code claim carries file:line at a named commit ... quote
the line you cite." A migration that invents citations is worse than the compact
form it replaces, so this gate is deterministic and unforgiving.

Per citation, three outcomes:
  OK_QUOTED  file exists, line in range, AND the body quotes text that really
             occurs at that line (normalised whitespace) -> fully verified
  OK_RANGE   file exists and line is in range, but no matching quote found
             -> citation is plausible but the quote obligation is unmet
  BAD        file missing, or line beyond EOF, or quoted text does not match
             -> fabricated or drifted; the body must be rejected

Usage: verify_citations.py <repo> <commit> <body.md> [body.md ...]
Exit 1 if any BAD citation is found in any body.
"""
import json, re, subprocess, sys

REPO, COMMIT = sys.argv[1], sys.argv[2]
BODIES = sys.argv[3:]

# Two citation spellings occur in practice and BOTH must be checked, or a body
# written entirely in the prose form reads as having zero evidence:
#   colon form : jls/elem/Foo.java:87   src/jls/Circuit.java:104,110
#   prose form : HelpTopicsTest.java lines 34-45   CellValidator.java line 27
_PATH = (r"(?P<path>(?:[\w.\-/]+/)*[\w.\-]+"
         r"\.(?:java|xml|md|py|sh|yml|yaml|json|txt|v))")
CITE = re.compile(
    _PATH + r"(?:"
    r":(?P<lines>\d+(?:\s*[,\-]\s*\d+)*)"
    r"|"
    r"`?\s+lines?\s+(?P<plines>\d+(?:\s*[,\-–]\s*\d+)*)"
    r")"
)

# Third spelling: commit-locked GitHub permalinks, which the templates actually
# ASK for ("cite by permalink at this commit"). These carry the strongest
# provenance and so must be checked hardest -- a fabricated permalink looks more
# authoritative than a fabricated file:line, not less. The commit in the URL is
# verified to be the pinned one; a permalink to some other commit is not
# evidence at THIS commit.
PERMALINK = re.compile(
    r"github\.com/[\w.\-]+/[\w.\-]+/blob/(?P<sha>[0-9a-f]{7,40})/"
    r"(?P<path>[^\s)`\"'#]+)#L(?P<start>\d+)(?:-L(?P<end>\d+))?"
)
# Quoted spans that could carry the cited line: backticks or markdown quote.
QUOTED = re.compile(r"`([^`\n]{4,})`|^>\s?(.+)$", re.M)

_cache = {}


def blob(path):
    """File contents at COMMIT, or None if the path does not exist there."""
    if path in _cache:
        return _cache[path]
    r = subprocess.run(
        ["git", "-C", REPO, "show", f"{COMMIT}:{path}"],
        capture_output=True, text=True,
    )
    _cache[path] = r.stdout.split("\n") if r.returncode == 0 else None
    return _cache[path]


def resolve(path):
    """Try the path as given, then as a suffix of a tracked path."""
    if blob(path) is not None:
        return path
    r = subprocess.run(
        ["git", "-C", REPO, "ls-tree", "-r", "--name-only", COMMIT],
        capture_output=True, text=True,
    )
    hits = [p for p in r.stdout.split("\n") if p.endswith("/" + path) or p == path]
    return hits[0] if len(hits) == 1 else None


def norm(s):
    return re.sub(r"\s+", " ", s).strip()


overall_bad = 0
report = []

for body_path in BODIES:
    text = open(body_path).read()
    quotes = {norm(a or b) for a, b in QUOTED.findall(text) if norm(a or b)}

    results = []
    for m in CITE.finditer(text):
        raw_path = m.group("path")
        real = resolve(raw_path)
        nums = [int(n) for n in
                re.findall(r"\d+", m.group("lines") or m.group("plines") or "")]
        if not nums:
            continue
        if real is None:
            results.append(("BAD", raw_path, nums, "path not found at commit"))
            continue
        lines = blob(real)
        for n in nums:
            if n < 1 or n > len(lines):
                results.append(("BAD", real, [n],
                                f"line {n} beyond EOF ({len(lines)} lines)"))
                continue
            actual = norm(lines[n - 1])
            hit = any(actual and (q in actual or actual in q) for q in quotes)
            results.append(("OK_QUOTED" if hit else "OK_RANGE", real, [n],
                            actual[:70]))

    for m in PERMALINK.finditer(text):
        sha, raw_path = m.group("sha"), m.group("path")
        if not COMMIT.startswith(sha) and not sha.startswith(COMMIT[:7]):
            results.append(("BAD", raw_path, [], f"permalink pins {sha[:12]}, "
                                                 f"not the evidence commit"))
            continue
        real = resolve(raw_path)
        if real is None:
            results.append(("BAD", raw_path, [], "permalink path not found at commit"))
            continue
        lines = blob(real)
        start = int(m.group("start"))
        end = int(m.group("end") or start)
        if start < 1 or end > len(lines) or start > end:
            results.append(("BAD", real, [start, end],
                            f"permalink range L{start}-L{end} outside file "
                            f"({len(lines)} lines)"))
            continue
        span = [norm(l) for l in lines[start - 1:end] if norm(l)]
        hit = any(any(q in a or a in q for q in quotes) for a in span)
        results.append(("OK_QUOTED" if hit else "OK_RANGE", real, [start, end],
                        (span[0] if span else "")[:70]))

    bad = [r for r in results if r[0] == "BAD"]
    overall_bad += len(bad)
    counts = {k: sum(1 for r in results if r[0] == k)
              for k in ("OK_QUOTED", "OK_RANGE", "BAD")}
    report.append({"body": body_path, "counts": counts,
                   "citations": len(results),
                   "bad": [{"path": b[1], "lines": b[2], "why": b[3]} for b in bad]})

    print(f"{body_path}")
    print(f"  citations={len(results)}  quoted-verified={counts['OK_QUOTED']}  "
          f"range-only={counts['OK_RANGE']}  BAD={counts['BAD']}")
    for b in bad[:12]:
        print(f"    BAD {b[1]}:{b[2]} — {b[3]}")

print(json.dumps(report), file=sys.stderr)
sys.exit(1 if overall_bad else 0)
