# FEAT-026 - The four-state value core with a resolution fold

**Status:** proposed | **Cost:** 28-36 mw | **Owner program:** P1 |
**Spine rank:** S2

## Capability delivered

A JLS signal can say "I do not know" and "nobody is driving me", per bit,
instead of quietly being zero, and when several drivers contend the answer is a
property of the set of drivers rather than of the order somebody drew the wires.
This is what lets a design that came from outside JLS - a synthesized netlist, a
third-party core, an external simulator's waveform - be compared honestly rather
than compared after silently coercing every unknown to a definite value. It is
also the storage substrate every later value-domain feature rides on.

## Consumed by capstones

| CAP-NN | required/beneficial | what it needs from this feature |
|---|---|---|
| CAP-04 | required | X on a floating CMOS input, and resolution that is a fold rather than first-driver-wins |
| CAP-08 | required | a real core has tri-state buses and undriven nets; first-driver-wins misexecutes them |
| CAP-09 | required | a verdict must be able to say "unknown" rather than silently resolving to 0, and don't-care-aware grading needs the fourth state |
| CAP-15 | required | the toolchains produce X and Z; a two-state comparator must either model them or declare every X a mismatch |
| CAP-03 | required | supplies the three-plane record whose spare code points radix 3 and 4 occupy |
| CAP-05 | beneficial | contention and undriven nets on a real board are not "0" |
| CAP-02 | required | a drawn RV32 machine's bus behavior is honest only with real resolution |

## Prerequisite features

| FEAT-NNN | why |
|---|---|
| - | **None.** P1 lands inside `jls.core` and depends on no other program (`docs/capability-roadmap/README.md:223`). Its spine row S2 - widening the permitted value set with unreachable stubs - is banded at 0.2 wk and gates on nothing. |

## Prerequisite tasks

| TASK-NNNN | title | why |
|---|---|---|
| TASK-0056 | Widen the value permits and migrate the value representation | The permitted set, the accessors, and the plane-encoded immutable value that replaces `BitSet` |
| TASK-0057 | The resolution fold | Replaces first-active-driver-in-net-order with an order-independent fold |

## Acceptance criteria

1. `null` is no longer the currency of "high impedance" anywhere on the value
   channel: `Put.currentValue`, `Input.setValue`/`getValue`,
   `Output.propagate`, `WireNet.value` and the tri-state events all carry a
   value type that can express Z explicitly.
2. Z is per bit, not per signal. A fixture drives half a bus and leaves the
   other half undriven, and the trace, the VCD and the stdout rendering all show
   it per bit.
3. Multi-driver resolution is a commutative, associative fold. A property test
   permutes net order over a generated multi-driver fixture and asserts the
   resolved value is invariant - the test that fails at HEAD by construction.
4. Conflict is a value, not only a warning: two active drivers disagreeing
   resolves to X and that X is observable at every rendering point.
5. Every existing golden either stays byte-identical or is named in the commit
   with the semantic change that justifies its new expectation. There is no
   third category.
6. The migration keeps the tree green at every commit - the dual-mode
   discipline, not a long-lived red branch.

## Related GitHub issues

| # | title | relationship |
|---:|---|---|
| 232 | Simulation hot path: per-signal `java.util.BitSet` allocation and bit-by-bit conversion churn the GC; evaluate a value-typed (long,width) signal representation | closes its value-representation half - #232 is about allocation, this feature is about semantics, and they are the same migration |
| - | the four-state semantics and the resolution fold themselves | **no issue** |

## Design notes

The value domain and the resolution rule are both *specified* at HEAD, and the
specification is what changes: `docs/simulation-semantics.md:42-68` ("two states
plus HiZ", HiZ as a null reference, all-or-nothing per signal, and "nearly every
element's `react` treats a null (HiZ) input as zero before computing") and
`:409-447` (resolution scans the net in net order and delivers the first active
driver; "There is no wired-AND/OR and no conflict (X) state"). Do not restate
those; the feature's job is to replace them, and the doc rewrite is part of the
work.

The representation is already determined by the committed roadmap and must not
be re-litigated here: `sealed interface LogicValue permits Word, Wide` with
`record Word(int width, long a, long b, long u)` for widths up to 64 and a
sparse-plane `Wide` above (`docs/capability-roadmap/README.md:126-140`). That
record is three bit planes, which is eight code points for five kernel states -
the spare capacity FEAT-028 spends.

The band is the roadmap's own: 17-22 wk for the four-state core under the
dual-mode discipline, plus 6-9 for strength (which is FEAT-027's, counted
there), plus U and reset 2-4, the 1164 projection 2-3, kernel hygiene 1-2
(`README.md:215-222`). The registry's 28-36 mw is that program total.

Note the asymmetry with FEAT-030: this feature is permitted to change goldens
where the semantics genuinely changed. FEAT-030 is not permitted to change any.
Landing them in the same commit destroys both signals.

## Risks

- **Every `react` implementation is a call site.** There are 27 `react`
  implementations across 35 registered element types, and
  `docs/simulation-semantics.md:63-68` records that nearly all of them coerce
  HiZ to zero on the way in. Each coercion is a decision - keep it, or propagate
  the unknown - and getting one wrong changes a lab's behavior silently.
- **`Memory` alone.** The roadmap measures `Memory.java` holding 51 of the
  tree's `BitSet` references at 1,547 lines and says to schedule it alone
  (`README.md:294-296`). Treat that as binding scheduling advice.
- **This is the single largest feature in the plan after FEAT-017.** At bus
  factor 1, 28-36 mw is six to eight months. If it slips, everything in the
  parity and physical columns slips with it; the demo slices of CAP-04, CAP-09
  and CAP-15 should be written so they do not all wait on the full program.
- **X is not free pedagogically.** A first-year student who has never seen X
  now sees it. Kill criterion K9 (the pedagogy floor) outranks the value domain;
  the rendering and the default visibility need the same care the analog palette
  gets in FEAT-049.

## Evidence

- Value domain and HiZ-as-null at HEAD: `docs/simulation-semantics.md:42-68`;
  `src/jls/elem/Put.java:385` (`@Nullable BitSet currentValue`).
- First-driver-in-net-order resolution, with the warning text and the "no
  conflict (X) state" statement: `src/jls/elem/WireNet.java:443-490`;
  `docs/simulation-semantics.md:409-447`.
- Scale of the migration: 338 `BitSet` references across `src/jls/elem/*.java`
  at `addc6c5`.
- Allocation cost of the current representation: `BRIEF.md` §13 - about 50% of
  in-loop allocation among named non-`byte[]` classes.
- Representation and cost band: `docs/capability-roadmap/README.md:126-140`
  (the type), `:215-222` (the 28-36 wk breakdown), `:223` (no cross-program
  dependency).
- Spine placement and the cheap first row: `10-capstone-plan.md` §2.1 S2
  (0.2 wk, score 15.0).
- **Cost reconciliation.** Band 28-36 mw. Tasks named for it: TASK-0056,
  TASK-0057, totalling 4 wk. The named tasks are the leading, dividable slices
  of this feature, not the whole of it; the residual has no task id, because
  the registry's task space is closed at TASK-0112. Do not read 4 wk as the
  feature.
