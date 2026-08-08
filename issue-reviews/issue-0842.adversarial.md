# Issue #842: TASK-C370-1: per-element live heap is measured on a generated design at scale and committed as data, so every capacity claim in the feature has a denominator
- Lens: adversarial
- Date: 2026-08-08
- Reviewer: Claude Sonnet 5
- Verdict: needs-rework

## Summary

The task's stated goal — take a per-element live-heap measurement before the
layout change lands, so the capacity claim in #370 is falsifiable — is
methodologically sound. But the task as written is not actually startable:
its one named prerequisite doesn't deliver what it's cited for, its baseline
numbers trace to evidence that doesn't exist in this repository, and its
"generated design" fixtures don't exist and aren't scoped to build. These are
not nitpicks; they mean AC-1 and AC-2 cannot be satisfied as written without
first doing undocumented, unscoped work.

## Findings, most severe first

### 1. The cited prerequisite doesn't define the method this task needs (hidden assumption / broken dependency)

`ordering_after: ["#335 FEAT-009 (the measurement gate defines the method)"]`
and AC-1 both assume #335 hands this task a named live-heap-per-element
measurement method. Reading #335's own scope boundary (§1, "In scope"),
its six deliverables are: per-cycle active fraction, CPI, events-per-
instruction, a tracked CPU-scale fixture, an events-per-cycle **equality**
gate, and ns/event + **bytes-allocated-per-event** bands. That last item is
allocation *during simulation execution* (a churn/throughput metric), not
the *static live-heap footprint of a loaded, idle circuit model* that #842
needs. Nowhere in #335's six planned tasks (TASK-0022 through TASK-0026) is
a live-heap-per-element methodology produced — the closest artifact, the
`getThreadAllocatedBytes`/heap-snapshot method #370 uses in its own
evidence, isn't in #335's task table at all.

So either (a) #335 was never going to define this method and the
`ordering_after` line is wrong, or (b) the method is expected to be
invented inside #842 itself, in which case the dependency on #335 is
decorative and AC-1's "by the gate's named method" has no referent to
point at. Confirmed independently: no measurement code exists in the tree
at all (`grep -rn "getThreadAllocatedBytes\|ThreadMXBean" --include=*.java .`
returns nothing).

