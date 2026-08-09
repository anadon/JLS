# Issue #434: TASK-0102: two drawable level converters and a lock-step contract in which the digital loop owns the clock — an exact crossing tick, a ramped D-A, and no rollback machinery anywhere
- Lens: visionary
- Date: 2026-08-08
- Reviewer: Claude Opus 5
- Verdict: endorse-with-reframing

## What this issue is actually for

Strip the deliverable list and one idea is left: **JLS should be able to teach where the
analog/digital boundary sits, by making the boundary a thing you draw rather than a thing an
engine hides.** Sampling is `Adc -> Register <- Clock`, not a parameter. An n-bit converter is
an R-2R ladder you drew. A sample-rate parameter is refused by name because it would create a
second owner of time. That claim is excellent, it is not served by any tool students currently
use (Falstad shows you analog, LTspice shows you analog, neither shows you the *crossing rule*),
and it is worth building.

The second idea is the ownership inversion: the discrete loop owns `now`, the solver's private
`t` is the only thing that moves backwards, only accepted results become posted events. That is
a genuinely elegant reframing of a problem the industry solves with rollback machinery, and it
is the most valuable thing in this issue.

Everything else — four element types, the sealed-permits tax, the palette collision, the
registration checklist — is packaging. My argument is that the packaging is cut along the wrong
seams, that it makes the good ideas unbuildable for months, and that a different cut delivers
the falsifiable core in days.

## 1. The program this task belongs to is declared out of scope by the project's own roadmap

`docs/capability-roadmap/README.md` §6 "What still stays out", ground (a) *different tool class*:

> **Continuous-time and analog.** ... Supporting these means being a SPICE-class solver — a
> different tool, not a deeper digital model.

`docs/capability-roadmap/sweep-06-physical-boundary.md:83`: "No continuous-time solver, and none
should be added." `:553-556`: "Adding one is building SPICE, which is ground (a)."

`ARCHITECTURE.md`'s recorded decision on #221 makes `jls.sim.Simulator` the **sole** execution
strategy and binds any future strategy to observational identity with
`docs/simulation-semantics.md`'s two-states-plus-HiZ domain.

Against that, #368 states plainly: *"Owner: **UNOWNED** in `docs/capability-roadmap/` — no
program pays for this today."* And the documents that argue the reversal —
`11-analog-determination.md`, `docs/plan/evidence/BRIEF.md` with its Decision D9 —
**do not exist in this tree**: there is no `docs/plan/` directory at all, and the filename does
not appear anywhere on disk. Verified: `src/jls/analog` absent, `git grep` for
`nextEventTime|class Adc|class Dac|AnalogRegion` over `src/` returns nothing, `ElementRegistry.ALL`
is 35 rows, `LogicElement` has 24 permits — every O-claim in §2 reproduces.

This is not a nit about paperwork. A visionary reading has to say it out loud: **a task cannot
be the third level of a program whose direction reversal is argued only in documents the
repository does not contain, against a roadmap section that says the opposite.** Before this
task is fundable, `docs/capability-roadmap/README.md` §6(a) needs an amendment naming what
changed, and `ARCHITECTURE.md`'s recorded-decisions section needs an entry — the same treatment
i18n, FlatLaf, and the plugin trust boundary each got. That amendment is a half-day of writing
and it is the single highest-value thing anyone could do to this program right now.

## 2. The reframe: A-STEP is not an analog contract. It is the external-participant contract.

Read §7.9 and §7.10 with the word "analog" deleted:

> An element may own a private sub-time, may iterate that sub-time freely inside one `react` call
> at one instant, and must publish only accepted results as future-dated events on the integer
> tick lattice, holding exactly one pending self-event.

