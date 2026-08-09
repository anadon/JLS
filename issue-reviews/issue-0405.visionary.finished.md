# Issue #405: TASK-0010: a waveform dump is written as it is produced, and its cost is proportional to the changes it contains
- Lens: visionary
- Date: 2026-08-08
- Reviewer: Claude Opus 5
- Verdict: endorse-with-reframing

## What this issue is really for

Strip the apparatus and the ask is: *a batch run that dumps a waveform should
cost memory proportional to nothing in particular, so a long grading run can
produce a waveform at all.* That is the parent feature's own words — #353 §1,
third bullet: "Peak heap during a batch run with `-vcd` does not grow with the
number of dumped samples: the dump is written as it is produced." The audience
section here says the same thing in the instructor's voice: "Long runs simply
cannot produce a dump they cannot hold."

The issue then, in O7 and Open Question 1, concedes that its own deliverable
does not achieve that, and recommends default (b): write a sentence in
`docs/batch-interface.md` saying peak memory is still proportional to total
recorded changes. That is honest, and it is exactly where the design should be
challenged rather than accepted. **The recommended default converts the parent
feature's headline capability criterion into a documented non-capability, and
does so inside a child issue whose completion criteria the parent will read as
"landed."**

## The load-bearing arithmetic the issue does not do

`eventTrace` / `probeTrace` retain one `TraceSample` per recorded change for
the whole run (`BatchSimulator.java:24,33`, `LinkedList` nodes). Per change:
a `LinkedList` node (~32 B) + a `TraceSample` record (~24 B) + a `BitSet`
holding a `long[1]` (~56 B) ≈ **110 B**. The rendered text of that same change
is one VCD line — O2's fixture emits 70,176 chars for 9,886 changes, ≈ 7 chars
per change — so the triple materialization O1 attacks costs roughly
7×2 (StringBuilder `char[]`) + 7×1 (compact `String`) + 7 (`byte[]`) ≈ **28 B**
per change at peak.

Applied to O4's own run (1,112,009 chars ⇒ ~155k changes): the three copies
this task deletes are ~4.4 MB; the retained trace they sit beside is ~17 MB.
These are envelope figures from object layout, not measurements — but the ratio
is not close enough for the sign to be in doubt. **This task removes the
smaller term and documents the larger one.** After it lands, an instructor whose
run OOMs still OOMs, at roughly 80% of the same run length, and
`docs/batch-interface.md` will now say so.

The issue's own Threats section anticipates this ("Peak-heap claims are not
measured here") and §10 routes the case where "streaming the writer produces no
measurable improvement in peak RSS" to FEAT-006. I think that branch is the
likely one, and a task should not be shaped so that its most probable outcome
is the one that forwards its purpose to another issue.

## The reframing: stream at the recording site, not at the rendering site

The issue's O6 asserts the header cannot be written first, because the `$var`
set and the `_probe` disambiguation need `signals` fully populated. That is true
of a *post-hoc rendering pass*. It is false of the simulator, and the code
already proves it:

```
runSim():  initSimulation();  findWatched(circuit());  findProbes(circuit());  runEventLoop();
```

`BatchSimulator.java:115-129`. Every input the header needs — the watched
element set, the probe set, each signal's `getFullName()` and `getBits()`, the
time-0 seed value of every signal, and the top circuit's name — is fully
determined **before the first event fires**. The name-ordered merge, the
`_probe` disambiguation loop, and therefore the `vcdId` assignment are all
computable at `runSim` entry. The entire header and the `#0`/`$dumpvars` block
can be written before the event loop starts; every subsequent change can be
emitted as it is recorded, and nothing needs to be retained for the dump at all.

Concretely:

- **Open the sink in `runSim`**, after `findProbes`: build the name→(bits, code)
  map once, write the header, hold the time-0 group.
