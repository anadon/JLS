# Issue #512: CAP-28: every public performance claim is a published, reproducible measurement — and JLS's simulation speed has a number a competitor comparison can cite
- Lens: visionary
- Date: 2026-08-08
- Reviewer: Claude Opus 5
- Verdict: endorse-with-reframing

## What this capstone is actually for

The issue braids two ends that have almost nothing to do with each other.

**End A (marketing).** #510 §4 gate 4: Digital publishes "120 kHz on a 2012 i5" and wins
every performance conversation by default. JLS publishes nothing. Cost: ≤1 mw, mostly
prose. The deficit is genuinely epistemic.

**End B (engineering).** The 2026-08-08 correction comment on this issue found the real
thing: `docs/capability-roadmap/keystone-c-performance.md` §2 already publishes exactly
the numbers AC-1 asks for (318 ns/event, 8,090 cycles/s, 2,331,793 events on a censused
1551-element fixture), and §12's reproduction recipe compiles ten harness files out of a
scratchpad directory that is not in the repository. `grep -rl KernelProbe src test riscv`
returns nothing; `riscv/bench_kernel.py` builds the *fixture*, not the *measurement*.
Meanwhile #442, #476, #393 and #879 each carry a before/after measurement criterion that
only that uncommitted instrument can satisfy.

End B is worth an order of magnitude more than End A, and the capstone's title, its
Outcome, and PF-1's name ("benchmark suite formalization") all describe End A. That
mis-naming is what I want to fix, because it determines where the work lands: a
"benchmark suite" lands in `test/` as scaffolding, and scaffolding does not satisfy AC-2
(an independent party reproducing from the doc alone must then clone and build), does not
serve four other issues, and does not survive the next reorganization.

## Reframe 1 — the deliverable is an instrumented simulator, not a benchmark suite

`jls.sim.Simulator` already has the seam. `src/jls/sim/Simulator.java:269`
(`protected void afterEvent(SimEvent event) {}`) and `:285` (`probeSample`) are empty
extension points that `BatchSimulator` already consumes to write VCD. `KernelProbe` and
`KernelProbe2` were written against exactly those seams; they were thrown away because
nobody claimed they were a feature.

Make them a feature. A first-party run report — `jls -b -stats report.json circuit.jls`,
one more row in the `FLAGS` table at `src/jls/JLSStart.java:759-789`, alongside the `-vcd`
precedent — emitting event counts by payload and callback, dup-suppression ratio, max
queue depth, the element/net/width census, and `initSimulation` vs `runEventLoop` phase
timing separately. Consequences:

- **AC-1 and AC-2 become trivially true and stay true.** The "committed command" is a
  documented CLI flag under `docs/batch-interface.md`'s stability contract, run against a
  *released jar*. An independent party reproduces the number without a JDK, a checkout, or
  `javac` on package-private classes.
- **The four engine issues get one comparable instrument** instead of four hand-rolled
  probes producing four incomparable numbers — which is this capstone's own failure mode
  occurring internally.
- **It lands on JLS's only 5/5 axis.** #510 scores JLS category-best on exactly one
  dimension: testing/grading, on the strength of a *documented, versioned batch interface*.
  A run report is that moat widened, not a benchmark side-quest. No competitor documents
  grading semantics; none documents simulation cost either.
- **It is pedagogy, which the issue never notices.** "Your ripple-carry adder fires 6× the
  events of the carry-lookahead" is a lab exercise. Glitch and hazard counting is a
  classic teaching point that JLS currently computes and discards.
- **It is the SAIF precursor.** `docs/capability-roadmap/README.md` calls #72 SAIF "the
  cheapest real item in six sweeps" precisely because "JLS already computes every datum in
  a SAIF file … and throws all of it away." Toggle counts per net are the same counters.
  lf-05 (fault and power) and sweep-04's coverage tier consume them too.

One instrument, five consumers. That is a capability. "A benchmark suite" is not.

## Reframe 2 — split the deterministic half from the wall-clock half, then make the published number a build output

AC-1 asks for "events/s and cycles/s at stated node counts" as one thing. It is two things
with opposite properties:

- **Event counts, events/cycle, dup-suppression ratio, queue depth, census** are properties
  of the circuit and the algorithm. The simulator is deterministic, so these are
  *byte-reproducible on any machine*. They can be gated as an exact golden.
- **Wall-clock ns/event and cycles/s** are properties of the runner. They need a band, and
  keystone-c §8.1 already warns in its own voice that "a timing assertion in CI is a flake
  factory."

Conflating them forces AC-4's gate to be a wall-clock band, which is the weak half: a band
loose enough not to flake on a shared runner will not notice a 20% rise in events per cycle
— an *algorithmic* regression, the one that actually matters and that no hardware upgrade
can mask. Split them and the exact half becomes a golden-file diff, red the moment the work
per cycle changes, with zero flake.

Then apply the pattern this project already lives by. The README's whole integrity story is
"the artifact is regenerated and re-checked in CI" — `bom.json`, `SHA256SUMS`,
`.buildinfo`, byte-reproducible jar. Do the same here: **`docs/performance.md` contains no
hand-typed numbers.** It cites a checked-in report file that the instrument emits; CI
re-emits it and diffs. A stale published number becomes structurally impossible rather than
caught by a gate. That is the reframing that makes AC-4's problem disappear instead of
solving it, and it is in the project's existing grain, not a new discipline.

