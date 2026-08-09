# Issue #613: TASK-C487-3: a tool JLS does not control renders the verdict — a board routed 25% over its declared maximum FAILS the external DRC naming the net, and the shortened one passes
- Lens: visionary
- Date: 2026-08-08
- Reviewer: Claude Opus 5
- Verdict: endorse-with-reframing

## What this issue is really for

Strip away the PCB vocabulary and #613 is not about signal integrity at all. It is the
one place in the entire CAP-18 stack (#313) where **JLS surrenders the verdict to an
authority it does not control, in both directions**. #487 says so itself: "acceptance is
not 'JLS emits a file' but 'a tool JLS does not control fails a board it should fail and
passes one it should pass'." The maximum-length constraint is the carrier; the cargo is
an epistemic method — *an oracle that only ever says yes is indistinguishable from an
oracle that is not running.*

That method is more valuable to JLS than the constraint is, and the project needs it
today, in shipped code, independently of anything in the PCB roadmap.

## The finding that reframes the task

JLS already shells out to three external adjudicators — `test/jls/hdl/IverilogCompileTest.java`,
`test/jls/hdl/GhdlCompileTest.java`, `test/jls/hdl/scan/YosysGroundTruthTest.java`, plus
`test/jls/hdl/imp/ImportPipelineTest.java` and `test/jls/AutogradeBridgeExampleTest.java`.
**Every one of them asserts only the passing direction.** `IverilogCompileTest` asserts
that every committed golden compiles; nothing anywhere asserts that a deliberately broken
export *fails* to compile, or that Yosys's `write_json` ground truth *disagrees* with a
mutated scan. By #613's own argument those five tests are today indistinguishable from
tests that are not running — a stubbed `iverilog` that always exits 0 would keep the suite
green forever.

So the project does not lack an external adjudicator. It lacks the **negative direction**
at the external boundary, and #613 proposes to invent it once, privately, inside a KiCad
test that sits behind five feature-sized prerequisites (#486, #336, #366, #318, #319) and
a 5.5–9.5 mw feature. The value is real; the delivery vehicle is the slowest and narrowest
one available.

The project also already has the doctrine in-house under a different name. `test/jls/ui/package-info.java`
line 41-43 records the "meaningfulness discipline": every assertion helper is itself pinned
by an assert-the-assertion test via `assertThrows(AssertionError.class, ...)`. And `pom.xml`
runs PIT with `<mutationThreshold>80</mutationThreshold>` — mutation testing is *precisely*
the thesis "a check that passes on a program that should fail is not a check." #613 is
asking for mutation testing of an external oracle, and neither the task nor #487 says so.
Naming it that way is not cosmetic: it makes the requirement general, it inherits an
existing vocabulary the maintainer already accepts, and it stops the pattern being
re-argued from scratch at the next external tool.

## Alternative framing 1 (primary): lift the method out of the domain

Split #613 into the reusable half and the KiCad half, and land the reusable half now.

- **`ExternalOracle` (test infrastructure, ~0.5 mw, no prerequisites).** A shared harness
  beside `ToolLocator` that runs an external tool against a *pair* of artifacts — one that
  must be accepted, one deliberately mutated that must be rejected — and fails the test if
  either direction is wrong, including the case where the tool accepts both. Retrofit it to
  the three armed tools immediately: a Verilog golden with one port width corrupted must
  fail `iverilog -g2005`; a VHDL entity with a deleted `end` must fail `ghdl -a`; a scan
  expectation with one direction flipped must disagree with `yosys write_json`. That closes
  a live gap in shipped tests and costs nothing that #613 was not going to spend anyway.
- **#613 then becomes an instantiation, not an invention.** `KicadSiDrcTest` supplies a
  fixture pair and a tool identity to a harness whose two-directional discipline is already
  proven and already regression-guarded. The task's criterion 1 stops being a hand-written
  ordering rule ("recorded first") and becomes a property the harness enforces mechanically.

This is a strictly better cut than the one #487 §2 chose. #487 cut *vocabulary | emitter |
round trip | back-annotation* — a cut along the data pipeline. The seam that actually matters
here is *method | domain*: the round-trip scope's risk is not KiCad-specific, and pricing it
as if it were means paying for it once per external tool forever.

## Alternative framing 2: three cells, not two — and the control cell is the important one

The issue's fixture design is 2×1: over-length board fails, shortened board passes. That
pair does not establish what the acceptance claims. A `.kicad_pcb` routed 25% over could
exit nonzero for clearance, courtyard overlap, an unrouted net, or a stale netlist — and
the test goes green while JLS's rule file does nothing. "Naming the net" narrows it but does
not close it, because the net is named in every violation touching that net.

Make it 2×2 and drop one cell:

| | rule file applied | rule file absent |
|---|---|---|
| **over-length board** | FAIL, `length` constraint, net CLK | **PASS** ← the control |
| **shortened board** | PASS | PASS |

The control cell is what proves the emitted file is the causal agent. Without it, criterion
1 is satisfiable by a board that was broken for unrelated reasons. Additionally assert the
violation *kind* (`length` / `LENGTH_CONSTRAINT`) and not merely a nonzero exit — #487's own
Evidence 5 already enumerates KiCad's constraint kinds first-hand, so the stronger assertion
costs one JSON field in `kicad-cli pcb drc --format json`.

Second fixture-design point: do not commit two hand-made boards. A pair of binaries drifts —
someone regenerates one and the failing direction quietly becomes a second passing direction.
Commit **one** board plus a deterministic, documented route-lengthening step that produces the
over-length variant, so `1.25` is a parameter rather than a magic number frozen into two
opaque files. Criterion 4's "re-runnable by someone who was not there" is better served by a
generator than by a second artifact.

## Alternative framing 3: the tool-identity seam is being cut in the wrong place

Criterion 3 says "opt-in through the shipped tool-locator plus assumption idiom, with the
checker's container pinned by digest." Those are two mechanisms answering two different
questions, and the shipped one cannot satisfy the recorded one. `ToolLocator.findOnPath`
(`test/jls/hdl/ToolLocator.java`) returns whatever `kicad-cli` is on `PATH`, of entirely
unknown version; a digest belongs to `docker run image@sha256:…`. As written, criterion 1's
"container digest … recorded" is unreachable through the idiom criterion 3 mandates.

The right seam is one level up and benefits everything: an `ExternalTool` resolver that
returns a **`ToolIdentity`** — either (PATH binary, captured `--version` string) or (OCI image,
digest) — and that every external test prints into its failure message and its recorded
evidence. Today `iverilog`, `ghdl` and `yosys` results are silently version-dependent on
whatever CI installed; the repo worries about exactly this class of drift for the JDK (README's
advisory newest-GA lane) but not for its external oracles. Solving it once inside `KicadSiDrcTest`
is the worst of the three available options.

## Alternative framing 4: the required-lane pattern already exists in this repo

Criterion 3 wants the leg opt-in and hermetic-by-absence, and never "a required gate that a
missing container turns green." That is a real tension: a leg that skips by default leaves the
capstone's only external claim unverified in ordinary CI, which is how a claim rots.

JLS has already solved this shape, twice, for the Wayland GUI: a required `gui-wayland` lane
that **provisions its own environment** (`scripts/wayland-rig.sh`) so absence is a lane failure
rather than a silent skip, *plus* `scripts/wayland-rig-selftest.sh`, which drives the unmodified
rig against a **stub toolchain — no JBR, no compositor, no network** — and asserts each scenario
is classified with the documented exit code, on every event. That is exactly the guard #613 needs
and does not ask for: a stub `kicad-cli` that always exits 0 must make `KicadSiDrcTest` **fail**,
everywhere, with no container present. Then the heavyweight lane can skip honestly, because the
harness's own verdict logic is guarded independently of the tool.

## Alignment with the project's arc

The work pulls with the arc, not against it. ARCHITECTURE.md's recorded plugin-trust decision
puts external tool integrations on the subprocess boundary and keeps them there; `kicad-cli`
belongs there alongside Yosys/GHDL/Icarus. "JLS does not route" is the right boundary and #487
holds it. `src/jls/hdl/board/PcfEmitter.java` with a committed golden is a genuine
data-not-code precedent.

The one arc-level caution: the acceptance recorded here binds a *pedagogy tool* to the
continued behaviour of a large external ecosystem for a claim no default user exercises (K9
guarantees the surface is invisible to first-years). That is defensible only if the external
adjudication method is generalized — if it is a one-off, JLS acquires a KiCad dependency and
gains a method it cannot spend anywhere else. Framing 1 is what converts the cost into an asset.

## What I would keep verbatim

Criterion 5 (evaluate K18-2 explicitly; a recorded narrowing is a success, an unrecorded
silence is not) is the best-designed clause in the task and should be copied into other
externally-adjudicated work. Criterion 1's ordering discipline is right in substance; framing 1
just makes it mechanical instead of procedural.

## Concrete recommendation

Endorse the outcome. Reframe the delivery: file the `ExternalOracle` two-directional harness
and its stub-driven selftest as a standalone task with no prerequisites, retrofit it to
iverilog/ghdl/yosys now, extend `ToolLocator` to a `ToolIdentity`-returning `ExternalTool`, and
re-scope #613 to (a) the fixture pair generated from one board, (b) the 2×2 control cell, and
(c) the violation-kind assertion. #613's stated acceptance criteria all survive that reframing
unchanged — they simply stop being the first and only place the project knows how to do this.