- **A one-timestamp buffer**, `TreeMap<String,BitSet>`, holds the changes at the
  current simulation time. When a change arrives at a later time, flush the
  buffer in name order (which is precisely today's per-timestamp order) preceded
  by `#t`, and start a new group. Its size is bounded by |S|, not |C| — the
  first genuinely bounded thing in this path.
- **`afterEvent` / `probeSample` write into the buffer** instead of appending to
  a retained list, comparing against a per-signal "last value emitted" map
  (also O(|S|)) for the dedup they already do.
- **`writeVcd()` becomes close**: flush the final group, emit the trailing
  `#now` when `now` exceeds the last change time, `close()`, rename the temp
  file over the target.

What this deletes, versus what the issue proposes to build: `fold` and its
`TreeMap<Long,BitSet>` per signal, the shared `TreeSet<Long> times`, the
`Sig` record, the whole `toVcd` body — and the time-ordered change list the
issue plans to build and sort, which never has to exist. The 90-line method
becomes a small sink class plus three call sites. H2 is satisfied not by a
better loop but by the absence of a loop over signals: there is no
per-timestamp scan of `signals` to accidentally leave behind, so §10's "a second
|S|-proportional term survives" failure mode is structurally impossible rather
than tested for. H3 is satisfied by construction. P4 (`grep toVcd` empty) and
FEAT-005 §5 criterion 3 fall out. Open Question 1 **disappears**: there is no
residual to document, because in the `-vcd`-without-`-r` case — precisely the
autograder case the audience section names — nothing is retained.

Byte-identity survives, but three details are where it would be lost, and they
are the ones I would put in the test first:

1. **Time 0 is not a separate mechanism.** `fold` uses `byTime.put`, so a change
   recorded *at* t=0 during the loop overwrites the seed in `$dumpvars`, and the
   value section skips `t == 0`. The streaming form must therefore pre-populate
   the t=0 group with every signal's seed, let t=0 changes overwrite entries in
   it, and wrap that group in `$dumpvars`/`$end` rather than `#0`+lines.
2. **Last-write-wins within a timestamp** is `put` semantics on the group map —
   free, but only if the group is a map keyed by name, not a list.
3. **The `_probe` rename is resolved once**, at open, into the name→code map;
   `probeSample` keys by the raw probe name and must be translated through it.

## Cost of the retention with `-r`

`getTraceSamples` / `BatchTracePrinter` (`JLSStart.java:267`) genuinely needs
the retained samples, so retention cannot simply be deleted. The honest shape is
that retention becomes *one sink among several*: an in-memory sink installed
only when `JLSInfo.printTrace` (or a caller asks for `getTraceSamples`), and a
VCD sink installed when `vcdFileName != null`. That is a strictly better
statement than today's, where `-vcd` alone silently pays for the printer's data
structure.

## Why this is the seam the project's arc already wants

ARCHITECTURE.md records the direction toward typed seams (#223,
`docs/extension-points.md`) and, separately, that the trace printer was already
pushed out of the headless core behind `getTraceSamples` (#77). A `TraceSink`
consumed by the simulator is the completion of that move, and it has four
consumers already visible in the tree and the issue graph: the VCD writer, the
`-r` printer, the interactive trace window, and **#214's in-editor test panel**,
which this issue itself lists as "a future reader of the same trace data" and
notes will be affected by Open Question 1's answer. A panel that wants to show a
waveform *as the run proceeds* needs a sink, not a post-run map. An `Appendable`
parameter on one method serves exactly one of those four; it is a plumbing
change wearing an architecture change's clothes.

It also removes a real duplication the issue never mentions: the HiZ-normalize
and dedup-against-previous logic exists twice, at `BatchSimulator.java:161-178`
and `:305-311`, once per trace map. One sink front-end has it once.

Finally, #232 (per-signal `BitSet` churn) and this issue are correctly
separated in §12 — but note that record-time streaming makes the retained
`BitSet` population, which is a large part of what #232 measures at whole-run
scope, simply not exist in the `-vcd` path. The two scopes stay distinct; one of
them gets much smaller for free.

## What I am disregarding, and what I am not

I am not disregarding the byte-identity contract, the goldens, or the
`blocked_by: [373]` edge — all three are correct and all three hold unchanged
under the reframing (same names, same order, same codes, same bytes; still
emitted against #373's naming function).

I **am** disregarding Open Question 1's recommended default (b) and, with it,
prediction P6 as the shape of the deliverable. P6 asks for a documented
statement that peak memory is proportional to recorded changes, pinned by a
drift test. Under the reframing, the sentence to write in
`docs/batch-interface.md` §4 is the opposite one — that `-vcd` without `-r`
retains no per-change state — and the thing to pin is a test that runs a long
fixture and asserts the retained sample count is zero (or that
`getTraceSamples()` is empty), which is a far stronger and less driftable
assertion than prose-versus-code review.

I would also drop P2's "ratio of inner-loop work to emitted lines" assertion:
under the reframing there is no inner loop to instrument, and a test that
measures a quantity the new design does not have is a test written against the
old design's shape.

## Residual risks the reframing introduces

- **A partial file is observable during the run**, not merely on I/O failure —
  a longer window than §7.11 contemplates. Temp-and-rename (Open Question 2,
  recommended yes) covers it, and now earns its keep rather than riding along.
- **An open file handle for the run's duration**, and a `stop()`/early-exit path
  that must still close and rename. Worth one test.
- **Cost.** The 4 d estimate is for the smaller change; this is closer to a
  week. The end state is less code than either the current tree or the issue's
  target, so the extra days buy a smaller system, not a larger one.

## Verdict

**endorse-with-reframing.** The evidence work here is exemplary and the target
end state — no method returning the whole dump, cost proportional to changes,
byte-identical output — is exactly right. But the seam is cut one layer too
late. Cut it at the recording site instead of the rendering site and the same
end state arrives with the fold, the change list, the `TreeSet` of times and
Open Question 1 all deleted rather than rebuilt, the parent feature's stated
capability actually delivered instead of documented away, and the trace sink
that #214 and the interactive trace window will both want existing as a seam
rather than as a private method parameter.
