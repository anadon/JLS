# Issue #486: FEAT-058: a net can say how long it is and a driver how fast it switches, and `jls -check` says whether the lumped model the design is simulated under is still valid
- Lens: adversarial
- Date: 2026-08-08
- Reviewer: Claude Sonnet 5
- Verdict: sound-with-concerns

## Summary

Open issue. The code anchors this issue's claims rest on (`WireNet.java`,
`Adder.java`, `docs/simulation-semantics.md`, `docs/file-format.md`,
`docs/capability-roadmap/sweep-02-timing.md`) all check out exactly against
current HEAD, and the transmission-line arithmetic ($l_{crit}=v t_r/6$,
worked to 74AC/74LS/breadboard figures) is internally consistent — I
recomputed all seven worked numbers and they match. The DAG bookkeeping
(`blocked_by: [367, 336, 319]`, `blocks: [487]`) is also solid: I fetched
#367, #336, #319 and #490 directly and every mirrored edge this issue
claims is genuinely present on the far side. What's wrong is concentrated
in the evidentiary basis for cost/scope, one unresolved physics ambiguity
that undermines the issue's own flagship example, and two acceptance
criteria that can pass without the feature working.

## Findings, most severe first

### 1. The cited planning corpus never existed on any branch this repo descends from

Evidence §7 and the Cost reconciliation cite `docs/plan/capstones/CAP-18-...md`,
`docs/plan/features/FEAT-058-...md`, and `docs/plan/evidence/highfreq-determination.md`
as the source of the scope table and every cost-band derivation, with the
caveat that they "do not resolve at `2d0ca9d`" but land later in `3a81a4a`.
That framing implies the docs exist on the default branch today, just not
at the older evidence commit. They do not exist at all: `docs/plan/` is
absent from the working tree (`find /home/user/JLS -iname "*docs/plan*"`
returns nothing), and `git log --oneline HEAD -- docs/plan` returns **zero
commits** — the directory never touched this branch's history. It only
ever existed on `origin/claude/jls-virtual-hardware-linux-njsoma`, where it
was added and then removed in commit `742da74` ("docs: remove the planning
corpus now that it is encoded in issues... The maintainer ruled that this
branch will not be merged and will be deleted"). `742da74` is not an
ancestor of `HEAD` or of `master`. So the "not present at `2d0ca9d`, landed
at `3a81a4a`" framing is misleading by omission — the honest statement is
"never present on any branch that ships," which the issue's own Completion
Criteria ("Every cited evidence document and permalink resolves on the
default branch at close") can never satisfy for these three paths as
written. The cost band (3-6 mw) and the physics-derivation citation both
rest on documents nobody reviewing this issue can open. (The sibling
review of #487 independently found the same defect for its own citations
of this corpus — this is a shared, not isolated, problem across the
CAP-18 trio.)
**Recommendation:** strike the `docs/plan/` citations or replace them with
in-issue citations (as #486 already does for the worked arithmetic), and
correct the Completion Criteria checkbox to not depend on paths that will
never resolve.

### 2. The velocity default is an unresolved open question that contradicts the issue's own flagship example

§ Open Questions, question 2, asks what propagation velocity applies when
a net declares a length but no medium, recommending default (b): "default
to FR-4 stripline v = 1.4457e8 m/s and print the assumption," marked
**"must be settled before the report's golden is frozen"** — i.e. genuinely
unresolved. But the Intended Audience section's headline motivating example
is explicitly **not** FR-4: "a 150 mm jumper is 2.1x critical length for a
74AC part (v = 0.7c...) and 0.24x for a 74LS part" — a breadboard, velocity
≈ 2.1e8 m/s, nearly 50% faster than the recommended FR-4 default. Under the
committed two-attribute interface in §3 ("Provides. Two declared attributes
and one report... neither adds a third"), velocity is not a declared
attribute at all — so if default (b) ships as recommended, the breadboard
example that headlines the whole issue's pedagogical case would silently
compute a **wrong** critical length (using 1.446e8 instead of 2.1e8 m/s,
understating $l_{crit}$ by ~31%) unless velocity is somehow smuggled in as
a third input, which criterion 3 of § Integration Criteria explicitly
forbids ("neither adds a third"). The issue never reconciles this: either
velocity must be declarable (making it a third attribute, contradicting
criterion 3), or the FR-4 default silently mis-scores the exact breadboard
scenario the issue leads with.
**Recommendation:** resolve Open Question 2 before implementation starts,
not after; if velocity must be declarable per net/medium, update §3's "two
attributes" framing and criterion 3's wording everywhere it is repeated
(this issue, #487, #490) rather than treating it as a footnote.

### 3. Acceptance criterion 2 is trivially satisfiable and proves nothing about the lint's correctness

Integration Criterion 2 — "the lint is quiet where nobody opted in" — is
tested by running over `examples/` with default attributes and observing
"not assessable" everywhere. But Global Invariant 1 already guarantees
"both attributes are absent by default," and no example circuit in
`examples/` declares either new attribute (they can't yet — the attributes
don't exist). So this criterion is true **by construction** the moment the
two attributes default to absent, regardless of whether $l_{crit}=v t_r/6$
is computed correctly, off by a sign, or not computed at all. It cannot
distinguish a correct implementation from a stub that always prints "not
assessable." The only criterion that actually exercises the arithmetic is
Integration Criterion 1 ("run the lint over a table of (edge rate,
declared length, propagation velocity)"), and that table is left completely
unspecified: no row count, no coverage requirement (does it need to hit
both PASS and FAIL verdicts? boundary cases at ratio == 1?), and the test
class itself "does not exist yet." An implementer under schedule pressure
could pass criterion 1 with a single trivial row and still ship a broken
lint that happens to also pass criterion 2 for free.
**Recommendation:** before this is picked up, pin the table in criterion 1
to specific required rows (at minimum: ratio < 1, ratio == 1, ratio > 1,
and both single-input-missing cases), and note in criterion 2 explicitly
that it is a regression guard on defaults, not a correctness test.

### 4. The strictness constant `k`'s storage location is unspecified and threatens the "exactly two attributes" invariant

$k$ is "exposed... as a declared parameter" in the interface transformation
section, but nowhere does the issue say *where* it is declared: a per-net
attribute, a circuit-level setting, or a `-check` CLI flag. This matters
because Integration Criterion 3 — arguably the issue's most load-bearing
claim, repeated verbatim in #490's Global Invariant 5 ("no third attribute
was added") — asserts that FEAT-059 (#490) and FEAT-060 (#487) both consume
*exactly* the two attributes this feature declares "and neither adds a
third." If $k$ rides as a per-net attribute (the natural reading of
"declared parameter" next to "declared length" and "declared edge rate"),
it silently becomes a third net-level input the two downstream rungs may
or may not need to consume, quietly invalidating the "not throwaway"
argument the whole CAP-18 permanence ordering rests on. If instead $k$ is
a CLI flag or circuit-level global, it doesn't ride FEAT-047's format bump
the way the declared length does, and the "riding rather than minting a
format version" cost argument in Evidence §1 doesn't apply to it.
**Recommendation:** state explicitly in §3 where $k$ lives and thread that
choice through criterion 3's wording on this issue and #490's.

### 5. Cost band is an admitted non-reconciliation, not a number to plan against

The Cost reconciliation paragraph is commendably honest that the carried
figure (3-6 mw) doesn't match any of its own three derivations (3.5-7 mw
staged path, 2.5-5 mw permanence itemisation, 0.5-3 mw format-bump
residual) and that "no number was adjusted to fit." That transparency is
good practice, but it means a scheduler who reads "3-6 mw" as an estimate
is trusting a number the issue itself says none of its derivations
reproduce — worth flagging as a planning risk even though the issue does
not hide it.
**Recommendation:** pick one derivation method as authoritative (or widen
the printed band to cover all three, e.g. 0.5-7 mw) rather than carrying a
number no calculation actually produces.

### 6. Process overhead is large relative to the described code change

For a feature explicitly scoped to "no element type, no palette entry, no
format version of its own, no GUI surface and no solver" — in code terms,
two small field additions plus a report command — the issue carries a full
capstone-style apparatus: a YAML dependency block, a mermaid DAG, a
five-paragraph re-planning protocol, and cross-references into three other
multi-thousand-word sibling issues (#367, #336, #319, #487, #490) that
must each be independently understood before this issue's ordering claims
can be trusted (as this review had to do). That overhead is not itself
wrong, but it is real cost not counted anywhere in the 3-6 mw band, and it
is a lot of ceremony to load onto a contributor before they write the
first line of the lint.

## What's solid

- Every code/doc citation pinned to the current tree (`WireNet.java:22-30`,
  `Adder.java:33`/`:261`, `docs/simulation-semantics.md:26`,
  `docs/file-format.md:220-222`, `sweep-02-timing.md:110`) is accurate.
- The $l_{crit}=v t_r/6$ arithmetic and all seven worked numeric examples
  independently recompute correctly.
- The `blocked_by`/`blocks` mirrored edges against #367, #336, #319, #487
  and #490 are genuine and consistent when checked against those issues'
  own machine blocks — the DAG bookkeeping itself is not the problem here.
- Scope discipline is real and specific: invariant 4 makes a change to
  `WireNet.propagate` "out of contract, not merely out of scope," which is
  a testable, falsifiable boundary rather than a vague promise.
