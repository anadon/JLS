# Issue #353: FEAT-005: a long batch run costs linear time and bounded memory — the stimulus parse, the load-time net walk and the waveform dump stop being superlinear
- Lens: visionary
- Date: 2026-08-08
- Reviewer: Claude Opus 5
- Verdict: endorse-with-reframing

## What this issue is really for

Stripped of the tier apparatus, #353 asks for one thing: **the cost of a batch run
should be a function of the run, not of the run squared**, so that the `riscv/`
trajectory (#200/#201/#202) becomes a thing you can actually execute. That goal is
not merely compatible with the project's arc — it is the arc's own stated trigger.
`ARCHITECTURE.md`'s recorded decision on simulation execution strategy (#221) keeps
the discrete-event interpreter as the *sole* strategy and names the revisit trigger
as "a concrete CPU-scale design on the `riscv/` trajectory … that is unusably slow."
#353 is the correct first response to that trigger: it removes accidental superlinear
cost *before* anyone argues for a second execution strategy. Fixing a quadratic string
concatenation is exactly the work that keeps #221's "no levelized pass" decision honest
for another five years. Endorsed on the merits.

The evidence block is unusually good. I re-derived all four anchors against the working
tree and every line number still resolves exactly: `SigSim.java:64/67/71/74`, `:85/:89`,
`Circuit.java:1345/1355/1368/1369` (and `finishLoad` still spans 1300–1422),
`BatchSimulator.java:366/368/384/386/393/455/460/475/518`. That is rare and worth saying.

But the decomposition cuts the seam in the wrong two places, and one of them means the
headline capability is not actually delivered.

## Reframing 1 (load-bearing): the dump does not become bounded-memory under TASK-0010

§1 promises "Peak heap during a batch run with `-vcd` does not grow with the number of
dumped samples." TASK-0010 as written — "`toVcd()` replaced by a sink-taking writer,
the per-timestamp scan becomes one walk of a time-ordered change list" — **cannot deliver
that**, because `afterEvent` (`BatchSimulator.java:177`) and `probeSample` (`:310`) append
every recorded sample to `eventTrace`/`probeTrace` for the run's whole duration, and the
"time-ordered change list" is built *from* those maps. You go from three whole-run copies
to two. Peak live set stays Θ(|C|). The feature would close with its own §1 bullet false.

The elegant route the issue never considers: **VCD is already a streaming format — emit it
from the event loop instead of after it.** Everything needed is already in place:

- `findWatched`/`findProbes` run *before* `runEventLoop()` (`BatchSimulator.runSim:118-129`)
  and enumerate the complete signal set with time-0 values. So the `$var` header, the
  identifier assignment, and the `#0`/`$dumpvars` block can all be written before the
  first event — no post-hoc knowledge required.
- `now` is monotone nondecreasing: `eventQueue` is a `PriorityQueue` ordered by (time, seq)
  and the loop only ever assigns `now = event.getTime()` (`Simulator.java:228`). So
  timestamps arrive in the order VCD wants them.

That yields a writer holding exactly two bounded structures: a `Map<name,BitSet> lastEmitted`
(size |S|, replacing the `events.getLast()` dedup) and a per-timestamp pending buffer (size
≤ |S|, flushed name-sorted when the clock advances). **Byte identity falls out rather than
being defended**: name-sorted flush reproduces §4.3's "in name order" ordering, and
last-write-wins inside the buffer reproduces `fold`'s semantics. Live set becomes Θ(|S|) —
the number of *signals*, not samples — which is the capability §1 claims.

This is strictly simpler than the planned task. It **deletes** `toVcd`, `fold`, the `times`
`TreeSet`, the `TreeMap<Long,BitSet>` per signal, and the O(|T|×|S|) nested scan at `:455/:460`
as a *consequence* of the design rather than as three separate line items. And it turns
§5's criterion 3 (`grep toVcd` returns nothing) into something much stronger and equally
mechanical: **`eventTrace` is not populated when `-r` is off.** Today retention is gated on
`printTrace || vcdFileName != null` (`:144`, `:298`); under this reframing `-vcd` needs no
retention at all and the gate collapses to `printTrace` alone. That single-condition change
is the real proof of bounded memory — a grep for a deleted method name is not.

**Free capability this hands to #354.** A streaming emitter means a run killed by SIGINT or
by the time limit still leaves a valid, parseable partial VCD on disk. #354's gate is
literally "survives SIGINT with a parseable transcript." The reframed TASK-0010 does not
merely *unblock* #354, it pre-delivers half of it. The mirrored `blocks: [354]` edge
understates the relationship.

## Reframing 2: TASK-0008 does not belong in this feature, and it is the only expensive part

TASK-0008 is 1.5 wk of 3.8 wk, it is the *sole* "necessity" ordering edge in §6, and it is
the only child that breaks a golden — inside a feature whose entire gate is byte identity.
Invariant 4.3 has to carve out an exception for it ("byte-identical **except where TASK-0008's
naming change is declared**"), §7 needs a whole contract-deviation clause for it, and §2's
rationale for keeping it is purely scheduling ("settling the function after the writer means
regenerating every golden twice").

Under Reframing 1 that argument evaporates: the header is emitted from the pre-loop signal
enumeration, so the naming convention is a *pure input* to the writer, consumed wherever it
comes from. TASK-0010 does not care whether the convention is frozen before or after it lands
— it just calls `getFullName()` (or its successor). The coupling was an artifact of building
the header inside `toVcd()`.

**I am disregarding the stated decomposition here.** Cut TASK-0008 wholly to FEAT-004 (#336),
which already co-owns it. What remains is: two independent tasks, no critical path, no
regenerated goldens, no exception in invariant 3, no contract-deviation clause, and a single
unambiguous gate ("not one byte of output changes; the cost curve does"). Cost drops to the
2.3 wk the reconciliation paragraph already identifies as the unshared remainder — and the
scheduler no longer has two numbers to choose between.

Incidentally, §1's "every waveform golden in `test/resources/`" points at the wrong place:
`test/resources/` holds only `hdl/` and `orientation-geometry.txt`. The VCD goldens are
inline Java string constants in `test/jls/VcdExportGoldenTest.java:67` and `:147`, so
"regenerate the goldens" means hand-editing source literals — one more reason not to import
a golden-breaking change into this feature. Also: `docs/batch-interface.md` §4 names
`BatchSimulator.toVcd` as the emitter by method name, so deleting it makes a *normative*
document stale. The feature says it "changes allocation, not format"; it still owes that
doc edit, and §3's Modifies list should say so.

## Reframing 3: the `finishLoad` quadratic is a data-structure typo, not a rewrite

`ends` is declared `LinkedList<WireEnd>` (`Circuit.java:1345`) and the only reason it is a
list is `ends.remove()` at `:1359` taking the head. Change it to `LinkedHashSet<WireEnd>`
and take the head via `iterator().next()`: insertion order is preserved verbatim (satisfying
the file-order constraint recorded at `Circuit.java:76`), `remove(Object)` becomes O(1), and
the partition is **identical by construction** — `WireNet` already backs both its members
with `LinkedHashSet` (`WireNet.java:22,24`), so duplicate `add` calls are idempotent and
block order is insertion order either way. That is a four-line diff whose correctness argument
is "the two collections have the same iteration order and the same idempotence."

§7 accordingly worries about the wrong thing. Its REFUTED contingency — "if the linear
`finishLoad` rewrite cannot reproduce the partition byte-for-byte, that half is re-planned
alone" — budgets research risk against a hazard the type choice eliminates. Do not price
this at a third of TASK-0009.

## Reframing 4: don't make the stimulus string linear — make it not exist

TASK-0009's contract is "per-token and per-line concatenation becomes one `StringBuilder`."
That fixes the exponent and keeps the design flaw: `initSim` builds a complete normalized copy
of the entire stimulus program in the heap (`newSignals`) purely so it can construct a *second*
`Scanner` over it at `SigSim.java:79` and re-tokenize what it just tokenized. One `StringBuilder`
leaves an Θ(input) allocation and two full lexes.

The reframing is a one-pass token source: a small iterator that reads lines from the input
`Scanner`, rewrites `-?0[xX]…` tokens to decimal inline, stops at `#`, and yields tokens. The
existing state machine at `:80-203` consumes it unchanged — it only ever calls `next`,
`hasNext`, `hasNextLong`, `hasNextBigInteger`. Zero intermediate string, one lex, and the
`for`/`until`/`end` grammar frozen by `docs/batch-interface.md` §2 is untouched because the
consumer is untouched.

Same instinct on the second quadratic. "The signal-name-to-`InputPin` scan becomes one map"
puts the map inside `SigSim`, where it is rebuilt per `initSim` call and helps nobody else.
A name→element index belongs on `Circuit`, next to the `SpatialIndex` that already exists for
exactly this reason (#43 replaced editor linear scans with an index). `findWatched`,
`findProbes`, `HdlExporter`, and the `-t` resolution all want the same lookup. Cutting the seam
at `Circuit` turns a local patch into a shared facility; cutting it inside `SigSim` guarantees
the next consumer writes the same O(signals × elements) loop again. (Open Question 1's
last-wins preservation is unaffected either way — build the index last-wins and say so.)

## Reframing 5: assert complexity, not wall-clock

"Doubling the line count roughly doubles the time … within a tolerance that is a number in a
test" is a timing-ratio gate. On a project whose CI already carries a headless-sway GUI lane,
a nightly cron, reproducible-rebuild verification, JaCoCo floors and PIT thresholds — with a
declared bus factor of 1 — a ratio-with-tolerance test is a flake generator that the one
maintainer will eventually mute. That pulls against the README's "keep `mvn verify` green"
discipline more than it protects the win.

Deterministic substitutes exist for all three sites, and they are *stronger* assertions:

- `finishLoad`: instrument nothing; assert each wire end is dequeued from `visit` exactly once
  on a fixture with a long chain. Machine-independent, and it pins the algorithm rather than
  the hardware.
- the parse: assert the intermediate string does not exist (the token source is the type), plus
  one large-input smoke test with a 100× margin — old code takes minutes, new takes milliseconds,
  so no tolerance tuning is needed.
- the dump: assert `getTraceSamples()` is empty after a `-vcd`-only run. That is a
  *bounded-memory* assertion expressed as a behavioral one, and it cannot flake.

Keep the 50,000-cycle end-to-end number as a recorded measurement handed to #335 (§5 criterion 4
is right to want the band), but do not make a wall-clock budget a required gate.

## What I would file instead

One feature, two tasks, no critical path, ~2 wk:

1. **Linear load and linear parse.** `LinkedHashSet` in `finishLoad`; a token source in
   `SigSim.initSim`; a name index on `Circuit` that `SigSim` and `findWatched`/`findProbes`
   share. Gate: counting/structural assertions plus one large-input smoke test.
2. **Streaming VCD.** Header and `$dumpvars` emitted before `runEventLoop`; value changes
   written from `afterEvent`/`probeSample` through a name-sorted per-timestamp buffer flushed
   on clock advance; `toVcd` deleted; trace retention gated on `printTrace` alone;
   `docs/batch-interface.md` §4's emitter reference updated. Gate: existing goldens byte-identical,
   `getTraceSamples()` empty on a `-vcd`-only run, and a partial file left behind by an
   interrupted run parses.

TASK-0008 moves to #336. The boundary note's refusal to merge #353 with #354 is correct and I
would not touch it — cost and capacity are genuinely different outcomes, and the "fast failure
vs slow failure" ordering argument is the right reason to keep the edge.
