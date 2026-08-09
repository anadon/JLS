# Issue #391: TASK-0057: multi-driver resolution stops depending on the order the wires were drawn — a commutative, associative, idempotent per-bit fold over a cached driver list
- Lens: adversarial
- Date: 2026-08-08
- Reviewer: Claude Sonnet 5
- Verdict: needs-rework

## Summary

The core engineering idea — replace positional first-driver-wins with a per-bit
commutative/associative/idempotent fold, proven by exhaustive test — is sound and the
code-level observations (O1, O2, O4, O5) check out against the current tree
(`src/jls/elem/WireNet.java:443-485`, `:406-407`, `:132-147`, `:277-288`; 18 files still
reference `TriStateOff`). But the issue's single stated hard prerequisite is dead on
arrival: it names an issue that was closed as a duplicate less than 30 minutes after the
comment naming it was posted, and the issue has not been updated since. This is not a
hypothetical risk, it is the issue's current, verifiable state.

## Findings, most severe first

### 1. [HIGH] The prerequisite this task is `blocked_by` no longer exists as filed, and the issue's own comment thread already shows the pattern repeating

§ Status says the unfiled prerequisite ("TASK-0056") is real and non-optional: "the fold
is inexpressible... over `@Nullable BitSet`" (O3). Comment #1 (2026-08-08T18:27:33Z)
"discharges" that gap by declaring "the value-representation swap is filed as **#881**...
Read every 'TASK-0056' reference in this body as #881. ... The machine block reads
`blocked_by: [881]`."