Note also: `docs/machine-calibration.md` does **not** exist at this checkout, contrary to
#413 §1's claim that it does. So there is no second evidence record to reconcile with yet —
decide now that `docs/performance.md` is the single home, or the project will grow two
records of the same constants, which is the exact failure CAP-28 exists to prevent.

## Reframe 3 — publish a calibrated model, not a scalar; and stop letting #413 block the whole capstone

Digital's "120 kHz on a 2012 i5" is a scalar about one machine and one circuit. Matching it
with a scalar concedes the frame. #413 §7.10 already writes down the better claim:
events/cycle ≈ α(C)·β·|L(C)|, with α the active fraction and β events per active element.
Publish **α, β and ns/event with their fixtures, plus the scaling table**, and a reader can
compute the expected throughput of *their own* circuit. That is a claim a competitor cannot
beat by buying a faster laptop, it is the correct pedagogical object, and it is the form
`ARCHITECTURE.md:354-357`'s "unusably slow" revisit trigger needs in order to become
testable at all (keystone-c §2 asks for exactly this restatement).

It also dissolves the ordering. The capstone hard-orders on #413 because the anchor is
`riscv/build/k2000.jls`, inside the tree D5 deletes. True — for the *CPU-scale* number. But
PF-1's own "2–3 smaller standard circuits (counter, memory loop)" are not in `riscv/` and
never were. A parameterized N-bit counter authored today under `test/fixtures/` (the only
location the root `.gitignore`'s `!test/fixtures/**/*.jls` exemption permits) delivers
AC-1, AC-2 and AC-4 immediately, sidesteps #378's unsettled large-fixture policy, and is
*better* evidence than a 120 KB blob because an independent party can regenerate it at any
N and check the scaling law rather than one point on it. Scope the #413 ordering to the
CPU-anchored flagship figure only; everything else can start now.

## Where I disregard the stated acceptance criteria: PF-4 and AC-5

AC-5 requires a published head-to-head against Digital and Logisim-Evolution including a
workload a competitor wins, costed at 0.5–1 mw. That estimate is off by a lot, and the
artifact pulls against the project's arc.

The cost: building two other tools, hand-translating equivalent circuits into three editors
whose semantics are *not* equivalent (Digital has per-bit HiZ, JLS does not;
Logisim-Evolution's simulation is GUI-redraw-throttled per its #786), then defending that
translation's fairness permanently. It is not a benchmark, it is a standing obligation.

The strategy: #510 §5's positioning is "JLS is the maintained, modern successor in the
Digital tradition," aimed at a named pool of contributors Digital has rejected, against a
project at 3 commits YTD and 23 months without a release. Spending weeks to publish
"Digital's dying engine beats ours" is the one artifact that undercuts that pitch, and
KC-28-1 — admirable in intent — commits the capstone to shipping it.

The honesty AC-5 is reaching for is available for a paragraph. #510 §2 already enumerates,
sourced, where JLS loses: hierarchy and parameterization, chronogram, FPGA flow, value
domain, learning on-ramp. Publish *that* table in `docs/performance.md` beside JLS's own
number and Digital's published claim, with an explicit statement that the two numbers are
not comparable and why. That is more honest than a manufactured head-to-head, because a
head-to-head's fairness claim is the part nobody can verify. Reduce PF-4 to: publish the
harness and the invitation ("run it against your tool and send us the number"), and let a
comparison arrive from someone with no stake in the result.

## Alignment ledger

- **Strengthens:** #442's bands, #476/#393/#879's measurement criteria, sweep-04 coverage,
  #72 SAIF, lf-05, `ARCHITECTURE.md`'s revisit trigger, and the batch-interface moat.
- **Duplicates (already handled):** PF-3 vs #442 — the "do not build twice" boundary is
  correctly drawn.
- **Duplicates (not yet handled):** `docs/performance.md` vs `docs/machine-calibration.md`.
  Settle on one before either exists.
- **Pulls against:** PF-4/AC-5 only, for the reasons above.

## The version of this capstone I would endorse without qualification

1. Commit the instrument as a shipped, documented CLI surface (`-stats`), on the existing
   `afterEvent`/`probeSample` seams. This is PF-1, renamed and re-homed out of `test/`.
2. Publish `docs/performance.md` whose numbers are all citations into a checked-in report
   the instrument emits; include α, β, the scaling table, and the honest deficit table.
3. Gate the deterministic half exactly (golden diff) and the wall-clock half by band,
   consuming #442's machinery.
4. Publish the harness and an open invitation instead of a self-run head-to-head.
5. Keep KC-28-1's spirit — publish the unflattering number — and add the provenance
   defect the correction comment identified: the numbers were measured before the tooling
   that produces them was committed, and here is the commit that closed the gap.

The Outcome sentence, AC-1 through AC-4, and the kill criterion are right. The capstone is
one noun away from being right: not *publication*, but *instrumentation* — of which
publication is the cheapest downstream consequence.
