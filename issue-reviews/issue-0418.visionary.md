# Issue #418: TASK-0046: EDIF, BLIF and a SPICE netlist become documented recipes CI actually runs, and the direct-emitter refusal is written where a reader searching for "EDIF" will find it
- Lens: visionary
- Date: 2026-08-08
- Reviewer: Claude Opus 5
- Verdict: rethink

## What this issue is really for

Stripped of apparatus, the issue makes one good claim: *a person who searches
this tree for "EDIF" should land on a command that has been run, not on three
costed refusals.* That claim is right, it is cheap, and it aligns with how this
project already treats external tooling — `docs/icestick-bitstream-handoff.md`
opens by saying what JLS does not do and then hands you the route.

Everything else in the issue — the new document, the markdown-parsing drift
test, the `blocked_by` on an unfiled two-week writer — is machinery invented to
deliver that one claim, and each piece pulls against the project's own
trajectory. Three days and a two-week critical path to discharge a rejection
whose own verdict priced the fix at "a half-day documenting the recipe"
(`docs/standards-adoption/11-costed-rejections.md` L811) is the tell.

## 1. The blocking dependency is self-inflicted, and removing it is the whole win

`blocked_by` is empty in the YAML but the prose declares TASK-0045 (an unfiled
Yosys-JSON writer, itself behind #336) a genuine prerequisite because "every
recipe's first argument is a netlist only it produces." That is false at HEAD.

- `jls -export design.v` ships today and writes structural Verilog-2005
  (README "Command-line options"; `src/jls/hdl/VerilogEmitter.java`).
- Yosys reads it. `scripts/icestick-handoff.sh:145` runs
  `yosys -p "synth_ice40 -top $TOP -json $JSON" "$VERILOG"` over exactly that
  output, end to end onto real hardware.
- `test/jls/hdl/imp/ImportPipelineTest.java:109-113` already builds a
  `read_verilog … write_json` script and runs it as a subprocess.
- The costed rejection the issue cites **prescribes this exact route**:
  L303, `jls -export design.v` → `yosys -p 'read_verilog design.v; write_edif
  design.edn'`.

O1's "observed failure" is manufactured. It invokes `-export exp.json`, a suffix
JLS never claimed, observes the documented usage error, and converts that into a
dependency on a feature that does not exist. Substitute `.v` and O1 disappears.

The counter-argument for JSON — no re-parse, no name mangling through a text
language — is real for the round-trip criterion (#321 I3) but not for these three
sinks: all of them go through `synth`, which flattens to cells and discards the
distinction the JSON front door preserves. **The Verilog front door is
sufficient for EDIF, BLIF and SPICE, and it is available now.** When TASK-0045
lands, each recipe gains an alternate first line; nothing is rewritten.

Removing this dependency is worth more than the issue's entire stated
deliverable: it takes a capability that is currently gated behind FEAT-004 →
TASK-0045 → this task and makes it a half-day of work against HEAD.

## 2. The drift mechanism should be a script, because that is what this repo does

The issue proposes a fourth, novel mechanism for "documented external-tool
handoff proven by CI": HTML-comment markers around fenced blocks, a bespoke
extractor in test code, and set equality asserted both ways (§7.5, §7.6, H2, P5,
falsification 10.2). The repository already has three instances of this pattern
and none of them works that way:

| Doc | Executable source of truth | Guard |
|---|---|---|
| `docs/icestick-bitstream-handoff.md` | `scripts/icestick-handoff.sh` | `icestick-handoff-selftest.sh` (stub PATH, CI every push) |
| `docs/vcd-interop.md` | `examples/autograde/autograde.py` | `test/jls/AutogradeBridgeExampleTest.java` |
| README "Wayland" | `scripts/wayland-rig.sh` | `wayland-rig-selftest.sh` |

`ExtensionPointCatalogTest` is cited as precedent but is a different animal: it
cross-checks a *table* against *constants*, not prose against a subprocess.

**Concrete alternative.** `scripts/netlist-handoff.sh design.jls --format
edif|blif|spice`, shaped exactly like `icestick-handoff.sh`: all-or-nothing
preflight naming every missing tool, one `yosys -p` per format, version echoed.
`scripts/netlist-handoff-selftest.sh` drives it against a stub `yosys` and
asserts control flow — so the mechanism is proven on **every** CI run, not only
where the toolchain is installed. One JUnit test runs the real script when
`ToolLocator.findOnPath("yosys")` is non-null and asserts P2–P4's content.
The document shows the script invocation and quotes the yosys line it runs.

What this buys, beyond matching the house style:

- **P5 becomes vacuous.** There is exactly one copy of each command and it is the
  executable one. `parse(D) = exec` is not asserted; it is structural.
- **§11's "a skipped test proves nothing" threat is answered** rather than
  deferred to #386. The selftest needs no yosys.
- **The reader gets something to run.** A fenced block in markdown is a thing to
  retype; a script is a thing to invoke. For the instructor audience this issue
  names, that is the difference between a route and a description of a route.
- The bespoke extractor, the marker vocabulary, H2, P5 and falsification 10.2 all
  stop existing.

## 3. P8 does not describe `master`, and the fix is to cite the mechanism, not snapshot it

O5 quotes an `HdlExporter.REJECTED` map with four named types. That map does not
exist on `master` — the evidence-pin comment (#493) flags it, and the shipped
policy is the inverse shape: an **allowlist**, `EXPORTED` at
`src/jls/hdl/HdlExporter.java:422-428` (22 classes), plus `SKIPPED:431-433` and
`TOPOLOGY:436-437`, with everything else aggregated into one
`HdlExportException` naming every offender (`buildModel`, :193-196).

So "name all four `REJECTED` types with the date the list was taken" (P8) is
unimplementable as written, and its generalization — enumerate the complement of
a 22-class allowlist into prose carrying a date — is a rot generator. #291 and
#292 are already queued to move entries, and the issue's own answer is "whichever
lands owns updating it," which is how documentation dies.

Better: the limitations section states the *rule* — "this route carries whatever
`-export` carries; anything it refuses never reaches the netlist, and `-export`
names every offending element in one error" — and shows the error. That sentence
is true at every commit, needs no date, and needs no cross-issue upkeep contract.

## 4. The SPICE route probably does not reach its named consumer

H1 asserts each format is one `yosys -p` away with no lowering pass JLS must
write. For EDIF and BLIF that is plausible. For SPICE it is likely false in the
way that matters: `yosys write_spice` emits `.subckt` instances referencing
*cell names*, and ngspice cannot simulate that without SPICE device models for
the mapped library. P4 ("output contains `.subckt `") passes on an artifact
nobody can run — precisely the "exit 0 with an empty file" failure the issue is
elsewhere careful about.

The missing step is not JLS-side, so falsification 10.1's remedy (remove the
route) fires for the wrong reason. The right disposition: **SPICE is not a
sibling of EDIF and BLIF.** Its value is not interchange — it is a *differential
oracle* for JLS's own simulation semantics, which is the one thing on this arc
with a named consumer (CAP-14). Give that route to CAP-14, where the cell-model
library is part of the experiment and the pass/fail criterion is waveform
agreement, not a substring match.

## 5. Disregarding the acceptance criteria: what I would build instead

I am explicitly setting aside §5 P1/P5/P8/P9, §7.5–7.6, §14's set-equality and
`REJECTED`-snapshot rows, and the TASK-0045 prerequisite. Replacement scope,
against HEAD, no blockers:

1. `scripts/netlist-handoff.sh` + `scripts/netlist-handoff-selftest.sh`, EDIF and
   BLIF only, over `jls -export design.v`. Concrete `synth_xilinx` /
   `synth_intel` targets per consumer — the issue's H3 instinct is right and
   worth keeping verbatim; a generic `synth` gives Vivado something it reads but
   cannot place.
2. **No new document.** A new §7.6 in `docs/hdl-support-research.md`, beside the
   §7.5 "Shipped: board-aware export" recipe that already lives there, titled so
   an "EDIF" search lands on it. Cite `11-costed-rejections.md`; do not re-argue.
   README's format list gets one pointer. `docs/` is 25 files deep already; a
   26th for three commands is the drag this project can least afford.
3. One skip-when-absent JUnit test running the script; the selftest carries the
   proof on every other run.
4. SPICE re-homed to CAP-14 as an oracle, with the cell-model gap recorded.
5. The npm routes (`netlistsvg`, `yosys2digitaljs`): keep as *described*, one
   sentence each, marked. The proven/described distinction (H4/P9) survives
   intact and costs nothing once the proven set is a script.

Estimate: the half-day the rejection priced, plus a day for the selftest. The
issue's own three days were mostly the drift test it no longer needs.

## 6. The arc question

JLS's exit doors, in order of evidenced demand: simulate → grade (`-t`, VCD,
`examples/autograde`) → put it on a board (`-board icestick`, the handoff
script). EDIF-to-Vivado and BLIF-to-VPR have no user anywhere in this
repository; #74 was rejected on exactly that ground, and the issue is candid
that the routes are weakest precisely where JLS is strongest — `Memory`,
`RegisterFile` and `SubCircuit` are outside `EXPORTED`, so the CPU designs on
the `riscv/` trajectory cannot use any of these routes at all.

That is not an argument for doing nothing; discoverability is worth a half-day.
It is an argument against letting this become a document with a maintenance
contract, a bespoke test harness, and a place in a feature's critical path. If
there is a maintainer-week to spend on this arc, FEAT-019's I3 — write, re-import
through the shipped `NetlistImporter`, assert partition isomorphism — is worth
more than all three of these formats combined, because it turns HDL export from
a one-way bridge into a checkable transformation. These recipes are downstream
of that and cost the project nothing to not have.

## What to keep, unchanged

- The discoverability framing. It is the best sentence in the issue.
- H3: name a concrete `synth_` target; no placeholder ships.
- P7: record the verified version per recipe and print the observed one on
  failure, so version drift reads as version drift.
- "Cite the rejection, do not re-argue it," and write the refusal as a cost
  judgment naming its alternative.

## Note on citations

`docs/plan/` does not exist on `master` — BRIEF.md/D8, the CAP-14 capstone and
this task's own plan document are all off-tree for anyone reading the default
branch. Anchor the replacement work on things that survive: the costed
rejections doc, `docs/hdl-support-research.md`, `docs/icestick-bitstream-handoff.md`,
and the three scripts above.
