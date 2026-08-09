# Issue #581: FEAT-C34-3: `brew install --cask jls` installs the dmg, and the cask's caveat tells a macOS user about Gatekeeper before they hit it
- Lens: adversarial
- Date: 2026-08-08
- Reviewer: Claude Sonnet 5
- Verdict: needs-rework

## Summary of the ask

A Homebrew cask for JLS, wrapping the existing macOS dmg, with a
Gatekeeper-workaround caveat kept in lockstep with the README, and a
release-workflow step that bumps the cask's version/sha256
automatically. Filed as PF-3 of the CAP-34 distribution capstone
(#518). Three sub-tasks exist in the repo's issue-reviews history
(#858/TASK-C581-1, #859/TASK-C581-2, #860/TASK-C581-3) that
self-identify as children of this issue, though `get_sub_issues` on
#581 returns an empty list — the parent/child link is asserted in
prose on the children but not wired up as a GitHub sub-issue
relationship on #581 itself. Minor bookkeeping gap, not a blocker.

## Findings, most severe first

**1. AC-5's verification target does not exist anywhere in the
repository.** AC-5: "The per-release cask update-and-review cost is
recorded against KC-34-1's 0.5 mw threshold." I grepped the full tree
(`grep -rn "KC-34" --include="*.md"` outside `issue-reviews/`) and
found zero matches — no ledger file, no threshold definition, nothing
naming KC-34-1 except the capstone #518 itself (which states the kill
criterion but doesn't operationalize "0.5 mw" into anything a script
or a human could check a number against). An acceptance criterion that
says "record cost X against threshold Y" when Y is undefined can be
satisfied by writing down any number and calling it compliant — it is
unfalsifiable as written. **Recommendation:** either define KC-34-1's
measurement method (what counts as "review cost," how it's timed) in
#518 or a ledger doc before #581 is picked up, or drop the "recorded
against KC-34-1" clause from AC-5 and replace it with a self-contained
measurement (e.g., "record wall-clock time from tag push to cask
merge for N releases").

**2. The boundary note cites a section of #82 that does not exist.**
Quoted from #581: "#82 §10 and #128 record the macOS
signing/notarization position." I fetched #82 in full: its body has
exactly seven numbered sections (`## 1. Capability Statement & Scope
Boundary` through `## 7. Re-planning Protocol`), followed by
unnumbered `## Open Questions & Decisions Needed` and `## Completion
Criteria`. There is no `§10`. The actual macOS-unsigned content lives
in #82's "Out of scope (owned elsewhere)" bullet ("macOS signing —
**closed won't-fix** (#128, #135): the `.dmg` ships unsigned by
choice, documented") and in its "Adjudication record" paragraph. This
phantom `§10` citation also appears in #443 (twice) — it is a
systemic defect in this corpus, not unique to #581 — but here it sits
inside the sentence that tells an implementer where to go verify "the
unsigned-by-choice stance is inherited, not re-decided here." Anyone
following the citation to check what's actually decided will not find
a §10 to read. Note also that #581 cites only `(#128)` where README.md
line 40 and #82's own §1 cite the pair `(#128, #135)` — an unexplained
narrowing of which closed issues back the stance. **Recommendation:**
fix the citation to `#82 §1 ("Out of scope") / #128 / #135 / #338`
before this becomes the text an implementer is asked to verify
"unchanged" against.

**3. AC-1 and AC-3 are silent on architecture, but the underlying dmg
is Apple-silicon-only.** README.md:37 is explicit: `JLS-<version>-
aarch64.dmg` (Apple silicon) — "Intel Macs: use the jar below." There
is no x86_64 dmg. A Homebrew cask with only an arm64 artifact must
either declare `depends_on arch: :arm64` (so `brew install --cask jls`
fails cleanly with a clear message on Intel) or the issue is silent on
what happens when an Intel user runs the exact command the title
promises. AC-1 as written ("on a clean macOS machine") doesn't say
which architecture that machine is, so the criterion could be
satisfied by testing only Apple silicon while Intel is left broken or
undocumented — the acceptance test is gameable by omission. AC-3's
verbatim-caveat requirement compounds this: the README bullet this
issue points at *also* contains "Intel Macs: use the jar below," a
sentence that only makes sense in the context of a full install-matrix
list and reads as a non sequitur inside a cask caveat shown to someone
who has, by definition, just installed the arm64 dmg via brew.
**Recommendation:** add an explicit architecture-gating criterion
(`depends_on arch: :arm64` with a tested failure message on Intel),
and scope AC-3's "verbatim" quote to a sentence range that excludes
the Intel-Mac aside, or state that the caveat text is a paraphrase-
with-fidelity-check rather than a literal substring match.

**4. AC-3's "asserted equal to the README's Gatekeeper paragraph" has
no defined boundary, so the drift check it mandates is gameable.** The
Gatekeeper text is not a standalone paragraph — it's embedded mid-
bullet inside README.md's macOS list item (lines 37-43), sandwiched
between the dmg filename/architecture note and the "Intel Macs" aside
discussed above. "The README's Gatekeeper paragraph" has at least
three plausible readings (the whole bullet; the two Gatekeeper-
specific sentences; a still-narrower substring), each yielding
different cask caveat text and each satisfying a naively-written test
that just diffs against whatever range the implementer picked. Because
AC-3's stated purpose is "if the README narrows or changes, the cask
fails rather than diverging," an unscoped anchor defeats that purpose:
a future README edit outside the (unspecified) chosen range would
silently escape the check. **Recommendation:** name the exact anchor
(e.g., a markdown comment `<!-- gatekeeper-caveat:start/end -->`
bracketing the intended span) so both the cask generator and the drift
test read the same substring.

**5. AC-2's "no manual step beyond approval" glosses over a real
external dependency the issue itself leaves open.** The boundary notes
say: "Whether the cask is submitted to `homebrew/cask` or served from
an own tap is an open question for the executor." If the executor
picks `homebrew/cask` (the literal reading of the issue title, which
promises a bare `brew install --cask jls` — a project tap would
typically require `brew install --cask anadon/jls/jls` or a `brew tap`
step first, which AC-1 doesn't mention either), then "propagates...
with no manual step beyond approval" is not fully within this
project's control: `homebrew/cask` is a third-party repository whose
maintainers review and merge on their own schedule, via their own
bots and audit rules (`brew audit --cask`, notability heuristics).
"Verified once end to end" (AC-2) could pass on a single lucky fast
merge while the steady-state process routinely stalls for days,
undermining the very automation the AC is meant to certify. Neither
AC-1 nor AC-2 states which of the two submission paths the acceptance
run exercises, so the criterion is satisfiable under either branch
while only actually delivering the "no manual step" property under
one of them (a self-owned tap, where the release workflow can commit
directly). **Recommendation:** either commit to the tap path in this
issue (matching the literal `brew install --cask jls` promise, which
`homebrew/cask` submission cannot guarantee — Homebrew's naming rules
may not grant JLS the bare `jls` cask name at all), or rewrite AC-2 to
say "propagates via an opened PR to homebrew/cask, merge time outside
this project's control" if that branch is chosen.

**6. AC-2's "cask cannot silently point at a stale artifact" promise
is undercut by the dmg build leg's current CI status.** In
`.github/workflows/release.yml`, the `installers` job's macOS leg
(`macos-latest`, dmg) carries `experimental: true` (line 310) under
`continue-on-error: ${{ matrix.experimental }}` (line 312) — confirmed
directly in the checked-out workflow file, and independently
documented by #443/TASK-0027 as one of "four of five legs [that] are
`experimental: true`, so a broken installer cannot fail a release."
#581's own `ordering_after` comment acknowledges this ("the dmg
already ships; #443 TASK-0027 strengthens but does not gate") but AC-2
doesn't say what the release-automation step should do on a tag whose
dmg leg failed and produced no asset: skip the cask bump (leaving it
pointing at the previous release, silently one version behind — the
exact failure mode AC-2 claims to prevent), fail the whole release
workflow (a scope increase this issue never asks for), or publish a
cask update anyway with a stale sha256. This is a real, currently-live
edge case, not a hypothetical: at time of review the dmg leg is
best-effort by design. **Recommendation:** add an explicit AC (or
amend AC-2) specifying the automation's behavior when the tagged
release has no dmg asset — most likely, no-op with a loud CI warning,
never publish a wrong sha256.

**7. AC-4 depends on content that does not yet exist, with no ordering
edge to say so.** AC-4: "The listing metadata carries the project
description shared with CAP-27 (#511)." #511 (CAP-27) itself states
verified-at-filing: "the README has zero screenshots" and lists PF-1
("README shop window") as `unfiled`/not built. There is no single
"the project description" artifact CAP-27 currently owns to share —
the closest existing candidate is the one-line Maven
`<description>` in `pom.xml:15` ("An educational digital logic circuit
editor and simulator..."), which #581 never names. `ordering_after: []`
declares nothing blocks this feature, which is consistent for AC-1
through AC-3 but not obviously true for AC-4 if "shared with CAP-27"
means a description CAP-27 hasn't written yet. **Recommendation:**
either name `pom.xml`'s `<description>` as the authoritative source
now (cheapest fix, no cross-issue dependency), or add an explicit
`ordering_after: [511]` (or the relevant PF-1 sub-issue) if AC-4 truly
means to wait on CAP-27's shop-window copy.

**8. Solid parts, briefly.** AC-1's sha256-match-the-attested-release-
asset requirement is concrete, testable, and grounded in real
infrastructure — `SHA256SUMS-installers-<os>-<arch>` already exists
per README.md:49-52 and is exactly the file a cask-update script would
read. The `ordering_after: []` decision for the feature as a whole is
honestly reasoned rather than hand-waved: it explicitly weighs and
accepts the dmg leg's experimental status (see finding 6) instead of
hiding it. The unsigned-by-choice inheritance is the right call in
principle — this feature correctly declines to re-litigate a settled
decision — the problem is only the broken pointer to where that
decision lives (finding 2).

## Bottom line

The feature-level intent is sound and correctly scoped (surface an
existing, documented stance; don't re-decide it), but three of five
acceptance criteria have real gaps that would let an implementation
pass review while missing the goal: AC-5 checks against an undefined
number, AC-3's "verbatim" anchor is unscoped inside a bullet that
contains an irrelevant Intel-Mac aside, and AC-2's "no manual step"
and "cannot silently point at a stale artifact" promises both collide
with facts already on record elsewhere in this repository (the
homebrew/cask third-party review process, and the dmg leg's
`continue-on-error: true` status). None of these are fatal to the
outcome, but each should be tightened before an executor starts,
since two of them (findings 2 and part of 6) are citation/fact errors
this review could verify directly against the checked-out tree and
the cited issues.
