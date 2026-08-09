# Issue #368: FEAT-048: a drawn design crosses between continuous and discrete in both directions at places the student chose — with an exact crossing time, no rollback machinery, and a domain check that keeps the two worlds apart otherwise
- Lens: visionary
- Date: 2026-08-08
- Reviewer: Claude Opus 5
- Verdict: endorse-with-reframing

## What this issue is really for

Two unrelated things are stapled together in one feature. The first is a
**mixed-signal crossing mechanism** (two one-bit bridges, a bisection to the tick
lattice, an inverted time ownership). The second is a **type system for
terminals** — the port/net domain alphabet and its enforcement sites — which the
issue itself says is shared with #344 (radix) and #341 (strength) and which it
carries only because a bridge needs something to be the sanctioned exception to.

The bundling is what produces every one of the issue's own open questions. OQ1
(the residual has no task id) exists because half the feature isn't the feature.
OQ2 (who pays for the descriptor) is the same observation stated as an
accounting problem. §4 invariant 5 — "the domain check and the bridges ship
together" — is the bundling promoted to a release constraint, and it is the one
piece of this issue I am explicitly disregarding below.

## The trajectory this pulls against, stated plainly

The repository's own capability roadmap, at HEAD, in two independently derived
passes, places continuous-time analog **permanently out of scope on ground (a),
different tool class**: `docs/capability-roadmap/README.md:1037-1043`
("Supporting these means being a SPICE-class solver — a different tool, not a
deeper digital model") and `sweep-06-physical-boundary.md:553-556` ("Adding one
is building SPICE, which is ground (a)"). §6 of that document is explicitly a
*re-derivation under the capability frame, not an inheritance* — i.e. the frame
that reversed the sign on SDF, EDIF, BLIF, EVCD and SAIF looked at analog again
and kept it out. The issue does not dispute this; it records it: "Owner:
**UNOWNED** in `docs/capability-roadmap/` — no program pays for this today."

The arithmetic in the capstone this feature serves makes the stake concrete.
#308's own standalone band is **28.5–42 mw** for the required analog set
(FEAT-045 5–7 + FEAT-047 2–3 + FEAT-046 17.5–26 + this feature 4–6), and #308's
corrected kill threshold cites **61.5–94 mw** for the heart-rate capstone's
required rows. Against that, #308's *solver-free rung* — a `Memory` wavetable, a
`Register`, a host audio sink, a WAV codec — is **3–4.5 mw with zero analog
code** and delivers "a student hears the circuit they drew" outright. That
asymmetry is the most important fact in this cluster and it is not this issue's
fault, but it governs whether this issue should be funded at all.

Meanwhile the roadmap's *in-scope* P1 axis — drive strength, open-drain,
pull-up/pull-down (#341) — is the one that turns `sweep-01`'s "the flagship
IEEE 91 symbol-conformance claim is being made with a hole in it" into a closed
claim, unblocks #67 EVCD by its own recorded revisit trigger, unblocks the
digital half of #112 IBIS, and makes #22 I²C possible at all (the sweep's words:
"The lab is not hard today; it is impossible"). It needs **the same descriptor
and the same check sites** this issue would build for the analog axis.

## Reframing 1 — the descriptor is a `PortType`, and it belongs to #341, not here

Concretely: replace `Put.bits` as a domain-carrying sentinel with a value type —
`PortType(domain, width /* -1 = any */, radix, strength)` — hung off the element
registry that **already shipped** (`src/jls/elem/ElementRegistry.java`, 35 typed
rows, `elem.element-provider` in `docs/extension-points.md:30`). Then:

- §1's "single most load-bearing design fact" — that the domain check must sit
  *above* the width check because `Put.java:34-35`'s zero-bits sentinel defeats
  it — **stops existing**. It is not a law of mixed-signal design; it is a
  consequence of overloading one `int` to mean both a width and a wildcard. A
  `PortType.compatible(a, b)` predicate has no ordering problem to get wrong.
- §5 criterion 3 ("assert refusal at all four sites") is testing that four copies
  of the same guard were all edited. `SimpleEditor.java:4014`, `:4141`, `:4246`
  and `:4357` are four textual duplicates of
  `if (b1 > 0 && b2 > 0 && b1 != b2) { overlapMessage = "Bits don't match"; }`.
  The right invariant is **one** connection predicate with four call sites, and
  a ratchet test in the family of `HeadlessCoreRatchetTest` /
  `NotificationRatchetTest` asserting no fifth open-coded compatibility check
  reappears. That is a smaller change *and* a stronger guarantee than criterion 3.
- Whichever axis funds it, the message vocabulary is built once, which is the
  outcome §2 says it wants ("three refusal vocabularies that disagree at the
  edges") but cannot reach from inside the analog program.

**I am disregarding §4 invariant 5.** Its justification — "the domain check
landing without the bridges would make legal designs undrawable" — is only true
once *analog elements exist* (#331). Before that, a one-element alphabet is a
tautology that refuses nothing. The real ordering constraint is
`analog elements ⇒ bridges`, and it belongs on #331, not as a hold on a
general-purpose type system. With the constraint correctly homed, the descriptor
ships with #341, gets exercised immediately by strength and radix, and the
analog axis costs a fraction — which is exactly OQ2's recommended default,
except that the recommendation cannot be executed while invariant 5 stands.

## Reframing 2 — the ownership inversion is a general seam, not analog plumbing

The best idea in this issue is buried in §3: *a region that owns private time can
be an ordinary self-scheduling participant in the discrete event loop, and then
no rollback exists to be needed.* That is not a mixed-signal trick. It is the
general answer for **any externally paced participant** in `jls.sim`:

- the host audio sink and source of #346 (a device with its own clock),
- a wall-clock throttle for interactive demos,
- a co-simulation bridge to a subprocess (the boundary ARCHITECTURE.md's
  §222 decision already sanctions, and that #61/#62/#63 already sit on),
- and — pointedly — the levelized/compiled second strategy that ARCHITECTURE.md's
  §221 decision keeps the door open for, whose equivalence criterion is stated in
  exactly this vocabulary.

The elegant move is to file the seam on its own: a `jls.sim` extension point
(`sim.paced-participant`, contract `nextSelfEventTime()` + `advanceTo(t)`),
catalogued in `docs/extension-points.md` per #223, with `Clock.java:392,421`
retrofitted as its first implementation and the observational
`Simulator` peek — already sanctioned by the loop's own doc comment at
`Simulator.java:210-213` — as its only kernel footprint. That seam is worth
building whether or not a solver ever exists, it keeps FEAT-046 a *module*
rather than a kernel change (which is the whole claim `grand-architecture` §10
makes for itself), and it is the honest way to satisfy §221's "the interpreter
remains JLS's only execution strategy" while admitting a continuous region.

## Reframing 3 — quiescence, which deletes criterion 6 and OQ4

§1 accepts as inevitable that "a design containing a continuous region always has
a pending event," so `eventQueue.isEmpty()` never fires, `"Simulation: No More
Activity"` becomes permanently unreachable, and `maxTime` silently becomes SPICE's
`.tran tstop`. That is a real regression against a documented stability contract
(`docs/batch-interface.md:141`, `docs/simulation-semantics.md:121`) and the issue's
answer is to *document* it (criterion 6, OQ4).

Don't accept it. A region at DC steady state with no pending breakpoint and no
D2A activity has nothing to do; it should **withdraw its self-event and re-post
when awakened**. Quiescence is a first-class, standard notion in continuous
solvers, it is exactly the analog of "the queue drained," and making it explicit
means every stop reason keeps its meaning, `-d` keeps its meaning, and criterion
6 plus OQ4 both evaporate. The hard case (an LC tank that oscillates forever)
genuinely never quiesces — and *that* design ending at its time limit is correct
and needs no documentation amendment at all. `Clock` always has a next event
because a clock is periodic; a solver is not, and modelling it on `Clock` imports
a property it does not have.

## What I would keep unchanged

- **The bisection to the integer tick lattice, and criterion 1.** "The published
  digital stream is invariant under the timestep controller's parameters" is the
  right invariant, it is stated as a *property* rather than a golden, and it is
  precisely consonant with this project's reproducibility culture (byte-identical
  jar, `.buildinfo`, the golden corpus). If any part of this feature survives a
  descope, this is the part.
- **Refusing a sample-rate parameter by name**, and the `d ≥ 1 tick` publication
  floor. Both are "one owner of time" restated; both are correct.
- **`vlow > vhigh` refused as a typo rather than reinterpreted.** Consistent with
  the `LoadError` taxonomy's stance that a file is refused, never repaired.

## The out-of-the-box alternative the issue never considers

Three of the four serving capstones (#308, #303, #305) are pedagogically about
**sampling and thresholding a real-world signal, then processing it digitally**.
That payload is deliverable with no solver, no bridges, no domain alphabet and no
time inversion: a *sampled host source* element — WAV, microphone, or a recorded
PPG/CSV trace — that drives an N-bit digital bus at a declared rate, plus #346's
sink on the way out. A drawn beat counter counting a real heart-rate trace is a
capstone-shaped artifact at #308's 3–4.5 mw rung price. The genuinely continuous
part — R-2R ladder physics, LC ringing, comparator hysteresis on a solved
trajectory — is the part the roadmap already calls a different tool class.

And if the *physics* is wanted, the project's own settled stance for external
capability is "orchestrate external tools, never reimplement" (Yosys, GHDL,
iverilog, all behind subprocess boundaries with skip-if-absent tests). ngspice
with its XSPICE `adc_bridge`/`dac_bridge` — the exact elements D-A7 says these
bridges copy — is that tool, and JLS would be the digital half of a co-simulation
rather than the author of 17.5–26 mw of pure-Java SPICE. The honest cost is
stated, not hidden: co-simulation forfeits the "same bits on every platform"
determinism claim FEAT-046 exists to make. That is a real trade, and it deserves
to be *made* explicitly rather than settled by omission — the grading goldens can
stay purely digital, and the mixed-signal lab can be the skip-if-absent tier that
`GhdlCompileTest` and `IverilogCompileTest` already establish as a shipped pattern.

## Disposition

Endorse the engineering; reframe the packaging. Split the port/net descriptor out
now, re-home it under #341 where the roadmap already pays for it, drop invariant 5
in favor of an ordering edge on #331, extract the paced-participant seam as a
catalogued extension point, and make quiescence first-class instead of documenting
its absence. If the analog program is never funded — which is the roadmap's
recorded position at HEAD and which this issue honestly flags as UNOWNED — the
descriptor and the seam are the two pieces that still deserve to exist, and both
are strictly better off outside this issue.
