# Issue #695: TASK-C533-2: each step animates the propagation wavefront along the wires, highlighting the producing and consuming elements as its delay is spent
- Lens: adversarial
- Date: 2026-08-08
- Reviewer: Claude Sonnet 5
- Verdict: needs-rework

## Summary

#695 is the "observable" half of #533 (FEAT-C23-4), layered on top of its
prerequisite #693 (TASK-C533-1, the step/wavefront controls). The feature
intent is reasonable and the task decomposition against #533/#693 is
internally consistent (`ordering_after: [TASK-C533-1]` correctly names #693;
`part_of_feature: 533` correctly names the parent). But the acceptance
criteria assume a data model the simulator does not have, contradict each
other on what "the delay is spent" actually means, are silent about a whole
class of events the criteria's own wording claims to cover universally, and
name no verification mechanism for what is fundamentally a rendering claim.

## Findings, most severe first

### 1. AC1's core premise — "the affected wire(s)" — has no data to work from

`SimEvent` (`src/jls/sim/SimEvent.java:88-98`) carries only a `time`, `seq`,
a `callBack` (the *consuming* `Reacts`), and a `Payload`. It has no field
for the producing element, the `Wire`, or the `WireNet` involved. Confirmed
at the actual post site, `WireNet.propagate` (`src/jls/elem/WireNet.java`
~500-503):

```java
sim.post(new SimEvent(now, (Reacts) element, new SimEvent.PinChanged()));
```

— one event per attached input, carrying nothing that identifies which
`Wire` segment(s) lead to it or which `Output`/element drove the net. AC1
says flatly: "Advancing one event animates the affected wire(s) from
producer to consumer and highlights both ends." As written, an implementer
cannot derive "the affected wire(s)" from the event that is being stepped —
that plumbing (thread producer/net identity through `SimEvent`/`Payload`,
or capture it out-of-band at post time) is a real design decision this task
doesn't scope, and neither does #693, which it depends on. This is a
feasibility gap, not a detail: it's the central object the acceptance
criteria are built around. Recommend: either fold "identify the event's
wire/producer" into #693 (control layer, which already touches the post
path) as an explicit AC, or add it explicitly to #695 with its own
acceptance test, before estimating band_mw.

### 2. AC3 vs. the Outcome narrative: is delay-duration proportional or not?

The Outcome text promises: "Per-gate delay becomes time a student watches
pass rather than a parameter they are told about" — i.e., a NOT gate
(default delay 5, `docs/simulation-semantics.md` §7) and an Adder (30×bits,
same table) should visibly differ. AC3 then says: "Animation duration is a
view setting independent of simulated time; changing it changes no
simulated value and no recorded output." Read literally, AC3 permits (and a
minimal implementation would likely choose) one fixed animation length for
every step regardless of the stepped event's actual delay — which satisfies
AC3 word-for-word while flatly contradicting the pedagogical promise the
issue opens with. Compounding this: per `docs/simulation-semantics.md`
§6.1 ("There is no per-wire or per-segment delay... all delay in JLS lives
in elements") and §6.2 (transport delay), by the time an event is dequeued
its delay has *already* elapsed in one discrete jump — there is no
"elapsing" happening during the step to visualize honestly; any
proportionality would have to be manufactured for display purposes only.
The issue needs to state, unambiguously, whether animation length scales
with the stepped event's delay (and if so, by what mapping/clamp for huge
delays) or is uniform — because those are different features with
different acceptance tests, and AC3's current wording lets either one claim
compliance.

### 3. AC1 quantifies over "one event," but several event kinds have no wire and no separate producer

`SimEvent.Payload` includes `MemoryWrite`, `MemoryRead`, and `StateChanged`,
which are self-scheduled: the callback is the *same* element that posted
the event, with no wire in between. Evidence:

- `src/jls/elem/Memory.java:1384-1397`: `sim.post(new SimEvent(now+accessTime, this, new MemoryWrite(...)))` / `new MemoryRead(...)`
- `src/jls/elem/StateMachine.java:789-790`: `sim.post(new SimEvent(now+propDelay, this, new StateChanged(newState)))`