I fetched #881 directly: `"state":"closed","state_reason":"duplicate"`, closed at
2026-08-08T18:53:58Z — **26 minutes after** the #391 comment that named it as the
prerequisite. #881's own closing comment says it was superseded by #878 + #879
("cross-agent duplicate... Lower number wins"). So as of today (2026-08-09), the
authoritative prerequisite for #391 is #878 (the value type) plus its still-unnumbered
sibling (#878's `blocks:` field literally reads `- 999999  # TASK-C232-2 ... replace with
its number at the link pass`) — but #391's body and its only comment do not mention #878
at all. A contributor who reads #391 today and follows its own explicit instruction
("Confirm TASK-0056 has landed" in §8, cross-referenced to #881) will look at a closed
duplicate and either stall or, worse, treat the dependency as satisfied because "the
referenced issue is closed."

This is exactly the failure mode the issue itself describes happening to it once already
(TASK-0056 → #475 closed duplicate → #232 → #881, now closed duplicate → #878/#879). The
issue has now gone stale on its own prerequisite twice in under a week, and nothing in the
process caught the second occurrence.

**Recommendation:** before any implementation work starts, re-verify `blocked_by` against
the live issue tracker rather than trusting this issue's comment thread, and get the
machine block edited (not just re-commented) to point at #878 and its plumbing sibling.
Do not start on the strength of comment #1's "Read every 'TASK-0056' reference as #881."

### 2. [HIGH] Comment #1 asserts a `blocked_by` edge on #387 that #387's current body does not carry

Comment #1 §2 says: "#387... now carries `blocked_by: [391]` — the edge its own body said
'a link pass adds the number' and no link pass ever added." I fetched #387 directly. Its
machine block, as it stands today, reads:

```yaml
blocked_by: []         # see the note: the TASK-0057 edge is real and is added in the link pass
```

`blocked_by` is still `[]`. The comment's own factual claim about the *current* state of a
sibling issue is false at the time I checked it (and presumably was already false when
posted, since a comment cannot edit another issue's YAML block). This means the "narrowed
`blocks` edge" (`blocks: [387]` instead of `blocks: [341]`) that comment #1 asserts as
settled is a one-sided claim — #391 says it blocks #387, but #387 does not (yet) say it is
blocked by #391. Anyone doing dependency-graph tooling off the machine blocks alone will
get an asymmetric, silently-wrong graph.

**Recommendation:** treat comment #1's relationship claims as proposals, not settled fact,
until the target issues' own YAML blocks are edited to match. Verify #387's `blocked_by`
before closing #391 (DoD line: "`blocked_by` is empty; the TASK-0056 edge... has been
added by the link pass" — the same standard should apply to the reverse edge into #387,
but the DoD never asks for it).

### 3. [MEDIUM] The acceptance criteria are gameable on the algebraic-law proof

§10 H1-refutation and the DoD both require the three laws "proven by exhaustive test... not
asserted in a comment," and P1/P2 specify permutation tests over "the whole alphabet." But
the *size* of the alphabet is an open question the issue defers to elsewhere (Open Question
1: three planes vs. five states vs. two-plane IEEE 1364 pair — "Blocks execution... decides
the operator's domain"). A implementer under schedule pressure could ship
`foldIsOrderIndependentOverEveryDriverPermutation()` against whatever alphabet #881/#878
happens to expose (today: nothing, since the type doesn't exist), pass P1/P2 by construction
over a 3-symbol or incomplete alphabet, and satisfy the letter of "exhaustive... over the
whole alphabet" while not actually covering `U` or reserved code points — the issue's own
Threats to Validity §11 flags this ("Exhaustive permutation is only exhaustive over the
alphabet chosen... P1 certifies a subset") but does not make alphabet completeness a
checkable, numbered DoD item; it only appears as a *threat*, not a *gate*. A reviewer
skimming the DoD checklist (which is 25+ items) can plausibly miss that the exhaustiveness
claim needs to be checked against the alphabet decided by a *different, not-yet-existing*
issue.

**Recommendation:** promote "P1/P2 cover every code point defined by #878's frozen field
list, not a subset" from a Threats-to-Validity prose note into its own DoD checkbox with a
concrete count (e.g. "N=4 or N=5 symbols, cross product size stated and matched against
#878's enum").

### 4. [MEDIUM] "No code path selects a driver by position" is checked by a text grep, which a semantically-position-dependent implementation can trivially pass

P7 / DoD: `git grep -n 'first active driver in net order'` returning nothing outside
history is offered as evidence that no path is positional. That's a grep over a specific
English phrase in a warning string and a comment — it proves the *text* changed, not that
the *algorithm* changed. An implementation that folds `List<LogicVector>` with an operator
that is silently non-commutative in some corner (e.g. mishandles the reserved `U` code
point, or a five-value alphabet where the "commutative, associative, idempotent" table in
§7.10 wasn't extended past `{0,1,X,Z}`) would still pass the grep. The real check is P1/P3
(permutation invariance, reviewer reordering `ends`), which the issue does specify — but
P7's grep is listed as an independent, separately-checkable DoD line, and a rushed reviewer
could tick P7 off without re-running P1/P3, since they're in different bullets.

**Recommendation:** fine as a secondary sanity check, but the DoD should state explicitly
that P7 is necessary-not-sufficient and cannot substitute for P1/P3's permutation proof —
worth a one-line note to close the gap for whoever grades the PR against this checklist.

### 5. [MEDIUM] Scope boundary between this task and #881/#878's successor is not self-enforcing, and the issue admits it

§7.4 says the operator's callers are "`WireNet.propagate`, and later TASK-0058's strength
extension, which is why the operator must be a value-level function rather than a method on
`WireNet`" — reasonable. But §7.12 simultaneously requires this task to retire neither
`Splitter`/`Binder`'s whole-port HiZ special cases (deferred to "the mode flip") *nor* fix
the `makeNet`/`recheck` width discrepancy (O5, deferred to a separate filed issue) *nor*
migrate the 18-file `TriStateOff` surface past "deprecated alias, sealed arm kept one
epoch." All three are real, adjacent temptations for an implementer touching exactly this
code (they'll be staring at `Splitter.react`, `Binder.react`, and `makeNet`/`recheck` while
building the driver-list cache in O5's own two walks). The issue does flag this ("Threats
to Validity... booby-trapped") but the enforcement mechanism is entirely social ("adjacent
work discovered en route is filed as new issues" — one DoD bullet, unaudited by any
listed command). Given how this repo's issue graph has already shown (see finding 1) that
"file a follow-up and update the parent" reliably fails to happen even within the same
day, this is a real risk, not a theoretical one.

**Recommendation:** acceptable as scoped, but flag in the PR template/reviewer checklist
that a diff touching `Splitter.java`, `Binder.java`, or the `makeNet:139`/`recheck:280`
width lines beyond what's needed for the driver-list cache should be rejected outright
rather than "recorded."

### 6. [LOW] `evidence_commit` is already stale for #391 specifically, though the fix is easy

The pinned `2d0ca9dcd9db78b36c3caf4c6c57dd8701862cc7` does not resolve in this checkout
(`git cat-file -t` → "commit" is misleading here — actually confirmed both hashes resolve
oddly in this container's `git cat-file -t`, but #493's own audit — filed the same day —
records `2d0ca9d` as living only on a to-be-deleted branch, with `828822672fc3a8e2cb6da2
5192472079f04c29dd` as the durable master-branch replacement). #391 is not in #493's list
of "issues carrying an affected citation" (the seven branch-only files), and I independently
verified `WireNet.java:443-485` and `docs/simulation-semantics.md` §9/§10 match the issue's
quoted text at current HEAD, so the *content* citations in #391 are not actually broken —
but the issue still declares the dying commit hash as canonical and has not been updated to
point at #493's replacement pin the way #878/#881 were.

**Recommendation:** low-cost, do it in the same pass as finding 1 — update `evidence_commit`
to `8288226...` per #493's ruling.

## What's solid (no action needed)

- The core technical claim (O1-O5) is accurate against the current tree: `WireNet.propagate`
  is genuinely positional, the conflict-detection is genuinely a boolean rather than a value,
  and the 18-file `TriStateOff` surface and the `makeNet`/`recheck` width discrepancy are both
  real and independently verifiable.
- The algebraic framing (commutative/associative/idempotent, Z as identity, disagreement to
  X) is a legitimate and well-specified operator, and the two-state backward-compatibility
  requirement (H2, P5, P6) is concrete and testable against named, existing tests.
- Explicitly deferring `Splitter`/`Binder` HiZ retirement and the width discrepancy, rather
  than silently doing them here, is the right scoping call even though enforcement is weak
  (see finding 5).
- The falsification criteria (§10) are genuinely falsifiable and name concrete next actions,
  which is more than most issues in this tracker manage.

## Bottom line

The technical design is not the problem; the dependency bookkeeping is. This issue cannot be
picked up as written today without first re-resolving `blocked_by` against the live tracker
state (finding 1), and its own comment thread's claims about sibling-issue edges should not
be trusted without independent verification (finding 2). Fix the prerequisite pointer, tighten
the alphabet-completeness and position-freedom DoD items (findings 3-4), and this is workable.
