# Issue #883: TASK-C880-1: the 30-submission corpus exists with its planted pairs declared and its independent solutions generated, not mutated
- Lens: adversarial
- Date: 2026-08-08
- Reviewer: Claude Sonnet 5
- Verdict: needs-rework

## Summary

This is task 1 of 3 under FEAT-C25-0 (#880), itself filed to give CAP-25
(#506) a cheap premise test before 14-21 mw is funded. The task is scoped
sensibly (build the fixture corpus, assert nothing about separation) and
its boundary against #884/#885 is the right shape. But the issue body
contains an unfixed internal contradiction in its own Boundary section, the
one open question that gates AC-1 is left unresolved at filing time, and
one of the three mandated "disguise" transforms (no-op buffers) assumes a
JLS primitive that does not exist in this codebase.

## Findings, most severe first

### 1. Boundary section is self-contradictory and the maintainer's own correction comment did not fix it

The issue body's Boundary section reads:

> **No scoring, no fingerprinting, no canonicalization here.** Those are #883.
>
> **No separation claim here.** That is #884, and it is the only place the word "separation" may appear in a result.

This issue *is* #883 — the first bullet points readers to itself for the
work it just said doesn't belong here. The one comment on this issue
(2026-08-08, by anadon/Claude Code) explicitly corrects a different field
(the YAML `blocks:` line) and establishes the authoritative roster:

> `883` = this issue (corpus/manifest), `884` = TASK-C880-2 (scoring/fingerprint,
> `blocked_by: [883]`), `885` = TASK-C880-3 (verdict, `blocked_by: [884]`)

Applying that roster to the Boundary prose: "no scoring here, those are
#883" should read "#884", and "no separation claim here, that is #884"
should read "#885". Both bullets are off by one and neither was corrected
by the comment, which only touched the machine block. A reader (or an
implementer scoping a follow-up PR) who trusts the prose over the buried
YAML fix will misfile scope — e.g. believe #884 owns the separation verdict
it doesn't, or that this task can defer scoring to itself.
**Recommendation:** before work starts, post a second correcting comment
(or accept a body edit if the repo's discipline ever allows it) fixing
both Boundary bullets to #884/#885 respectively, so the boundary claims
agree with the roster the first comment established.

### 2. The decision that gates AC-1 is left unresolved, so the task is not actually shovel-ready

AC-1 requires: "the assignment is small and stated... The choice is
written down with its reasoning." But Open Question 1 says:

> **Which assignment?** Recommended default: a 4-bit comparator or a 2-bit
> ALU slice... Blocks AC-1.

The issue's own "Why this issue exists" section frames this task as
"deliberately first and deliberately cheap" — implying it is ready to pick
up — but the load-bearing design decision (what the 27 independent
solutions are solutions *to*) is an unresolved open question the issue
itself says blocks the first acceptance criterion. This isn't fatal (open
questions with recommended defaults are this tracker's normal idiom), but
it means "cheap" is unverified: the actual difficulty of authoring 27
genuinely-distinct correct decompositions is entirely a function of which
assignment gets picked, and that pick hasn't been made or reasoned about
in the issue text itself, only deferred to a "recommended default."
**Recommendation:** resolve Open Question 1 (or explicitly ratify the
recommended default) before estimating/starting work, and move the
reasoning for the choice out of "open question" into the AC-1 prose itself
per what AC-1 actually demands.

### 3. AC-3's "inserted no-op buffers" transform presupposes a primitive this codebase doesn't have

AC-3 mandates three planted-pair disguises: "moved components, renamed
wires, inserted no-op buffers." ARCHITECTURE.md's module layout lists
"~30 concrete element classes (gates, register, memory, mux, state
machine, …)" in `jls.elem`, and a directory listing confirms there is no
`Buffer` class:

```
src/jls/elem/*.java  →  AndGate, OrGate, NorGate, NandGate, XorGate,
NotGate, TriState, DelayGate, ... (no Buffer.java)
```

`grep -ri buffer src/jls/elem` matches only `TriState.java` (a tri-state
buffer, which has an enable input and Hi-Z output state — not a
transparent no-op in JLS's two-state-plus-HiZ semantics per
ARCHITECTURE.md's simulation-strategy note) and `TriProp.java`. There is no
zero-effect pass-through gate to "insert." Implementers will have to
improvise — e.g. two chained `NotGate`s, or a `DelayGate` with delay 0 —
and the issue is silent on which. That silence matters here specifically:
AC-2 requires independent solutions to differ by "different gate
decompositions," and a double-NOT is exactly the kind of decomposition
trick an independent solution might legitimately use on its own. If the
planted-pair disguise and an independent solution's genuine decomposition
choice converge on the same construction, the corpus's manifest-declared
"transform class" for the planted pair becomes indistinguishable, in
graph-structure terms, from ordinary independent variation — quietly
undermining the very separation #884/#885 exist to measure, even though
this task's own boundary says it "asserts nothing about separation."
**Recommendation:** the issue should specify which JLS element(s)
constitute the "no-op buffer" transform, and AC-2's enumeration procedure
should be written to avoid using that same construction as a legitimate
decomposition choice, so the disguise stays distinguishable in principle
from independent variation.

### 4. AC-2's feasibility (27 non-degenerate independent solutions) is deferred to a "finding," which lets this task pass while quietly producing exactly the degenerate corpus #880 warns about

The Boundary section explicitly allows the outcome "if 30 is too small,
that is a finding to report," citing #880 KC-25-0-1. That kill criterion,
however, is scoped to the *measurement* task (#880/#884/#885), not to this
one — yet the actual risk (a small assignment yielding only superficially
different decompositions) is baked in at the moment this task authors the
27 solutions, several steps before anyone runs a fingerprint. Nothing in
this issue's own acceptance criteria requires the author to sanity-check
non-degeneracy before calling AC-2 satisfied (e.g., no requirement that the
27 solutions differ in some structurally checkable way, such as topologically
distinct netlists). As written, AC-2 can be satisfied by 27 files that
happen to differ (different wire names, different layout) while being
functionally and structurally nearly identical gate-for-gate — which
*looks* like it discharges "independence is structural, not asserted" while
in fact reproducing the mutation problem AC-2's own second sentence
prohibits, just laundered through "the author typed it out by hand instead
of running a mutator." The AC as worded checks provenance (a stated
procedure, no algorithmic mutation) but not structural diversity, so it is
gameable by a well-intentioned but rushed implementer under the "0.5-1 mw"
cost pressure noted below.
**Recommendation:** add a concrete, mechanically checkable diversity floor
to AC-2 (e.g., a minimum pairwise structural-edit-distance or gate-count
distribution requirement over the 27 solutions), not just "stated
procedure, not mutation."

### 5. Cost estimate looks optimistic against the actual deliverable list

`band_mw: "0.5-1"` for: deciding/justifying an assignment (contingent on
resolving Open Question 1), writing and committing an enumeration
procedure over correct decompositions, authoring 27 independent solutions
by hand under that procedure, constructing 3 planted pairs across 3
transform classes, writing a machine-readable manifest, and wiring a
mechanical functional-equivalence check (AC-4) against a vector file for
all of it — with the explicit prohibition that none of the 27 may be
produced by mutating another. This is a full day-plus of careful circuit
authorship even before the equivalence tooling is written, especially
since Finding 3/4 above mean "small" and "genuinely different" are in
tension the assignment choice has to resolve by hand. Not disqualifying,
but worth flagging as a likely underestimate before committing to the
band.

## What's solid

- **AC-4** (mechanical functional equivalence of planted pairs via the
  assignment's vector file) is concrete, testable, and grounded in
  capability JLS already has — the `-t` test-vector batch engine
  (README.md "Command-line options"; `docs/batch-interface.md`).
- **AC-5** (manifest as sole source of truth, no duplicate list in scoring
  code) is a clean, enforceable-later contract and correctly anticipates
  the coupling risk with #884.
- **AC-6** and the fixtures-not-people naming rule are unambiguous and
  consistent with CAP-25's evidence-not-verdict framing (#506 KC-25-3).
- The **exclusion of subcircuit repackaging** pending PF-1's flattening
  policy is consistent with both #880's Open Question 2 and #506's own
  cross-feature risk 5 — this task correctly declines to pre-decide a
  question that belongs upstream.
- The **boundary against #717/#300** (not the 300-submission CAP-06/CAP-21
  corpus) is accurate: #717 and #300 describe a different fixture format
  (lab-as-data plus golden score vectors) for a disjoint purpose.
- `blocked_by: []` is honest — nothing else in the tracker needs to land
  first for fixture authorship to begin, modulo Finding 2 above.

## Verdict rationale

Not `should-not-proceed`: the task's shape, boundary, and most acceptance
criteria are sound and the scope is genuinely small. Not `sound` or
`sound-with-concerns`: there is a live, uncorrected self-contradiction in
the shipped Boundary text (Finding 1), a load-bearing open question still
unresolved at filing (Finding 2), and a named transform class with no
grounding in the actual element palette (Finding 3) — each independently
fixable in the issue text without re-scoping the task, which is exactly
what `needs-rework` is for.
