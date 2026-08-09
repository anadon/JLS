# Issue #504: CAP-23: a student diagnoses a race condition without leaving JLS — from a glitch caught by a triggering logic analyzer to the two unequal-delay paths that caused it, in three clicks
- Lens: visionary
- Date: 2026-08-08
- Reviewer: Claude Opus 5
- Verdict: rethink

## What this issue is really for

Strip the six planned features away and the claim is: *JLS should be able to answer
"why is this signal what it is?" on the drawing the student made.* That claim is
right, it is the project's own, and it is already worked out in far more depth than
this issue reflects — `docs/capability-roadmap/lf-03-causal-debug.md` (P9 in
`AMENDMENT.md:325`) is a 800-line study of exactly this capability, with a design, a
cost table, three named failure modes, and a competitive verdict. CAP-23 cites P9
only in risk 1, as a coordination hazard ("never two cause-chain data models"), and
then proceeds to mint six features over P9's territory anyway (comment of 2026-08-04,
#527/#529/#532/#533/#534/#535). The destination survives this review intact. The
route does not: CAP-23 has chosen the wrong substrate noun, inverted the value
ordering, and priced 18–26 mw for an outcome the tree's own analysis reaches in a
fraction of that.

## 1. There is no "scheduled-event graph" to walk

PF-3, Open Question 1, risk 2 and AC-6 are all phrased over "the scheduler's own
event graph." That graph does not exist and, more importantly, *would not carry the
answer if it were retained.* `SimEvent` is `(time, seq, callBack, todo)` —
`src/jls/sim/SimEvent.java:96-102` — with no cause field and no producer. Of the
2,331,793 events in the flagship RV32I run, **1,919,891 (82.3%) are `PinChanged`**,
a zero-field record (`SimEvent.java:30-31`) meaning literally "something upstream
moved" (`keystone-c-performance.md:126-127`). Retaining a ring buffer of those, as
KC-23-2 proposes, retains four-fifths noise.

Open Question 1's recommended default — *"whatever the scheduler already keys on"* —
is therefore the wrong answer to its own blocking question, and lf-03 says so
explicitly: **"Per-event journalling is the obvious design and it is wrong"**
(lf-03:154). The right record is keyed to the *value site*, not the event:

```java
record NetChange(long time, int siteId, Value value,
                 int producerId, int causeIndex, Kind kind)
```

~380 K records ≈ 9 MB for the whole 6004-cycle CPU; tens of kilobytes for a
classroom circuit. Two write points (`WireNet.propagate:454-510`, around
`Simulator.java:239`), and the over-approximating `causalInputs()` trick — all
attached inputs, exactly correct for 21 of 25 `react`s, merely wide for four — makes
it correct on day one. CAP-23 never mentions any of this.

## 2. Alternative A — the journal is the seam; every panel is a view over it

Reframe the whole capstone around one core artifact instead of six GUI features:

- **The chronogram (PF-1)** stops being a new data path. `Trace.Change(BitSet, long)`
  (`Trace.java:51`) *is* a per-site change list already; the journal is that record
  plus two ints. The panel becomes a renderer.
- **Cross-probing (PF-2)** stops being plumbing. `producerId` and `siteId` are fields
  on the record the waveform lane is drawn from; edge→element is a field read.
- **The cause chain (PF-3)** is the backward walk over `causeIndex`, using
  `Trace.firstChangeAtOrBefore` (`Trace.java:445-458`) — the search is already written
  and already unit-tested (`TraceWindowingTest`).
- **AC-5, KC-23-1 and the entire demo slice dissolve.** CAP-23 spends its first
  3–4 mw and its first kill criterion proving that a tap can be free when a *GUI panel*
  is closed. That is a self-inflicted problem: gate the journal, not the panel, behind
  the single early return that `BatchSimulator.afterEvent:144-145` already demonstrates
  and `grand-architecture.md:325-330` already mandates. The hot-plane question then has
  the same answer it has for VCD tracing today.

