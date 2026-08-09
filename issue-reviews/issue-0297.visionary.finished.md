# Issue #297: CAP-04: a drawn CPU becomes a buildable 74-series breadboard whose real electrical behavior — floating, contention, fan-out — simulates as wired
- Lens: visionary
- Date: 2026-08-08
- Reviewer: Claude Opus 5
- Verdict: endorse-with-reframing

## What this issue is actually for

Strip the eleven feature numbers, the DAG walks and the 86-132 mw sum, and one sentence
survives: *a JLS simulation cannot currently be wrong in the ways a bench is wrong, and it
should be able to be.* That is a real and valuable claim about what JLS should become, and it
is the best-argued audience claim in the capstone set — the failure it names (undriven input
silently reads `0`) is genuine and is anchored precisely: `src/jls/elem/LogicElement.java:480`
sets every input to `0` at `initSim`, and `docs/simulation-semantics.md` §2 records that
"nearly every element's `react` treats a null (HiZ) input as zero before computing". JLS today
teaches a lie about undriven logic. Fixing that is worth doing.

Everything else in this issue is *mechanism* — a package library, a packing pass, a second
canvas, per-view geometry, per-section versioning, a four-state value core. My argument is
that the mechanism is roughly three times larger than the goal requires, that two of its
central design choices work against the project's own recorded architecture, and that one of
them makes the simulator confidently wrong in a *new* way. I am explicitly disregarding
AC-2, AC-3 as written, and AC-4 below, and giving reasons.

## Reframe 1 — floating is nondeterminism, not a constant, and the issue's own §1 contradicts itself

AC-3 pins "an undriven 74LS173 CLK resolves to `1` at `pull` strength" and §1 step 6 says the
student should "observe the input go **HIGH**, not LOW, and the register free-run exactly as
the real board would." Those two sentences cannot both be true. A stable HIGH on a clock input
produces no edges; a register whose CLK is stably HIGH does not free-run, it holds. The
free-running behaviour on a real board comes from the fact that a floating TTL input sits near
1.4 V — right at the switching threshold — and is therefore an antenna: it picks up mains hum
and adjacent switching and clocks erratically. "Floating TTL reads HIGH" is a rule of thumb
about *DC bias*, not a model of behaviour, and a simulator that renders it as a deterministic
`1` at `pull` strength has replaced one confident falsehood (`0`) with another.

The honest model, and the one that actually teaches, is that a floating node is
**undetermined**: its value is not a function of the circuit. The concrete alternative:

- Represent undriven at the net, where JLS already represents it — `WireNet.propagate` already
  carries `null` for HiZ (`src/jls/elem/WireNet.java:404-445`) and already detects multi-driver
  conflict there, in about fifty lines. The strength lattice's real home is that one method,
  not a new value domain.
