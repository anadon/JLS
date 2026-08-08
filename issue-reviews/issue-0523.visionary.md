# Issue #523: FEAT-C05-1: the netlist KiCad receives is provably the circuit JLS simulated — a stable-id net-partition isomorphism gates the export, and parity is narrowed in writing where it cannot be proven
- Lens: visionary
- Date: 2026-08-08
- Reviewer: Claude Opus 5
- Verdict: endorse-with-reframing

## What this issue is actually for

Stripped of its provenance (the #307 → #298 merge, the "no filed row owns it" argument),
this is a claim about JLS's epistemics, not about KiCad: **an artifact JLS emits must have
an oracle proving it is the design, and a claim wider than its oracle must be narrowed in
writing.** That is the deepest recurring commitment in this repository — the byte-reproducible
jar plus `.buildinfo`, the `SHA256SUMS`/attestation split with its explicit "note the scope of
each guarantee" paragraph in README, `ARCHITECTURE.md`'s rule that any second simulation
strategy must agree bit-for-bit with the #202 RV32I golden as a *differential oracle*,
`ArchitectureRulesTest`'s bytecode ratchets, `HelpTopicsTest`'s link checker. #523 is that
culture applied to the PCB boundary. The direction is right and I would not want it dropped.

Three things pull against the issue as written: it under-claims its own generality (the check
is homed at one capstone when six emitters need it), it over-claims its own strength (the
title says "provably the circuit JLS simulated"; the construction cannot support that), and it
picks a JLS-authored parser as the oracle when the tool named in the title is the better one.

## The cancellation hole — why the construction proves less than the title

AC-2 fixes the source side as "the one shared partition pushed through the packing binding"
and the emitted side as a re-parse of the same file the emitter wrote from that same binding.
Both sides therefore pass through `pin(L, π(e), t)` — the function #366 §3 itself flags as
"the silent-when-wrong one: a permutation of it produces a file that parses, imports, and
yields a wrong board." **Any error in the packing binding or the library pinout is applied to
both sides of the comparison and cancels.** The isomorphism is invariant under exactly the
failure class that ruins a board. The same is true one level up: if `jls.netlist`'s jump-alias
fold is wrong, simulation and export are wrong together and the check stays green.

So what the test really proves is narrower and still worth having: *the emitter did not lose or
scramble connectivity relative to the IR it was handed, and the file it wrote parses back to
what it meant.* That is emitter fidelity, not design parity. Note the irony — TASK-C523-2
(#630) exists to narrow the parity claim in writing, while the feature's own title states the
widest version of the claim the evidence cannot support. Fix the title first; it is the cheapest
correction in the issue and the one most likely to be inherited by downstream prose.

## Reframing 1 — home the parity harness at the pivot (#336), not at the capstone

#307 §4 argued no feature can own the check because it compares one feature's output to
another's, and the answer taken was to mint a third feature row. There is a better seam. Both
sides of every such comparison meet at **one** place: the shared partition. #336 already
carries IC-1 ("exactly one implementation, at least two consumers") and IC-6 ("delete
`HdlExporter` and the package still compiles") — invariants about the partition's status as
the single source of connectivity truth. "Every emitted artifact re-parses to this partition"
is the natural third member of that family, and #336 is upstream of *all* the emitters, not
just KiCad's.

Concretely: one seam `ArtifactPartitionView` — parse artifact bytes → partition keyed by
terminal identity — with one implementation per artifact kind, and one parameterized test over
{emitter × fixture}. That single harness discharges #523's AC-1, #336 IC-1, #366 IC-3, #321's
Yosys-JSON parity, TASK-0090's gEDA `.sch`, `PcfEmitter`'s pin bindings, the #297 breadboard
wiring list, and the VCD variable declarations — and it makes the seventh emitter's parity a
20-line class instead of a new feature row. As filed, emitters two through six either duplicate
this work or ship without it, which is the exact fate #336 exists to prevent for the partition
itself. The generalization costs materially less than the difference between the 2–3 mw band
here and re-litigating the same check five more times.

## Reframing 2 — arm KiCad as the oracle instead of writing a second parser

AC-1 has JLS parse the `.net` JLS wrote. A grammar JLS's writer and JLS's reader agree on but
KiCad rejects passes this check silently; that is the same cancellation, one layer out. Yet
#366's two weakest criteria (IC-1 "verified by opening it, not by parsing it", IC-2 "imports
with zero hand editing") are recorded as *manual procedures* precisely because nobody armed
the tool — and #366 OQ-4 leaves "(b) the tool armed in a CI lane" as the named successor with
no owner. #298's own walk-through already shells out to `kicad-cli`.

This project arms external toolchains as a matter of course: `iverilog` and `ghdl` gate the HDL
goldens and skip cleanly when absent (`test/jls/hdl/IverilogCompileTest.java`,
`GhdlCompileTest.java`, `ToolLocator.java`), Yosys is a documented dependency, and the
`gui-wayland` lane boots a whole compositor headlessly. A `kicad-cli` lane on the same
`ToolLocator` pattern turns the comparison into *JLS's partition versus KiCad's own reading of
the file*, which is literally the sentence in this issue's title, and simultaneously converts
#366 IC-1/IC-2 from perishable manual rituals into gates. **If I had one hour of this feature's
budget I would spend it there, not on a hand-written `.net` parser.** This is the same
"falsify the premise in an afternoon" instinct #366 §6 already applies to embedded symbols;
it just has not been pointed at the acceptance path.

## Reframing 3 — AC-3 is an architecture rule, not a test outcome

AC-3 says that if the cascade rule migrates into the emitter, the isomorphism test fails and
KC-05-2 fires. Using a fixture-level partition mismatch to detect a *code-location* fact gives
the worst possible diagnostic: a red test naming a net, for a cause that is a package
boundary. The tree already owns the right instrument — `test/jls/ArchitectureRulesTest.java`
checks compiled bytecode (issue #155) and hosts exactly this species of rule for the collab
layers. Express "no connected-component walk outside `jls.netlist`" and "no cascade synthesis
in the emitter packages" as bytecode rules; then AC-3's well-definedness worry evaporates
instead of being policed by a corpus.

## Reframing 4 — generate the narrowed claim, do not write it

TASK-C523-2 (#630) spends criteria 3, 4 and 5 policing prose: a doc sentence stronger than the
check, a fixture class added with no matching line, a claim scattered across the repo. Every
one of those is a symptom of the claim being *authored* rather than *derived*. Emit the parity
statement from the harness: the run that produces the green/red matrix writes both the netlist
header line and the documentation fragment naming the covered fixture classes, and a golden
pins the pair. Then a fixture class with no evidence cannot be described as having parity,
because nothing writes that sentence. This is the `CliFlagTableTest` / SBOM / `.buildinfo`
pattern the project already lives by, and it dissolves three of the five acceptance criteria
rather than testing them.

## What I would keep verbatim

AC-2's "artifact against source, never walk against walk", and #627's AC-4 — the deliberately
corrupted emission that must turn the test red naming the net. Assert-the-assertion is the
only reason a check like this stays meaningful five years out, and it matches
`test/jls/ui/package-info.java`'s existing discipline. Keep both, in whatever home the check
ends up.

## Sequencing note, offered as a caution rather than an objection

Nothing this issue verifies exists: no `src/jls/netlist`, no `pkg`/`pcb` packages, and
`footprint|refdes|pinout` still returns nothing across `src/`. A verification row specified in
this much detail ahead of ~15–20 mw of unbuilt substrate will drift against whatever #366's
emitter actually becomes. The generalized harness at #336 does not have that problem — it is
buildable the day the partition package exists, with the existing Verilog/VHDL/PCF emitters as
its first three subjects, and KiCad simply joins the table later. That ordering also gives
#366's TASK-0089 a check to be written *against*, instead of one written *about* it afterward.

## Disregarded

I am not treating AC-1's requirement of a JLS-authored `.net` re-parser, or the single-capstone
scope, as binding. The stated acceptance criteria are a correct instinct cut along the wrong
seam: the check belongs to the partition that both sides pivot on, and its oracle should be the
tool named in the title. Endorsed in purpose; re-home it, generalize it, arm KiCad, and rename
it to what it proves.