Under this framing the funded 3–4 mw (#508 item 6) buys the journal core plus the
headless printer — shippable, headless, gradeable — instead of a waveform panel and a
negative result.

## 3. Alternative B — rewind is a query, not a re-simulation

§1 step 4 asks for *displayed canvas state at time T*. CAP-23 answers it with
deterministic re-simulation from 0, accepts O(T) as "honest," concedes the UI must
show replay progress (risk 3), stakes AC-2 on byte-identical replay under the
interactive engine, and adds KC-23-3 to stop the project papering over failure with
snapshots.

None of that is necessary. If every site's value history is in the journal, the
displayed state at T is `firstChangeAtOrBefore(site, T)` for each visible site —
O(log n) per site, exact by construction, no determinism claim required, no progress
bar, no exclusion-2 pressure at all. **Re-simulation is only needed to *resume* from
T, which §1 never asks for.** This is the reframing that makes the issue's hardest
technical problem disappear rather than resisting it: CAP-23 defends #498 §8
exclusion 2 by accepting an expensive mechanism; the query defends it by not needing
one.

I am explicitly disregarding **AC-2 and KC-23-3** on these grounds. They are
acceptance criteria for a chosen mechanism, not for the outcome.

## 4. The omitted hard dependency, and it is load-bearing

`SimEvent.sequence` is a **mutable `static long`**, incremented non-atomically in the
constructor (`SimEvent.java:87,119`), and `compareTo` breaks same-time ties on it
(`:143-146`). PF-6 wants a re-simulation running against a live interactive session.
Two `Simulator`s sharing one global counter do not merely offset each other's
sequence numbers — they *interleave* them, so relative tie-break order within each run
diverges, nondeterministically, under thread timing. AC-2 would then fail as a flake
and be diagnosed as a rewind bug.

lf-03 calls the per-`Simulator` counter (P1-S0) a **hard prerequisite** and notes it
is on the critical path of exactly one thing in the roadmap, and this is it.
CAP-23's `blocked_by` is empty and risk 4 asserts determinism is "already pinned by
goldens." It is pinned for one engine on one thread. Fix the machine block.

## 5. Alternative C — instruments without new elements (PF-5)

ARCHITECTURE.md's "Adding an element today (the honest list)" is sixteen places, and
ends: *"If you find yourself doing this, read #78 first."* PF-5 adds two elements —
~32 touchpoints, two help pages plus `Map.jhm`/`JLSHelpTOC.xml` rows, two `SaveTags`
rows, `AllElementsRoundTripTest` fixtures, and a change to the normative
`docs/file-format.md`. It is the only PF that touches the save format, and it
duplicates machinery that ships: the word generator is `SigSim`/`TestGen` driven by
the `-t` grammar, which is already a *documented stability contract*
(`docs/batch-interface.md`); the trigger is `Stop`/`Pause` (`Stop.java:147-161`)
generalized from "input non-zero" to a predicate.

Reframe PF-5 as a **trigger predicate over the journal**, expressed in the existing
watch/`-t` vocabulary and evaluated in the same gated hook. Zero new elements, zero
format change, no #78 debt — and AC-3 (interactive VCD == batch VCD byte for byte)
becomes true by construction rather than by a golden, because there is one stimulus
path, not two.

## 6. The ordering is inverted against the project's own leapfrog analysis

lf-03 names exactly three axes where JLS could lead: schematic-native causality, a
**headless deterministic `--why` artifact**, and free/offline reach. On the batch
artifact it is unambiguous: *"That is the thing that makes causal debugging gradeable,
diffable and CI-testable, and it is what nobody else has"* — priced at 2–3 weeks once
the journal exists, because it is a printer.

CAP-23 puts it in **Open Question 4, "rides along," owned by the CAP-06 lineage**, and
makes "graders and CI stay untouched" a selling point. Meanwhile PF-1 (a waveform
panel) and PF-4 (forward gate-by-gate stepping) are the two items that are *catch-up*:
hneemann's Digital's "single gate mode" is PF-4, verified in lf-03's own source table.
So the capstone funds the two features competitors already have, defers the one
feature nobody has, and does it in the tool whose entire test culture is headless
goldens (`HeadlessCoreRatchetTest`, `BatchSimulationGoldenTest`, `VcdExportGoldenTest`)
while AC-1 depends on the display-lane Swing harness.

The same inversion shows in the hazard framing. The roadmap prices P4's glitch
detector at **~1.5 weeks** and calls it *"the highest teaching-value-per-week item in
six sweeps — no file-format change, no value domain, no arcs"*
(`capability-roadmap/README.md:475`). JLS's transport-delay model already produces
real runt pulses (`simulation-semantics.md:206-224` — no inertial suppression), so
detector + existing `Trace` + a `--why` tree delivers "hazards stop being an
instructor's assertion" for a few weeks, not 18–26 mw. CAP-23's risk 1 sees P4 only
as a data-model collision hazard and never notices P4 already owns step 1's trigger.

## 7. What I would keep, and what the re-cut looks like

Keep §1 nearly verbatim — six observations at one commit is the best outcome
statement I have read in this series, and it should survive the re-derivation as the
acceptance narrative. Keep AC-6 with "journal" substituted for "event graph": an
invariant test that every reported cause is a true ancestor is the right oracle. Keep
the refusal to feed viewer values back into simulation (KC-23-4).

Concretely:

- **PF-0 (new, first):** the `jls.sim` causality journal — site index, `NetChange`,
  two write points, over-approximating `causalInputs()`, off behind one early return.
  Requires P1-S0 (per-`Simulator` sequence counter). Note lf-03's ordering constraint:
  the site index and any future levelization slot table are the same table, so it is
  designed once, here, or twice, later.
- **PF-0b:** `jls -b --why 'net@time'` printing a deterministic tree to stdout, under
  `docs/batch-interface.md`'s stability contract. This is the ship-first item.
- **PF-1/2/3 collapse** into renderers over the journal; PF-3's data model is P9's,
  adopted not minted.
- **PF-6 splits:** display rewind becomes a journal query (keep); re-simulation and
  external-viewer cursor sync are dropped, as #508 already recommends.
- **PF-5 becomes a trigger predicate**, not two canvas elements.
- **Strike AC-2, AC-5, KC-23-1, KC-23-3** as mechanism artifacts; strike Open Question
  1's recommended default and replace it with net-change granularity; promote Open
  Question 4 from "rides along" to the first deliverable.

That is a capstone whose funded slice ships something a grader can run, whose GUI is
optional decoration over a tested core, and whose hardest claims are arithmetic rather
than promises. It also fits inside #508's 3–4 mw far better than the current demo
slice, which spends its whole budget proving that a tap it did not need to place is
free.
