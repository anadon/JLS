# Issue #836: TASK-C333-4: a design whose boundary lookahead is too low is refused by name, naming the boundary that caused it, instead of running slowly and silently
- Lens: adversarial
- Date: 2026-08-08
- Reviewer: Claude Sonnet 5
- Verdict: needs-rework

## Summary

The engineering instinct — fail fast and name the offending boundary rather than let a
poorly-lookahead-provisioned design silently degrade toward serialization on a cluster —
is sound, and the hand-off from TASK-C333-3 (#834, "this task consumes a lookahead it is
given") is clean. But the acceptance criteria lean on a "boundary"/"cut" abstraction that
does not exist anywhere in this codebase, the headline anti-gaming criterion (AC-3) is
satisfiable by a threshold that provides no actual protection, AC-4's regression guard is
verifiable today only vacuously, and the issue never says which of the project's three
enforced error-reporting paths carries the refusal.

## Findings, most severe first

### 1. [High] AC-1/AC-2 presuppose a "boundary"/"cut" abstraction that has not been built, and it is not in this issue's own `ordering_after`

AC-1 requires lookahead be "computed per boundary from the elements at the cut" and
"unit-tested against a cut whose minimum delay is known by construction." AC-2 requires
the refusal to name "the boundary." I verified directly against this checkout:

```
$ grep -rliE "boundary|PartitionSet|BoundaryDescription" src/ test/
(no output)
```

Nothing in `src/` or `test/` models a partition boundary or a "cut" of elements today.
Parent issue #333 (FEAT-056) states this explicitly as a hard precondition of the whole
family: "there is nothing to synchronize between until the design exists as parts with
named boundaries" — that's FEAT-055 (#332), which the independent review of sibling task
#839 confirms is unbuilt ("ABSENT" at every planned scope). #836's `ordering_after` names
only `"TASK-C333-3"` (#834, the advance rule) — #332 appears nowhere, exactly the gap
already found on #839 for the same reason. An implementer picking up #836 today has no
"cut" to compute a lookahead from and no boundary object to name in a message.

**Recommendation:** add #332 (and whatever boundary-description artifact FEAT-055
lands) to `ordering_after`, or state explicitly that AC-1/AC-2 cannot be exercised until
a partitioned design exists to have boundaries at all.

### 2. [High] AC-3 is satisfiable by a threshold that defeats the issue's own stated purpose

AC-3: "the threshold is declared once, as a named constant, with its basis recorded next
to it — a reader can find out why it is that number." This requires the basis to be
*recorded*, not that it be *principled* or that it actually catch anything. A constant
declared as `MIN_LOOKAHEAD = 1` with a one-line comment ("chosen as the smallest
representable delay") satisfies AC-3's literal text, and combined with AC-4 ("no existing
fixture gains a new refusal") gives an implementer every incentive to pick the most
permissive number that clears the existing suite — which is precisely the "mystery
slowdown on a cluster... which nobody can attribute" scenario the issue's own Outcome
section exists to prevent. Nothing in AC-1 through AC-5 requires a fixture that
*should* be refused to actually be refused. (This mirrors, and independently confirms
from #836's own text, a gap already flagged against parent #333's criterion 4.)

**Recommendation:** add an acceptance criterion requiring a fixture with deliberately
near-zero lookahead (e.g., a boundary crossing a zero-delay combinational path) that
*must* trigger the refusal — so the threshold is exercised by a case that would
otherwise silently serialize, not merely declared with a comment.

### 3. [Medium] AC-4's regression guard is vacuous against today's fixture corpus

AC-4: "no existing fixture gains a new refusal; designs above the threshold are
unaffected." Every fixture currently in `test/` is a single, whole (unpartitioned)
design — finding #1 established that no partitioning mechanism exists yet, so no
fixture in the repository today has a "boundary" at all. Run against the current
corpus, a check for "no new refusals" is trivially true regardless of whether the
lookahead computation or the threshold is correct, because the computation has zero
inputs to run against. As written, AC-4 cannot detect a regression until FEAT-055
lands enough partitioned fixtures for it to have something to check — and the issue
gives no indication the author has connected AC-4's real testability to that
dependency.

**Recommendation:** either scope AC-4 explicitly to "the whole-design golden suite,
run at partition count 1, is unaffected" (testable today, but weaker than the stated
claim), or make it depend on a minimal partitioned fixture set from FEAT-055/#332 and
say so.

### 4. [Medium] The refusal's delivery mechanism is unspecified against a codebase that enforces exactly three

ARCHITECTURE.md's "Error-reporting contracts" section is explicit: load failures are
`LoadError`s from a fixed, test-enforced category taxonomy (`IO_ERROR`, `NOT_A_CIRCUIT`,
`MALFORMED`, `NEWER_FORMAT`, `UNKNOWN_ELEMENT`, `ELEMENT_ERROR`, `LIMIT_EXCEEDED`);
user-visible dialogs go only through `TellUser` (`NotificationRatchetTest` bans other
`JOptionPane` call sites); and the CLI contract (#42, `JLSStart.java`) fixes diagnostics
to one `jls: error: ...` line on stderr with exit codes 0/1/2. None of these obviously
fits "a design whose minimum lookahead falls below a declared threshold is refused
before the run starts, naming the design and the offending boundary" for what is, per
#333, a cluster/multi-host launch — a scenario this project's current CLI has no flag
for at all (`grep -n '"-' src/jls/JLSStart.java` shows no cluster/partition/host flag
today). #836 never states which of the three sanctioned paths carries the message, or
what category/exit code applies. Left unspecified, an implementer is equally likely to
bolt on a fourth ad hoc reporting path that `NotificationRatchetTest` or the `LoadError`
taxonomy tests were built to prevent.

**Recommendation:** name the delivery mechanism explicitly (new `LoadError` category?
CLI-only stderr line with a specific exit code? something else for the not-yet-existing
cluster launch surface?) as an acceptance criterion, not left implicit.

### 5. [Low] `ordering_after` cites `"TASK-C333-3"` with no resolved issue number

Same defect independently found on sibling #839 for its own citation of the same task:
a reader of #836 alone cannot tell that `TASK-C333-3` resolves to the filed, open #834
without separately searching the tracker.

**Recommendation:** cite `#834` alongside the label, matching how other resolved
dependencies are cited elsewhere in this family.

### 6. [Low] `band_mw: "2-3"` does not obviously price the synthetic test fixture AC-1 requires

AC-1 demands a unit test "against a cut whose minimum delay is known by construction" —
since no boundary-cut fixture-building tooling exists yet (finding #1), this task
implicitly includes building that scaffolding from scratch, on top of the lookahead
computation and refusal wiring itself. The estimate carries no visible breakdown showing
that cost is included, and it lands inside a family (#333's six filed children) already
found to sum 16-25 mw against #333's own stated 10-18 mw band.

**Recommendation:** show what "2-3" covers, or fold it into the same band-vs-sum
reconciliation #333's Completion Criteria already requires of the family.

## What's solid (one line each)

- The core motivation — a named, attributable refusal beats a silent slowdown nobody can
  diagnose on a cluster — is the right engineering call for a conservative-discipline
  distributed simulator.
- Deriving lookahead "per boundary... not taken as a global constant" (AC-1) is the
  correct granularity given #834's own advance-rule formula (`t ≤ min_j(T_j + L_j)`,
  per-peer, not global).
- The scope hand-off from #834 ("this task consumes a lookahead it is given" / "the
  lookahead value and the refusal... are TASK-C333-4") is clean and avoids duplicating
  #834's advance-rule work.
- AC-5 ("the refusal is a refusal, not a warning — the run does not start and then
  degrade") is unambiguous and directly testable in isolation, once a refusal path
  exists to test.

## Verdict rationale

The idea is right and the scope boundary against #834 is well drawn, but the issue is
not safe to hand to an implementer as written: two of its five acceptance criteria
(AC-1, AC-2) depend on a "boundary"/"cut" abstraction this repository does not have and
that isn't in the issue's own dependency list; the anti-gaming criterion (AC-3) can be
satisfied by exactly the permissive, unprincipled threshold the issue's Outcome section
says it exists to prevent; the regression guard (AC-4) has nothing to regress against
yet; and the refusal's delivery mechanism is left unspecified against a codebase that
enforces a fixed, tested taxonomy of error-reporting paths. `needs-rework`.