Nothing in that sentence is about Newton, Trapezoidal, or voltage. It is the general contract
for **any participant whose internal model is not the event loop's** — and JLS has other such
participants queued: HDL co-simulation (#33/#59), the Yosys/GHDL/Icarus subprocess integrations
that `ARCHITECTURE.md` §"Plugin trust boundary" already commits to keeping out-of-process, and
the RISC-V reference-model differential work (`riscv/fuzz_diff.py`, #200–#202).

The consequence is large and the issue never notices it: **TASK-0097 is not a real prerequisite
for the thing this task is trying to prove.** H1 (inversion deletes rollback), H2 (lattice
quantization makes the digital stream independent of internal step control), H4 (the ≥1-tick
publication floor is what terminates the loop), P8/P9 (the step-cap regimes), P10 (exactly one
pending self-event), P11 (the outcome-line change) are *all* falsifiable against a participant
whose "trajectory" is a **closed form** — an RC exponential evaluated in `Math.exp`, with a
deliberately jittered internal step schedule to stand in for a timestep controller. No Newton,
no Trapezoidal, no LTE. P4 — "the payoff the crossing policy exists for" — becomes "three
different internal step schedules, identical published ticks", which is a *stronger* test than
three LTE tolerances because the schedules can be adversarial rather than merely 10× apart.

So: **file A-STEP as its own task, gated on nothing, and land it against a toy participant.**
Days of work, not weeks, and it answers the question the issue itself says means *Stop* if
refuted (§10, H1). Discovering H1 is wrong after FEAT-046 has shipped a Newton solver is the
expensive ordering; discovering it against a 40-line exponential is the cheap one. This also
gives the solver work a written contract to be built *against* rather than beside, which is what
§12's "built separately, JLS acquires three refusal vocabularies" argument asks for everywhere
else.

## 3. `nextEventTime()` is the wrong seam, and the tree shows why

The issue is proud that the engine footprint is one five-line observational method. But the
region does not want the queue head's timestamp; it wants to know **how long it may run
uninterrupted**. Exposing the raw peek couples every future self-scheduling participant to the
queue's internals, and it invites exactly the unconditional conservative cap §7.10 calls
arithmetically fatal.

It is also not sound as an "exact" cap. `docs/simulation-semantics.md` §7 lists **Splitter,
Binder, Extend, Constant, pins, jumps, wires, subcircuit boundary at delay 0**, and `:231`
states that such elements "propagate within the same timestamp". A `Dac` driven by a `Register`
through a `Binder` therefore changes at the *same tick* the register fires. `nextEventTime() - t`
is then `0`, the region's cap is zero, and whether the region sees the new drive at all depends
on `SimEvent` sequence order within the tick — i.e. on `getElementsInStableOrder()`. Regime 2's
"exact, because `Clock` self-schedules" rests on an unstated global invariant (*no element ever
posts at the current instant*) that the delay table refutes.

The better seam is a **quiescence horizon** that the engine computes and the region requests:
`long horizonFrom(long t)` returning the earliest tick at which any *boundary input of this
region* can change, derived from the region's port set rather than from the global queue. That
is exactly the shape §5's P8 already asserts for the `Dac`-free case ("a structural assertion
over the region's port set, not a timing observation" — #368 criterion 2), generalized to all
three regimes. It makes regime 1 and regime 2 the same code path, it is honest about zero-delay
fan-in, and it does not hand every future participant a pointer at the event queue.

## 4. Four element types is two, and the issue's own mathematics says so

§7.10 defines one thresholding function whose `vlow < vhigh` case gives dead band and hysteresis
and whose `vlow == vhigh` case is an ideal comparator; it defines one PWL ramp whose `trise = 1`,
`rout = 0` case is an ideal step-to-the-lattice. `IdealAdc` and `IdealDac` are therefore
**parameter presets of `Adc` and `Dac`, not distinct behaviours** — Open Question 3 recommends
separate types "so the abstraction ladder is visible in the palette", which is a UI argument
being paid for with a type-system tax.

That tax is larger than §8 admits. `ARCHITECTURE.md` §"Adding an element today (the honest
list)" is sixteen places; §8's checklist covers about eight of them and omits the help page
under `resources/help/elements/**`, the `Map.jhm` topic, the `JLSHelpTOC.xml` entry, the
`HelpTopicsTest` palette list (which *fails the build* until the topic exists), and the
`AllElementsRoundTripTest` fixture. Multiply the missing eight by four types.

Halving the type count halves all of it, and the "prove the behavioural rung agrees with the
resistor rung" test — the thing the ideal pair exists for — is *better* served by comparing a
`SubCircuit` containing an R-2R ladder against a configured `Dac`, because that is the
comparison a student actually makes, and JLS already has the differential-golden machinery for
it.

## 5. TASK-0105 is not feature-scale, and the palette already has the axis

O4's alarm is right — `PaletteContractTest.paletteIsTotalOverTheElementRegistry` will stay green
while forcing analog buttons onto the first-year toolbar, and *a passing test is the failure
mode*. But the remedy priced as a blocking feature on an unfiled task inside an unfiled feature
already half-exists: `src/jls/edit/Palette.java:36` declares `public enum Group` carrying
`(rows, cols, standalone)`, and `ElementType`'s javadoc explicitly reserves audience/GUI
concerns for the palette side ("palette icon, category, help topic ... never appear here").

A `defaultHidden` boolean on `Group`, a `Group.ANALOG`, and a view toggle is a day's work inside
this task. It also retires the hardcoded `NON_PALETTE_TAGS = {SubCircuit, WireEnd, TestGen}` set
by turning "no palette row" into "audience: none", which is a strict improvement #78 wants
anyway. Keeping it as an external hard blocker means this task cannot start until two unfiled
issues under two unowned features land — and the honest description of that state is *not
schedulable*, not *blocked*.

## 6. If the roadmap's §6(a) survives the amendment, the inversion is what makes co-simulation cheap

Worth stating even though I expect it to be declined. `ARCHITECTURE.md` §"Plugin trust boundary"
records that external tool integrations "already sit on that subprocess boundary and stay there."
The ownership inversion makes the analog region a **pure function of a bounded window** —
`(boundary inputs, t_from, t_to) -> accepted trajectory` — with no callbacks into the engine and
no un-committing. That is precisely the shape you can put behind a pipe; a rollback-based
coupling is precisely the shape you cannot. H1 is therefore not just an argument against
building rollback, it is an argument that JLS *need not own the solver at all*.

The stated objection is determinism (#351 "owns determinism across platforms"), and it is a real
one against JLS's reproducibility culture. But P-b answers it better than the issue realizes:
bisecting to the integer tick lattice with a declared dead band is a **quantizer**, and the
golden that matters is the digital stream, not the trajectory. If the published tick is provably
invariant under a 10× LTE change (P4), the same argument extends to invariance under a different
*solver*, given a dead band wider than cross-platform float drift. That is a stronger and more
interesting claim than P4, it is testable, and it is the claim that decides whether JLS must
write a SPICE or may borrow one. Nobody has stated it.

## Acceptance criteria I am explicitly disregarding

- **"Four registered, drawable ... element types"** (§13, P12) — two, per §4 above.
- **TASK-0097 as `blocked_by`** — a genuine prerequisite for a shipping `Dac -> RC -> Adc` loop,
  not for A-STEP or for H1/H2/H4/P8–P11, which are the design's whole content (§2 above).
- **TASK-0105 as `blocked_by`** — absorb it as a `Group.defaultHidden` flag (§5 above).
- **"the engine footprint is `nextEventTime()` and nothing else"** (§14) — replace with a
  region-scoped horizon query (§3 above).

## What I would not touch

Sample rate refused by name. Sampling drawn as `Adc -> Register <- Clock`. `vlow > vhigh`
refused rather than swapped. The ≥1-tick publication floor as a termination requirement rather
than a preference. Ramping rather than stepping into the solver. The lattice-defined crossing,
never read off an accepted point. The `PinChanged` reuse and the *one pending, not this object*
invariant that O7 forces. Documenting the `"Simulation Time Limit"` outcome change in
`docs/batch-interface.md` §3.1 in the same commit. Every one of these is right, and several are
insights this project would not have arrived at by accident.

## Verdict

**endorse-with-reframing.** The thesis — the boundary is drawn, the digital loop owns time,
rollback is deleted rather than deferred — is the best idea in this program and should survive
intact. The packaging should not: split A-STEP into a solver-independent task that can land now,
collapse four types to two, absorb the palette dimension, replace the queue peek with a horizon,
and — first — amend `docs/capability-roadmap/README.md` §6(a) and `ARCHITECTURE.md`'s recorded
decisions so this program has a written owner and a written reversal instead of citations to
documents that are not in the tree.