- Instead of resolving a floating input to a constant, **run the design twice and diff**: once
  with every floating node pulled 0, once pulled 1, and mark X exactly where the two runs
  disagree. X propagates for free, through unmodified element `react` methods, because there is
  no X in either run — only a disagreement between two ordinary runs. The 74LS/74HC distinction
  becomes a *report* ("this node is biased HIGH on TTL and will read as 1 more often than not,
  but it is not determined") rather than a simulated constant.
- This is already an idiom in this tree: `docs/simulation-semantics.md` and #221's recorded
  decision use a differential golden run as the binding oracle for any second execution
  strategy. A dual-run X oracle is the same move, applied to a different axis.

What that buys: **FEAT-026 (#322), the 28-36 mw four-state value core, leaves this capstone's
critical path entirely.** It is the single largest line in the cost table, it requires touching
the `react` of ~35 element types, and it is a normative change to a document `docs/simulation-
semantics.md` declares normative. The dual-run approach is an under-approximation for k>1
floating nodes (all-0/all-1 misses interactions; k+1 runs or per-node runs recover most of it)
and diverges hard on sequential feedback — but "everything downstream of your floating clock is
undetermined" is the pedagogically *correct* answer, not a limitation. If the maintainer wants
per-bit X in the value domain eventually, fine — but it should be funded by a capstone whose
outcome needs per-bit X, not smuggled in as the cheapest way to draw an `X` on one pin.

## Reframe 2 — the board is a separate file, not a second view of one artifact

Open Question 2 already lands on the right instinct: store no connectivity, derive it by
union-find over hole occupancy. Take that one step further and the board stops being a *section
of the `.jls` file* at all. It becomes what tier A already emits — `placement.brd` plus
`wiring.net` in a `plan/` directory — read back and simulated. Schematic and board are separate
files, exactly as KiCad separates them, and exactly as this project already binds designs to
physical targets: `src/jls/hdl/board/PinBindings.java` parses a user-written `-pins` text file,
described in its own javadoc as "binding UX option (a): headless and autograder-friendly."

Consequences, all of them subtractive:

- **AC-4 evaporates.** There is no breadboard section for an older reader to skip, so the "one
  artifact, two views, one older reader" criterion has nothing to assert.
- **FEAT-014 (#318, 11-17 mw) and FEAT-013 (#319, 4-7 mw) leave this capstone's required set.**
  Both are worth doing for other reasons and serve six-plus other capstones each; neither is
  needed *here* once board geometry lives outside the `.jls`.
- The identity binding refdes needs already ships: `Circuit.getElementsInStableOrder`
  (`src/jls/Circuit.java:479`, issue #181) and the `ElementId`-addressed op vocabulary in
  `src/jls/collab/op/`. Refdes assignment keys on the existing stable id; §2's minimality
  argument for #318 ("the breadboard's geometry has no stable instance to hang on") is answered
  by code already in the tree once the geometry is not required to round-trip inside the save.

## Reframe 3 — cut the canvas, keep the plan and the truth

Tier B is the only genuinely private cost in the table — #329 at 9-15 mw with
`serves_capstones: [297]` — and it drags #316 (SimpleEditor decomposition, 12-20 mw) behind it.
For that ~21-35 mw the student gets to drag 35 DIP packages onto a rendered breadboard and
jumper hole to hole. That is data entry. It reproduces Fritzing, whose longest-standing
complaint this issue itself cites, and §3 concedes that the discrepancy overlay has a
"*messaging* problem that no feature's completion criteria will surface" — the issue is
budgeting a canvas whose central UX risk it cannot test.

Ask what tier B is *for*. Two things: (a) demonstrating a deviation between intent and build,
and (b) tier C's payload. Both are reachable without a canvas:

- A **deviation file** — three lines of text saying "cut the jumper between U3.11 and U7.1",
  or an inline "open this net" toggle on the schematic — reproduces §1 step 6 exactly, at
  1-2 mw, and is gradeable and diffable in a way a canvas gesture never is.
- The discrepancy check becomes `jls -breadboard --check mywiring.net`: the student wires the
  *physical* board, transcribes what they actually wired (or exports it), and the tool tells
  them where they diverged. That is the workflow of every real EDA back-annotation flow, it is
  headless, and it is assignable.

The issue already contemplates this outcome — AC-5/KC-04-3 defer tier B if the coverage commons
cannot absorb a second canvas. I would not make that a fallback. I would make it the plan.

## Reframe 4 — one physical-target layer, three emitters, not a breadboard-specific asset

`src/jls/hdl/board/` is JLS's existing answer to "get this design onto real hardware," and its
`Boards.java` javadoc records the project's stance on physical-target data explicitly: "Kept
deliberately tiny (hypothesis H2 of #213): a board is one `Board` value here, with its pin map
transcribed from the vendor documentation, and the table grows on demand rather than through a
general board-description format."

FEAT-040 (#349) proposes the opposite — a general, extensible-by-text-file package and pinout
library — and three capstones (#297, #298, #307) consume it. The visionary shape is not "a
breadboard package library" but **one target-and-binding layer with several emitters**: a target
is parts, pins and constraints, whether it is an iCE40 dev board, a bag of 74LS DIPs, or a PCB
footprint set; the emitters (PCF today; BOM, wiring list, KiCad netlist tomorrow) are views of
one binding. That is the seam that makes #297, #298 and #307 stop being three capstones that
"share a package layer by convention" and start being one capability with three outputs — and
it retires §3's entire ownership problem, which this issue currently resolves by deferring to
#298. Deference is a governance answer to an architecture question.

## Against the project's arc

ARCHITECTURE.md's "Recorded decisions" section is the clearest statement of this project's
temperament, and it is uniformly one of *declining speculative work with a named revisit
trigger*: i18n is a non-goal until an instructor asks; out-of-process plugin isolation is
"reserved for a future untrusted-provider case… not built speculatively"; a second simulation
strategy is refused because "classroom-scale gate circuits are the present workload" and will be
reconsidered only when a concrete `riscv/` design is "unusably slow interactively." Every one of
those declines a smaller, better-motivated body of work than this issue proposes.

86-132 mw standalone, 31-46 mw marginal, is one to two and a half years of a single
maintainer's capacity for a capability with no requesting instructor named anywhere in the
issue. The **7-14 mw demo slice — headless build plan, BOM, wiring list, static DRC — is the
part that fits this project's temperament**, and it is the part that carries the whole audience
argument: "here is your drawn CPU as 7x 74LS00, 3x 74LS74, and here are the three inputs you
left floating" needs no canvas, no per-view geometry and no four-state core. Ship that, plus
Reframe 1's dual-run undetermined-node report (a few mw at the `WireNet` seam), and the issue's
own headline claim — that JLS can now be wrong the way a bench is wrong — is true at perhaps
a sixth of the cost. Then let a real lab pull the rest, in this project's own recorded-decision
idiom: record CAP-04's tiers B and C as deferred with the revisit trigger "an instructor runs a
breadboard lab against the tier-A plan and asks for the board view."

## What I would keep verbatim

AC-1 checks 1, 3, 4, 5, 6 and 7 (packing totality, power completeness, no floating input, no
contention, fan-out with `not DC-limited` rather than `PASS`, determinism and additive-only
diff) are excellent and I would not change a word. AC-6 (a new element type must fail loudly if
it has no packing disposition) is the kind of standing invariant this codebase does well and
already has precedent for. AC-7 — a stranger builds the board from the plan and photographs it
— is the single most valuable criterion in the issue and the one no amount of feature work
substitutes for. AC-1 check 2 becomes trivial under Reframe 2: with the board a separate file
there is one partition pass over one artifact, and KC-04-2's 60 s budget stops being a risk.

## Verdict

**endorse-with-reframing.** The destination is right and under-served: JLS should be able to be
wrong the way a bench is wrong, and a drawn design should become an orderable, buildable board.
The route is roughly three times larger than the destination requires, rests on a floating-input
model that contradicts its own walk-through, and buys a second canvas the project cannot yet
test or afford. Ship the tier-A plan and the undetermined-node report; put the board in its own
file; defer the canvas behind a demand trigger; and unify the package layer with the
`jls.hdl.board` target layer that already exists rather than beside it.
