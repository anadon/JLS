# Issue #393: TASK-0064: events that model no elapsed time stop going on the queue, the golden corpus stays byte-identical, and a combinational loop becomes a named diagnostic instead of a hang
- Lens: visionary
- Date: 2026-08-08
- Reviewer: Claude Opus 5
- Verdict: redirect

## What this issue is actually for

Two unrelated goods are bundled in one task:

1. **Stop paying queue overhead for events that model no elapsed time** — a constant
   factor in service of #362, and behind it the drawn-processor capstones (#295, #301).
2. **A combinational loop becomes a named diagnostic instead of a hang** — a
   *correctness and pedagogy* fix for the tool README.md calls "an educational digital
   logic circuit editor and simulator."

(2) is the more valuable of the two for what JLS *is*, it is independent of (1), it is
landable today, and as written it can only ship if (1) survives kill criterion K3 and two
unlanded prerequisites (#476, #442). That is backwards. Separately, (1) as scoped is
aimed at a seam that the measurement does not support and that its own prerequisite
largely occupies. I am disregarding the "no golden changes — that is the entire
acceptance criterion" framing not because byte-identity is wrong (it is right) but
because it is the acceptance criterion of a *different, smaller* change that gets more
of the win; the reasons follow.

## 1. The 82.3% headline does not belong to this task

The 2026-08-08 comment tells the author to restate the premise as
`docs/capability-roadmap/keystone-c-performance.md` § 2's census: `PinChanged
1,919,891 (82.3%)` of `fired 2,331,793` on `riscv/build/k2000.jls`. But those events are
posted by `WireNet.propagate` (`src/jls/elem/WireNet.java:507`) to **every** sink of a
changed net, and the census's own callback histogram shows where they land:

```
Mux 875,291  Register 428,298  Splitter 250,115  AndGate 207,456
Adder 108,025  ShiftRegister 97,122  XorGate 82,011  NotGate 67,983
Memory 67,545  Binder 57,026  OrGate 50,860  Extend 16,000
```

The zero-delay set `Z` on this fixture is `Splitter + Binder + Extend = 323,141`
callbacks — **13.9% of fired events**, not 82.3%. § 2 also records the fixture as "225
logic elements, 297 nets, **flat**": there is no `SubCircuit`, no `InputPin`, no
`OutputPin`, no `JumpStart` in it at all. The remaining ~68% of zero-time events are
notifications to `Mux`, `Register`, `Adder`, `Memory` — the *timed frontier* `F(n)`,
which § 7.10 says this task still posts. The task therefore cannot claim them, and
keystone-c § 8.3's "stage 1: loop −30…−40%" is sized against the 82.3% number, not
against the 14% this cut reaches.

Two honest corrections follow. The fraction is understated for the *hierarchical*
designs #295/#301 actually care about (pins and subcircuit boundaries are `Z` members and
k2000 has none), so the number to measure against is not k2000 — it is a subcircuit-heavy
fixture that does not exist yet. And on the one fixture that does exist, the leg is worth
roughly 0.10 s of a 0.742 s loop *before* subtracting the cone-walk cost that § 5 of the
comment correctly flags as unbudgeted.

## 2. § 7.10 has a fork in it, and both branches are bad

"Today every `e ∈ C(n)` contributes a queued event at `t`; after this task only `F(n)` is
posted, **at `t + d(e)`**."

- **Read literally** — the sweep evaluates the frontier element itself and posts its
  `NewValue(t+d)` directly — the change is unsound. Today a `Mux` fed by two different
  cones at the same timestamp gets **one** coalesced `PinChanged` (`SimEvent.equals` over
  `(time, callBack, todo)`, `docs/simulation-semantics.md` § 3), and reads settled inputs
  under the § 6.1 read-latest rule. A per-cone sweep has no visibility of the other cone
  still pending at `t`, so it would evaluate the `Mux` against half-settled inputs. H1
  fails, and P1 fails with it.
- **Read safely** — the frontier is still posted as `PinChanged` at `t`, exactly as today
  — the change is sound and is worth the 14% of § 1.

The issue must pick, and picking the sound branch is picking the small win. Note also
that § 7.10's `|{evaluations of e at t}| = 1` is *stronger* than today's engine, not equal
to it: `dupCheck` coalesces only while an event is **pending** (`dupCheck.remove(event)`
at `Simulator.java:225`), so a zero-delay element already re-reacts several times per
timestamp today when a second cone reaches it after it fired. Each of those intermediate
evaluations can produce a distinct trace sample —
`BatchSimulator.afterEvent` appends a `TraceSample` whenever the value differs from the
previous one, at `event.getTime()`, so same-timestamp transients are *in the VCD goldens*.
Collapsing them to one evaluation is precisely the kind of change P1 exists to catch,
and H2 as stated asserts the collapse is free. It is not obviously free.

## 3. The radically simpler seam: drain the present, do not compile the cone

The property this task wants is "an event that models no elapsed time should not pay for
the heap." That property does not need a cone, a levelizer, an elaboration-time
topological order, or cached `WireNet` adjacency. It needs one observation about the
existing ordering key:

> Within a timestamp, `SimEvent.compareTo` orders by `seq`, and `seq` is assigned at
> construction, which happens at the `post` site. So the pending events at `time == now`
> are **already in post order**, and every one of them precedes every event at a later
> time.

Therefore: give `Simulator` a plain `ArrayDeque<SimEvent> present` alongside the heap.
`post` routes `event.getTime() == now` to the deque and everything else to the
`PriorityQueue`; `runEventLoop` drains the deque before it polls the heap, and starts a
fresh deque each time the heap hands it a new `now`. The fired sequence is *identical by
construction* — not by a levelization argument, not by a `SubCircuit`-crossing proof, not
by a `Memory`/`Adder` exclusion list — because a FIFO of equal-time events at the current
time is exactly the head of the priority queue. That is ~20 lines in one file, touches no
element, changes no document's meaning, needs no `blocked_by`, and it removes the heap
sift for **all 82.3%** of zero-time events, including every `PinChanged` to a `Mux` or a
`Register` that the cone can never touch.

It also composes rather than competes: `dupCheck` (or #476's intrusive flag, or
keystone-c § 7.2.4's per-element pending slot) is orthogonal to *which container* holds a
pending event.

**And this is why the performance leg is likely already spent.** #476's time-bucketed
calendar queue *is* a present-bucket by construction: a bucket for `now` turns each
zero-time post into an append and each poll into a pop, with no comparisons. If #476
lands as described, #393's remaining measurable delta on any fixture is the `SimEvent`
allocation (keystone-c § 7.2.2 interns `PinChanged` in one line) plus the dispatch itself.
A task whose declared prerequisite subsumes most of its declared win should not be
executed before that prerequisite is measured — and the issue's own § 6 concedes the
inverse ("measuring the closure against the priority queue attributes the queue's cost to
the closure") without noticing that the symmetric error is the one it is set up to make.

## 4. The derivation the issue asks to invent already exists — and its enumeration is short

H3 and P4 want the zero-delay set derived from declared delay, "never a hard-coded class
list." JLS already has the type-level answer and the issue never names it:
`jls.elem.Timed` (issue #78's capability interface, `src/jls/elem/Timed.java`), implemented
by exactly the 18 timed classes. `Z` is `!(e instanceof Timed) || ((Timed) e).getDelay() == 0`
— derived twice over, by type for the natively-zero elements and by value for a
`DelayGate` set to 0, which is precisely P4. `Splitter`, `Binder`, `Constant`,
`InputPin`, `OutputPin`, `SubCircuit` carry no `getDelay` at all, so "declared delay of 0"
is not literally available for them; `Timed` is what makes the derivation total. Naming it
also kills H3's falsification path.

Two corrections fall out of the same seam. `src/jls/elem/JumpStart.java` posts at `now`
(`new SimEvent(now, jend, newValue)`) and is a zero-delay element that § 6's normative
enumeration in this issue omits — `docs/simulation-semantics.md:285` does list "jumps",
so the issue's derived list is already a lossy copy of the document it derives from. And
`Clock` is *not* `Timed` yet posts `now + when`; it is safe only because it has no inputs
and so can never be a cone interior node. Both are arguments for deriving from `Timed`
plus "posts only at `now`", and against any list at all.

## 5. The diagnostic is the deliverable, and it should be its own issue today

O2 is the strongest observation in the issue: there is no combinational-loop diagnostic
anywhere in `src/`, and neither loop-exit condition can fire, so a student who draws a
loop gets a hang. That is a defect in an *educational* tool, and it is worth more to
#295/#301/#306 than 14% of a loop.

It requires none of this task. The check is a strongly-connected-component pass over the
subgraph induced on `Z` (per § 4's derivation) using the connectivity `Circuit.finishLoad`
has already resolved, run once at `initSimulation` — before any event exists. It is
O(elements + nets), it runs at t=0 where nobody profiles, it names every element in the
cycle because SCC membership *is* that list, and it discharges H4 as a proof rather than
as the iteration cap H4's falsification branch fears. Its report belongs in the seam
ARCHITECTURE.md § "Error-reporting contracts" already specifies — `jls.TellUser`, the
only place allowed to raise a user-visible message and the path `WireNet.propagate:477`
already uses for the multi-driver warning — with the batch surface honoring the
`jls: error: …`/exit-1 CLI contract. The issue says "named diagnostic" eleven times and
never says `TellUser`; that omission is what makes it look like a simulator-internal
concern rather than a student-facing one.

Unbundled, it lands against an empty `blocked_by`, changes no golden (a loop circuit has
no golden today — it hangs), and needs exactly one new fixture plus the
`docs/simulation-semantics.md` paragraph § 7.11 already specifies. Open Question 1
("fatal or recoverable") is answerable now and the issue's own recommended default (fatal)
is right: a combinational loop has no defined value.

## Concrete alternative, stated as three issues

1. **Combinational-loop detection at elaboration** — SCC over the `Timed`-derived
   zero-delay subgraph in `initSimulation`; `TellUser` + CLI contract; normative paragraph.
   No prerequisites. Ships the student-facing half of #393 now.
2. **Present-timestamp drain** (§ 3) — or nothing, if #476 lands its bucket first.
   Acceptance criterion is unchanged (whole corpus byte-identical) and is far easier to
   *believe*, because the equivalence argument is one paragraph about `seq` monotonicity
   rather than a levelization proof crossing `SubCircuit` boundaries in both directions.
3. **The zero-delay closure itself** — kept on file, re-measured *after* (2) and #476, on
   a hierarchical fixture, with the cone-construction cost (Open Question 6) measured
   first. If the residual is a few percent, close it and say so; that is a good outcome,
   not a failure.

## What I would keep from #393 verbatim

O3's seq-conservation framing, O4's finding that `SubCircuit.react` makes the cone cross
instance boundaries in both directions, O5's `Memory`/`Adder` trap, and the discipline
that a semantic difference is reverted rather than documented (#362 § 4 invariant 4).
Those survive every reframing above and should be carried onto whichever issue inherits
the work. § 11's threat list is the best part of the document.

## Verdict

**redirect.** The correctness deliverable is being held hostage by a performance
deliverable whose measured basis belongs to a different set of events, whose central
formula has an unsound reading and a low-value reading, and whose remaining win is mostly
inside its own prerequisite. Split the diagnostic out and land it; replace the closure
with the present-timestamp drain (or let #476 supply it); re-open the levelized cone only
when a hierarchical measurement says it is still there.
