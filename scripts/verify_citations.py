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

Importable: check_body(repo, commit, text) returns the per-citation results;
apply_repairs.py's quote-upgrade generator builds on the same regexes rather
than forking them.
"""
import json, re, subprocess, sys

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


class Tree:
    """File access at one commit, cached."""

    def __init__(self, repo, commit):
        self.repo, self.commit = repo, commit
        self._cache = {}
        self._ls = None

    def blob(self, path):
        if path in self._cache:
            return self._cache[path]
        r = subprocess.run(
            ["git", "-C", self.repo, "show", f"{self.commit}:{path}"],
            capture_output=True, text=True,
        )
        self._cache[path] = r.stdout.split("\n") if r.returncode == 0 else None
        return self._cache[path]

    def resolve(self, path):
        """Try the path as given, then as a suffix of a tracked path."""
        if self.blob(path) is not None:
            return path
        if self._ls is None:
            r = subprocess.run(
                ["git", "-C", self.repo, "ls-tree", "-r", "--name-only",
                 self.commit], capture_output=True, text=True)
            self._ls = r.stdout.split("\n")
        hits = [p for p in self._ls if p.endswith("/" + path) or p == path]
        return hits[0] if len(hits) == 1 else None


def norm(s):
    return re.sub(r"\s+", " ", s).strip()


_ABSENCE_KEYS = (
    "does not exist", "not found at", "no longer exists", "cannot be verified",
    "is absent", "does not resolve", "unresolvable", "never merged",
    "absent at", "not present at",
)


def documented_absent(text, path):
    """True if the body anywhere states that `path` is missing at the commit."""
    low = text.lower()
    needle = path.lower()
    for i in range(len(low)):
        i = low.find(needle, i)
        if i < 0:
            break
        ctx = low[max(0, i - 400):i + len(needle) + 400]
        if any(k in ctx for k in _ABSENCE_KEYS):
            return True
        i += len(needle)
    return False


def rejoin_wrapped(raw):
    """Undo markdown line-wrapping inside file paths.

    Blind spot 6: a long path wrapped across a line break reads as a truncated,
    unresolvable path and gets flagged as fabrication -- e.g.
        docs/standards-adoption/03-accessibility-
        conformance.md:510
    A wrap only ever splits a path where the renderer could: after a hyphen,
    underscore or dot, with no space before the newline. NOT after "/": a line
    ending in a bare directory ("git grep -- docs/") would glue onto the next
    line's citation and invent "docs/docs/foo.md". A genuine wrap after "/"
    leaves a COMPLETE filename, which resolve()'s suffix search already finds
    — only a wrap mid-filename (after - _ .) needs rejoining.
    """
    return re.sub(r"(?<=\w[-_.])\n[ \t]*"
                  r"(?=[\w.\-/]*\.(?:java|xml|md|py|sh|yml|yaml|json|txt|v)\b)",
                  "", raw)


def check_body(repo, commit, text, tree=None):
    """All citation results for one body text:
    [(status, path, nums, detail, match_span), ...].
    NOTE: spans index into rejoin_wrapped(text), not the raw text — callers
    that splice by span must run rejoin_wrapped first themselves."""
    tree = tree or Tree(repo, commit)
    text = rejoin_wrapped(text)
    quotes = {norm(a or b) for a, b in QUOTED.findall(text) if norm(a or b)}
    results = []

    for m in CITE.finditer(text):
        raw_path = m.group("path")
        real = tree.resolve(raw_path)
        nums = [int(n) for n in
                re.findall(r"\d+", m.group("lines") or m.group("plines") or "")]
        # Drop line 0: no file has one. "pom.xml:0" is `grep -c` output meaning
        # ZERO MATCHES, not a citation, and reading it as one invents a defect.
        nums = [n for n in nums if n > 0]
        if not nums:
            continue
        if real is None:
            # A path that does not resolve is normally fabrication. But a body
            # legitimately QUOTES a bad citation inherited from an old filing
            # in order to refute it. Absence is a property of the PATH,
            # established once anywhere in the body — not of each mention.
            refuted = documented_absent(text, raw_path)
            results.append(
                ("ABSENT_DOCUMENTED" if refuted else "BAD", raw_path, nums,
                 "path absent, documented as such" if refuted
                 else "path not found at commit", m.span()))
            continue
        lines = tree.blob(real)
        for n in nums:
            if n < 1 or n > len(lines):
                # A line citation quoted in order to refute it ("the original
                # filing cited Foo.java:392; that code never merged") is
                # refutable exactly like an absent path. Check BOTH spellings:
                # the body usually writes the bare filename, while `real` is
                # the resolved repo path.
                refuted = (documented_absent(text, real)
                           or documented_absent(text, raw_path))
                results.append(
                    ("ABSENT_DOCUMENTED" if refuted else "BAD", real, [n],
                     f"line {n} beyond EOF ({len(lines)} lines)"
                     + ("; documented as such" if refuted else ""), m.span()))
                continue
            actual = norm(lines[n - 1])
            hit = any(actual and (q in actual or actual in q) for q in quotes)
            results.append(("OK_QUOTED" if hit else "OK_RANGE", real, [n],
                            actual[:70], m.span()))

    for m in PERMALINK.finditer(text):
        sha, raw_path = m.group("sha"), m.group("path")
        if not commit.startswith(sha) and not sha.startswith(commit[:7]):
            results.append(("BAD", raw_path, [],
                            f"permalink pins {sha[:12]}, "
                            f"not the evidence commit", m.span()))
            continue
        real = tree.resolve(raw_path)
        if real is None:
            results.append(("BAD", raw_path, [],
                            "permalink path not found at commit", m.span()))
            continue
        lines = tree.blob(real)
        start = int(m.group("start"))
        end = int(m.group("end") or start)
        if start < 1 or end > len(lines) or start > end:
            results.append(("BAD", real, [start, end],
                            f"permalink range L{start}-L{end} outside file "
                            f"({len(lines)} lines)", m.span()))
            continue
        span = [norm(l) for l in lines[start - 1:end] if norm(l)]
        hit = any(any(q in a or a in q for q in quotes) for a in span)
        results.append(("OK_QUOTED" if hit else "OK_RANGE", real,
                        [start, end], (span[0] if span else "")[:70],
                        m.span()))
    return results


def main():
    repo, commit = sys.argv[1], sys.argv[2]
    bodies = sys.argv[3:]
    tree = Tree(repo, commit)
    overall_bad = 0
    report = []

    for body_path in bodies:
        text = open(body_path).read()
        results = check_body(repo, commit, text, tree)
        bad = [r for r in results if r[0] == "BAD"]
        overall_bad += len(bad)
        counts = {k: sum(1 for r in results if r[0] == k)
                  for k in ("OK_QUOTED", "OK_RANGE", "BAD",
                            "ABSENT_DOCUMENTED")}
        report.append({"body": body_path, "counts": counts,
                       "citations": len(results),
                       "bad": [{"path": b[1], "lines": b[2], "why": b[3]}
                               for b in bad]})

        print(f"{body_path}")
        print(f"  citations={len(results)}  "
              f"quoted-verified={counts['OK_QUOTED']}  "
              f"range-only={counts['OK_RANGE']}  "
              f"absent-documented={counts['ABSENT_DOCUMENTED']}  "
              f"BAD={counts['BAD']}")
        for b in bad[:12]:
            print(f"    BAD {b[1]}:{b[2]} — {b[3]}")

    print(json.dumps(report), file=sys.stderr)
    sys.exit(1 if overall_bad else 0)


if __name__ == "__main__":
    main()
