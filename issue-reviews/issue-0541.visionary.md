# Issue #541: FEAT-C24-6: one command exports the whole handout figure set — schematic, TikZ, timing figure, animation — from one circuit and one recorded run
- Lens: visionary
- Date: 2026-08-08
- Reviewer: Claude Opus 5
- Verdict: rethink

## What this issue is really for

Strip the tier vocabulary away and #541 is one claim: *a figure set that cannot lie about
the run it came from*. CAP-24 (#505) risk 4 names the defect — five figures composed from
possibly-different runs — and #541 is the only place in the cluster that forbids it. That
goal is correct, it is the most defensible thing in CAP-24, and nothing below argues
against it. What I am rejecting is the mechanism: a new command whose input is an artifact
that does not exist, guarded by an arity contract and a two-task roster policing
self-consistency after the fact.

## The load-bearing input does not exist, and its lineage forbids citing it

AC-2 makes everything turn on "one recorded-run artifact". There is no such artifact in
JLS. `grep` over `src/` and `test/` for recording/replay finds only `jls.collab` — the
collaboration transport, unrelated. The phrase's entire provenance is #498 §7.2, which is
a *rescue* of a branch-only design study that says of itself, in the issue body: "**It is
explicitly non-normative.** Its own status line says so ... Nothing in it may be cited as
settled policy." §7.2's proposed sentence ("an interactive session is a recording device;
the recording, not the session, is the contract") is a *proposed replacement text* for
`docs/vcd-interop.md`, gated behind a decision issue, a CHANGELOG entry, an
`ARCHITECTURE.md` decision block and a five-site consistency pass — scheduled as M2 of a
6–9 month programme that has not started.

So #541's central criterion either (a) creates an unowned dependency on session recording
and a new sidecar format, which is a subsystem larger than all of CAP-24, or (b) quietly
means "a VCD file", in which case say VCD and the recording-is-the-contract citation is
decoration. #539 AC-4 has the same defect, and #538 already resolves it honestly for its
own artifact: its headless leg reads a VCD. The dedup and disposition comments on this
issue have now argued arity, refusal wording, PDF-in-the-determinism-set and roster shape
across four comments and ~1,400 words without anyone naming the file type of the input.

## Where the recording is thinnest is exactly where the bundle needs it richest

`docs/batch-interface.md` §4.1: the VCD carries "one signal per **watched element** ...
plus one signal per **probed wire net**". `BatchSimulator` accumulates `TraceSample`s only
for watched elements (`src/jls/sim/BatchSimulator.java:117-128, 215`) and probes
(`probeSample`, `:295`). That is a deliberately *lossy* recording. #539 wants N cycles of
a canvas region with signal values overlaid — full net state at every timestep over an
arbitrary region, which no recording in this project carries. Replay-from-artifact
therefore forces either a new whole-state dump format or a silent re-run, and a silent
re-run is precisely the defect #541 exists to prevent. The record-then-replay seam is cut
in the wrong place.

## Reframing 1 — one process, one simulation, many sinks

Make the defect structurally impossible rather than contractually forbidden.

```
jls -b -t vectors.txt -d 200 -figures handout.spec -o figs/ circuit.jls
```

One `Simulator` instance in one JVM. The print-SVG/PDF renderer (#536), the TikZ emitter
(#537), the WaveJSON writer (#538) and the frame encoder (#539) are *sinks* attached to
that one run, exactly as `-vcd` and `-r` already share one trace ("the consumers share one
trace and neither requires the other's flag", §4.1). Two figures from two runs is then not
a rejected input — it is an unreachable state, because there is one run object and no API
for a second. AC-2's refusal clause, #874's "each artifact records its run identity" and
#875's "assert they agree, with a planted mismatch" all become machinery guarding a defect
the design cannot exhibit. That is the trade this project makes everywhere else: `jls.sim`
is headless *by construction* with a ratchet, not by review; the loader has no per-element
switch because `SaveTags` makes drift unrepresentable.

## Reframing 2 — stamp provenance, and the bundle stops being the guarantee

The bundle only protects figures the bundle made. Instructors will make figures other ways
— one exporter at a time, on a laptop, six weeks apart, from an edited circuit. Give every
figure emitter a provenance stamp instead: a deterministic triple of (circuit content
hash, run-parameter hash, JLS version) written into each artifact's metadata channel — SVG
`<metadata>`, PDF `/Info` (fixed, no timestamps), a TikZ `%` comment, a WaveJSON member, an
APNG text chunk. Add one ten-line checker, `jls -figcheck figs/`, that fails when two
artifacts in a directory disagree. Cost is a fraction of the bundle; coverage is strictly
larger — it catches the stale figure regenerated by hand three commits later, which the
bundle command cannot see. Content hashes keep AC-3's byte-identical re-run intact.

## Reframing 3 — the input is a manifest, and `make` is the composer

The bundle's real input is not a run; it is an *editorial selection*: which window, which
signals, which canvas region, which subcircuit. #538 already owns a GUI selection path.
Put that selection in a small text file (`handout.spec`) checked into the course repo next
to the circuit. Then the whole feature is a pure function of `(circuit bytes, spec bytes,
JLS version)`, one hash over the output tree proves AC-3, and the figures regenerate in CI
on a circuit change with a three-line rule. This is the shape the repository already
believes in: plain-text saves that "diff cleanly in version control" (README), byte-
reproducible artifacts, `.buildinfo`, `docs/reproducibility.md`.

And note the precedent for composition: JLS does not ship an autograde command; it ships
`examples/autograde/autograde.py` plus `docs/vcd-interop.md`'s bridge pattern, pinned by
`test/jls/AutogradeBridgeExampleTest.java`. The idiomatic form of #541 in this tree is
`examples/handout/` — a Makefile, a `handout.spec`, a `handout.tex` — built in CI. It
delivers AC-1 verbatim (the in-tree LaTeX document builds clean), gives instructors
incremental rebuild the bundle command does not, and adds zero CLI surface.

## "One command" collides with a recorded CLI decision

`docs/picocli-evaluation-2026-07.md` §Verdict rejects picocli "for the current CLI shape;
re-evaluate if and when the CLI grows subcommands", and names `jls export` / `jls sim` /
`jls grade` as the trigger that would flip it. An "export-handout-bundle command" is
either JLS's first subcommand — a CLI-shape decision that belongs in one deliberate issue,
not smuggled in as PF-6 — or it is a flag, in which case `docs/batch-interface.md` §6
blesses it outright ("a new flag, or a new optional output gated behind a new flag").
Reframings 1 and 3 take the flag path. The existing `-export` + `-board` + `-pins` trio
already emits multiple correlated artifacts from one invocation without a subcommand; that
is the shipped precedent for what #541 wants.

## Trajectory: this issue is being elaborated out of order

- CAP-24's own gate KC-24-1 says: if the demo slice cannot get byte-identical SVG across
  two platforms, "stop and re-plan the determinism claim **before funding PF-2..PF-6**".
  #541 is PF-6. No such measurement is recorded on #505.
- #508 puts CAP-24 in "keep-strategic (cheap slice now, rest gated)", recommends cutting
  PF-4, and states a planning ratchet: "no new tier:feature/tier:task until two capstones
  close." On 2026-08-08 this issue absorbed one task and then filed two new ones.
- The result is that the *glue* issue now carries four comments, a resolved
  `ordering_after` table and a two-task roster, while #536 — which owns the only genuinely
  hard thing in CAP-24, deterministic text metrics with no OS font fallback — has one
  comment and no task. Planning effort has flowed to the cheapest, most reversible node.

Under this lens #541 should be dormant, not decomposed. It is the last thing CAP-24 needs
and the first thing that becomes trivial once #536–#538 exist as pure functions.

## What I am disregarding, and what survives

I am disregarding AC-2's arity/refusal contract, the recorded-run input in AC-2 and #539
AC-4, and the #874/#875 roster with its run-identity assertions and planted-mismatch
negative check. They are all correct answers to a question that the one-process design
deletes. AC-1 survives unchanged and is the best criterion here (the in-tree LaTeX
document building in CI is a real, external, falsifiable oracle). AC-3 survives and gets
easier (hash the output tree). AC-4 survives — headless is where handouts get regenerated.
The arity-neutral "all artifact kinds" wording from the third comment is right and should
be kept whichever framing wins.

## Concrete restatement

> The figure emitters are pure functions of `(circuit, run parameters, selection)`, each
> stamping that triple into its own output. `jls -b -figures spec -o dir circuit.jls`
> attaches every emitter as a sink on one in-process simulation, so no two figures can
> come from two runs. `jls -figcheck dir` verifies the stamps agree for figures produced
> any other way. `examples/handout/` ships the Makefile and the LaTeX document CI builds.

Band: this is smaller than 1–2 mw once the emitters exist, because the composition is a
loop and a spec parser. Re-file it as one task after #536's demo slice retires KC-24-1 —
not before.
