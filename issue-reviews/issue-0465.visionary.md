# Issue #465: TASK-0103: five semiconductor device families with stated parameter tiers and stamp goldens — and an inspector that says before simulation whether a downloaded vendor file will load
- Lens: visionary
- Date: 2026-08-08
- Reviewer: Claude Opus 5
- Verdict: rethink

## What this issue is actually for

Strip the apparatus and the claim is: *a student should be able to draw the analog front
end of an instrument, simulate it, and probe it* — CAP-12's photodiode → transimpedance
amp → high-pass → Sallen-Key → comparator → `Adc` → drawn beat counter (#305). That is a
good goal and I want the project to have it. Everything in #465 — five device families,
Gummel-Poon, `Limiting`, `MosCommon`, level dispatch, `ModelInspector`, `NOTICE` — is
*route*, not goal. My objection is entirely to the route.

## 1. The programme reverses a recorded scope refusal, in documents that are not in the tree

The capability roadmap declines continuous-time analog three separate times, under a
stated frame, and names this exact work as the reason:

- `docs/capability-roadmap/sweep-03-elements-and-hdl.md:631-634` — "Continuous-time
  analog. Supporting them means being a SPICE-class solver, which is a different tool
  class, not a bigger version of this one."
- `docs/capability-roadmap/README.md:1037-1042` — same verdict under ground (a),
  re-derived rather than inherited.
- `docs/capability-roadmap/sweep-06-physical-boundary.md:553-556` — "Adding one is
  building SPICE, which is ground (a)."

`ARCHITECTURE.md` §"Recorded decisions" — the normative home for exactly this kind of
call — contains no analog entry at all, and does contain #221: "the `jls.sim.Simulator`
event-queue interpreter remains JLS's **only** simulation execution strategy," with a
revisit trigger (an unusably slow CPU-scale design) that analog does not satisfy and an
equivalence criterion (bit-for-bit agreement with the RV32I golden under two-state+HiZ
semantics) that an MNA/Newton solver cannot meet by construction. An MNA solver is a
second execution strategy under any honest reading. The README's first sentence still
says "an educational digital logic circuit editor and simulator."

The reversal is real and may well be right — but it lives entirely in
`docs/plan/evidence/11-analog-determination.md` and `BRIEF.md`, and **`docs/plan/` does
not exist in this checkout** (`ls docs/plan` → no such file). D-A13, D-A14, D8, D9, D10
are cited as binding throughout #465 and are unreadable by any contributor. So the
governing decision of a multi-month programme is invisible, and the roadmap that *is*
visible says the opposite. Before another analog task is filed, the deliverable should be
an `ARCHITECTURE.md` recorded-decision entry and an amendment to the three roadmap rows
above — one paragraph that says "JLS acquires electrical content, here is why the ground-
(a) refusal is withdrawn, here is what it costs." That is cheaper than `NOTICE` and worth
more than all fourteen predictions.

## 2. The alternative the issue never considers: don't write the models, drive the simulator

JLS's own recorded architectural posture for other-tool-class work is **delegation across
a subprocess boundary**: Yosys, Icarus, GHDL, nextpnr (`ARCHITECTURE.md` plugin-trust
decision; README "Optional development tools"; tests skip cleanly when the tool is
absent). ngspice is BSD-3 — GPL-3.0-compatible in either direction, as #465's own D8
quote establishes. Two tiers, both of which the project already knows how to build:

- **Tier A — batch, for pure-analog work.** `sweep-06-physical-boundary.md:82` already
  scopes a structural SPICE `.subckt` printer over `HdlExporter.buildModel` as "~1 wk."
  Emit the netlist, run `ngspice -b`, read the raw output back as trace samples beside
  the existing VCD path. JLS holds the node↔drawn-element map because JLS wrote the
  netlist, so FEAT-049's invariant 5 ("no student-reachable path emits a matrix-
  singularity message") is a translation table, not a solver property.
- **Tier B — in-process, for CAP-12's mixed-signal loop.** `libngspice` exposes exactly
  the contract this programme needs: `ngSpice_Init` with output/status callbacks,
  `ngSpice_Init_Sync` with `ng_get_vsrc_data`/`ng_get_isrc_data` for an external program
  supplying source values per timestep and reading node voltages back each step. On JDK
  25 that is `java.lang.foreign` — no JNI, no new Maven dependency. The A/D-D/A bridge
  (#368/#434) becomes the callback boundary rather than a thing built on top of a solver
  you wrote.

What this dissolves, not defers: the five device families; `Limiting` and the `icheck`
port; `MosCommon` and the level-3 deferral; card-grammar level dispatch; unknown-parameter
tiering; `ModelInspector`; the absorbed-BSD `NOTICE` obligation; #397 in its entirety (you
do not validate against ngspice nightly — you *are* ngspice); and most of TASK-0104's
convergence hardening, which is where the 200-circuit corpus and the 6–10 mw sit. It also
delivers something #465 explicitly cannot: every vendor `.lib` works, BSIM4 included, so
the "download the model for the part you will solder" promise is kept rather than hedged.

**The one real cost, which the programme should decide before anything else.** FEAT-049
invariant 7 demands cross-platform bit-identical analog results (`StrictMath` everywhere;
zero hits in `src/` today, verified). A subprocess or an FFM call cannot give that; analog
goldens become tolerance-based while the digital half stays bit-exact. That is the whole
trade: **bit-identical results from a device set that refuses most real files, versus
tolerance-based results from every device set that exists.** For a teaching lab I think
the second is obviously right, and #465 never poses the question. The secondary costs —
the self-contained-jar deployment model, and platform coverage down to riscv64 — are real
and are the strongest argument for Tier A as the default with graceful "analog
unavailable" degradation, which is precisely the iverilog/ghdl pattern the repo already
ships.

## 3. `ModelInspector` is a hedge whose value scales with the product's incompleteness

"14 subckts, 31 models, 12 fully supported" is a good report only in a world where 19 are
not supported. It is the artifact that tells a student, politely and early, that the tool
will not do the thing they came to do — and it must be built, goldened and maintained in
proportion to how incomplete the model set is. Under delegation the inspector's job is
done by running the engine that will actually read the file, and the report shrinks to a
diagnostic. Any design where a component's usefulness is proportional to your own gap is
worth one hard look before it gets ~200 lines and a golden.

## 4. If the project owns the solver anyway, this is still the wrong cut

- **The seam is the stamp interface, not the device taxonomy.** A device is
  `(params, v) → (residual, Jacobian, limit request)`. Get that right and five families
  are a data exercise; get it wrong and they are five wrong things. That interface is
  owned by TASK-0097, **which is not filed** (§Status admits it). Building five consumers
  of an unwritten interface, plus a limiter that feeds an escape ladder that does not
  exist, is upside-down regardless of scope philosophy.
- **Cut vertically, not horizontally.** One diode, end to end — palette entry → netlist →
  stamp → Newton → limiter → probe waveform → golden → a student's drawn RC-plus-diode
  clamp — teaches the project more than five families half-attached to nothing, and it is
  the increment that can be killed cheaply if the ngspice route wins.
- **P3 is a test of a tautology.** `gummelPoonWithoutEarlyOrKneeParametersEqualsEbersMoll`
  asserts bit-identity between your code with `q_b == 1.0` and your own hand-computed
  reference. It cannot fail for any reason a student cares about; §10 even pre-writes the
  "it was a last-bit division artifact" escape. The genuinely load-bearing item is P4/
  `icheck`, and that belongs with the Newton loop in TASK-0097, not in a device-model task.

## 5. Two things the issue treats as blockers that are not

- **The flag collision (O2, Open Question 1, marked "Blocks execution") does not exist.**
  `JLSStart` matches the *longest* flag name, not single letters: `src/jls/JLSStart.java:849-858`
  ("longest flag-name match, so -vcd beats -v"), and the table already carries `vcd`,
  `export`, `board`, `pins`, `savetext`. `-inspect` fails today only because there is no
  `inspect` row; add one and it works exactly as `-savetext` does. The recommendation to
  burn a scarce single letter would make the CLI worse for a reason that is measured
  wrong.
- **`NOTICE` is present-tense repo hygiene, not analog scope.** There is no `NOTICE`
  (verified) while the shaded jar already bundles `org.tukaani:xz`, `org.jfree:org.jfree.svg`
  and `com.formdev:flatlaf` (`pom.xml:63-84`) and ships a CycloneDX BOM. That gap is real
  today and should be a 30-minute independent issue, not a checkbox held hostage inside a
  multi-month solver task.

## What I would keep

The evidence discipline is excellent and I verified it: 14 `FlagSpec` rows, `BASELINE =
Set.of()`, no `jls/analog`, no `StrictMath`, no `NOTICE`. The `icheck` insight — a limited
step forces another iteration regardless of residual, and getting it wrong yields
plausible, reproducible, wrong answers — is the single most valuable sentence in the
analog corpus and should survive into whatever route wins (under delegation it becomes an
acceptance test on the co-simulation sync, not a port). Parameter tiering as progressive
disclosure is a genuinely good idea that is orthogonal to who owns the solver: it is a
dialog/descriptor concern and belongs with TASK-0105.

## Verdict: rethink

I am disregarding the acceptance criteria as written. Not because they are unmeetable —
they are unusually well specified — but because meeting all fourteen predictions produces
a teaching-grade SPICE with five device families that will refuse most files a student
downloads, at the cost of a device library, a limiter port, a card reader, an inspector,
a hard corpus, and an external-oracle validation task, in a project whose own roadmap says
three times that this is a different tool class. The goal is right; keep it. Before this
task is executed, the programme owes two decisions in the repository rather than in absent
documents: (1) record the withdrawal of the ground-(a) refusal in `ARCHITECTURE.md` and
amend the three roadmap rows; (2) decide the bit-identity-versus-real-device-library trade
explicitly, with a spike that drives `libngspice` through the Panama FFM sync callbacks
against CAP-12's front end. If that spike works, #465, #397 and most of TASK-0104 close
unwritten. If it fails, #465 comes back — but as one diode end-to-end behind a filed
TASK-0097, not five families ahead of it.