AC1's "advancing one event animates the affected wire(s) from producer to
consumer and highlights both ends" has no defined behavior when producer
and consumer are the same object and no wire is involved at all (an
internal memory access completing, a state-machine transition landing).
Either the criterion needs a carve-out for these payload kinds (e.g. "for
wire-carrying events only; internal transitions highlight the element
alone"), or the issue needs to say what "both ends" means when there's only
one end. Left unstated, this is exactly the kind of gap where the shipped
behavior (silently skip animation for these events, or crash on a null
wire reference) could go either way and still "pass" an under-specified
review.

### 4. No named verification mechanism, unlike its own sibling issue

Compare #693 AC3: "a test asserts the stepped sequence equals the free-run
sequence element for element" and AC4: "asserted rather than assumed." #695
has none of that language — every AC is a bare behavioral claim ("animates
...", "highlights ...", "is visible") with no test class or assertion
mechanism named. `test/jls/ui/package-info.java` describes exactly the
tooling that would be needed: Layer 2 (Xvfb + synthesized input,
`InteractiveSimulatorSmokeTest` is the named precedent) and Layer 3
(`RenderAssert`/`RenderBoundsTest`-style semantic paint checks, explicitly
*not* pixel goldens). Note also that `ARCHITECTURE.md` (lines ~223-227)
describes Layers 2/3 as "reserved," while `test/jls/ui/package-info.java`
says Layer 2 is "present, growing" and Layer 3 has a "starter present" —
that's a stale-doc drift a contributor picking this issue up would trip
over independent of the issue itself, but it means the issue's author
cannot have safely assumed the reader knows what's actually available. As
written, AC1/AC2 ("animates," "highlights") are gameable: a change that
sets a `highlighting = true` boolean on the model with no actual paint
work, checked only by a Layer-1 headless assertion, would satisfy the
letter of the criteria without a student ever seeing anything move.
Recommend naming the specific Layer-2/3 mechanism (or a new
`RenderAssert` helper) as part of the "done" bar, the way #693 does for its
own criteria.

### 5. Undisclosed interaction with the existing `Animate` button/timer

`src/jls/edit/InteractiveSimulator.java` already has an `Animate` button
(line 58) whose documented behavior is "repeat step every second" (line
136) via a `java.util.Timer` — and that path is flagged in its own comment
as "a pre-existing off-EDT path in the #49 series" requiring re-dispatch
(`draw()`, lines 1227-1249). #695 introduces a second, finer-grained
animation (per-event, duration a "view setting") that necessarily has to
interleave with — or replace — that existing 1-second auto-repeat without
naming it once. If both timers are meant to coexist, the wavefront
animation duration must fit inside whatever the auto-repeat interval is,
and the reused `Reacts`-adjacent naming clash (`Element.highlight`,
`src/jls/elem/Element.java:41-42`, already means "drawn selected/hovered")
needs a new, distinct visual channel so simulation-highlight and
selection-highlight don't stomp on each other during a step. Neither
concern is mentioned. Recommend the issue explicitly say whether it
supersedes, disables, or must interoperate with the existing `Animate`
button, and that any new per-event highlight state is kept separate from
`Element.highlight`.

### 6. Scale assumption left implicit

AC2 asks to animate "every wire carrying an event at that timestamp
concurrently." The recorded architecture decision on simulation strategy
(`ARCHITECTURE.md`, "Simulation execution strategy," #221) explicitly notes
CPU-scale designs exist on the `riscv/` trajectory (#200-#202) even though
they're not the present workload. A wavefront on such a circuit could
carry a large number of simultaneous events; the issue doesn't say what
"animates ... concurrently" degrades to at that scale (cap the count? skip
animation above a threshold? accept a slow frame?). Minor relative to
findings 1-4, but worth a line in the issue rather than leaving it to
implementer discretion.

## What's solid

- The interactive-only / headless-no-op requirement (AC4) is consistent
  with the codebase's existing headless discipline (`HeadlessCoreRatchetTest`,
  issue #77) and costs nothing new to enforce since `InteractiveSimulator`
  already lives in `jls.edit`, not `jls.sim`.
- The task's dependency wiring (`ordering_after: [TASK-C533-1]` → #693,
  `part_of_feature: 533` → #533) is correct and matches the boundary
  comment on #533 distinguishing it from #535.
- No new third-party dependency or licensing exposure is implied by a pure
  Swing animation/paint feature.

## Verdict rationale

The two acceptance criteria that carry the feature's actual pedagogical
payload (AC1, AC2) rest on data the event model doesn't currently expose
(finding 1) and are internally inconsistent about what they're even
promising to show (finding 2), with an unaddressed gap for a whole payload
category (finding 3) and no stated way to check any of it (finding 4).
These are fixable with a rewrite of the issue body, not a sign the feature
is a bad idea — hence **needs-rework** rather than should-not-proceed.
