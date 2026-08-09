# Issue #583: FEAT-C34-5: the question "should JLS be a real Debian package?" gets a written go/no-go with its maintenance arithmetic, and "no, with reasons" is a legitimate answer
- Lens: adversarial
- Date: 2026-08-08
- Reviewer: Claude Sonnet 5
- Verdict: needs-rework

## Summary of the ask

A document-only feature (band 1-2 mw): research Debian's Intent-To-Package
(ITP) process, price the recurring maintenance burden in maintainer-weeks,
answer the sponsor question by name or "none found," and record a dated
go/no-go with what would flip it. Explicitly out of scope: touching the
existing `scripts/build-installer.sh` deb build (owned by #443/#338). Parent:
#518 (CAP-34), which marks this PF-5 as optional and gates it with
KC-34-2 ("proceeds only if a sponsor materializes; self-NMU at bus factor 1
is refused by name"). No comments on the issue at fetch time; issue is open
(confirmed via the GitHub API).

## Findings, most severe first

**1. (High) The issue frames archive-readiness as an open research question the corpus has already answered in-tree, and doesn't point the executor at it.**
`docs/standards-adoption/10-desktop-and-housekeeping.md` (§AppStream
metainfo) states, about the very deb this issue's evidence line leans on:
"JLS's deb installs into `/opt/jls`... which is fine for a third-party
package but is not the layout Debian or Fedora archives accept, so archive
inclusion is not on the table either." That is a load-bearing prior finding
directly contradicting #583's framing that "the deb artifact already exists
and installs" as if it were meaningful head start toward Debian inclusion.
The jpackage-produced deb bundles its own JRE and installs outside the FHS
paths Debian's archive requires; a real Debian package is not "gate the
existing build," it is closer to a from-scratch `debian/` packaging effort
(system-JRE dependency, no bundled runtime, correct install paths, DEP-5
copyright file, etc.) — which is exactly the kind of "packaging-policy
obligation JLS's bundled runtime would trigger" AC-1 asks the document to
write down, except the document is being commissioned as if this were
undiscovered territory. Recommendation: have #583 cite and reconcile with
`docs/standards-adoption/10-desktop-and-housekeeping.md` explicitly (it's
already sitting in the tree with the answer to a chunk of AC-1), so the
executor doesn't independently re-derive a conclusion the repo already
recorded, and so the "no, with reasons" branch can cite it directly rather
than starting from zero.

**2. (Medium-High) The sponsor gate (KC-34-2/AC-3) likely pre-decides the outcome, but the issue doesn't let the document short-circuit on it.**
AC-3 requires the sponsor question answered "by name or 'none found, searched
here'"; KC-34-2 says a go verdict is unavailable without one. For a
single-maintainer pedagogy tool with no established Debian-contributor
relationships, "none found" is the overwhelmingly likely outcome — at which
point the verdict is "no" regardless of what AC-2's cost arithmetic says.
Yet AC-1, AC-2 and AC-5 are written as unconditional, full-depth
requirements (concrete ITP steps, per-release *and* per-freeze cost with a
defensible derivation, "what would change the answer") with no sequencing
guidance. As written, an executor either (a) does the full costing exercise
even after a quick, decisive sponsor-search failure — wasted maintainer-weeks
on a document whose real decision already landed — or (b) skips the
arithmetic once the sponsor search fails, under-delivering against the
letter of AC-1/AC-2 while still plausibly claiming the feature "closes."
Recommendation: sequence AC-3 first and make AC-1/AC-2's required depth
explicitly conditional on a "no sponsor found" outcome (e.g., "if no
sponsor, AC-1/AC-2 may be sketched rather than fully derived, because they
are no longer decision-relevant").

**3. (Medium) The acceptance criteria are self-graded with no reviewer or falsification anchor, and AC-1's "who must do each" step is nearly vacuous at bus factor 1.**
Nobody but the filing maintainer is positioned to check AC-2's arithmetic
("so it can be argued with" — argued with by whom?), so a shallow or
motivated estimate (inflated per-freeze cost to justify a foregone "no," or
deflated to justify a foregone "go") satisfies the letter of every AC. AC-1's
"who must do each [ITP step]" is close to content-free for a project that is,
by the repo's own frequent self-description, bus-factor-1: the honest answer
to every step is "the maintainer, or a sponsor if AC-3 finds one," which
adds no information. Recommendation: anchor AC-2 to something checkable —
e.g., Debian's own published mentors.debian.net ITP/sponsorship timelines,
or the actual per-release cost once measured on the other CAP-34 channels —
rather than leaving the estimate's plausibility to the author's say-so.

**4. (Medium) Sibling kill-criterion KC-34-1 is silently not applied, though it looks directly relevant.**
CAP-34's KC-34-1 states a general, numeric rule for every channel in the
capstone: "a channel whose review/update process costs >0.5 mw per release
cycle is dropped with the arithmetic recorded." #583 cites only KC-34-2 and
never asks the document to test its own AC-2 output (maintainer-weeks per
release cycle) against KC-34-1's 0.5-mw bar. Since AC-2 already produces
exactly the number KC-34-1 needs, the omission reads as an inconsistency
rather than a deliberate exemption — nothing in #583 or #518 says PF-5 is
exempt from KC-34-1, but nothing applies it either. Recommendation: either
add "AC-2's estimate is checked against KC-34-1's 0.5 mw/cycle threshold" as
an explicit criterion, or state plainly that PF-5 is exempt from KC-34-1 and
why (optional/roster-completeness status is a plausible reason, but it isn't
written down).

**5. (Low-Medium) Cost/feasibility risk: the research depth AC-1 demands assumes Debian-packaging expertise nobody on this project currently has.**
Correctly writing "the packaging-policy obligations JLS's bundled runtime
would trigger" requires real fluency with Debian's stance on vendored
runtimes (Debian Java Policy, DFSG, the archive's practice of requiring
`openjdk-*-jre` dependencies rather than a bundled JRE) — a genuinely deep
area for a first-time investigator to get right rather than merely
plausible-sounding. `band_mw: "1-2"` (echoed verbatim from CAP-34's own PF-5
row) is optimistic for research-from-zero at this depth; a wrong-but-tidy
analysis that has to be redone later is a real risk the issue doesn't
budget for.

**6. (Low) "Per release cycle and per Debian freeze" mixes two cadences without specifying how they combine.**
JLS releases on `v*` tag pushes (README/ARCHITECTURE — no fixed calendar
cadence); Debian stable freezes run on a roughly two-year cycle with their
own RC-bug-response and transition-tracking obligations. AC-2 asks for both
numbers with "the derivation shown" but doesn't say over what horizon they
should be amortized or reconciled, so two executors could produce
non-comparable figures that both technically satisfy AC-2.

## What's solid

- Scope hygiene is good: the issue explicitly disclaims touching
  `scripts/build-installer.sh` or gating (owned by #443/#338), stays a
  document-only deliverable, and requires "go" work to be filed as a new
  issue rather than absorbed here (AC-4) — no scope creep here.
- Being marked optional in CAP-34 with no ordering edge is accurate and
  consistent with #518's `planned_features` and `ordering_after: []`.
- The issue is open, its title matches its body, and its KC-34-2 citation is
  a verbatim, accurate quote of #518's kill criterion — no fabrication or
  misquote found.

## Bottom line

The instinct — write down the arithmetic instead of debating it — is sound,
and the boundary against touching real packaging work is well drawn. But the
issue commissions research that a repo document already partially answers
(finding 1), lets a likely-decisive sponsor gate coexist with unconditional
full-depth costing requirements (finding 2), and leaves its central
numeric acceptance criterion (AC-2) both unfalsifiable (finding 3) and
disconnected from the sibling kill-criterion it should obviously be checked
against (finding 4). Recommend revising the acceptance criteria before this
is picked up: sequence the sponsor search first, cite the existing
desktop-housekeeping doc, and wire AC-2 to KC-34-1.