**Recommendation:** either drop the #335 dependency and let #842 define its
own method explicitly (stating that as a deviation from #370's plan), or add
the missing task to #335's roster first and truly block on it. Don't leave
AC-1 pointing at a method nobody has committed to build.

### 2. The baseline it claims to "re-take rather than quote" is unverifiable in this repository

The issue body states: "The existing figures — ~1,190 B on a 1,551-element
processor design and ~2,150 B on a 60,004-element wire-heavy chain — are
re-taken here rather than quoted." Those figures originate in #370, which
attributes them to `docs/plan/evidence/` and cites two specific commits:
`2d0ca9dcd9db78b36c3caf4c6c57dd8701862cc7` and
`3a81a4a7d6a0f108ec201e632732d308cc02b3fc`. Both are absent from this
repository's history (`git cat-file -e <hash>` fails for each), and neither
`docs/plan/` nor `docs/machine-calibration.md` exists anywhere in the tree.

This means the "old" numbers this task exists to replace cannot be traced to
any artifact a contributor working from this checkout can inspect. A
reviewer cannot tell whether #842's re-measurement is actually independent
of the old number (the stated point of doing it before the layout lands) or
whether whoever executes it will simply copy the same ~1,190/~2,150 figures
forward because there's nothing else to anchor to. AC-2's promise —
"reproducible by a reader" — is undercut before the task even starts: the
thing being reproduced isn't reproducible itself.

**Recommendation:** either locate/re-commit the evidence documents these
figures came from, or strike the "re-taken rather than quoted" framing and
state plainly that the prior numbers are unverified inherited folklore, not
a baseline.

### 3. AC-1's "named committed generated design" doesn't exist and building it is unscoped

No generator script and no fixture at either stated scale (1,551 or 60,004
elements) exists in `test/fixtures/` (contents: `fork-4.6-shiftregister.jls`,
`headless-canary-gate.jls`, `legacy-4.1`, `riscv-sum1to10.jls` — none close
to those sizes) or anywhere else in the tree. AC-5 says "no product change —
this task measures and commits, it does not optimise," which reads as
"this is a small task." But satisfying AC-1 and AC-3 requires designing and
committing a synthetic-design generator capable of producing both a
"processor-like" and a "wire-heavy chain" shape at controlled element
counts, validating that the shapes are representative of what they claim to
be, and doing so twice (below and, per AC-4, ideally above the ~165,000-
element wall). That's non-trivial test-infrastructure engineering, not
"take a measurement" — it just doesn't touch `src/jls/**` product code, so
AC-5's framing lets it hide.

**Recommendation:** either scope the generator explicitly as a named
sub-deliverable with its own acceptance criterion, or point at an existing
generator (there isn't one) so the estimate (`band_mw: "2-3"`) has a real
basis.

### 4. AC-4's wall figure and its escape hatch are inherited from the same unverifiable source, and the escape hatch is the only realistic outcome

The "~165,000 runtime elements" wall and the "4.2x for two lines" claim
about #353/FEAT-005 fixing it both come from #370's arithmetic over the
same missing evidence documents (finding 2). Independently, `git grep`
confirms the cited lines are real and current
(`src/jls/Circuit.java:1345` — `LinkedList<WireEnd> ends = new
LinkedList<WireEnd>();`; `:1368-1369` — `WireEnd vend = visit.remove();` /
`ends.remove(vend);`, an O(W²) removal), so the *mechanism* is real even if
the *threshold number* isn't independently checkable.

More importantly: #353 (FEAT-005, the fix for this exact wall) is itself
unstarted — all three of its planned tasks are "Not filed" — and #842 does
not list #353 in `ordering_after`. Given that, generating and loading a
design above ~165,000 elements to satisfy AC-4's primary branch will hit the
very O(W²) load-time defect #370 describes, making that branch impractical
to execute within any reasonable time budget. AC-4 is written as if there's
a live choice between "measure above the wall" and "WAIVE it," but as
currently sequenced the WAIVE branch is effectively the only reachable
outcome. That's fine as an outcome, but the AC should say so rather than
presenting a false choice — a reader/executor could waste real time trying
the first branch, or a reviewer could later dispute the WAIVED comment as
"not really tried."

**Recommendation:** state directly in the task body that AC-4's WAIVED path
is the expected outcome pending #353, rather than leaving it as an
apparently open decision.

### 5. Reproducibility and "gate's named method" are underspecified enough to be gameable

AC-2 requires the JVM version and heap settings be committed, which is
good, but nothing in the ACs pins: number of trials, whether the JVM is
warmed up or measured cold, which GC is used, whether "live heap" means a
forced-GC heap snapshot or an allocation-tracking sum, or an acceptable
variance band. Live-heap measurement is materially sensitive to JIT
warm-up and GC timing. As written, AC-1/AC-2 can be satisfied by running
the JVM once, taking whatever number comes out, and committing it — which
is exactly the kind of single-sample, unreproducible number the task's own
stated purpose (avoid an "unfalsifiable" claim) is trying to prevent.

**Recommendation:** add an AC specifying trial count / methodology
(e.g., "N runs, median reported, ±X% band, forced full GC before
measurement") so "reproducible by a reader" is checkable rather than
aspirational.

## What's solid

- The core rationale — measure before the optimization lands, or the
  improvement is unfalsifiable — is correct and well-argued, and matches
  the stated Open Question 2 in #370 about not overstating the achieved
  factor as a round "order of magnitude."
- Reporting bytes-per-element rather than a raw total (AC-1) is the right
  normalization for a claim meant to generalize across circuit shapes.
- Measuring both a processor-like and a wire-heavy shape (AC-3) is a
  reasonable hedge against citing a single favorable number.
- AC-4's line-anchored citation (`Circuit.java:1345,1368-1369`) is
  independently verified accurate against the current tree — this part of
  the issue is not stale.
- AC-5's "no product change" boundary is the right instinct even though its
  practical scope (finding 3) is larger than it appears.

## Bottom line

Don't start this task as scoped. The dependency on #335 needs to be fixed
or dropped (finding 1), the baseline it claims to re-measure needs a real
source or an honest disclaimer (finding 2), and the fixture-generation work
implied by AC-1/AC-3 needs to be named and estimated rather than absorbed
silently under "measures and commits" (finding 3). None of this blocks the
underlying idea — it blocks doing the work against solid ground.
